package com.docreader.app.data.model

data class DocContent(
    val title: String,
    val blocks: List<DocBlock>
)

sealed class DocBlock {
    data class Heading(val text: String, val level: Int) : DocBlock()  // level 1-6
    data class Paragraph(val text: String) : DocBlock()
    data class BulletItem(val text: String, val indent: Int = 0) : DocBlock()
    data class NumberedItem(val text: String, val index: Int, val indent: Int = 0) : DocBlock()
    data object Divider : DocBlock()
}

/** Extracts plain text from the document, preserving structure for TTS chunking. */
fun DocContent.toPlainText(): String = buildString {
    blocks.forEach { block ->
        when (block) {
            is DocBlock.Heading -> appendLine(block.text)
            is DocBlock.Paragraph -> appendLine(block.text)
            is DocBlock.BulletItem -> appendLine("• ${block.text}")
            is DocBlock.NumberedItem -> appendLine("${block.index}. ${block.text}")
            is DocBlock.Divider -> appendLine()
        }
        appendLine()
    }
}
