package com.astran.russianspy.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
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
}

@Composable
private fun PlaceholderScreen(name: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Text(text = "TODO: $name", modifier = Modifier.padding(24.dp))
    }
}