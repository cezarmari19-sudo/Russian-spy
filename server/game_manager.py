import random
import string
import time
from typing import Optional

from models import (
    GameRoom, GamePhase, Player, Role, RoomFunction, Room,
    SpyTaskType, SpyTaskInstance, SPY_TASK_ALLOWED_FUNCTIONS, Corpse,
)


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

    def create_room(self, host_player_id: str, host_name: str, account_id: Optional[str] = None) -> GameRoom:
        code = self._generate_room_code()
        room = GameRoom(room_code=code, host_id=host_player_id)
        host = Player(id=host_player_id, name=host_name, current_room_id="entrance", account_id=account_id)
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
        return None

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