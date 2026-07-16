from dataclasses import dataclass, field
from enum import Enum
from typing import Optional
import time


class Role(str, Enum):
    FBI_AGENT = "FBI_AGENT"
    RUSSIAN_SPY = "RUSSIAN_SPY"


class RoomFunction(str, Enum):
    SURVEILLANCE = "SURVEILLANCE"
    COMMS_MONITOR = "COMMS_MONITOR"
    FORENSICS_LAB = "FORENSICS_LAB"
    ARMORY = "ARMORY"
    SERVER_ROOM = "SERVER_ROOM"
    OFFICE = "OFFICE"
    BREAK_ROOM = "BREAK_ROOM"
    ENTRANCE = "ENTRANCE"


class GamePhase(str, Enum):
    LOBBY = "LOBBY"
    IN_PROGRESS = "IN_PROGRESS"
    SPY_WON = "SPY_WON"
    FBI_WON = "FBI_WON"


@dataclass
class Player:
    id: str
    name: str
    role: Role = Role.FBI_AGENT
    is_alive: bool = True
    current_room_id: str = "entrance"
    is_wearing_gloves: bool = False
    connected: bool = True

    def to_dict(self, reveal_role: bool = False):
        return {
            "id": self.id,
            "name": self.name,
            "role": self.role.value if reveal_role else None,
            "isAlive": self.is_alive,
            "currentRoomId": self.current_room_id,
            "connected": self.connected,
        }


@dataclass
class Room:
    id: str
    name: str
    function: RoomFunction
    x: float
    y: float
    width: float
    height: float
    connected_room_ids: list[str] = field(default_factory=list)

    def contains_point(self, px: float, py: float) -> bool:
        return self.x <= px <= self.x + self.width and self.y <= py <= self.y + self.height

    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "function": self.function.value,
            "x": self.x,
            "y": self.y,
            "width": self.width,
            "height": self.height,
        }


@dataclass
class DnaSample:
    id: str
    room_id: str
    actual_owner_id: str
    displayed_owner_id: str
    completeness: int
    is_analyzed: bool = False
    was_tampered_with: bool = False

    def is_reliable(self) -> bool:
        return self.completeness >= 70

    def to_dict(self):
        return {
            "id": self.id,
            "roomId": self.room_id,
            "displayedOwnerId": self.displayed_owner_id,
            "completeness": self.completeness,
            "isAnalyzed": self.is_analyzed,
        }


@dataclass
class IntelMessage:
    id: str
    sender_id: str
    sent_at_millis: int
    is_delivered: bool = False
    was_intercepted_by_death: bool = False
    requires_delay: bool = True


@dataclass
class GameRoom:
    """Reprezinta o camera/lobby de joc (partida), nu o camera fizica din cladire."""
    room_code: str
    host_id: str = ""
    phase: GamePhase = GamePhase.LOBBY
    players: dict[str, Player] = field(default_factory=dict)
    dna_samples: dict[str, DnaSample] = field(default_factory=dict)
    intel_messages: list[IntelMessage] = field(default_factory=list)
    bomb_planted: bool = False
    bomb_armed_at_millis: int = 0
    created_at: float = field(default_factory=time.time)
    # Cele 4 camere de supraveghere ale RUNDEI curente: fiecare e un dict
    # {"roomId": str, "x": float, "y": float} - generate random la start_game(),
    # aceleasi pentru toti jucatorii din runda respectiva.
    surveillance_cameras: list[dict] = field(default_factory=list)

    def alive_fbi_agents(self) -> list[Player]:
        return [p for p in self.players.values() if p.is_alive and p.role == Role.FBI_AGENT]

    def spy(self) -> Optional[Player]:
        for p in self.players.values():
            if p.role == Role.RUSSIAN_SPY:
                return p
        return None

    def public_state_dict(self, requesting_player_id: str):
        requester = self.players.get(requesting_player_id)
        reveal = requester is not None
        return {
            "roomCode": self.room_code,
            "phase": self.phase.value,
            "players": [p.to_dict(reveal_role=(p.id == requesting_player_id)) for p in self.players.values()],
            "bombPlanted": self.bomb_planted,
        }