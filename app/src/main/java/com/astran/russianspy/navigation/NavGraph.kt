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
import com.astran.russianspy.ui.GameCanvasScreen
import com.astran.russianspy.ui.LobbyScreen
import com.astran.russianspy.ui.tasks.SurveillanceScreen
import com.astran.russianspy.viewmodel.GameViewModel

@Composable
fun RussianSpyNavGraph() {
    val navController = rememberNavController()
    val gameViewModel: GameViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.LOBBY) {

        composable(Routes.LOBBY) {
            LobbyScreen(
                viewModel = gameViewModel,
                onRoomReady = {
                    navController.navigate(Routes.GAME_MAP)
                }
            )
        }

        composable(Routes.GAME_MAP) {
            GameCanvasScreen(
                onEnterTask = { room ->
                    when (room.function) {
                        RoomFunction.SURVEILLANCE -> navController.navigate(Routes.SURVEILLANCE_TASK)
                        RoomFunction.FORENSICS_LAB -> navController.navigate(Routes.FORENSICS_TASK)
                        else -> { /* camera fara task momentan */ }
                    }
                }
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