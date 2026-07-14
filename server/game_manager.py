import random
import string
import time
from typing import Optional

from models import GameRoom, Player, Role, RoomFunction, Room


BUILDING_LAYOUT = [
    Room("entrance", "Intrare", RoomFunction.ENTRANCE, 0, 1),
    Room("office1", "Birouri", RoomFunction.OFFICE, 1, 0),
    Room("surveillance", "Supraveghere", RoomFunction.SURVEILLANCE, 1, 1),
    Room("comms", "Monitorizare Comunicatii", RoomFunction.COMMS_MONITOR, 1, 2),
    Room("forensics", "Laborator Criminalistic", RoomFunction.FORENSICS_LAB, 2, 0),
    Room("armory", "Armurerie", RoomFunction.ARMORY, 2, 1),
    Room("server_room", "Camera Servere", RoomFunction.SERVER_ROOM, 2, 2),
    Room("break_room", "Camera Pauza", RoomFunction.BREAK_ROOM, 3, 0),
    Room("office2", "Birouri 2", RoomFunction.OFFICE, 3, 2),
]

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

    def create_room(self, host_player_id: str, host_name: str) -> GameRoom:
        code = self._generate_room_code()
        room = GameRoom(room_code=code)
        host = Player(id=host_player_id, name=host_name, current_room_id="entrance")
        room.players[host_player_id] = host
        self.rooms[code] = room
        return room

    def join_room(self, room_code: str, player_id: str, player_name: str) -> tuple[Optional[GameRoom], Optional[str]]:
        room = self.rooms.get(room_code)
        if room is None:
            return None, "Camera nu exista"
        if room.phase.value != "LOBBY":
            return None, "Jocul a inceput deja"
        if len(room.players) >= MAX_PLAYERS:
            return None, "Camera este plina"

        player = Player(id=player_id, name=player_name, current_room_id="entrance")
        room.players[player_id] = player
        return room, None

    def remove_player(self, room_code: str, player_id: str):
        room = self.rooms.get(room_code)
        if room is None:
            return
        if player_id in room.players:
            room.players[player_id].connected = False

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
        return None

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


# instanta globala, folosita de main.py
game_manager = GameManager()