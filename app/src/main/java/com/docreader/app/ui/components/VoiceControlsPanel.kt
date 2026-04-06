package com.docreader.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.docreader.app.data.model.CURATED_VOICES
import com.docreader.app.viewmodel.SleepTimerOption
import com.docreader.app.viewmodel.VoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceControlsPanel(
    voiceViewModel: VoiceViewModel,
    onDismiss: () -> Unit
) {
    val state by voiceViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Voice Reading", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Progress
        if (state.totalChunks > 0) {
            LinearProgressIndicator(
                progress = { state.currentChunkIndex.toFloat() / state.totalChunks },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Section ${state.currentChunkIndex + 1} of ${state.totalChunks}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playback controls
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { voiceViewModel.skipBack() }) {
                Icon(Icons.Default.Replay10, contentDescription = "Previous section", modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { if (state.isPlaying) voiceViewModel.pause() else voiceViewModel.play() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { voiceViewModel.skipForward() }) {
                Icon(Icons.Default.Forward10, contentDescription = "Next section", modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { voiceViewModel.stop() }) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(32.dp))
            }
        }

        // Speed slider
        Spacer(modifier = Modifier.height(8.dp))
        Text("Speed: ${String.format("%.1f", state.speedRate)}x", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = state.speedRate,
            onValueChange = { voiceViewModel.setSpeed(it) },
            valueRange = 0.5f..2.0f,
            steps = 5,
            modifier = Modifier.fillMaxWidth()
        )

        // Sleep timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { voiceViewModel.showSleepTimerDialog() }) {
                Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (state.sleepTimerMinutesRemaining != null)
                        "Sleep: ${state.sleepTimerMinutesRemaining}m left"
                    else "Sleep timer"
                )
            }
            if (state.sleepTimerMinutesRemaining != null) {
                TextButton(onClick = { voiceViewModel.cancelSleepTimer() }) {
                    Text("Cancel")
                }
            }
        }

        // Voice selection chips
        Spacer(modifier = Modifier.height(8.dp))
        Text("Voice", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CURATED_VOICES.take(4).forEach { voice ->
                FilterChip(
                    selected = state.selectedVoice == voice,
                    onClick = { voiceViewModel.selectVoice(voice) },
                    label = { Text(voice.displayName.substringBefore(" ("), style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    // Sleep timer dialog
    if (state.showSleepTimerDialog) {
        SleepTimerDialog(
            onSelect = { voiceViewModel.setSleepTimer(it) },
            onDismiss = { voiceViewModel.dismissSleepTimerDialog() }
        )
    }
}
