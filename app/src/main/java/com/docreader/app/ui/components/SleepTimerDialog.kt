package com.docreader.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.docreader.app.viewmodel.SleepTimerOption

@Composable
fun SleepTimerDialog(
    onSelect: (minutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set sleep timer") },
        text = {
            Column {
                listOf(
                    SleepTimerOption.FIFTEEN,
                    SleepTimerOption.THIRTY,
                    SleepTimerOption.FORTY_FIVE,
                    SleepTimerOption.SIXTY
                ).forEach { option ->
                    TextButton(
                        onClick = { onSelect(option.minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(option.label)
                    }
                }

                TextButton(
                    onClick = { showCustomInput = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Custom...")
                }

                if (showCustomInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { if (it.all { c -> c.isDigit() }) customMinutes = it },
                        label = { Text("Minutes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            val mins = customMinutes.toIntOrNull()
                            if (mins != null && mins > 0) onSelect(mins)
                        }) {
                            Text("Set")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
