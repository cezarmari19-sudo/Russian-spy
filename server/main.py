import json
import asyncio
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware

from game_manager import game_manager, MAX_PLAYERS
from models import GamePhase
from accounts import account_manager

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

active_connections: dict[str, dict[str, WebSocket]] = {}

# Mapare GLOBALA accountId -> WebSocket-ul de "prezenta" conectat acum
# (populata de endpoint-ul /ws/account/{account_id} de mai jos, deschis de
# client o singura data la pornirea aplicatiei, independent de camerele de
# joc). Folosita STRICT pentru invitatii de prietenie live - daca prietenul nu
# e in aceasta mapare, inseamna ca nu are aplicatia deschisa acum si
# invitatia nu poate fi livrata (nu se stocheaza, se pierde, conform cerintei).
account_connections: dict[str, WebSocket] = {}

# tine minte ultima pozitie X/Y cunoscuta pentru fiecare jucator, per camera de joc
last_positions: dict[str, dict[str, dict]] = {}

# Codurile de camera care au ACUM un task de fundal activ pentru urmarirea
# expirarii unui meeting - evita pornirea a doi watcheri suprapusi pentru
# aceeasi camera daca se raporteaza rapid mai multe corpuri (nu se poate
# oricum, report_corpse blocheaza un al doilea meeting cat unul e activ, dar
# ramane o plasa de siguranta ieftina).
_meeting_watchers: set[str] = set()


@app.get("/")
async def health_check():
    return {"status": "ok", "service": "russian-spy-server"}


async def broadcast_to_room(room_code: str, message: dict, exclude_player_id: str = None):
    connections = active_connections.get(room_code, {})
    payload = json.dumps(message)
    for player_id, ws in list(connections.items()):
        if player_id == exclude_player_id:
            continue
        try:
            await ws.send_text(payload)
        except Exception:
            pass


async def broadcast_lobby_update(room_code: str):
    room = game_manager.get_room(room_code)
    if room is None:
        return
    # isAlive e INCLUS aici (nu doar id/name/connected) - clientul are nevoie
    # de el ca sa stie cine mai e in viata cand construieste lista de vot din
    # meeting (fara asta, jucatorii morti apareau in numaratoarea "X din Y au
    # votat" desi nu pot vota niciodata, si votul parea mereu "incomplet").
    players_payload = [
        {
            "id": p.id, "name": p.name, "connected": p.connected,
            "isAlive": p.is_alive, "color": p.color,
        }
        for p in room.players.values()
    ]
    await broadcast_to_room(room_code, {
        "type": "lobby_update",
        "players": players_payload,
        "hostId": room.host_id
    })


async def _resolve_and_broadcast_meeting(room_code: str):
    """Rezolva intalnirea activa curenta (daca exista) si trimite rezultatul la
    toata camera. Extrasa separat ca sa poata fi apelata din DOUA locuri:
    1) watcher-ul de fundal, cand expira cronometrul de vot
    2) handler-ul de cast_vote, INSTANT, cand ultimul jucator viu voteaza -
       fara sa mai astepte tick-ul urmator din watcher (pana la 1s intarziere)
    Nu face nimic daca nu exista un meeting activ sau daca a fost deja rezolvat
    (resolve_meeting e idempotent din perspectiva asta - a doua chemare
    returneaza None)."""
    result = game_manager.resolve_meeting(room_code)
    if result is None:
        return

    room = game_manager.get_room(room_code)
    ejected_id = result["ejectedPlayerId"]
    ejected_name = room.players[ejected_id].name if (room and ejected_id and ejected_id in room.players) else None

    await broadcast_to_room(room_code, {
        "type": "meeting_resolved",
        "ejectedPlayerId": ejected_id,
        "ejectedPlayerName": ejected_name,
        "wasSpy": result["wasSpy"],
    })

    # Cineva tocmai a devenit "mort" (daca ejected_id nu e None) - retrimitem
    # lista de jucatori actualizata, ca isAlive sa fie corect pe client
    # inainte de urmatorul meeting eventual.
    await broadcast_lobby_update(room_code)

    if result["wasSpy"]:
        await broadcast_to_room(room_code, {"type": "game_over", "winner": "FBI_AGENT"})


async def _meeting_watcher(room_code: str):
    """Task de fundal pornit la fiecare meeting nou: asteapta pana expira
    timpul de vot SAU pana toti jucatorii vii au votat deja (verificat la
    fiecare tick de 1s), apoi rezolva rezultatul si il trimite la toata
    camera. Daca toti voteaza inainte de expirare, cast_vote rezolva deja
    INSTANT (vezi handler-ul de mai jos) - acest watcher gaseste meeting-ul
    deja rezolvat in acel caz si iese fara sa faca nimic (resolve_meeting e
    idempotent). O singura instanta ruleaza per camera odata."""
    try:
        while True:
            room = game_manager.get_room(room_code)
            if room is None or room.active_meeting is None:
                return
            if game_manager.is_meeting_expired(room_code):
                break
            await asyncio.sleep(1.0)

        await _resolve_and_broadcast_meeting(room_code)
    finally:
        _meeting_watchers.discard(room_code)


@app.websocket("/ws/account/{account_id}")
async def account_presence_websocket(websocket: WebSocket, account_id: str):
    """Conexiune GLOBALA de 'prezenta', independenta de camerele de joc.

    Clientul o deschide o singura data, la pornirea aplicatiei (cat timp exista
    un accountId salvat local), si o tine deschisa cat timp aplicatia e activa -
    indiferent daca jucatorul e in meniul principal, in ecranul de prieteni,
    intr-un lobby sau intr-un meci. Astfel invitatiile live de la prieteni
    (friend_room_invite) pot fi livrate oriunde in aplicatie, nu doar cand
    jucatorul e deja conectat la o camera de joc.
    """
    await websocket.accept()
    account_connections[account_id] = websocket

    try:
        while True:
            # Nu asteptam nicio actiune de la client pe acest canal - e strict
            # pentru livrare de evenimente server->client (invitatii). Doar
            # tinem conexiunea deschisa si ignoram orice ar trimite clientul.
            await websocket.receive_text()
    except WebSocketDisconnect:
        if account_connections.get(account_id) is websocket:
            account_connections.pop(account_id, None)


@app.websocket("/ws/{room_code}/{player_id}")
async def websocket_endpoint(websocket: WebSocket, room_code: str, player_id: str):
    await websocket.accept()

    if room_code not in active_connections:
        active_connections[room_code] = {}
    active_connections[room_code][player_id] = websocket

    if room_code not in last_positions:
        last_positions[room_code] = {}

    await broadcast_lobby_update(room_code)

    room = game_manager.get_room(room_code)
    if room:
        snapshot = [
            {
                "playerId": p.id,
                "roomId": p.current_room_id,
                "x": last_positions[room_code].get(p.id, {}).get("x"),
                "y": last_positions[room_code].get(p.id, {}).get("y"),
            }
            for p in room.players.values()
        ]
        await websocket.send_text(json.dumps({
            "type": "positions_snapshot",
            "positions": snapshot
        }))
        if room.surveillance_cameras:
            await websocket.send_text(json.dumps({
                "type": "surveillance_cameras_assigned",
                "spots": room.surveillance_cameras
            }))

    try:
        while True:
            raw = await websocket.receive_text()
            try:
                data = json.loads(raw)
            except json.JSONDecodeError:
                continue

            action = data.get("action")

            if action == "move":
                target_room_id = data.get("targetRoomId", "")
                error = game_manager.move_player(room_code, player_id, target_room_id)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    await broadcast_to_room(room_code, {
                        "type": "player_moved",
                        "playerId": player_id,
                        "targetRoomId": target_room_id
                    })

            elif action == "position_update":
                x = data.get("x")
                y = data.get("y")
                if x is not None and y is not None:
                    last_positions[room_code][player_id] = {"x": x, "y": y}
                    await broadcast_to_room(room_code, {
                        "type": "position_update",
                        "playerId": player_id,
                        "x": x,
                        "y": y
                    }, exclude_player_id=player_id)

            elif action == "start_game":
                error = game_manager.start_game(room_code)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    room = game_manager.get_room(room_code)
                    for pid, ws in active_connections.get(room_code, {}).items():
                        player = room.players.get(pid)
                        if player:
                            await ws.send_text(json.dumps({
                                "type": "game_started",
                                "yourRole": player.role.value
                            }))
                            # Task-urile de spion se trimit DOAR spionului - un agent
                            # FBI nu trebuie sa stie ca exista task-uri alocate cuiva,
                            # altfel ar deduce imediat cine e spionul.
                            if player.role.value == "RUSSIAN_SPY":
                                await ws.send_text(json.dumps({
                                    "type": "spy_tasks_assigned",
                                    "tasks": [t.to_dict() for t in room.spy_tasks]
                                }))
                    # Camerele de supraveghere ale rundei (aceleasi pentru toti jucatorii),
                    # trimise imediat dupa game_started catre toata camera.
                    await broadcast_to_room(room_code, {
                        "type": "surveillance_cameras_assigned",
                        "spots": room.surveillance_cameras
                    })
                    # Arhiva de ADN e populata automat (o mostra de referinta
                    # per jucator) - trimitem lista catre toti, clientul o
                    # afiseaza in camera DNA_ARCHIVE ca niste sloturi colorate,
                    # fara nume/rol asociat (vezi DnaSample.to_dict).
                    await broadcast_to_room(room_code, {
                        "type": "dna_archive_ready",
                        "samples": [
                            s.to_dict() for s in room.dna_samples.values()
                            if s.is_reference
                        ]
                    })
                    # Trimitem si lista de jucatori actualizata (isAlive=True
                    # pentru toti, roluri resetate) - ca ecranele care depind
                    # de lobbyPlayers sa porneasca de la o stare corecta.
                    await broadcast_lobby_update(room_code)

            elif action == "kick_player" or action == "ban_player":
                target_player_id = data.get("targetPlayerId", "")
                if action == "kick_player":
                    error = game_manager.kick_player(room_code, player_id, target_player_id)
                    kicked_message_type = "you_were_kicked"
                else:
                    error = game_manager.ban_player(room_code, player_id, target_player_id)
                    kicked_message_type = "you_were_banned"

                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    # Anuntam TINTA inainte sa-i inchidem conexiunea, ca sa stie
                    # exact de ce a fost deconectata (nu doar "player_disconnected").
                    target_ws = active_connections.get(room_code, {}).get(target_player_id)
                    if target_ws is not None:
                        try:
                            await target_ws.send_text(json.dumps({"type": kicked_message_type}))
                            await target_ws.close()
                        except Exception:
                            pass
                        active_connections.get(room_code, {}).pop(target_player_id, None)
                        last_positions.get(room_code, {}).pop(target_player_id, None)

                    await broadcast_lobby_update(room_code)

            elif action == "set_spy_task_count":
                count = data.get("count", 5)
                error = game_manager.set_spy_task_count(room_code, player_id, count)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    await broadcast_to_room(room_code, {
                        "type": "spy_task_count_changed",
                        "count": count
                    })

            elif action == "complete_spy_task" or action == "disable_spy_device":
                task_id = data.get("taskId", "")
                room = game_manager.get_room(room_code)

                # Verificare de RISC: daca exista un agent FBI CONECTAT in aceeasi
                # camera cu jucatorul care incearca task-ul chiar acum, actiunea
                # esueaza (spionul a fost vazut) si generam un eveniment de
                # supraveghere vizibil, la fel ca la trimiterea de intel.
                witnessed = False
                if room and action == "complete_spy_task":
                    acting_player = room.players.get(player_id)
                    if acting_player:
                        witnessed = any(
                            p.role.value == "FBI_AGENT" and p.connected
                            and p.current_room_id == acting_player.current_room_id
                            for p in room.players.values()
                        )

                if witnessed:
                    await websocket.send_text(json.dumps({
                        "type": "spy_task_witnessed",
                        "taskId": task_id
                    }))
                    await broadcast_to_room(room_code, {
                        "type": "surveillance_event",
                        "eventType": "SPY_SENDING_INTEL",
                        "fromRoomId": room.players[player_id].current_room_id
                    }, exclude_player_id=player_id)
                else:
                    if action == "complete_spy_task":
                        error = game_manager.complete_spy_task(room_code, player_id, task_id)
                    else:
                        error = game_manager.disable_spy_device(room_code, player_id, task_id)

                    if error:
                        await websocket.send_text(json.dumps({"type": "error", "message": error}))
                    else:
                        await broadcast_to_room(room_code, {
                            "type": "spy_task_updated",
                            "taskId": task_id,
                            "isCompleted": action == "complete_spy_task"
                        })
                        if action == "complete_spy_task" and game_manager.check_spy_win(room_code):
                            room.phase = GamePhase.SPY_WON
                            await broadcast_to_room(room_code, {"type": "game_over", "winner": "RUSSIAN_SPY"})

            elif action == "kill_player":
                target_player_id = data.get("targetPlayerId", "")
                victim_pos = last_positions.get(room_code, {}).get(target_player_id, {})
                victim_x = victim_pos.get("x", 0.0)
                victim_y = victim_pos.get("y", 0.0)

                error, corpse = game_manager.kill_player(
                    room_code, player_id, target_player_id, victim_x, victim_y
                )
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    # Victima primeste un mesaj separat, clar, ca sa stie ca a
                    # murit (trece in mod spectator pe client) - restul camerei
                    # primeste doar corpul aparut pe harta, fara sa afle cine e
                    # ucigasul (killerId ramane ascuns pana la raport/analiza).
                    victim_ws = active_connections.get(room_code, {}).get(target_player_id)
                    if victim_ws is not None:
                        try:
                            await victim_ws.send_text(json.dumps({"type": "you_were_killed"}))
                        except Exception:
                            pass

                    await broadcast_to_room(room_code, {
                        "type": "corpse_found",
                        "corpse": corpse.to_dict(reveal_killer=False)
                    })
                    # Victima tocmai a devenit is_alive=False - retrimitem lista
                    # de jucatori, ca isAlive sa fie corect pe TOTI clientii
                    # inainte de un eventual meeting viitor (asa se repara
                    # bug-ul cu "X din Y au votat" numarand gresit mortii).
                    await broadcast_lobby_update(room_code)

            elif action == "report_corpse":
                corpse_id = data.get("corpseId", "")
                room = game_manager.get_room(room_code)

                error, meeting = game_manager.report_corpse(room_code, player_id, corpse_id)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    corpse = room.corpses.get(corpse_id) if room else None
                    if corpse is not None:
                        # Corpul dispare de pe harta pentru toti (reported=True),
                        # si identitatea victimei se dezvaluie - dar killerId
                        # ramane ascuns (reveal_killer=False), la fel ca inainte.
                        await broadcast_to_room(room_code, {
                            "type": "corpse_found",
                            "corpse": corpse.to_dict(reveal_killer=False)
                        })

                    if meeting is not None:
                        # Anunta toata camera ca s-a chemat o intalnire de urgenta -
                        # clientul aduce toti jucatorii vii in meeting_room si
                        # deschide ecranul de vot cu numaratoare inversa.
                        await broadcast_to_room(room_code, {
                            "type": "meeting_called",
                            "reason": "BODY_REPORTED",
                            "reporterId": meeting.reporter_id,
                            "reporterName": meeting.reporter_name,
                            "durationSeconds": meeting.duration_seconds,
                        })
                        # Trimitem lista de jucatori (cu isAlive corect) chiar
                        # inainte de meeting, ca ecranul de vot sa numere corect
                        # din start cine e viu si cine nu.
                        await broadcast_lobby_update(room_code)
                        # Cazul particular: daca exista un SINGUR jucator viu
                        # (raportorul insusi), ar putea vota instant si sa
                        # rezolve meeting-ul chiar inainte ca watcher-ul sa
                        # apuce sa porneasca - nu e o problema, cast_vote de mai
                        # jos gestioneaza si acest caz corect (idempotent).
                        # Pornim watcher-ul de fundal care va rezolva automat
                        # votul la expirarea timpului, chiar daca nu toti au votat.
                        if room_code not in _meeting_watchers:
                            _meeting_watchers.add(room_code)
                            asyncio.create_task(_meeting_watcher(room_code))

            elif action == "tamper_corpse_dna":
                # Doar spionul, doar pe un corp aflat in Morga si inca
                # neextras - vezi validarile complete in game_manager.
                corpse_id = data.get("corpseId", "")
                error = game_manager.tamper_corpse_dna(room_code, player_id, corpse_id)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    # Nu dezvaluim noul dna_completeness catre restul camerei
                    # (ar da de gol ca s-a intamplat ceva suspect si CINE a
                    # facut-o) - doar confirmam actiunea catre autor.
                    await websocket.send_text(json.dumps({
                        "type": "corpse_dna_tampered",
                        "corpseId": corpse_id,
                    }))

            elif action == "extract_corpse_dna":
                corpse_id = data.get("corpseId", "")
                room = game_manager.get_room(room_code)
                error, sample = game_manager.extract_corpse_dna(room_code, player_id, corpse_id)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    corpse = room.corpses.get(corpse_id) if room else None
                    # Anuntam toata camera ca mostra a fost recoltata (corpul
                    # ramane in morga, dar acum arata "ADN extras") - completeness-ul
                    # devine vizibil abia acum (to_dict ascunde valoarea pana
                    # la extractie).
                    await broadcast_to_room(room_code, {
                        "type": "corpse_dna_extracted",
                        "corpse": corpse.to_dict(reveal_killer=False) if corpse else None,
                        "sample": sample.to_dict() if sample else None,
                    })

            elif action == "move_dna_sample_to_lab":
                sample_id = data.get("sampleId", "")
                error = game_manager.move_dna_sample_to_lab(room_code, player_id, sample_id)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    room = game_manager.get_room(room_code)
                    sample = room.dna_samples.get(sample_id) if room else None
                    await broadcast_to_room(room_code, {
                        "type": "dna_sample_moved",
                        "sample": sample.to_dict() if sample else None,
                    })

            elif action == "place_sample_in_lab_machine":
                sample_id = data.get("sampleId", "")
                error = game_manager.place_sample_in_lab_machine(room_code, player_id, sample_id)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    room = game_manager.get_room(room_code)
                    if room is not None:
                        await broadcast_to_room(room_code, {
                            "type": "lab_machine_updated",
                            "harvestedSampleId": room.lab_machine_harvested_sample_id,
                            "referenceSampleId": room.lab_machine_reference_sample_id,
                        })

            elif action == "compare_dna_samples":
                error, result = game_manager.compare_dna_samples(room_code, player_id)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    # Rezultatul e STRICT privat - trimis doar autorului actiunii,
                    # niciodata broadcastat catre restul camerei.
                    await websocket.send_text(json.dumps({
                        "type": "dna_comparison_result",
                        "result": result.to_dict() if result else None,
                    }))

            elif action == "cast_vote":
                # targetPlayerId absent sau null in JSON => vot de "skip" (None).
                target_player_id = data.get("targetPlayerId")
                error = game_manager.cast_vote(room_code, player_id, target_player_id)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    # Anuntam camera CINE a votat (nu si cu CE) - pastreaza votul
                    # secret pana la rezolvarea finala, dar arata progresul
                    # ("X din Y au votat"), la fel ca la Among Us.
                    await broadcast_to_room(room_code, {
                        "type": "vote_cast",
                        "voterId": player_id
                    })
                    # Rezolvare INSTANT daca acesta a fost ultimul vot necesar -
                    # nu mai asteptam pana la 1s (tick-ul watcher-ului) sau pana
                    # expira cronometrul intreg. resolve_meeting e idempotent,
                    # deci nu exista risc de dubla rezolvare daca watcher-ul
                    # ajunge sa verifice chiar in acelasi moment.
                    if game_manager.all_alive_players_voted(room_code):
                        await _resolve_and_broadcast_meeting(room_code)

            elif action == "delete_room":
                error = game_manager.delete_room(room_code, player_id)
                if error:
                    await websocket.send_text(json.dumps({"type": "error", "message": error}))
                else:
                    await broadcast_to_room(room_code, {"type": "room_deleted"})
                    active_connections.pop(room_code, None)
                    last_positions.pop(room_code, None)

            elif action == "spy_send_intel":
                room = game_manager.get_room(room_code)
                if room:
                    player = room.players.get(player_id)
                    if player:
                        await broadcast_to_room(room_code, {
                            "type": "surveillance_event",
                            "eventType": "SPY_SENDING_INTEL",
                            "fromRoomId": player.current_room_id
                        }, exclude_player_id=player_id)

    except WebSocketDisconnect:
        game_manager.remove_player(room_code, player_id)
        if room_code in active_connections and player_id in active_connections[room_code]:
            del active_connections[room_code][player_id]
        if room_code in last_positions and player_id in last_positions[room_code]:
            del last_positions[room_code][player_id]

        # Daca nu mai ramane niciun jucator conectat in camera (fie in LOBBY,
        # fie in timpul meciului), stergem camera automat - nu ramane nimic
        # "agatat" in memoria serverului.
        if not game_manager.has_connected_players(room_code):
            game_manager.rooms.pop(room_code, None)
            active_connections.pop(room_code, None)
            last_positions.pop(room_code, None)
            return

        await broadcast_to_room(room_code, {
            "type": "player_disconnected",
            "playerId": player_id
        })
        await broadcast_lobby_update(room_code)


@app.post("/create_room")
async def create_room(player_id: str, player_name: str, account_id: str = None):
    room = game_manager.create_room(player_id, player_name, account_id)
    return {"roomCode": room.room_code}


@app.post("/join_room")
async def join_room(room_code: str, player_id: str, player_name: str, account_id: str = None):
    room, error = game_manager.join_room(room_code, player_id, player_name, account_id)
    if error:
        return {"error": error}
    return {"success": True}


@app.get("/public_rooms")
async def public_rooms():
    """Lista de lobby-uri publice disponibile (stil Among Us in beta - selectie
    random, fara scor complex). Apelat doar la refresh MANUAL din ecranul de
    lobby-uri, nu automat/periodic, ca sa nu incarce serverul degeaba."""
    rooms = game_manager.list_public_rooms()
    return {
        "rooms": [
            {
                "roomCode": r.room_code,
                "playerCount": len(r.players),
                "maxPlayers": MAX_PLAYERS,
            }
            for r in rooms
        ]
    }


@app.post("/set_room_privacy")
async def set_room_privacy(room_code: str, player_id: str, is_private: bool):
    """Doar host-ul poate schimba privat/public. Camerele private nu apar in
    /public_rooms - se poate intra in ele doar cu codul exact."""
    room = game_manager.get_room(room_code)
    if room is None:
        return {"error": "Camera nu exista"}
    if room.host_id != player_id:
        return {"error": "Doar host-ul poate schimba aceasta setare"}
    room.is_private = is_private
    return {"success": True, "isPrivate": room.is_private}


# ===========================================================================
# CONTURI SI PRIETENI - vezi accounts.py pentru detalii si limitari (in
# memorie, temporar, pana se adauga autentificarea reala prin email).
# ===========================================================================

@app.post("/account/register")
async def account_register(account_id: str, display_name: str):
    """Apelat la fiecare pornire a aplicatiei cu accountId-ul salvat local pe
    telefon (generat o singura data). Creeaza contul daca nu exista, sau doar
    actualizeaza numele daca exista deja."""
    account = account_manager.get_or_create_account(account_id, display_name)
    return account.to_public_dict()


@app.post("/account/regenerate_code")
async def account_regenerate_code(account_id: str):
    new_code, error = account_manager.regenerate_code(account_id)
    if error:
        return {"error": error}
    return {"friendCode": new_code}


@app.post("/account/set_code")
async def account_set_code(account_id: str, desired_code: str):
    success, error = account_manager.set_custom_code(account_id, desired_code)
    if not success:
        return {"error": error}
    account = account_manager.get_account(account_id)
    return {"friendCode": account.friend_code if account else desired_code.upper()}


@app.post("/account/send_friend_request")
async def account_send_friend_request(account_id: str, target_code: str):
    success, error = account_manager.send_friend_request(account_id, target_code)
    if not success:
        return {"error": error}
    return {"success": True}


@app.post("/account/respond_to_request")
async def account_respond_to_request(account_id: str, requester_account_id: str, accept: bool):
    error = account_manager.respond_to_request(account_id, requester_account_id, accept)
    if error:
        return {"error": error}
    return {"success": True}


@app.post("/account/remove_friend")
async def account_remove_friend(account_id: str, friend_account_id: str):
    error = account_manager.remove_friend(account_id, friend_account_id)
    if error:
        return {"error": error}
    return {"success": True}


@app.get("/account/friends_data")
async def account_friends_data(account_id: str):
    """Datele complete pentru ecranul de Prieteni: contul propriu (cu codul),
    lista de prieteni si lista de cereri primite, intr-un singur apel."""
    account = account_manager.get_account(account_id)
    if account is None:
        return {"error": "Cont inexistent"}
    friends = account_manager.list_friends(account_id)
    requests = account_manager.list_incoming_requests(account_id)
    return {
        "account": account.to_public_dict(),
        "friends": [f.to_public_dict() for f in friends],
        "incomingRequests": [r.to_public_dict() for r in requests],
    }


@app.post("/account/invite_to_room")
async def account_invite_to_room(account_id: str, friend_account_id: str, room_code: str):
    """Trimite o invitatie LIVE catre un prieten, direct pe WebSocket-ul lui
    activ - functioneaza doar daca prietenul e conectat chiar acum (nu se
    stocheaza nicaieri; daca nu e online, invitatia se pierde, ca cerut).
    """
    inviter = account_manager.get_account(account_id)
    if inviter is None:
        return {"error": "Cont inexistent"}
    if friend_account_id not in inviter.friends:
        return {"error": "Nu sunteti prieteni"}

    friend_ws = account_connections.get(friend_account_id)
    if friend_ws is None:
        return {"error": "Prietenul nu este conectat acum"}

    try:
        await friend_ws.send_text(json.dumps({
            "type": "friend_room_invite",
            "fromDisplayName": inviter.display_name,
            "fromFriendCode": inviter.friend_code,
            "roomCode": room_code
        }))
    except Exception:
        return {"error": "Nu am putut trimite invitatia"}

    return {"success": True}