package com.astran.russianspy.network

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Conexiune WebSocket GLOBALA, la nivel de aplicatie, separata de conexiunea
 * folosita in timpul unui joc/lobby (vezi NetworkClient). Scopul ei STRICT
 * este sa primeasca invitatii live de la prieteni (friend_room_invite), oriunde
 * in aplicatie - meniu principal, ecranul de prieteni, lobby sau in meci.
 *
 * E un singleton (object) pornit o singura data la lansarea aplicatiei
 * (MainActivity) si tinut deschis cat timp aplicatia e in prim-plan, cu
 * reconectare automata daca legatura pica (Render inchide des conexiuni
 * inactive pe planul gratuit).
 */
object AccountSocketManager {

    private const val RECONNECT_DELAY_MS = 3000L

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())

    private var webSocket: WebSocket? = null
    private var currentAccountId: String? = null
    private var onInvite: ((fromDisplayName: String, fromFriendCode: String, roomCode: String) -> Unit)? = null
    private var shouldStayConnected = false

    /**
     * Porneste (sau reporneste) conexiunea globala pentru acest accountId.
     * Sigur de apelat de mai multe ori (ex. din onResume) - daca e deja
     * conectat cu acelasi accountId, nu face nimic.
     */
    fun start(accountId: String, onInvite: (fromDisplayName: String, fromFriendCode: String, roomCode: String) -> Unit) {
        this.onInvite = onInvite
        shouldStayConnected = true

        if (webSocket != null && currentAccountId == accountId) {
            return // deja conectat cu acelasi cont, nimic de facut
        }

        currentAccountId = accountId
        connect(accountId)
    }

    private fun connect(accountId: String) {
        val url = "${ServerConfig.WS_BASE}/ws/account/$accountId"
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scheduleReconnect(accountId)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scheduleReconnect(accountId)
            }
        })
    }

    private fun scheduleReconnect(accountId: String) {
        if (!shouldStayConnected || currentAccountId != accountId) return
        mainHandler.postDelayed({
            if (shouldStayConnected && currentAccountId == accountId) {
                connect(accountId)
            }
        }, RECONNECT_DELAY_MS)
    }

    private fun handleIncomingMessage(text: String) {
        val json = JSONObject(text)
        if (json.optString("type") == "friend_room_invite") {
            val fromDisplayName = json.getString("fromDisplayName")
            val fromFriendCode = json.getString("fromFriendCode")
            val roomCode = json.getString("roomCode")
            mainHandler.post {
                onInvite?.invoke(fromDisplayName, fromFriendCode, roomCode)
            }
        }
    }

    /** Opreste definitiv conexiunea (ex. daca jucatorul se delogheaza din cont, task viitor). */
    fun stop() {
        shouldStayConnected = false
        webSocket?.close(1000, "Client stopped")
        webSocket = null
        currentAccountId = null
    }
}