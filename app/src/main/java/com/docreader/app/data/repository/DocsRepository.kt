package com.docreader.app.data.repository

import com.docreader.app.data.model.DocBlock
import com.docreader.app.data.model.DocContent
import com.docreader.app.session.SessionManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val DOCS_API_BASE = "https://docs.googleapis.com/v1/documents"

class DocsRepository(private val httpClient: OkHttpClient) {

    /** Fetches a Google Doc and parses it into a structured DocContent model. */
    suspend fun getDocument(docId: String): Result<DocContent> = withContext(Dispatchers.IO) {
        val token = SessionManager.accessToken
            ?: return@withContext Result.failure(IllegalStateException("Not logged in"))

        val request = Request.Builder()
            .url("$DOCS_API_BASE/$docId")
            .header("Authorization", "Bearer $token")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    RuntimeException("Docs API error: ${response.code}")
                )
            }
            val json = JsonParser.parseString(response.body?.string()).asJsonObject
            val content = parseDocument(json)
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses the Google Docs API response JSON into a list of DocBlocks.
     * Handles: headings (H1-H6), paragraphs, bullet lists, numbered lists.
     */
    private fun parseDocument(json: JsonObject): DocContent {
        val title = json.get("title")?.asString ?: "Untitled"
        val body = json.getAsJsonObject("body")
        val content = body.getAsJsonArray("content")

        val blocks = mutableListOf<DocBlock>()
        var numberedIndex = 1

        content?.forEach { element ->
            val obj = element.asJsonObject
            val paragraph = obj.getAsJsonObject("paragraph") ?: return@forEach

            val paragraphStyle = paragraph.getAsJsonObject("paragraphStyle")
            val namedStyle = paragraphStyle?.get("namedStyleType")?.asString ?: "NORMAL_TEXT"
            val bullet = paragraph.getAsJsonObject("bullet")

            val text = extractParagraphText(paragraph.getAsJsonArray("elements"))
            if (text.isBlank()) return@forEach

            when {
                bullet != null -> {
                    val listProperties = bullet.getAsJsonObject("listProperties")
                    val glyphType = bullet.get("nestingLevel")?.asInt ?: 0
                    // Determine if ordered or unordered from the list definition
                    // For simplicity: if glyphSymbol contains digits → numbered
                    val isOrdered = listProperties?.toString()?.contains("DECIMAL") == true
                    if (isOrdered) {
                        blocks.add(DocBlock.NumberedItem(text, numberedIndex++, glyphType))
                    } else {
                        numberedIndex = 1
                        blocks.add(DocBlock.BulletItem(text, glyphType))
                    }
                }
                namedStyle.startsWith("HEADING_") -> {
                    numberedIndex = 1
                    val level = namedStyle.removePrefix("HEADING_").toIntOrNull() ?: 1
                    blocks.add(DocBlock.Heading(text, level))
                }
                namedStyle == "NORMAL_TEXT" -> {
                    numberedIndex = 1
                    blocks.add(DocBlock.Paragraph(text))
                }
                else -> {
                    blocks.add(DocBlock.Paragraph(text))
                }
            }
        }

        return DocContent(title = title, blocks = blocks)
    }

    /** Concatenates all text runs in a paragraph element list. */
    private fun extractParagraphText(elements: JsonArray?): String {
        if (elements == null) return ""
        return buildString {
            elements.forEach { el ->
                val textRun = el.asJsonObject.getAsJsonObject("textRun") ?: return@forEach
                val content = textRun.get("content")?.asString ?: return@forEach
                append(content.trimEnd('\n'))
            }
        }.trim()
    }
}
