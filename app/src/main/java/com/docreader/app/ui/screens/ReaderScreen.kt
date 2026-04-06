package com.docreader.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docreader.app.data.model.DocBlock
import com.docreader.app.data.model.DocContent
import com.docreader.app.data.model.ReaderFont
import com.docreader.app.data.model.ReaderSettings
import com.docreader.app.data.model.ReaderTheme
import com.docreader.app.data.model.ReadingMode
import com.docreader.app.ui.components.ReaderSettingsPanel
import com.docreader.app.ui.components.VoiceControlsPanel
import com.docreader.app.viewmodel.ReaderViewModel
import com.docreader.app.viewmodel.VoiceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    docId: String,
    docTitle: String,
    readerViewModel: ReaderViewModel,
    voiceViewModel: VoiceViewModel,
    onBack: () -> Unit
) {
    val readerState by readerViewModel.uiState.collectAsState()
    val voiceState by voiceViewModel.uiState.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(docId) {
        readerViewModel.loadDocument(docId)
    }

    LaunchedEffect(readerState.docContent) {
        readerState.docContent?.let { voiceViewModel.prepare(it) }
    }

    // Show voice panel in bottom sheet
    LaunchedEffect(voiceState.showVoicePanel) {
        if (voiceState.showVoicePanel) scaffoldState.bottomSheetState.expand()
        else scaffoldState.bottomSheetState.partialExpand()
    }

    val settings = readerState.settings
    val bgColor = when (settings.theme) {
        ReaderTheme.LIGHT -> com.docreader.app.ui.theme.ReaderLightBackground
        ReaderTheme.DARK -> com.docreader.app.ui.theme.ReaderDarkBackground
        ReaderTheme.SEPIA -> com.docreader.app.ui.theme.ReaderSepiaBackground
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            VoiceControlsPanel(
                voiceViewModel = voiceViewModel,
                onDismiss = { voiceViewModel.toggleVoicePanel() }
            )
        },
        sheetPeekHeight = 0.dp,
        topBar = {
            TopAppBar(
                title = { Text(docTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { readerViewModel.toggleReadingMode() }) {
                        Icon(Icons.Default.SwapVert, contentDescription = "Toggle reading mode")
                    }
                    IconButton(onClick = { readerViewModel.toggleSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = {
                        voiceViewModel.toggleVoicePanel()
                        scope.launch { scaffoldState.bottomSheetState.expand() }
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice read")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures { readerViewModel.onUserInteraction() }
                }
        ) {
            when {
                readerState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                readerState.error != null -> Text(
                    text = readerState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
                readerState.docContent != null -> {
                    val doc = readerState.docContent!!
                    if (settings.readingMode == ReadingMode.SCROLL) {
                        ScrollReader(doc = doc, settings = settings, onInteraction = { readerViewModel.onUserInteraction() })
                    } else {
                        PaginatedReader(doc = doc, settings = settings, onInteraction = { readerViewModel.onUserInteraction() })
                    }
                }
            }

            // Inline settings panel overlay
            if (readerState.showSettings) {
                ReaderSettingsPanel(
                    settings = settings,
                    onSettingsChanged = { readerViewModel.updateSettings(it) },
                    onDismiss = { readerViewModel.toggleSettings() },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
private fun ScrollReader(
    doc: DocContent,
    settings: ReaderSettings,
    onInteraction: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .pointerInput(Unit) {
                detectTapGestures { onInteraction() }
            }
    ) {
        Text(
            text = doc.title,
            style = headingStyle(settings, 1),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        doc.blocks.forEach { block ->
            DocBlockView(block = block, settings = settings)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PaginatedReader(
    doc: DocContent,
    settings: ReaderSettings,
    onInteraction: () -> Unit
) {
    // Simple paginated view — chunks blocks into pages
    // A full implementation would measure text height per block
    // For now: show all content with scroll disabled (placeholder for real pagination)
    ScrollReader(doc = doc, settings = settings, onInteraction = onInteraction)
}

@Composable
private fun DocBlockView(block: DocBlock, settings: ReaderSettings) {
    val textColor = when (settings.theme) {
        ReaderTheme.LIGHT -> com.docreader.app.ui.theme.ReaderLightText
        ReaderTheme.DARK -> com.docreader.app.ui.theme.ReaderDarkText
        ReaderTheme.SEPIA -> com.docreader.app.ui.theme.ReaderSepiaText
    }

    when (block) {
        is DocBlock.Heading -> {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = block.text,
                style = headingStyle(settings, block.level).copy(color = textColor)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        is DocBlock.Paragraph -> Text(
            text = block.text,
            style = bodyStyle(settings).copy(color = textColor)
        )
        is DocBlock.BulletItem -> Row {
            Text("• ", style = bodyStyle(settings).copy(color = textColor))
            Text(block.text, style = bodyStyle(settings).copy(color = textColor))
        }
        is DocBlock.NumberedItem -> Row {
            Text("${block.index}. ", style = bodyStyle(settings).copy(color = textColor))
            Text(block.text, style = bodyStyle(settings).copy(color = textColor))
        }
        is DocBlock.Divider -> {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(textColor.copy(alpha = 0.2f)))
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun headingStyle(settings: ReaderSettings, level: Int): TextStyle {
    val baseSp = settings.fontSize + when (level) {
        1 -> 10; 2 -> 7; 3 -> 4; 4 -> 2; else -> 0
    }
    return TextStyle(
        fontSize = baseSp.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = resolveFont(settings),
        lineHeight = (baseSp * 1.3f).sp
    )
}

private fun bodyStyle(settings: ReaderSettings): TextStyle = TextStyle(
    fontSize = settings.fontSize.sp,
    fontFamily = resolveFont(settings),
    lineHeight = (settings.fontSize * settings.lineSpacing).sp
)

private fun resolveFont(settings: ReaderSettings): FontFamily = when (settings.font) {
    ReaderFont.SERIF -> FontFamily.Serif
    ReaderFont.SANS_SERIF -> FontFamily.SansSerif
    ReaderFont.MONOSPACE -> FontFamily.Monospace
}
