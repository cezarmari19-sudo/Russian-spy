package com.astran.russianspy.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astran.russianspy.data.PlayerPrefs
import com.astran.russianspy.model.RoomFunction
import com.astran.russianspy.ui.theme.TacticalColors
import com.astran.russianspy.ui.FindLobbyScreen
import com.astran.russianspy.ui.FriendsScreen
import com.astran.russianspy.ui.GameCanvasScreen
import com.astran.russianspy.ui.LobbyScreen
import com.astran.russianspy.ui.MainMenuScreen
import com.astran.russianspy.ui.PublicLobbiesScreen
import com.astran.russianspy.ui.SettingsScreen
import com.astran.russianspy.ui.SurveillanceMonitorsScreen
import com.astran.russianspy.ui.WaitingRoomScreen
import com.astran.russianspy.ui.tasks.SurveillanceScreen
import com.astran.russianspy.viewmodel.GameViewModel

@Composable
fun RussianSpyNavGraph() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = viewModel()
    val context = LocalContext.current

    // Pornim conexiunea GLOBALA de prezenta (pentru invitatii de prieteni) o
    // singura data, la lansarea aplicatiei - ramane activa in tot navigatia,
    // indiferent de ecran (meniu, prieteni, lobby, meci).
    LaunchedEffect(Unit) {
        val accountId = PlayerPrefs.getAccountId(context)
        gameViewModel.startAccountPresence(accountId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.MAIN_MENU) {

            composable(Routes.MAIN_MENU) {
                MainMenuScreen(
                    onStartClick = { navController.navigate(Routes.FIND_LOBBY) },
                    onCreateLobbyClick = { navController.navigate(Routes.LOBBY) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    onFriendsClick = { navController.navigate(Routes.FRIENDS) },
                    onLobbiesClick = { navController.navigate(Routes.PUBLIC_LOBBIES) }
                )
            }

            composable(Routes.PUBLIC_LOBBIES) {
                PublicLobbiesScreen(
                    viewModel = gameViewModel,
                    onRoomReady = {
                        navController.navigate(Routes.WAITING_ROOM)
                    },
                    onBack = { navController.popBackStack() },
                    onNeedsName = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onDone = { navController.popBackStack() }
                )
            }

            composable(Routes.FRIENDS) {
                FriendsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.FIND_LOBBY) {
                FindLobbyScreen(
                    viewModel = gameViewModel,
                    onRoomReady = {
                        navController.navigate(Routes.WAITING_ROOM)
                    },
                    onBack = { navController.popBackStack() },
                    onNeedsName = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.LOBBY) {
                LobbyScreen(
                    viewModel = gameViewModel,
                    onRoomReady = {
                        navController.navigate(Routes.WAITING_ROOM)
                    },
                    onNeedsName = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.WAITING_ROOM) {
                WaitingRoomScreen(
                    viewModel = gameViewModel,
                    onGameStarted = {
                        navController.navigate(Routes.GAME_MAP) {
                            popUpTo(Routes.MAIN_MENU) { inclusive = false }
                        }
                    },
                    onLeaveLobby = {
                        // La fel ca la iesirea din meci: curatam stack-ul pana la
                        // MAIN_MENU (inclusiv WAITING_ROOM/LOBBY/FIND_LOBBY), ca
                        // sa nu ramana ecrane vechi de lobby in spate.
                        navController.navigate(Routes.MAIN_MENU) {
                            popUpTo(Routes.MAIN_MENU) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.GAME_MAP) {
                GameCanvasScreen(
                    viewModel = gameViewModel,
                    onEnterTask = { room ->
                        when (room.function) {
                            RoomFunction.SURVEILLANCE -> navController.navigate(Routes.SURVEILLANCE_TASK)
                            RoomFunction.FORENSICS_LAB -> navController.navigate(Routes.FORENSICS_TASK)
                            else -> { /* camera fara task momentan */ }
                        }
                    },
                    onOpenSurveillanceMonitors = {
                        navController.navigate(Routes.SURVEILLANCE_MONITORS)
                    },
                    onLeaveGame = {
                        // Curatam TOT stack-ul de navigare pana la MAIN_MENU (inclusiv
                        // GAME_MAP), ca sa nu ramana WAITING_ROOM/LOBBY vechi in spate.
                        navController.navigate(Routes.MAIN_MENU) {
                            popUpTo(Routes.MAIN_MENU) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SURVEILLANCE_MONITORS) {
                SurveillanceMonitorsScreen(
                    viewModel = gameViewModel,
                    onExit = { navController.popBackStack() }
                )
            }

            composable(Routes.ROLE_REVEAL) {
                PlaceholderScreen(name = "Alocare rol")
            }

            composable(Routes.SURVEILLANCE_TASK) {
                SurveillanceScreen(
                    activeEvent = gameViewModel.activeSurveillanceEvent.value,
                    onExit = { navController.popBackStack() }
                )
            }

            composable(Routes.FORENSICS_TASK) {
                PlaceholderScreen(name = "Analiza ADN")
            }

            composable(Routes.PHONE_TASK) {
                PlaceholderScreen(name = "Telefon criptat")
            }

            composable(Routes.PLANT_BOMB_TASK) {
                PlaceholderScreen(name = "Planteaza bomba")
            }

            composable(Routes.REPORT_BODY) {
                PlaceholderScreen(name = "Raporteaza cadavru")
            }

            composable(Routes.GAME_OVER) {
                PlaceholderScreen(name = "Sfarsit joc")
            }
        }

        // Banner GLOBAL de invitatie de prieten, stil "Among Us" - un card ingust
        // care aluneca din varful ecranului, FARA sa blocheze restul ecranului
        // (spre deosebire de un AlertDialog modal). Apare deasupra a orice ecran
        // curent, indiferent unde e jucatorul in aplicatie. Sursa e
        // gameViewModel.incomingFriendInvite, populata de AccountSocketManager
        // prin canalul global de prezenta.
        val incomingInvite = gameViewModel.incomingFriendInvite.value
        AnimatedVisibility(
            visible = incomingInvite != null,
            enter = slideInVertically(animationSpec = tween(300)) { -it },
            exit = slideOutVertically(animationSpec = tween(250)) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            if (incomingInvite != null) {
                Surface(
                    shape = com.astran.russianspy.ui.theme.tacticalCardShapePublic(14.dp),
                    color = TacticalColors.Surface,
                    shadowElevation = 10.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TacticalColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .drawBehind {
                                drawRect(
                                    color = TacticalColors.Accent,
                                    size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${incomingInvite.fromDisplayName} te invita",
                                color = TacticalColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Camera ${incomingInvite.roomCode}",
                                color = TacticalColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(onClick = { gameViewModel.dismissFriendInvite() }) {
                            Text("Ignora", color = TacticalColors.TextSecondary, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val roomCodeToJoin = incomingInvite.roomCode
                                gameViewModel.dismissFriendInvite()
                                // Iesim intai din orice lobby/meci curent (daca exista), ca
                                // sa nu ramanem cu doua conexiuni WS de joc deschise odata.
                                gameViewModel.leaveLobby()
                                val playerName = PlayerPrefs.getPlayerName(context)
                                gameViewModel.joinRoom(playerName, roomCodeToJoin)
                                navController.navigate(Routes.WAITING_ROOM) {
                                    popUpTo(Routes.MAIN_MENU) { inclusive = false }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalColors.Accent),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Alatura-te", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Text(text = "TODO: $name", modifier = Modifier.padding(24.dp))
    }
}