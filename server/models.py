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


class SpyTaskType(str, Enum):
    """Catalogul COMPLET de task-uri posibile pentru spion. La inceputul fiecarei
    runde, serverul alege random N dintre acestea (N = setarea hostului), nu se
    folosesc toate deodata. Fiecare are o durata (RAPID/MEDIU/LUNG, ca la Among
    Us) si o lista de camere in care poate aparea."""
    PHOTOGRAPH_DOCUMENTS = "PHOTOGRAPH_DOCUMENTS"       # rapid - fotografiaza documente
    STEAL_KEYS = "STEAL_KEYS"                           # rapid - fura chei
    PLANT_LISTENING_DEVICE = "PLANT_LISTENING_DEVICE"   # mediu - plaseaza dispozitiv de ascultare
    HACK_SURVEILLANCE_CAMERA = "HACK_SURVEILLANCE_CAMERA"  # lung - instaleaza sistem de spionat pe camera
    SEND_ENCRYPTED_MESSAGE = "SEND_ENCRYPTED_MESSAGE"   # mediu - trimite mesaj cifrat
    ERASE_FORENSIC_EVIDENCE = "ERASE_FORENSIC_EVIDENCE"  # lung - sterge probe criminalistice


# Durata (in secunde) de "hold" necesara pentru fiecare tip de task - folosita de
# client ca sa stie cat sa tina apasat, si de server ca sa valideze ca nu s-a
# trisat (nu se marcheaza complet mai devreme decat durata minima).
SPY_TASK_DURATIONS_SECONDS: dict[str, float] = {
    SpyTaskType.PHOTOGRAPH_DOCUMENTS.value: 3.0,
    SpyTaskType.STEAL_KEYS.value: 2.0,
    SpyTaskType.PLANT_LISTENING_DEVICE.value: 6.0,
    SpyTaskType.HACK_SURVEILLANCE_CAMERA.value: 10.0,
    SpyTaskType.SEND_ENCRYPTED_MESSAGE.value: 5.0,
    SpyTaskType.ERASE_FORENSIC_EVIDENCE.value: 8.0,
}

# In ce camere (dupa RoomFunction) poate aparea fiecare tip de task. HACK_SURVEILLANCE_CAMERA
# e special - camerele valide pentru el se calculeaza dinamic (doar unde exista o
# camera de supraveghere activa in runda asta), nu dupa RoomFunction.
SPY_TASK_ALLOWED_FUNCTIONS: dict[str, list] = {
    SpyTaskType.PHOTOGRAPH_DOCUMENTS.value: [RoomFunction.OFFICE, RoomFunction.FORENSICS_LAB],
    SpyTaskType.STEAL_KEYS.value: [RoomFunction.OFFICE, RoomFunction.ARMORY],
    SpyTaskType.PLANT_LISTENING_DEVICE.value: [
        RoomFunction.OFFICE, RoomFunction.BREAK_ROOM, RoomFunction.ARMORY,
        RoomFunction.SERVER_ROOM, RoomFunction.COMMS_MONITOR, RoomFunction.FORENSICS_LAB,
    ],
    SpyTaskType.SEND_ENCRYPTED_MESSAGE.value: [RoomFunction.SERVER_ROOM, RoomFunction.COMMS_MONITOR],
    SpyTaskType.ERASE_FORENSIC_EVIDENCE.value: [RoomFunction.FORENSICS_LAB],
}


@dataclass
class SpyTaskInstance:
    """O instanta CONCRETA a unui task, alocata spionului pentru runda curenta:
    tipul, camera SI punctul exact x/y din interiorul ei (ca la camerele de
    supraveghere - jucatorul trebuie sa fie fizic langa acel punct, nu doar in
    aceeasi camera, ca sa poata interactiona). Asta permite si mai multe
    task-uri diferite in aceeasi camera, fiecare cu propriul punct, fara sa se
    suprapuna. Pentru PLANT_LISTENING_DEVICE si HACK_SURVEILLANCE_CAMERA,
    obiectul plasat ramane vizibil/interactiv pe harta dupa completare - un
    agent FBI il poate gasi si "dezactiva" (aceeasi mecanica de hold), ceea ce
    reseteaza is_completed la False si spionul trebuie sa il refaca."""
    id: str
    task_type: str
    room_id: str
    x: float
    y: float
    is_completed: bool = False

    def to_dict(self):
        return {
            "id": self.id,
            "taskType": self.task_type,
            "roomId": self.room_id,
            "x": self.x,
            "y": self.y,
            "isCompleted": self.is_completed,
        }


@dataclass
class Player:
    id: str
    name: str
    role: Role = Role.FBI_AGENT
    is_alive: bool = True
    current_room_id: str = "entrance"
    is_wearing_gloves: bool = False
    connected: bool = True
    # accountId-ul persistent al jucatorului (din PlayerPrefs, pe telefon), daca a
    # fost trimis la conectare. Folosit STRICT pentru sistemul de ban - fara el,
    # un ban ar tine minte doar playerId-ul efemer si ar fi inutil.
    account_id: Optional[str] = None

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
    # Daca True, camera NU apare in lista publica de lobby-uri (doar cei cu codul
    # exact pot intra). Implicit False (public), la fel ca in Among Us.
    is_private: bool = False
    # Cele 4 camere de supraveghere ale RUNDEI curente: fiecare e un dict
    # {"roomId": str, "x": float, "y": float} - generate random la start_game(),
    # aceleasi pentru toti jucatorii din runda respectiva.
    surveillance_cameras: list[dict] = field(default_factory=list)
    # accountId-urile banate din ACEASTA camera (stil Among Us: ban = nu se mai
    # poate intoarce cat timp exista camera; kick = poate reintra). Legat de
    # accountId (persistent, salvat local pe telefon), NU de playerId (care e
    # generat nou la fiecare creare/alaturare de camera si ar face banul inutil -
    # jucatorul dat afara s-ar putea reconecta instant cu alt playerId).
    banned_account_ids: set[str] = field(default_factory=set)
    # Cate task-uri de spion sunt alese pentru fiecare runda - configurabil de
    # host din setarile camerei, INAINTE ca jocul sa inceapa (2-12). Implicit 5.
    spy_task_count: int = 5
    # Task-urile CONCRETE alocate spionului in runda curenta (subset random din
    # SpyTaskType, generat la start_game()). Gol in LOBBY.
    spy_tasks: list = field(default_factory=list)

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