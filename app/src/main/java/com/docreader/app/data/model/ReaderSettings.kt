package com.docreader.app.data.model

enum class ReaderTheme { LIGHT, DARK, SEPIA }
enum class ReaderFont { SERIF, SANS_SERIF, MONOSPACE }
enum class ReadingMode { SCROLL, PAGINATED }

data class ReaderSettings(
    val fontSize: Int = 18,             // sp, range 12–32
    val font: ReaderFont = ReaderFont.SERIF,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val readingMode: ReadingMode = ReadingMode.SCROLL,
    val lineSpacing: Float = 1.6f
) {
    companion object {
        val FONT_SIZE_MIN = 12
        val FONT_SIZE_MAX = 32
    }
}
