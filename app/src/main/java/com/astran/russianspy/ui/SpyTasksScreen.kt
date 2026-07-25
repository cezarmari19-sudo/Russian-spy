package com.astran.russianspy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astran.russianspy.data.BuildingLayout
import com.astran.russianspy.model.SpyTaskCatalog
import com.astran.russianspy.network.SpyTaskInfo
import com.astran.russianspy.ui.theme.SectionLabel
import com.astran.russianspy.ui.theme.TacticalBackground
import com.astran.russianspy.ui.theme.TacticalCard
import com.astran.russianspy.ui.theme.TacticalColors

/**
 * Lista completa de task-uri alocate spionului in runda curenta, cu progresul
 * general (X/Y completate) si camera unde trebuie facut fiecare - ca sa stie
 * unde sa mearga. NU permite completarea de aici direct (trebuie sa fii fizic
 * in camera respectiva pe harta - vezi butonul contextual din GameCanvasScreen).
 */
@Composable
fun SpyTasksScreen(
    tasks: List<SpyTaskInfo>,
    onBack: () -> Unit
) {
    val completedCount = tasks.count { it.isCompleted }

    TacticalBackground {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel(text = "Misiune secreta")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TASK-URI ($completedCount/${tasks.size})",
                        color = TacticalColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onBack) {
                    Text("Inapoi", color = TacticalColors.TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tasks) { task ->
                    val meta = SpyTaskCatalog.get(task.taskType)
                    val roomName = BuildingLayout.rooms.find { it.id == task.roomId }?.name ?: task.roomId

                    TacticalCard(modifier = Modifier.fillMaxWidth(), accentLeft = !task.isCompleted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        if (task.isCompleted) TacticalColors.Success.copy(alpha = 0.2f)
                                        else TacticalColors.SurfaceRaised
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (task.isCompleted) {
                                    Text("✓", color = TacticalColors.Success, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meta.title,
                                    color = if (task.isCompleted) TacticalColors.TextMuted else TacticalColors.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = roomName,
                                    color = TacticalColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}