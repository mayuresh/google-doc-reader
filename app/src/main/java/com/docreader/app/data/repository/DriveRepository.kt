package com.docreader.app.data.repository

import com.docreader.app.data.model.DriveItem
import com.docreader.app.session.SessionManager
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
private const val DOCS_MIME = "application/vnd.google-apps.document"
private const val FOLDER_MIME = "application/vnd.google-apps.folder"

class DriveRepository(private val httpClient: OkHttpClient) {

    /**
     * Lists files and folders in the given parent folder.
     * Pass parentId = "root" for the Drive root.
     * Only returns owned files — no shared docs (per requirements).
     */
    suspend fun listFolder(parentId: String): Result<List<DriveItem>> = withContext(Dispatchers.IO) {
        val token = SessionManager.accessToken
            ?: return@withContext Result.failure(IllegalStateException("Not logged in"))

        // Query: only docs and folders owned by the user in this parent
        val query = "'$parentId' in parents and trashed = false and " +
                "('me' in owners) and " +
                "(mimeType = '$FOLDER_MIME' or mimeType = '$DOCS_MIME')"

        val url = "$DRIVE_API_BASE/files?" +
                "q=${query.encode()}" +
                "&fields=files(id,name,mimeType,modifiedTime)" +
                "&orderBy=folder,name" +
                "&pageSize=200"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    RuntimeException("Drive API error: ${response.code}")
                )
            }
            val json = com.google.gson.JsonParser.parseString(response.body?.string())
                .asJsonObject
            val files = json.getAsJsonArray("files") ?: return@withContext Result.success(emptyList())

            val items = files.map { el ->
                val obj = el.asJsonObject
                DriveItem(
                    id = obj.get("id").asString,
                    name = obj.get("name").asString,
                    mimeType = obj.get("mimeType").asString,
                    modifiedTime = obj.get("modifiedTime")?.asString
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Searches for Google Docs across ALL folders by name.
     * Only returns owned docs.
     */
    suspend fun searchDocs(query: String): Result<List<DriveItem>> = withContext(Dispatchers.IO) {
        val token = SessionManager.accessToken
            ?: return@withContext Result.failure(IllegalStateException("Not logged in"))

        val driveQuery = "name contains '${query.replace("'", "\\'")}' and " +
                "mimeType = '$DOCS_MIME' and " +
                "'me' in owners and " +
                "trashed = false"

        val url = "$DRIVE_API_BASE/files?" +
                "q=${driveQuery.encode()}" +
                "&fields=files(id,name,mimeType,modifiedTime)" +
                "&orderBy=name" +
                "&pageSize=50"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    RuntimeException("Drive API error: ${response.code}")
                )
            }
            val json = com.google.gson.JsonParser.parseString(response.body?.string())
                .asJsonObject
            val files = json.getAsJsonArray("files") ?: return@withContext Result.success(emptyList())

            val items = files.map { el ->
                val obj = el.asJsonObject
                DriveItem(
                    id = obj.get("id").asString,
                    name = obj.get("name").asString,
                    mimeType = obj.get("mimeType").asString,
                    modifiedTime = obj.get("modifiedTime")?.asString
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun String.encode() = java.net.URLEncoder.encode(this, "UTF-8")
}
