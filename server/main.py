import json
import asyncio
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware

from game_manager import game_manager, MAX_PLAYERS
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
    players_payload = [
        {"id": p.id, "name": p.name, "connected": p.connected}
        for p in room.players.values()
    ]
    await broadcast_to_room(room_code, {
        "type": "lobby_update",
        "players": players_payload,
        "hostId": room.host_id
    })


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
                    # Camerele de supraveghere ale rundei (aceleasi pentru toti jucatorii),
                    # trimise imediat dupa game_started catre toata camera.
                    await broadcast_to_room(room_code, {
                        "type": "surveillance_cameras_assigned",
                        "spots": room.surveillance_cameras
                    })

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