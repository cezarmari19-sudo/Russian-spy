package com.astran.russianspy.network

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/** Datele publice ale unui cont: id, nume afisat, cod de prieteni. */
data class AccountInfo(
    val accountId: String,
    val displayName: String,
    val friendCode: String
)

/** Datele complete pentru ecranul de Prieteni, primite intr-un singur apel. */
data class FriendsData(
    val account: AccountInfo,
    val friends: List<AccountInfo>,
    val incomingRequests: List<AccountInfo>
)

/**
 * Client HTTP dedicat sistemului de conturi si prieteni - separat de
 * NetworkClient (care gestioneaza WebSocket-ul si camerele de joc), pentru ca
 * astea sunt apeluri simple, fara stare, fara conexiune persistenta.
 */
object AccountApi {
    private val client = OkHttpClient()

    private fun parseAccountInfo(json: JSONObject): AccountInfo {
        return AccountInfo(
            accountId = json.getString("accountId"),
            displayName = json.getString("displayName"),
            friendCode = json.getString("friendCode")
        )
    }

    private fun postForm(path: String, params: Map<String, String>, onResult: (JSONObject?, String?) -> Unit) {
        val query = params.entries.joinToString("&") { (k, v) -> "$k=${java.net.URLEncoder.encode(v, "UTF-8")}" }
        val url = "${ServerConfig.HTTP_BASE}$path?$query"
        val request = Request.Builder().url(url).post(RequestBody.create(null, ByteArray(0))).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(null, "Nu ma pot conecta la server: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body == null) {
                    onResult(null, "Raspuns gol de la server")
                    return
                }
                onResult(JSONObject(body), null)
            }
        })
    }

    private fun getRequest(path: String, params: Map<String, String>, onResult: (JSONObject?, String?) -> Unit) {
        val query = params.entries.joinToString("&") { (k, v) -> "$k=${java.net.URLEncoder.encode(v, "UTF-8")}" }
        val url = "${ServerConfig.HTTP_BASE}$path?$query"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(null, "Nu ma pot conecta la server: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body == null) {
                    onResult(null, "Raspuns gol de la server")
                    return
                }
                onResult(JSONObject(body), null)
            }
        })
    }

    /** Apelat la pornirea aplicatiei - creeaza contul daca nu exista, sau doar actualizeaza numele. */
    fun registerAccount(accountId: String, displayName: String, onResult: (AccountInfo?, String?) -> Unit) {
        postForm("/account/register", mapOf("account_id" to accountId, "display_name" to displayName)) { json, error ->
            if (json == null) {
                onResult(null, error)
                return@postForm
            }
            if (json.has("error")) {
                onResult(null, json.getString("error"))
            } else {
                onResult(parseAccountInfo(json), null)
            }
        }
    }

    fun regenerateCode(accountId: String, onResult: (newCode: String?, error: String?) -> Unit) {
        postForm("/account/regenerate_code", mapOf("account_id" to accountId)) { json, error ->
            if (json == null) {
                onResult(null, error)
                return@postForm
            }
            if (json.has("error")) {
                onResult(null, json.getString("error"))
            } else {
                onResult(json.getString("friendCode"), null)
            }
        }
    }

    fun setCustomCode(accountId: String, desiredCode: String, onResult: (newCode: String?, error: String?) -> Unit) {
        postForm("/account/set_code", mapOf("account_id" to accountId, "desired_code" to desiredCode)) { json, error ->
            if (json == null) {
                onResult(null, error)
                return@postForm
            }
            if (json.has("error")) {
                onResult(null, json.getString("error"))
            } else {
                onResult(json.getString("friendCode"), null)
            }
        }
    }

    fun sendFriendRequest(accountId: String, targetCode: String, onResult: (success: Boolean, error: String?) -> Unit) {
        postForm("/account/send_friend_request", mapOf("account_id" to accountId, "target_code" to targetCode)) { json, error ->
            if (json == null) {
                onResult(false, error)
                return@postForm
            }
            if (json.has("error")) {
                onResult(false, json.getString("error"))
            } else {
                onResult(true, null)
            }
        }
    }

    fun respondToRequest(accountId: String, requesterAccountId: String, accept: Boolean, onResult: (success: Boolean, error: String?) -> Unit) {
        postForm(
            "/account/respond_to_request",
            mapOf("account_id" to accountId, "requester_account_id" to requesterAccountId, "accept" to accept.toString())
        ) { json, error ->
            if (json == null) {
                onResult(false, error)
                return@postForm
            }
            if (json.has("error")) {
                onResult(false, json.getString("error"))
            } else {
                onResult(true, null)
            }
        }
    }

    fun removeFriend(accountId: String, friendAccountId: String, onResult: (success: Boolean, error: String?) -> Unit) {
        postForm("/account/remove_friend", mapOf("account_id" to accountId, "friend_account_id" to friendAccountId)) { json, error ->
            if (json == null) {
                onResult(false, error)
                return@postForm
            }
            if (json.has("error")) {
                onResult(false, json.getString("error"))
            } else {
                onResult(true, null)
            }
        }
    }

    fun fetchFriendsData(accountId: String, onResult: (FriendsData?, String?) -> Unit) {
        getRequest("/account/friends_data", mapOf("account_id" to accountId)) { json, error ->
            if (json == null) {
                onResult(null, error)
                return@getRequest
            }
            if (json.has("error")) {
                onResult(null, json.getString("error"))
                return@getRequest
            }
            val account = parseAccountInfo(json.getJSONObject("account"))
            val friendsArr = json.getJSONArray("friends")
            val friends = (0 until friendsArr.length()).map { parseAccountInfo(friendsArr.getJSONObject(it)) }
            val requestsArr = json.getJSONArray("incomingRequests")
            val requests = (0 until requestsArr.length()).map { parseAccountInfo(requestsArr.getJSONObject(it)) }
            onResult(FriendsData(account, friends, requests), null)
        }
    }

    /** Invita un prieten LIVE intr-o camera - functioneaza doar daca prietenul e conectat acum. */
    fun inviteToRoom(accountId: String, friendAccountId: String, roomCode: String, onResult: (success: Boolean, error: String?) -> Unit) {
        postForm(
            "/account/invite_to_room",
            mapOf("account_id" to accountId, "friend_account_id" to friendAccountId, "room_code" to roomCode)
        ) { json, error ->
            if (json == null) {
                onResult(false, error)
                return@postForm
            }
            if (json.has("error")) {
                onResult(false, json.getString("error"))
            } else {
                onResult(true, null)
            }
        }
    }
}