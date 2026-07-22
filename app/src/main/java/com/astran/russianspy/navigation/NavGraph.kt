package com.astran.russianspy.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
                    onRoomReady = { navController.navigate(Routes.WAITING_ROOM) },
                    onBack = { navController.popBackStack() },
                    onNeedsName = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onDone = { navController.popBackStack() })
            }

            composable(Routes.FRIENDS) {
                FriendsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.FIND_LOBBY) {
                FindLobbyScreen(
                    viewModel = gameViewModel,
                    onRoomReady = { navController.navigate(Routes.WAITING_ROOM) },
                    onBack = { navController.popBackStack() },
                    onNeedsName = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.LOBBY) {
                LobbyScreen(
                    viewModel = gameViewModel,
                    onRoomReady = { navController.navigate(Routes.WAITING_ROOM) },
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
                    onOpenSurveillanceMonitors = { navController.navigate(Routes.SURVEILLANCE_MONITORS) },
                    onLeaveGame = {
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
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1D22),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${incomingInvite.fromDisplayName} te invita",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Camera ${incomingInvite.roomCode}",
                                color = Color(0xFF9AA0A6),
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(onClick = { gameViewModel.dismissFriendInvite() }) {
                            Text("Ignora", color = Color(0xFF9AA0A6), fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val roomCodeToJoin = incomingInvite.roomCode
                                gameViewModel.dismissFriendInvite()
                                gameViewModel.leaveLobby()
                                val playerName = PlayerPrefs.getPlayerName(context)
                                gameViewModel.joinRoom(playerName, roomCodeToJoin)
                                navController.navigate(Routes.WAITING_ROOM) {
                                    popUpTo(Routes.MAIN_MENU) { inclusive = false }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
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