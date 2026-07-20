package com.astran.russianspy.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.PlayerPrefs
import com.astran.russianspy.network.AccountApi
import com.astran.russianspy.network.AccountInfo
import com.astran.russianspy.network.FriendsData

/**
 * Ecran Prieteni: codul propriu de 7 caractere (copiabil, regenerabil, sau
 * editabil manual), sectiune de adaugare dupa cod, cereri primite (accept/
 * refuz) si lista de prieteni deja adaugati.
 *
 * NOTA: sistemul de conturi e temporar in memorie pe server (vezi
 * server/accounts.py) - accountId-ul e generat local si salvat pe telefon
 * (PlayerPrefs), nu exista inca autentificare reala prin email.
 */
@Composable
fun FriendsScreen(
    onBack: () -> Unit
) {
    val context: Context = LocalContext.current
    val accountId = remember { PlayerPrefs.getAccountId(context) }

    var friendsData by remember { mutableStateOf<FriendsData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    var showEditCodeDialog by remember { mutableStateOf(false) }
    var addFriendCode by remember { mutableStateOf("") }

    fun refresh() {
        isLoading = true
        errorMessage = null
        AccountApi.fetchFriendsData(accountId) { data, error ->
            isLoading = false
            if (data != null) {
                friendsData = data
            } else {
                errorMessage = error ?: "Eroare necunoscuta"
            }
        }
    }

    LaunchedEffect(Unit) {
        val playerName = PlayerPrefs.getPlayerName(context).ifBlank { "Agent" }
        AccountApi.registerAccount(accountId, playerName) { account, error ->
            if (account != null) {
                refresh()
            } else {
                isLoading = false
                errorMessage = error ?: "Nu m-am putut conecta la server"
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0D0F12)) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PRIETENI", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) {
                    Text("Inapoi", color = Color(0xFFAAAAAA))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && friendsData == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            errorMessage?.let { msg ->
                Text(text = msg, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
            }

            actionMessage?.let { msg ->
                Text(text = msg, color = Color(0xFF3DDC5A), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }

            val data = friendsData
            if (data != null) {
                MyCodeCard(
                    friendCode = data.account.friendCode,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Cod prieteni", data.account.friendCode))
                        actionMessage = "Cod copiat."
                    },
                    onRegenerate = {
                        AccountApi.regenerateCode(accountId) { newCode, error ->
                            if (newCode != null) {
                                actionMessage = "Cod nou generat."
                                refresh()
                            } else {
                                errorMessage = error
                            }
                        }
                    },
                    onEdit = { showEditCodeDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = addFriendCode,
                        onValueChange = { addFriendCode = it.uppercase() },
                        label = { Text("Cod prieten") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            AccountApi.sendFriendRequest(accountId, addFriendCode) { success, error ->
                                if (success) {
                                    actionMessage = "Cerere trimisa."
                                    addFriendCode = ""
                                } else {
                                    errorMessage = error
                                }
                            }
                        },
                        enabled = addFriendCode.length == 7
                    ) {
                        Text("Trimite")
                    }
                }

                if (data.incomingRequests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("CERERI PRIMITE", color = Color(0xFF9AA0A6), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    data.incomingRequests.forEach { requester ->
                        RequestRow(
                            requester = requester,
                            onAccept = {
                                AccountApi.respondToRequest(accountId, requester.accountId, true) { success, error ->
                                    if (success) refresh() else errorMessage = error
                                }
                            },
                            onReject = {
                                AccountApi.respondToRequest(accountId, requester.accountId, false) { success, error ->
                                    if (success) refresh() else errorMessage = error
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("PRIETENII MEI (${data.friends.size})", color = Color(0xFF9AA0A6), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (data.friends.isEmpty()) {
                    Text("Niciun prieten adaugat inca.", color = Color(0xFF666666), fontSize = 13.sp)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(data.friends) { friend ->
                        FriendRow(
                            friend = friend,
                            onRemove = {
                                AccountApi.removeFriend(accountId, friend.accountId) { success, error ->
                                    if (success) refresh() else errorMessage = error
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditCodeDialog) {
        EditCodeDialog(
            onDismiss = { showEditCodeDialog = false },
            onConfirm = { newCode ->
                AccountApi.setCustomCode(accountId, newCode) { result, error ->
                    if (result != null) {
                        showEditCodeDialog = false
                        actionMessage = "Cod actualizat."
                        refresh()
                    } else {
                        errorMessage = error
                    }
                }
            }
        )
    }
}

@Composable
private fun MyCodeCard(
    friendCode: String,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1D22))
            .padding(16.dp)
    ) {
        Text("CODUL TAU", color = Color(0xFF9AA0A6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(friendCode, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Row {
            TextButton(onClick = onCopy) { Text("Copiaza") }
            TextButton(onClick = onRegenerate) { Text("Genereaza nou") }
            TextButton(onClick = onEdit) { Text("Editeaza") }
        }
    }
}

@Composable
private fun RequestRow(requester: AccountInfo, onAccept: () -> Unit, onReject: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1D22))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(requester.displayName, color = Color.White, fontWeight = FontWeight.Bold)
            Text(requester.friendCode, color = Color(0xFF9AA0A6), fontSize = 12.sp)
        }
        Row {
            TextButton(onClick = onAccept) { Text("Accepta", color = Color(0xFF4CAF50)) }
            TextButton(onClick = onReject) { Text("Refuza", color = Color(0xFFE53935)) }
        }
    }
}

@Composable
private fun FriendRow(friend: AccountInfo, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1D22))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(friend.displayName, color = Color.White, fontWeight = FontWeight.Bold)
            Text(friend.friendCode, color = Color(0xFF9AA0A6), fontSize = 12.sp)
        }
        TextButton(onClick = onRemove) { Text("Sterge", color = Color(0xFFE53935)) }
    }
}

@Composable
private fun EditCodeDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D22),
        title = { Text("Editeaza codul", color = Color.White) },
        text = {
            Column {
                Text("Exact 7 caractere, litere si cifre.", color = Color(0xFFCCCCCC), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 7) text = it.uppercase() },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.length == 7) {
                Text("Salveaza")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuleaza", color = Color(0xFFAAAAAA)) }
        }
    )
}