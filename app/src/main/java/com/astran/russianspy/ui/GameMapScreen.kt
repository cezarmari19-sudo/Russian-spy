package com.astran.russianspy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.Room
import com.astran.russianspy.viewmodel.GameViewModel

@Composable
fun GameMapScreen(
    viewModel: GameViewModel,
    onEnterTask: (Room) -> Unit
) {
    val currentRoomId by viewModel.currentRoomId
    val rooms = BuildingLayout.rooms
    val currentRoom = rooms.find { it.id == currentRoomId }

    val maxRow = rooms.maxOf { it.gridRow }
    val maxCol = rooms.maxOf { it.gridCol }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

            Text(
                text = "Esti in: ${currentRoom?.name ?: "necunoscut"}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (row in 0..maxRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0..maxCol) {
                            val room = rooms.find { it.gridRow == row && it.gridCol == col }
                            if (room != null) {
                                RoomCell(
                                    room = room,
                                    isCurrent = room.id == currentRoomId,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    onClick = {
                                        if (room.id != currentRoomId) {
                                            viewModel.moveToRoom(room.id)
                                        } else {
                                            onEnterTask(room)
                                        }
                                    }
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Apasa o camera pentru a te muta. Apasa camera in care esti pentru actiune.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun RoomCell(
    room: Room,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (isCurrent) Color(0xFF8B0000) else Color(0xFF37474F))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = room.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
    }
}
