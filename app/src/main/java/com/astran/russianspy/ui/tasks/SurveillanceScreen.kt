package com.astran.russianspy.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.model.SurveillanceEvent
import com.astran.russianspy.model.SurveillanceEventType

@Composable
fun SurveillanceScreen(
    activeEvent: SurveillanceEvent?,
    onExit: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "FEED LIVE - CAMERE SUPRAVEGHERE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (activeEvent != null && activeEvent.type == SurveillanceEventType.SPY_SENDING_INTEL) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⚠ ACTIVITATE SUSPECTA DETECTATA",
                            color = Color.Red,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Semnal criptat trimis din: ${activeEvent.relatedRoomId}",
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = "Fara activitate suspecta momentan...",
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iesi din camera")
            }
        }
    }
}
