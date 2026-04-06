package com.docreader.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.docreader.app.data.model.ReaderFont
import com.docreader.app.data.model.ReaderSettings
import com.docreader.app.data.model.ReaderTheme
import com.docreader.app.ui.theme.ReaderDarkBackground
import com.docreader.app.ui.theme.ReaderLightBackground
import com.docreader.app.ui.theme.ReaderSepiaBackground

@Composable
fun ReaderSettingsPanel(
    settings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .width(220.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Font size
        Text("Font size", style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (settings.fontSize > ReaderSettings.FONT_SIZE_MIN)
                    onSettingsChanged(settings.copy(fontSize = settings.fontSize - 1))
            }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease font")
            }
            Text("${settings.fontSize}sp", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = {
                if (settings.fontSize < ReaderSettings.FONT_SIZE_MAX)
                    onSettingsChanged(settings.copy(fontSize = settings.fontSize + 1))
            }) {
                Icon(Icons.Default.Add, contentDescription = "Increase font")
            }
        }

        // Theme
        Text("Background", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeCircle(color = ReaderLightBackground, label = "Light",
                selected = settings.theme == ReaderTheme.LIGHT,
                onClick = { onSettingsChanged(settings.copy(theme = ReaderTheme.LIGHT)) })
            ThemeCircle(color = ReaderSepiaBackground, label = "Sepia",
                selected = settings.theme == ReaderTheme.SEPIA,
                onClick = { onSettingsChanged(settings.copy(theme = ReaderTheme.SEPIA)) })
            ThemeCircle(color = ReaderDarkBackground, label = "Dark",
                selected = settings.theme == ReaderTheme.DARK,
                onClick = { onSettingsChanged(settings.copy(theme = ReaderTheme.DARK)) })
        }

        // Font family
        Text("Font", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderFont.entries.forEach { font ->
                val selected = settings.font == font
                Text(
                    text = font.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSettingsChanged(settings.copy(font = font)) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = "Done",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .padding(4.dp)
        )
    }
}

@Composable
private fun ThemeCircle(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
