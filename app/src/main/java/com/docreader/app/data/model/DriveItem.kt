package com.docreader.app.data.model

data class DriveItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val modifiedTime: String? = null
) {
    val isFolder: Boolean
        get() = mimeType == "application/vnd.google-apps.folder"

    val isGoogleDoc: Boolean
        get() = mimeType == "application/vnd.google-apps.document"
}
