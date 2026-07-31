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
    MORGUE = "MORGUE"
    DNA_ARCHIVE = "DNA_ARCHIVE"


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
    # Culoare stabila, unica in cadrul partidei (hex, ex. "#E53935"), asignata
    # la intrarea in lobby - stil Among Us. Folosita ca sa identificam vizual
    # mostrele de ADN de referinta fara sa dezvaluim numele/rolul jucatorului.
    color: str = "#9E9E9E"

    def to_dict(self, reveal_role: bool = False):
        return {
            "id": self.id,
            "name": self.name,
            "role": self.role.value if reveal_role else None,
            "isAlive": self.is_alive,
            "currentRoomId": self.current_room_id,
            "connected": self.connected,
            "color": self.color,
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
    """O mostra de ADN. Doua tipuri, distinse de `is_reference`:

    - Mostre de REFERINTA (is_reference=True): una per jucator din partida,
      generate automat la start_game() in camera DNA_ARCHIVE (arhiva de ADN).
      Nu apartin niciunui eveniment - reprezinta pur si simplu ADN-ul acelui
      jucator, la 100% completeness, si NU pot fi niciodata stricate/alterate
      (arhiva e pastrata in conditii sterile - "in camera unde e pastrat ADN
      nu poate fi stricat"). Clientii vad DOAR culoarea playerului asociat
      (player_color), niciodata numele/rolul, ca sa nu dea de gol spionul
      inainte de comparare.
    - Mostre RECOLTATE (is_reference=False): rezultatul extragerii de ADN de
      pe un cadavru din Morgue. Legate de un corpse_id, cu completeness variabil
      (vezi Corpse.dna_completeness) care POATE fi redus de spion inainte de
      extragere (tamper_corpse_dna in game_manager).
    """
    id: str
    room_id: str
    actual_owner_id: str
    displayed_owner_id: str
    completeness: int
    is_analyzed: bool = False
    was_tampered_with: bool = False
    is_reference: bool = False
    player_color: str = ""
    source_corpse_id: Optional[str] = None
    placed_in_lab_slot: bool = False

    def is_reliable(self) -> bool:
        return self.completeness >= 70

    def to_dict(self):
        return {
            "id": self.id,
            "roomId": self.room_id,
            "completeness": self.completeness,
            "isAnalyzed": self.is_analyzed,
            "isReference": self.is_reference,
            "playerColor": self.player_color,
            "sourceCorpseId": self.source_corpse_id,
            "placedInLabSlot": self.placed_in_lab_slot,
        }


@dataclass
class DnaComparisonResult:
    """Rezultatul unei comparari facute la masina din Laboratorul Criminalistic:
    o mostra recoltata (de pe un cadavru) vs. o mostra de referinta (din arhiva).
    Similarity scade odata cu completeness-ul mostrei recoltate - o mostra
    stricata de spion da un rezultat mai putin sigur/mai putin clar. Vizibil
    STRICT pentru cel care a facut compararea (nu se broadcasteaza in camera)."""
    harvested_sample_id: str
    reference_sample_id: str
    reference_player_color: str
    similarity_percent: int
    is_match: bool

    def to_dict(self):
        return {
            "harvestedSampleId": self.harvested_sample_id,
            "referenceSampleId": self.reference_sample_id,
            "referencePlayerColor": self.reference_player_color,
            "similarityPercent": self.similarity_percent,
            "isMatch": self.is_match,
        }


@dataclass
class Corpse:
    """Corpul unui agent FBI omorat de spion. Ramane pe harta, vizibil pentru
    TOTI jucatorii (spion si FBI), pana e raportat. Continutul real de ADN
    (dna_completeness) e ascuns clientilor pana cineva face minigame-ul de
    recoltare la fata locului - inainte de asta, clientii stiu doar CA exista
    un corp acolo, nu cat ADN se poate recolta de pe el."""
    id: str
    victim_id: str
    killer_id: str  # nu se trimite niciodata catre clienti inainte de raport
    room_id: str
    x: float
    y: float
    # Cat de complet e ADN-ul ramas pe corp (10-100). Random 70-100 daca
    # ucigasul nu purta manusi; redus drastic (~10) daca purta manusi -
    # calculat o singura data, la momentul omorului.
    dna_completeness: int = 0
    dna_recovered: bool = False
    reported: bool = False
    reported_by: Optional[str] = None
    # Devine True imediat dupa raportare - la acel moment corpul e mutat automat
    # in camera MORGUE (room_id devine "morgue", x/y sunt recalculate acolo).
    # Ramane vizibil in morga (pentru toti) pana i se extrage ADN-ul.
    in_morgue: bool = False
    # ADN-ul de pe corp poate fi extras o singura data (oricine, spion sau FBI,
    # aflat in morga langa el) - dupa aceea produce o DnaSample recoltata si
    # corpul nu mai poate fi "lucrat" din nou.
    dna_extracted: bool = False
    # id-ul DnaSample-ului rezultat din extractie, daca s-a facut deja.
    extracted_sample_id: Optional[str] = None

    def to_dict(self, reveal_killer: bool = False):
        return {
            "id": self.id,
            "victimId": self.victim_id,
            "killerId": self.killer_id if reveal_killer else None,
            "roomId": self.room_id,
            "x": self.x,
            "y": self.y,
            "dnaRecovered": self.dna_recovered,
            "reported": self.reported,
            "inMorgue": self.in_morgue,
            "dnaExtracted": self.dna_extracted,
            # dna_completeness ramane ascuns clientilor pana la extractie -
            # altfel s-ar putea deduce daca ucigasul purta manusi (10%) inainte
            # de a face minigame-ul, ceea ce ar da indicii nedorite.
            "dnaCompleteness": self.dna_completeness if self.dna_extracted else None,
            # Necesar clientului ca sa stie ce mostra sa trimita la Laborator
            # din Morga (butonul "Trimite ADN la laborator" - fara acest id,
            # clientul nu avea de unde sa afle sample_id-ul rezultat din
            # extractie, si mostra recoltata ramanea "prinsa" in morga,
            # nemaiajungand niciodata in laborator).
            "extractedSampleId": self.extracted_sample_id if self.dna_extracted else None,
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
class Meeting:
    """O intalnire de urgenta (chemata prin raportarea unui corp), stil Among
    Us: toti jucatorii vii sunt adusi in meeting_room, au un timp fix de vot,
    apoi jucatorul cel mai votat e exclus (majoritate simpla - daca e egalitate,
    inclusiv egalitate cu numarul de skip-uri, nimeni nu e exclus)."""
    started_at_millis: float
    duration_seconds: float = 75.0
    # player_id -> target_player_id votat, sau None daca a votat explicit "skip".
    # Un player_id care NU apare inca in acest dict inseamna ca nu a votat inca.
    votes: dict[str, Optional[str]] = field(default_factory=dict)
    reporter_id: str = ""
    reporter_name: str = ""
    resolved: bool = False

    def to_dict(self, remaining_seconds: float):
        return {
            "startedAtMillis": self.started_at_millis,
            "durationSeconds": self.duration_seconds,
            "remainingSeconds": remaining_seconds,
            "reporterId": self.reporter_id,
            "reporterName": self.reporter_name,
            "votedPlayerIds": list(self.votes.keys()),
        }


@dataclass
class LobbyChatMessage:
    """Un mesaj de chat trimis in camera de asteptare (lobby), inainte de
    inceperea partidei. Lista completa e limitata la 40 de mesaje - vezi
    GameManager.add_lobby_chat_message, care scoate automat cel mai vechi
    mesaj cand se adauga al 41-lea."""
    id: str
    sender_id: str
    sender_name: str
    text: str
    sent_at_millis: int

    def to_dict(self):
        return {
            "id": self.id,
            "senderId": self.sender_id,
            "senderName": self.sender_name,
            "text": self.text,
            "sentAtMillis": self.sent_at_millis,
        }


@dataclass
class LobbyPosition:
    """Pozitia unui jucator in camera FIZICA de lobby (holul de asteptare cu
    monitor si dulap) - complet separata de pozitia din jocul propriu-zis."""
    x: float
    y: float

    def to_dict(self):
        return {"x": self.x, "y": self.y}


@dataclass
class PlayerReport:
    """Un raport trimis de un jucator despre altul, in lobby. DOAR inregistrat -
    nu declanseaza nicio actiune automata (fara ban/kick automat asociat)."""
    id: str
    reporter_id: str
    reported_id: str
    reported_name: str
    reason: str  # "hacking" | "harassment" | "bad_language" | "name"
    sent_at_millis: int

    def to_dict(self):
        return {
            "id": self.id,
            "reporterId": self.reporter_id,
            "reportedId": self.reported_id,
            "reportedName": self.reported_name,
            "reason": self.reason,
            "sentAtMillis": self.sent_at_millis,
        }


class FbiTaskType(str, Enum):
    """Catalogul de task-uri COSMETICE pentru agentii FBI - la fel ca la spion,
    dar fara niciun efect real in joc (doar ocupatie, stil Among Us). Nu
    conteaza spre nicio conditie de victorie - task-ul real de Comunicatii
    (SOS Morse) e separat si are efect real, vezi GameRoom mai jos."""
    CHECK_EVIDENCE_LOCKER = "CHECK_EVIDENCE_LOCKER"
    FILE_INCIDENT_REPORT = "FILE_INCIDENT_REPORT"
    INSPECT_BADGE_SCANNER = "INSPECT_BADGE_SCANNER"
    CALIBRATE_METAL_DETECTOR = "CALIBRATE_METAL_DETECTOR"
    REVIEW_PERSONNEL_FILES = "REVIEW_PERSONNEL_FILES"


FBI_TASK_DURATIONS_SECONDS: dict[str, float] = {
    FbiTaskType.CHECK_EVIDENCE_LOCKER.value: 3.0,
    FbiTaskType.FILE_INCIDENT_REPORT.value: 5.0,
    FbiTaskType.INSPECT_BADGE_SCANNER.value: 2.0,
    FbiTaskType.CALIBRATE_METAL_DETECTOR.value: 6.0,
    FbiTaskType.REVIEW_PERSONNEL_FILES.value: 4.0,
}

FBI_TASK_ALLOWED_FUNCTIONS: dict[str, list] = {
    FbiTaskType.CHECK_EVIDENCE_LOCKER.value: [RoomFunction.FORENSICS_LAB, RoomFunction.ARMORY],
    FbiTaskType.FILE_INCIDENT_REPORT.value: [RoomFunction.OFFICE],
    FbiTaskType.INSPECT_BADGE_SCANNER.value: [RoomFunction.ENTRANCE, RoomFunction.OFFICE],
    FbiTaskType.CALIBRATE_METAL_DETECTOR.value: [RoomFunction.ARMORY],
    FbiTaskType.REVIEW_PERSONNEL_FILES.value: [RoomFunction.OFFICE, RoomFunction.BREAK_ROOM],
}


@dataclass
class FbiTaskInstance:
    """O instanta CONCRETA a unui task cosmetic de FBI, ALOCATA UNUI SINGUR
    agent (assigned_player_id) - spre deosebire de task-urile de spion (comune,
    oricare spion le poate face), fiecare agent FBI viu primeste propriul set
    de task-uri individuale la start_game. Fara nicio mecanica de dezactivare
    (nu exista "obiect plasat" ca la spion)."""
    id: str
    task_type: str
    room_id: str
    x: float
    y: float
    assigned_player_id: str
    is_completed: bool = False

    def to_dict(self):
        return {
            "id": self.id,
            "taskType": self.task_type,
            "roomId": self.room_id,
            "x": self.x,
            "y": self.y,
            "assignedPlayerId": self.assigned_player_id,
            "isCompleted": self.is_completed,
        }


@dataclass
class GameRoom:
    """Reprezinta o camera/lobby de joc (partida), nu o camera fizica din cladire."""
    room_code: str
    host_id: str = ""
    phase: GamePhase = GamePhase.LOBBY
    players: dict[str, Player] = field(default_factory=dict)
    dna_samples: dict[str, DnaSample] = field(default_factory=dict)
    intel_messages: list[IntelMessage] = field(default_factory=list)
    # Corpurile create in runda curenta (id -> Corpse). Raman in aceasta lista
    # chiar si dupa raportare - un corp raportat nu mai apare pe harta, dar e
    # pastrat pentru istoricul rundei.
    corpses: dict[str, Corpse] = field(default_factory=dict)
    # Cate secunde trebuie sa treaca intre doua omoruri consecutive ale
    # spionului - configurabil de host, la fel ca spy_task_count. Implicit 30s.
    kill_cooldown_seconds: float = 30.0
    # Momentul (epoch seconds) ultimului omor reusit - folosit ca sa calculam
    # daca a trecut cooldown-ul. 0 inseamna "niciun omor inca in aceasta runda".
    last_kill_at_millis: float = 0.0
    # Intalnirea activa curenta (raport de corp) - None daca nu e niciuna in
    # desfasurare acum. Doar UNA poate fi activa simultan intr-o camera.
    active_meeting: Optional[Meeting] = None
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
    spy_tasks: list[SpyTaskInstance] = field(default_factory=list)
    # Cele doua sloturi ale "masinii de comparare ADN" din laboratorul
    # criminalistic: id-ul mostrei recoltate (de pe un corp) si id-ul mostrei
    # de referinta (din arhiva), daca sunt puse. Un singur slot din fiecare
    # tip poate fi ocupat simultan - masina compara o singura pereche o data.
    lab_machine_harvested_sample_id: Optional[str] = None
    lab_machine_reference_sample_id: Optional[str] = None
    # Mesajele de chat din camera de asteptare (lobby) - pastrate in memorie,
    # DOAR ultimele 40 (cel mai vechi e scos automat cand se adauga al 41-lea -
    # vezi add_lobby_chat_message in game_manager.py). Nu supravietuiesc unui
    # restart de server (nu sunt persistate pe disk), la fel ca restul starii.
    lobby_chat_messages: list["LobbyChatMessage"] = field(default_factory=list)
    # Pozitiile jucatorilor in camera fizica de lobby (holul de asteptare cu
    # monitor si dulap) - separate de pozitiile din jocul propriu-zis
    # (Player.current_room_id/x/y, folosite doar dupa start_game()).
    lobby_positions: dict[str, "LobbyPosition"] = field(default_factory=dict)
    # Rapoartele trimise intre jucatori in lobby (cine a raportat pe cine, cu
    # ce motiv) - DOAR inregistrate, nu declanseaza nicio actiune automata
    # (fara ban/kick automat). Utile eventual pentru moderare manuala ulterioara.
    player_reports: list["PlayerReport"] = field(default_factory=list)
    # Task-urile cosmetice de FBI ale RUNDEI curente - fiecare agent FBI viu
    # primeste propriul set (assigned_player_id), generate la start_game().
    # Fara efect real in joc - doar ocupatie, ca la Among Us.
    fbi_tasks: list["FbiTaskInstance"] = field(default_factory=list)
    # Task-ul de Comunicatii (SOS Morse) - se deblocheaza DOAR cand toti
    # agentii FBI VII au terminat toate task-urile cosmetice de mai sus.
    # Odata deblocat, ORICE agent FBI viu poate incerca sa trimita SOS din
    # camera COMMS_MONITOR (cifru Morse + 2 butoane cu functie randomizata -
    # vezi comms_button_a_is_dot). La primul SOS trimis cu succes, se
    # inregistreaza sos_sent_at_millis - un cronometru de 2 minute (INVIZIBIL
    # pentru toti jucatorii, deocamdata) porneste de acolo; la expirare, FBI
    # castiga automat (CIA "captureaza" spionul). Trimiteri ulterioare de SOS
    # (de oricine, inclusiv spionul) nu au niciun efect suplimentar.
    comms_unlocked: bool = False
    # Care buton (A=stanga/albastru, B=dreapta/rosu) reprezinta PUNCTUL,
    # ales random o singura data cand comms_unlocked devine True - la fel
    # pentru toata runda, ca sa nu se poata "invata" din incercari anterioare
    # ale altui agent (fiecare runda noua = alta alocare random).
    comms_button_a_is_dot: bool = True
    sos_sent_at_millis: float = 0.0