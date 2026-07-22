package com.astran.russianspy.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.PlayerPrefs
import com.astran.russianspy.network.AccountApi
import com.astran.russianspy.network.AccountInfo
import com.astran.russianspy.network.FriendsData
import com.astran.russianspy.ui.theme.SectionLabel
import com.astran.russianspy.ui.theme.TacticalBackground
import com.astran.russianspy.ui.theme.TacticalButton
import com.astran.russianspy.ui.theme.TacticalCard
import com.astran.russianspy.ui.theme.TacticalColors

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

    TacticalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel(text = "Contacte")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("PRIETENI", color = TacticalColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onBack) {
                    Text("Inapoi", color = TacticalColors.TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && friendsData == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TacticalColors.Accent)
                }
            }

            errorMessage?.let { msg ->
                Text(text = msg, color = TacticalColors.Danger)
                Spacer(modifier = Modifier.height(12.dp))
            }

            actionMessage?.let { msg ->
                Text(text = msg, color = TacticalColors.Success, fontSize = 13.sp)
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

                Spacer(modifier = Modifier.height(18.dp))

                SectionLabel(text = "Adauga prieten")
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = addFriendCode,
                        onValueChange = { addFriendCode = it.uppercase() },
                        label = { Text("Cod prieten") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalColors.Accent,
                            unfocusedBorderColor = TacticalColors.Border,
                            focusedTextColor = TacticalColors.TextPrimary,
                            unfocusedTextColor = TacticalColors.TextPrimary,
                            cursorColor = TacticalColors.Accent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TacticalButton(
                        text = "Trimite",
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
                        enabled = addFriendCode.length == 7,
                        isPrimary = true,
                        height = 48.dp,
                        modifier = Modifier.width(110.dp)
                    )
                }

                if (data.incomingRequests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(22.dp))
                    SectionLabel(text = "Cereri primite")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
                SectionLabel(text = "Prietenii mei (${data.friends.size})")
                Spacer(modifier = Modifier.height(8.dp))

                if (data.friends.isEmpty()) {
                    Text("Niciun prieten adaugat inca.", color = TacticalColors.TextMuted, fontSize = 13.sp)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.friends.forEach { friend ->
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

                Spacer(modifier = Modifier.height(24.dp))
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
    TacticalCard(modifier = Modifier.fillMaxWidth(), accentLeft = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionLabel(text = "Codul tau")
            Spacer(modifier = Modifier.height(6.dp))
            Text(friendCode, color = TacticalColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                TextButton(onClick = onCopy) { Text("Copiaza", color = TacticalColors.Accent) }
                TextButton(onClick = onRegenerate) { Text("Genereaza nou", color = TacticalColors.TextSecondary) }
                TextButton(onClick = onEdit) { Text("Editeaza", color = TacticalColors.TextSecondary) }
            }
        }
    }
}

@Composable
private fun RequestRow(requester: AccountInfo, onAccept: () -> Unit, onReject: () -> Unit) {
    TacticalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(requester.displayName, color = TacticalColors.TextPrimary, fontWeight = FontWeight.Bold)
                Text(requester.friendCode, color = TacticalColors.TextSecondary, fontSize = 12.sp)
            }
            Row {
                TextButton(onClick = onAccept) { Text("Accepta", color = TacticalColors.Success) }
                TextButton(onClick = onReject) { Text("Refuza", color = TacticalColors.Danger) }
            }
        }
    }
}

@Composable
private fun FriendRow(friend: AccountInfo, onRemove: () -> Unit) {
    TacticalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(friend.displayName, color = TacticalColors.TextPrimary, fontWeight = FontWeight.Bold)
                Text(friend.friendCode, color = TacticalColors.TextSecondary, fontSize = 12.sp)
            }
            TextButton(onClick = onRemove) { Text("Sterge", color = TacticalColors.Danger) }
        }
    }
}

@Composable
private fun EditCodeDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalColors.Surface,
        title = { Text("Editeaza codul", color = TacticalColors.TextPrimary) },
        text = {
            Column {
                Text("Exact 7 caractere, litere si cifre.", color = TacticalColors.TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 7) text = it.uppercase() },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalColors.Accent,
                        unfocusedBorderColor = TacticalColors.Border,
                        focusedTextColor = TacticalColors.TextPrimary,
                        unfocusedTextColor = TacticalColors.TextPrimary,
                        cursorColor = TacticalColors.Accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.length == 7) {
                Text("Salveaza", color = TacticalColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuleaza", color = TacticalColors.TextSecondary) }
        }
    )
}