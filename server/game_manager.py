import random
import string
import time
from typing import Optional

from models import (
    GameRoom, GamePhase, Player, Role, RoomFunction, Room,
    SpyTaskType, SpyTaskInstance, SPY_TASK_ALLOWED_FUNCTIONS, Corpse, Meeting,
    DnaSample, DnaComparisonResult,
)

# Paleta de culori stil Among Us - asignate jucatorilor in ordinea intrarii in
# camera. Cu MAX_PLAYERS=15 avem nevoie de macar 15 culori distincte.
PLAYER_COLORS = [
    "#C51111", "#132ED1", "#117F2D", "#ED54BA", "#EF7D0D",
    "#F5F557", "#3F474E", "#D6E0F0", "#6B2FBB", "#71491E",
    "#38FEDC", "#50EF39", "#83A9EF", "#E7A9F0", "#5A445A",
]


# ATENTIE: aceste coordonate (x, y, width, height) TREBUIE sa ramana identice cu
# cele din app/src/main/java/com/astran/russianspy/data/BuildingLayout.kt de pe
# Android. Daca modifici harta intr-o parte, modific-o si in cealalta, altfel
# camerele de supraveghere generate de server vor cadea in locuri gresite pe
# harta clientului.
BUILDING_LAYOUT = [
    Room("hub_central", "Hol Central", RoomFunction.OFFICE, 2000, 2000, 500, 400),

    Room("entrance", "Intrare", RoomFunction.ENTRANCE, 2150, 2850, 200, 200),
    Room("hall_entrance", "", RoomFunction.OFFICE, 2200, 2400, 100, 450),

    Room("meeting_room", "Sala de Intalniri", RoomFunction.OFFICE, 2100, 1350, 300, 300),
    Room("hall_meeting", "", RoomFunction.OFFICE, 2200, 1650, 100, 350),

    Room("office1", "Birouri", RoomFunction.OFFICE, 1250, 2050, 350, 250),
    Room("hall_office1", "", RoomFunction.OFFICE, 1600, 2125, 400, 100),

    Room("office2", "Birouri 2", RoomFunction.OFFICE, 650, 2050, 300, 250),
    Room("hall_office2", "", RoomFunction.OFFICE, 950, 2125, 300, 100),

    Room("surveillance", "Supraveghere", RoomFunction.SURVEILLANCE, 400, 1100, 300, 250),
    Room("hall_surv", "", RoomFunction.OFFICE, 625, 1350, 100, 700),

    Room("forensics", "Laborator Criminalistic", RoomFunction.FORENSICS_LAB, 400, 500, 350, 250),
    Room("hall_forensics", "", RoomFunction.OFFICE, 500, 750, 100, 350),
    Room("hall_lab_armory", "", RoomFunction.OFFICE, 750, 550, 2150, 150),
    Room("hall_meeting_lab", "", RoomFunction.OFFICE, 2200, 700, 100, 650),

    # Morga: unde sunt mutate automat corpurile dupa ce sunt raportate. Legata
    # de laborator printr-un hol vertical scurt (deasupra laboratorului).
    Room("morgue", "Morga", RoomFunction.MORGUE, 400, 150, 350, 250),
    Room("hall_morgue", "", RoomFunction.OFFICE, 525, 400, 100, 100),

    # Arhiva de ADN: contine mostrele de referinta ale tuturor jucatorilor,
    # generate automat la inceputul rundei. Langa morga, cu hol propriu catre
    # laborator, ca sa poata circula liber intre cele 3 camere.
    Room("dna_archive", "Arhiva ADN", RoomFunction.DNA_ARCHIVE, 50, 150, 300, 250),
    Room("hall_dna_archive", "", RoomFunction.OFFICE, 350, 200, 50, 150),

    Room("server_room", "Camera Servere", RoomFunction.SERVER_ROOM, 2900, 1100, 300, 250),
    Room("hall_server", "", RoomFunction.OFFICE, 3000, 1350, 100, 700),

    Room("armory", "Armurerie", RoomFunction.ARMORY, 2900, 500, 300, 250),
    Room("hall_armory", "", RoomFunction.OFFICE, 3000, 750, 100, 350),

    Room("break_room", "Camera Pauza", RoomFunction.BREAK_ROOM, 2900, 2050, 350, 250),
    Room("hall_break", "", RoomFunction.OFFICE, 2500, 2125, 400, 100),

    Room("comms", "Monitorizare Comunicatii", RoomFunction.COMMS_MONITOR, 3400, 2050, 300, 250),
    Room("hall_comms", "", RoomFunction.OFFICE, 3250, 2125, 150, 100),
]

# Id-urile camerelor care sunt de fapt holuri (nu au task, nu au sens ca punct de
# supraveghere). Sunt marcate mai sus temporar ca OFFICE (in lipsa unui enum HALLWAY
# in models.py) - le excludem explicit aici dupa id.
_HALLWAY_IDS = {
    "hall_entrance", "hall_meeting", "hall_office1", "hall_office2", "hall_surv",
    "hall_forensics", "hall_lab_armory", "hall_meeting_lab", "hall_server",
    "hall_armory", "hall_break", "hall_comms", "hub_central",
    "hall_morgue", "hall_dna_archive",
}

MIN_PLAYERS = 1
MAX_PLAYERS = 15


class GameManager:
    def __init__(self):
        self.rooms: dict[str, GameRoom] = {}

    def _generate_room_code(self) -> str:
        chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        while True:
            code = "".join(random.choices(chars, k=5))
            if code not in self.rooms:
                return code

    def _assign_color(self, room: GameRoom) -> str:
        """Alege prima culoare din paleta nefolosita inca de niciun jucator
        conectat din camera. Daca s-au epuizat (nu ar trebui, MAX_PLAYERS <=
        len(PLAYER_COLORS)), repeta paleta ca fallback sigur."""
        used = {p.color for p in room.players.values()}
        for color in PLAYER_COLORS:
            if color not in used:
                return color
        return random.choice(PLAYER_COLORS)

    def create_room(self, host_player_id: str, host_name: str, account_id: Optional[str] = None) -> GameRoom:
        code = self._generate_room_code()
        room = GameRoom(room_code=code, host_id=host_player_id)
        host = Player(id=host_player_id, name=host_name, current_room_id="entrance", account_id=account_id)
        host.color = self._assign_color(room)
        room.players[host_player_id] = host
        self.rooms[code] = room
        return room

    def delete_room(self, room_code: str, requesting_player_id: str) -> Optional[str]:
        """Sterge camera, dar doar daca cere admin-ul (host-ul) si doar cat timp
        suntem inca in LOBBY. In timpul meciului (IN_PROGRESS) stergerea manuala
        nu e permisa - stergerea in timpul jocului se face doar automat, cand
        nu mai ramane niciun jucator conectat."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        if requesting_player_id != room.host_id:
            return "Doar adminul poate sterge camera"
        if room.phase.value != "LOBBY":
            return "Camera nu poate fi stearsa in timpul meciului"
        del self.rooms[room_code]
        return None

    def has_connected_players(self, room_code: str) -> bool:
        room = self.rooms.get(room_code)
        if room is None:
            return False
        return any(p.connected for p in room.players.values())

    def join_room(
        self, room_code: str, player_id: str, player_name: str, account_id: Optional[str] = None
    ) -> tuple[Optional[GameRoom], Optional[str]]:
        room = self.rooms.get(room_code)
        if room is None:
            return None, "Camera nu exista"
        if room.phase.value != "LOBBY":
            return None, "Jocul a inceput deja"
        if len(room.players) >= MAX_PLAYERS:
            return None, "Camera este plina"
        if account_id and account_id in room.banned_account_ids:
            return None, "Ai fost banat din aceasta camera"

        player = Player(id=player_id, name=player_name, current_room_id="entrance", account_id=account_id)
        player.color = self._assign_color(room)
        room.players[player_id] = player
        return room, None

    def remove_player(self, room_code: str, player_id: str):
        room = self.rooms.get(room_code)
        if room is None:
            return
        if player_id in room.players:
            room.players[player_id].connected = False
        self._migrate_host_if_needed(room)

    def _migrate_host_if_needed(self, room: GameRoom):
        """Daca host-ul curent nu mai e conectat SI suntem inca in LOBBY, promoveaza
        automat un alt jucator conectat la host (stil Among Us). In timpul
        meciului (IN_PROGRESS) nu schimbam host-ul - nu are sens sa transferi
        controale de lobby (start/kick/ban) dupa ce jocul a inceput deja."""
        if room.phase.value != "LOBBY":
            return
        current_host = room.players.get(room.host_id)
        if current_host is not None and current_host.connected:
            return  # host-ul curent e inca aici, nimic de facut

        for pid, player in room.players.items():
            if player.connected:
                room.host_id = pid
                return
        # daca nu mai e nimeni conectat, host_id ramane cum era (camera oricum
        # va fi stearsa automat de main.py cand ultimul WS se deconecteaza)

    def kick_player(self, room_code: str, requesting_player_id: str, target_player_id: str) -> Optional[str]:
        """Scoate un jucator din camera FARA sa il banze - poate reintra oricand
        cu acelasi cod. Doar host-ul poate da kick, doar in LOBBY, si nu isi
        poate da kick lui insusi."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        if room.phase.value != "LOBBY":
            return "Nu poti da kick in timpul meciului"
        if requesting_player_id != room.host_id:
            return "Doar hostul poate da kick"
        if target_player_id == requesting_player_id:
            return "Nu iti poti da kick tie insuti"
        if target_player_id not in room.players:
            return "Jucator invalid"

        del room.players[target_player_id]
        return None

    def ban_player(self, room_code: str, requesting_player_id: str, target_player_id: str) -> Optional[str]:
        """La fel ca kick, dar retine accountId-ul jucatorului in banned_account_ids -
        nu se va mai putea alatura acestei camere cat timp exista ea, chiar daca
        reintra cu alt playerId. Daca jucatorul nu are accountId (nu a deschis
        niciodata ecranul de Prieteni si PlayerPrefs nu l-a generat inca - caz
        rar), banul se comporta ca un simplu kick, fara persistenta."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        if room.phase.value != "LOBBY":
            return "Nu poti da ban in timpul meciului"
        if requesting_player_id != room.host_id:
            return "Doar hostul poate da ban"
        if target_player_id == requesting_player_id:
            return "Nu iti poti da ban tie insuti"

        target = room.players.get(target_player_id)
        if target is None:
            return "Jucator invalid"

        if target.account_id:
            room.banned_account_ids.add(target.account_id)
        del room.players[target_player_id]
        return None

    def start_game(self, room_code: str) -> Optional[str]:
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        if len(room.players) < MIN_PLAYERS:
            return f"Aveti nevoie de minim {MIN_PLAYERS} jucatori"

        player_ids = list(room.players.keys())
        spy_id = random.choice(player_ids)

        for pid, player in room.players.items():
            player.role = Role.RUSSIAN_SPY if pid == spy_id else Role.FBI_AGENT
            player.current_room_id = "entrance"

        room.phase = room.phase.__class__.IN_PROGRESS
        room.surveillance_cameras = self._generate_random_camera_spots()
        room.spy_tasks = self._generate_spy_tasks(room)
        self._generate_dna_archive(room)
        room.corpses = {}
        room.lab_machine_harvested_sample_id = None
        room.lab_machine_reference_sample_id = None
        return None

    def _generate_dna_archive(self, room: GameRoom):
        """Populeaza automat camera Arhiva ADN cu o mostra de referinta per
        jucator din partida (inclusiv spionul si agentii FBI) - fara nicio
        actiune din partea jucatorilor. Fiecare mostra e la 100% completeness
        si NU poate fi niciodata stricata (arhiva e sterila). Clientii vad
        doar culoarea jucatorului asociat mostrei, niciodata identitatea/rolul -
        asta se afla doar in urma unei comparari la masina din laborator."""
        room.dna_samples = {
            sample_id: sample
            for sample_id, sample in room.dna_samples.items()
            if not sample.is_reference
        }
        for pid, player in room.players.items():
            sample_id = f"dna_ref_{pid}"
            room.dna_samples[sample_id] = DnaSample(
                id=sample_id,
                room_id="dna_archive",
                actual_owner_id=pid,
                displayed_owner_id=pid,
                completeness=100,
                is_analyzed=False,
                was_tampered_with=False,
                is_reference=True,
                player_color=player.color,
            )

    def set_spy_task_count(self, room_code: str, requesting_player_id: str, count: int) -> Optional[str]:
        """Doar host-ul poate seta cate task-uri primeste spionul (2-12), si doar
        inainte ca jocul sa inceapa (in LOBBY) - schimbarea in timpul meciului
        nu are sens, task-urile sunt deja alocate."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        if requesting_player_id != room.host_id:
            return "Doar hostul poate schimba aceasta setare"
        if room.phase != GamePhase.LOBBY:
            return "Nu poti schimba numarul de task-uri in timpul meciului"
        if count < 2 or count > 12:
            return "Numarul de task-uri trebuie sa fie intre 2 si 12"
        room.spy_task_count = count
        return None

    def _generate_spy_tasks(self, room: GameRoom) -> list[SpyTaskInstance]:
        """Alege random `room.spy_task_count` task-uri din catalogul complet
        SpyTaskType, fiecare cu o camera valida random (pentru HACK_SURVEILLANCE_CAMERA,
        doar camerele care au chiar o camera de supraveghere activa in aceasta
        runda) SI un punct exact x/y in interiorul acelei camere - la fel ca la
        camerele de supraveghere, jucatorul trebuie sa fie fizic langa acel
        punct ca sa poata interactiona. Daca doua task-uri ajung in aceeasi
        camera, punctele lor sunt distantate (impartim camera in sub-zone),
        ca sa nu se suprapuna vizual/functional. Daca un tip de task nu are
        nicio camera valida disponibila (caz rar), e sarit si se alege alt tip.
        """
        all_task_types = list(SpyTaskType)
        random.shuffle(all_task_types)

        surveillance_room_ids = [cam["roomId"] for cam in room.surveillance_cameras]
        rooms_by_id = {r.id: r for r in BUILDING_LAYOUT}

        # Cate task-uri au fost deja plasate in fiecare camera, ca sa distantam
        # punctele urmatoarelor task-uri din aceeasi camera (nu suprapunem).
        tasks_per_room: dict[str, int] = {}

        tasks: list[SpyTaskInstance] = []
        type_index = 0
        attempts = 0
        # attempts previne o bucla infinita daca niciun tip nu mai are camere valide
        while len(tasks) < room.spy_task_count and attempts < len(all_task_types) * 3:
            task_type = all_task_types[type_index % len(all_task_types)]
            type_index += 1
            attempts += 1

            if task_type == SpyTaskType.HACK_SURVEILLANCE_CAMERA:
                valid_room_ids = surveillance_room_ids
            else:
                allowed_functions = SPY_TASK_ALLOWED_FUNCTIONS.get(task_type.value, [])
                valid_room_ids = [r.id for r in BUILDING_LAYOUT if r.function in allowed_functions]

            if not valid_room_ids:
                continue

            chosen_room_id = random.choice(valid_room_ids)
            chosen_room = rooms_by_id[chosen_room_id]

            # Distantam punctul in functie de cate task-uri sunt deja in camera
            # asta (impartim latimea camerei in benzi verticale) - simplu, dar
            # suficient ca sa nu apara doua task-uri exact pe aceeasi pozitie.
            existing_count = tasks_per_room.get(chosen_room_id, 0)
            band_count = existing_count + 1
            margin_x = chosen_room.width * 0.15
            margin_y = chosen_room.height * 0.15
            usable_width = chosen_room.width - margin_x * 2
            band_width = usable_width / max(band_count, 1)
            band_start = chosen_room.x + margin_x + band_width * existing_count
            spot_x = band_start + random.random() * band_width
            spot_y = chosen_room.y + margin_y + random.random() * (chosen_room.height - margin_y * 2)

            tasks_per_room[chosen_room_id] = existing_count + 1

            tasks.append(SpyTaskInstance(
                id=f"task_{len(tasks)}_{random.randint(1000, 9999)}",
                task_type=task_type.value,
                room_id=chosen_room_id,
                x=spot_x,
                y=spot_y,
            ))

        return tasks

    def complete_spy_task(self, room_code: str, player_id: str, task_id: str) -> Optional[str]:
        """Marcheaza un task de spion ca finalizat. Doar spionul insusi poate
        completa propriile task-uri (verificat prin rolul jucatorului, nu doar
        prin id - un agent FBI care ar trimite acest mesaj manual e respins)."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        player = room.players.get(player_id)
        if player is None or player.role != Role.RUSSIAN_SPY:
            return "Doar spionul poate completa acest task"

        task = next((t for t in room.spy_tasks if t.id == task_id), None)
        if task is None:
            return "Task invalid"
        task.is_completed = True
        return None

    def disable_spy_device(self, room_code: str, player_id: str, task_id: str) -> Optional[str]:
        """Un agent FBI a gasit un dispozitiv plasat de spion (ascultare sau
        camera hacked) si il dezactiveaza - reseteaza is_completed la False,
        spionul trebuie sa il refaca ca sa mai conteze pentru victorie. Doar
        task-urile de tip PLANT_LISTENING_DEVICE / HACK_SURVEILLANCE_CAMERA pot
        fi dezactivate (celelalte, ca fotografiatul, nu lasa in urma un obiect
        fizic de gasit)."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        player = room.players.get(player_id)
        if player is None or player.role != Role.FBI_AGENT:
            return "Doar un agent FBI poate dezactiva un dispozitiv"

        task = next((t for t in room.spy_tasks if t.id == task_id), None)
        if task is None:
            return "Dispozitiv invalid"
        if task.task_type not in (
            SpyTaskType.PLANT_LISTENING_DEVICE.value,
            SpyTaskType.HACK_SURVEILLANCE_CAMERA.value,
        ):
            return "Acest tip de task nu poate fi dezactivat"
        if not task.is_completed:
            return "Dispozitivul nu a fost inca plasat"

        task.is_completed = False
        return None

    def kill_player(
        self, room_code: str, killer_id: str, victim_id: str,
        victim_x: float, victim_y: float
    ) -> tuple[Optional[str], Optional[Corpse]]:
        """Spionul omoara un agent FBI. Returneaza (eroare, None) daca esueaza,
        sau (None, corpse) daca reuseste. Conditii, la fel ca la Among Us:
        - cel care ucide trebuie sa fie spionul viu
        - victima trebuie sa fie un agent FBI viu
        - amandoi trebuie sa fie in ACEEASI camera fizica
        - NU trebuie sa existe niciun alt jucator viu si conectat in acea
          camera (fara martori) - altfel omorul e blocat de "prea multa lume"
        - trebuie sa fi trecut kill_cooldown_seconds de la ultimul omor reusit
          din aceasta runda (evita spam-ul de omoruri)
        Manusile (is_wearing_gloves) NU blocheaza omorul - doar reduc drastic
        cantitatea de ADN lasata pe corp (vezi mai jos).
        victim_x/victim_y vin din last_positions (main.py) - pozitia jucatorilor
        nu e tinuta pe modelul Player, ci separat la nivel de conexiune."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista", None
        if room.phase != GamePhase.IN_PROGRESS:
            return "Jocul nu e in desfasurare", None

        killer = room.players.get(killer_id)
        if killer is None or killer.role != Role.RUSSIAN_SPY or not killer.is_alive:
            return "Doar spionul viu poate omori", None

        victim = room.players.get(victim_id)
        if victim is None or victim.role != Role.FBI_AGENT or not victim.is_alive:
            return "Tinta trebuie sa fie un agent FBI viu", None

        if victim.current_room_id != killer.current_room_id:
            return "Tinta nu e in aceeasi camera", None

        witnesses = [
            p for p in room.players.values()
            if p.id != killer_id and p.id != victim_id
            and p.is_alive and p.connected
            and p.current_room_id == killer.current_room_id
        ]
        if witnesses:
            return "Nu poti ucide - exista martori in camera", None

        now_millis = time.time() * 1000
        elapsed_seconds = (now_millis - room.last_kill_at_millis) / 1000.0
        if room.last_kill_at_millis > 0 and elapsed_seconds < room.kill_cooldown_seconds:
            remaining = room.kill_cooldown_seconds - elapsed_seconds
            return f"Asteapta {remaining:.0f}s pana la urmatorul omor", None

        # ADN ramas pe corp: 70-100% daca ucigasul nu purta manusi (identificabil
        # aproape sigur), sau doar 10% (aproape inutilizabil) daca purta manusi -
        # exact valorile cerute (70-100 fara manusi, poate scadea pana la 10 cu
        # manusi).
        if killer.is_wearing_gloves:
            dna_completeness = 10
        else:
            dna_completeness = random.randint(70, 100)

        victim.is_alive = False
        room.last_kill_at_millis = now_millis

        corpse = Corpse(
            id=f"corpse_{len(room.corpses)}_{random.randint(1000, 9999)}",
            victim_id=victim_id,
            killer_id=killer_id,
            room_id=victim.current_room_id,
            x=victim_x,
            y=victim_y,
            dna_completeness=dna_completeness,
        )
        room.corpses[corpse.id] = corpse
        return None, corpse

    def get_kill_cooldown_remaining_seconds(self, room_code: str) -> float:
        """Cate secunde mai raman pana spionul poate ucide din nou (0 daca
        poate ucide chiar acum). Folosit pentru raspunsuri catre client, nu
        pentru validare (validarea reala e in kill_player)."""
        room = self.rooms.get(room_code)
        if room is None or room.last_kill_at_millis == 0:
            return 0.0
        elapsed = (time.time() * 1000 - room.last_kill_at_millis) / 1000.0
        return max(0.0, room.kill_cooldown_seconds - elapsed)

    def report_corpse(self, room_code: str, reporter_id: str, corpse_id: str) -> tuple[Optional[str], Optional[Meeting]]:
        """Oricine (spion SAU agent FBI) aflat langa un corp nereportat il poate
        raporta - la fel ca la Among Us, raportarea nu are legatura cu rolul,
        oricine descopera corpul poate suna alarma. Marcheaza corpul ca
        raportat (dispare de pe harta pentru toti), dezvaluie identitatea
        victimei si PORNESTE un meeting: toti jucatorii vii sunt teleportati in
        meeting_room si primesc un timp fix ca sa voteze. NU dezvaluie cine e
        ucigasul - asta ramane ascuns, la fel ca inainte de raport.
        Returneaza (eroare, None) daca esueaza, sau (None, meeting) daca reuseste."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista", None
        if room.phase != GamePhase.IN_PROGRESS:
            return "Jocul nu e in desfasurare", None
        if room.active_meeting is not None:
            return "Exista deja o intalnire in desfasurare", None

        reporter = room.players.get(reporter_id)
        if reporter is None or not reporter.is_alive:
            return "Doar un jucator viu poate raporta", None

        corpse = room.corpses.get(corpse_id)
        if corpse is None:
            return "Corp invalid", None
        if corpse.reported:
            return "Corpul a fost deja raportat", None

        corpse.reported = True
        corpse.reported_by = reporter_id

        # Corpul e mutat automat in Morga - ramane acolo (vizibil pentru toti)
        # pana i se extrage ADN-ul. Pozitia exacta in morga e random in
        # interiorul camerei, ca sa nu se suprapuna cadavrele daca sunt mai
        # multe simultan.
        morgue_room = next(r for r in BUILDING_LAYOUT if r.id == "morgue")
        corpse.in_morgue = True
        corpse.room_id = "morgue"
        corpse.x = morgue_room.x + random.uniform(40, morgue_room.width - 40)
        corpse.y = morgue_room.y + random.uniform(40, morgue_room.height - 40)

        # Toti jucatorii VII sunt adusi in sala de intalniri - cei deja morti
        # (corpuri neraportate inca, daca sunt mai multe) raman unde sunt.
        for player in room.players.values():
            if player.is_alive:
                player.current_room_id = "meeting_room"

        meeting = Meeting(
            started_at_millis=time.time() * 1000,
            reporter_id=reporter_id,
            reporter_name=reporter.name,
        )
        room.active_meeting = meeting
        return None, meeting

    def tamper_corpse_dna(self, room_code: str, requesting_player_id: str, corpse_id: str) -> Optional[str]:
        """Doar spionul poate face asta, si doar cat timp corpul e in Morga si
        ADN-ul lui inca NU a fost extras (odata extras, mostra e deja o
        DnaSample separata si stricarea corpului nu mai are efect asupra ei -
        vezi extract_corpse_dna). Reduce dna_completeness la o valoare random
        intre 0 si 30 ("poate fi de la 0 la suta distrus pana la 30 la suta
        distrus random"), simuland faptul ca spionul a alterat proba fizic
        inainte sa apuce cineva sa o recolteze. Nu are cooldown si nu necesita
        ca spionul sa fie singur in camera - e o actiune discreta, rapida."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        if room.phase != GamePhase.IN_PROGRESS:
            return "Jocul nu e in desfasurare"

        spy = room.players.get(requesting_player_id)
        if spy is None or spy.role != Role.RUSSIAN_SPY or not spy.is_alive:
            return "Doar spionul viu poate strica probele"

        corpse = room.corpses.get(corpse_id)
        if corpse is None:
            return "Corp invalid"
        if not corpse.in_morgue:
            return "Corpul nu e in morga"
        if corpse.dna_extracted:
            return "ADN-ul a fost deja extras - nu mai poate fi alterat"

        corpse.dna_completeness = random.randint(0, 30)
        return None

    def extract_corpse_dna(
        self, room_code: str, requesting_player_id: str, corpse_id: str
    ) -> tuple[Optional[str], Optional[DnaSample]]:
        """Oricine (spion SAU agent FBI) aflat in Morga langa un corp raportat
        poate extrage ADN-ul lui, o singura data - genereaza o DnaSample
        RECOLTATA (is_reference=False), legata de corpse_id, cu exact
        completeness-ul curent al corpului (deja stabilit la kill_player, sau
        redus intre timp de spion prin tamper_corpse_dna). Dupa extractie,
        completeness-ul mostrei e fixat definitiv - stricarea corpului dupa
        acest punct nu mai are niciun efect (ceea ce respecta cerinta ca
        stricarea sa fie posibila DOAR pe corp, in morga, inainte de extractie)."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista", None
        if room.phase != GamePhase.IN_PROGRESS:
            return "Jocul nu e in desfasurare", None

        player = room.players.get(requesting_player_id)
        if player is None or not player.is_alive:
            return "Doar un jucator viu poate extrage ADN", None

        corpse = room.corpses.get(corpse_id)
        if corpse is None:
            return "Corp invalid", None
        if not corpse.in_morgue:
            return "Corpul nu e in morga", None
        if corpse.dna_extracted:
            return "ADN-ul a fost deja extras de pe acest corp", None

        sample_id = f"dna_harvest_{corpse_id}"
        sample = DnaSample(
            id=sample_id,
            room_id="morgue",
            actual_owner_id=corpse.victim_id,
            displayed_owner_id=corpse.victim_id,
            completeness=corpse.dna_completeness,
            is_reference=False,
            source_corpse_id=corpse_id,
        )
        room.dna_samples[sample_id] = sample
        corpse.dna_extracted = True
        corpse.dna_recovered = True
        corpse.extracted_sample_id = sample_id
        return None, sample

    def move_dna_sample_to_lab(
        self, room_code: str, requesting_player_id: str, sample_id: str
    ) -> tuple[Optional[str], Optional[DnaSample]]:
        """Muta o mostra RECOLTATA (din morga) in Laboratorul Criminalistic -
        aceasta chiar se muta (nu exista decat o singura copie, legata de un
        corp anume). Pentru o mostra de REFERINTA (din arhiva), NU se muta
        originalul (care ar disparea din arhiva pentru toti ceilalti) - in
        schimb se creeaza o COPIE noua, independenta, cu propriul id, trimisa
        direct in laborator; originalul ramane intact in arhiva, disponibil
        sa fie trimis din nou oricand, de oricine. Copia trimisa la laborator
        e stearsa automat dupa ce e folosita intr-o comparare (vezi
        compare_dna_samples), sau ramane in laborator pana atunci daca nu mai
        e folosita.
        Returneaza (eroare, None) daca esueaza, sau (None, mostra_trimisa) daca
        reuseste - main.py foloseste mostra intoarsa pentru broadcast (poate fi
        o copie noua, cu alt id decat sample_id primit, pentru referinte)."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista", None
        if room.phase != GamePhase.IN_PROGRESS:
            return "Jocul nu e in desfasurare", None

        player = room.players.get(requesting_player_id)
        if player is None or not player.is_alive:
            return "Doar un jucator viu poate transporta probe", None

        sample = room.dna_samples.get(sample_id)
        if sample is None:
            return "Mostra nu exista", None

        if sample.is_reference:
            # Referinta: se creeaza mereu o copie noua, trimisa direct la
            # laborator - originalul NU se muta niciodata (ramane disponibil
            # in arhiva pentru oricine, oricand).
            copy_id = f"{sample.id}_copy_{random.randint(100000, 999999)}"
            copy_sample = DnaSample(
                id=copy_id,
                room_id="forensics",
                actual_owner_id=sample.actual_owner_id,
                displayed_owner_id=sample.displayed_owner_id,
                completeness=sample.completeness,
                is_reference=True,
                player_color=sample.player_color,
            )
            room.dna_samples[copy_id] = copy_sample
            return None, copy_sample
        else:
            # Recoltata: se muta efectiv (exista o singura mostra per corp).
            if sample.room_id != "morgue":
                return "Mostra nu mai poate fi transportata de aici", None
            sample.room_id = "forensics"
            return None, sample

    def place_sample_in_lab_machine(
        self, room_code: str, requesting_player_id: str, sample_id: str
    ) -> Optional[str]:
        """Pune o mostra (adusa deja in laborator) intr-unul din cele doua
        sloturi ale masinii de comparare: slotul de mostre RECOLTATE (de pe un
        corp) daca sample.is_reference e False, sau slotul de REFERINTA daca e
        True. Jucatorul trebuie sa fie fizic in laborator."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        if room.phase != GamePhase.IN_PROGRESS:
            return "Jocul nu e in desfasurare"

        player = room.players.get(requesting_player_id)
        if player is None or not player.is_alive:
            return "Doar un jucator viu poate folosi laboratorul"

        sample = room.dna_samples.get(sample_id)
        if sample is None:
            return "Mostra nu exista"
        if sample.room_id != "forensics":
            return "Mostra trebuie adusa mai intai in laborator"

        sample.placed_in_lab_slot = True
        if sample.is_reference:
            room.lab_machine_reference_sample_id = sample_id
        else:
            room.lab_machine_harvested_sample_id = sample_id
        return None

    def compare_dna_samples(
        self, room_code: str, requesting_player_id: str
    ) -> tuple[Optional[str], Optional[DnaComparisonResult]]:
        """Ruleaza compararea masinii din laborator: mostra recoltata (de pe
        un corp) vs. mostra de referinta puse in cele doua sloturi. Similarity
        e calculat plecand de la completeness-ul mostrei recoltate - cu cat e
        mai putin completa (ex. stricata de spion la 0-30%), cu atat rezultatul
        e mai putin clar/mai putin sigur, chiar daca cele doua mostre
        apartin de fapt aceluiasi jucator. Rezultatul e vizibil STRICT pentru
        cel care a facut compararea (nu e broadcastat catre restul camerei)."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista", None
        if room.phase != GamePhase.IN_PROGRESS:
            return "Jocul nu e in desfasurare", None

        player = room.players.get(requesting_player_id)
        if player is None or not player.is_alive:
            return "Doar un jucator viu poate folosi laboratorul", None

        harvested_id = room.lab_machine_harvested_sample_id
        reference_id = room.lab_machine_reference_sample_id
        if not harvested_id or not reference_id:
            return "Ambele sloturi trebuie ocupate (o mostra recoltata si una de referinta)", None

        harvested = room.dna_samples.get(harvested_id)
        reference = room.dna_samples.get(reference_id)
        if harvested is None or reference is None:
            return "Mostra invalida", None

        same_owner = harvested.actual_owner_id == reference.actual_owner_id
        completeness = harvested.completeness

        if same_owner:
            # Mostra e cu adevarat a aceleiasi persoane - similarity urmareste
            # completeness-ul (o mostra stricata da un match mai slab/mai
            # incert, desi ADN-ul e real).
            similarity = completeness
        else:
            # Nu e aceeasi persoana - similarity ramane mic indiferent de
            # completeness (putin zgomot random ca sa nu fie mereu identic).
            similarity = random.randint(0, 12)

        similarity = max(0, min(100, similarity))
        harvested.is_analyzed = True

        result = DnaComparisonResult(
            harvested_sample_id=harvested_id,
            reference_sample_id=reference_id,
            reference_player_color=reference.player_color,
            similarity_percent=similarity,
            is_match=same_owner and harvested.is_reliable(),
        )

        # Dupa folosire, mostrele din masina "dispar" - nu mai raman in
        # laborator, gata pentru o comparare urmatoare (fiecare comparare e
        # o singura folosire). Mostra RECOLTATA dispare definitiv (era
        # oricum unica, legata de un singur corp - o data comparata, nu mai
        # e nevoie de ea). Mostra de REFERINTA era deja o COPIE creata la
        # move_dna_sample_to_lab (originalul ramane intact, nealterat, in
        # arhiva - vezi move_dna_sample_to_lab), deci stergerea ei aici NU
        # afecteaza arhiva, care ramane disponibila pentru viitoare trimiteri.
        room.dna_samples.pop(harvested_id, None)
        room.dna_samples.pop(reference_id, None)
        room.lab_machine_harvested_sample_id = None
        room.lab_machine_reference_sample_id = None

        return None, result

    def cast_vote(self, room_code: str, voter_id: str, target_player_id: Optional[str]) -> Optional[str]:
        """Inregistreaza votul unui jucator viu in intalnirea activa curenta.
        target_player_id = None inseamna vot explicit de "skip" (abtinere) -
        diferit de "nu a votat inca" (voter_id absent din dict). Un jucator
        poate vota o singura data; un vot ulterior il suprascrie pe primul
        (permite jucatorului sa se razgandeasca cat timp meeting-ul e activ)."""
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        meeting = room.active_meeting
        if meeting is None or meeting.resolved:
            return "Nu exista o intalnire activa"

        voter = room.players.get(voter_id)
        if voter is None or not voter.is_alive:
            return "Doar un jucator viu poate vota"

        if target_player_id is not None:
            target = room.players.get(target_player_id)
            if target is None or not target.is_alive:
                return "Tinta votului e invalida"

        meeting.votes[voter_id] = target_player_id
        return None

    def all_alive_players_voted(self, room_code: str) -> bool:
        """True daca FIECARE jucator viu conectat a votat deja (inclusiv cu
        skip) in intalnirea activa curenta - folosit ca sa rezolvam votul
        INSTANT quando ultimul jucator viu voteaza, fara sa mai asteptam
        expirarea cronometrului. Daca nu exista niciun meeting activ, returneaza
        False (nimic de rezolvat)."""
        room = self.rooms.get(room_code)
        if room is None or room.active_meeting is None:
            return False
        meeting = room.active_meeting
        if meeting.resolved:
            return False

        alive_player_ids = {
            p.id for p in room.players.values()
            if p.is_alive and p.connected
        }
        if not alive_player_ids:
            return False
        return alive_player_ids.issubset(meeting.votes.keys())

    def get_meeting_remaining_seconds(self, room_code: str) -> float:
        room = self.rooms.get(room_code)
        if room is None or room.active_meeting is None:
            return 0.0
        meeting = room.active_meeting
        elapsed = (time.time() * 1000 - meeting.started_at_millis) / 1000.0
        return max(0.0, meeting.duration_seconds - elapsed)

    def is_meeting_expired(self, room_code: str) -> bool:
        return self.get_meeting_remaining_seconds(room_code) <= 0.0

    def resolve_meeting(self, room_code: str) -> Optional[dict]:
        """Inchide intalnirea activa si calculeaza rezultatul: jucatorul cu cele
        mai multe voturi e exclus (is_alive = False). Majoritate SIMPLA - la
        egalitate (inclusiv egalitate cu numarul de skip-uri), NIMENI nu e
        exclus. Returneaza un rezumat pentru broadcast, sau None daca nu exista
        nicio intalnire activa de rezolvat."""
        room = self.rooms.get(room_code)
        if room is None or room.active_meeting is None:
            return None
        meeting = room.active_meeting
        if meeting.resolved:
            return None
        meeting.resolved = True

        # Numaram voturile per tinta - "None" (skip) e o "tinta" ca oricare alta
        # pentru scopul comparatiei de majoritate.
        tally: dict[Optional[str], int] = {}
        for target in meeting.votes.values():
            tally[target] = tally.get(target, 0) + 1

        ejected_player_id: Optional[str] = None
        if tally:
            max_votes = max(tally.values())
            top_targets = [t for t, count in tally.items() if count == max_votes]
            # Exact un singur "castigator" (fara egalitate) SI acela nu e skip (None).
            if len(top_targets) == 1 and top_targets[0] is not None:
                ejected_player_id = top_targets[0]

        was_spy = False
        if ejected_player_id is not None:
            ejected_player = room.players.get(ejected_player_id)
            if ejected_player is not None:
                ejected_player.is_alive = False
                was_spy = ejected_player.role == Role.RUSSIAN_SPY
                if was_spy:
                    room.phase = GamePhase.FBI_WON

        room.active_meeting = None

        return {
            "ejectedPlayerId": ejected_player_id,
            "wasSpy": was_spy,
            "voteCounts": {(k if k is not None else "SKIP"): v for k, v in tally.items()},
        }

    def check_spy_win(self, room_code: str) -> bool:
        """True daca toate task-urile spionului sunt completate ACUM (nu au fost
        dezactivate ulterior de un agent) - victorie automata a spionului, la
        fel ca la Among Us cand crewmate-ii termina toate task-urile."""
        room = self.rooms.get(room_code)
        if room is None or not room.spy_tasks:
            return False
        return all(t.is_completed for t in room.spy_tasks)

    def _generate_random_camera_spots(self, count: int = 4) -> list[dict]:
        """Alege `count` camere random (excluzand holurile) si un punct random exact
        in interiorul fiecareia, ca pozitie fixa a camerei de supraveghere pentru
        aceasta runda. Acelasi rezultat e trimis la toti jucatorii."""
        candidate_rooms = [r for r in BUILDING_LAYOUT if r.id not in _HALLWAY_IDS]
        chosen = random.sample(candidate_rooms, k=min(count, len(candidate_rooms)))

        spots = []
        for room in chosen:
            margin_x = room.width * 0.1
            margin_y = room.height * 0.1
            spot_x = room.x + margin_x + random.random() * (room.width - margin_x * 2)
            spot_y = room.y + margin_y + random.random() * (room.height - margin_y * 2)
            spots.append({"roomId": room.id, "x": spot_x, "y": spot_y})
        return spots

    def move_player(self, room_code: str, player_id: str, target_room_id: str) -> Optional[str]:
        room = self.rooms.get(room_code)
        if room is None:
            return "Camera nu exista"
        player = room.players.get(player_id)
        if player is None:
            return "Jucator invalid"
        if not player.is_alive:
            return "Jucatorul e mort"

        valid_room_ids = {r.id for r in BUILDING_LAYOUT}
        if target_room_id not in valid_room_ids:
            return "Camera destinatie invalida"

        player.current_room_id = target_room_id
        return None

    def get_room(self, room_code: str) -> Optional[GameRoom]:
        return self.rooms.get(room_code)

    def list_public_rooms(self, max_results: int = 8) -> list[GameRoom]:
        """Lista de lobby-uri publice disponibile, stil Among Us in beta: fara
        matchmaking sofisticat, fara scor - doar camere PUBLICE, in faza LOBBY,
        cu loc liber (nu pline), alese RANDOM din toate cele disponibile. Asta
        evita problema unei sortari fixe (ex: "cele mai goale primele"), care ar
        lasa mereu camerele aproape pline neobservate si ar impiedica sa se
        completeze vreodata un lobby - la fiecare refresh manual, selectia se
        schimba, dand sansa si camerelor goale si celor aproape pline sa fie
        vazute si completate.
        """
        available = [
            room for room in self.rooms.values()
            if not room.is_private
            and room.phase == GamePhase.LOBBY
            and len(room.players) < MAX_PLAYERS
            and len(room.players) > 0
        ]
        if len(available) <= max_results:
            return available
        return random.sample(available, k=max_results)


# instanta globala, folosita de main.py
game_manager = GameManager()