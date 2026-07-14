import json
import asyncio
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware

from game_manager import game_manager

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# tine evidenta conexiunilor WebSocket active, per room_code
active_connections: dict[str, dict[str, WebSocket]] = {}


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
        "players": players_payload
    })


@app.websocket("/ws/{room_code}/{player_id}")
async def websocket_endpoint(websocket: WebSocket, room_code: str, player_id: str):
    await websocket.accept()

    if room_code not in active_connections:
        active_connections[room_code] = {}
    active_connections[room_code][player_id] = websocket

    # la conectare, anuntam pe toti (inclusiv pe cel nou) starea curenta a lobby-ului
    await broadcast_lobby_update(room_code)

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
        await broadcast_to_room(room_code, {
            "type": "player_disconnected",
            "playerId": player_id
        })
        await broadcast_lobby_update(room_code)


@app.post("/create_room")
async def create_room(player_id: str, player_name: str):
    room = game_manager.create_room(player_id, player_name)
    return {"roomCode": room.room_code}


@app.post("/join_room")
async def join_room(room_code: str, player_id: str, player_name: str):
    room, error = game_manager.join_room(room_code, player_id, player_name)
    if error:
        return {"error": error}
    return {"roomCode": room.room_code}