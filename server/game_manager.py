import random
import string
import time
from typing import Optional

from models import GameRoom, GamePhase, Player, Role, RoomFunction, Room


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
        return None

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