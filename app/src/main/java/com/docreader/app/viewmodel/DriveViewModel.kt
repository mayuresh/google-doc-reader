package com.docreader.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docreader.app.data.model.DriveItem
import com.docreader.app.data.repository.DriveRepository
import com.docreader.app.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FolderEntry(val id: String, val name: String)

data class DriveUiState(
    val items: List<DriveItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val breadcrumb: List<FolderEntry> = listOf(FolderEntry("root", "My Drive"))
)

class DriveViewModel(private val driveRepository: DriveRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveUiState())
    val uiState: StateFlow<DriveUiState> = _uiState

    init {
        loadFolder("root")
    }

    fun loadFolder(folderId: String, folderName: String = "My Drive") {
        val current = _uiState.value
        val newBreadcrumb = if (folderId == "root") {
            listOf(FolderEntry("root", "My Drive"))
        } else {
            // Append if not already in breadcrumb
            val existingIndex = current.breadcrumb.indexOfFirst { it.id == folderId }
            if (existingIndex >= 0) {
                current.breadcrumb.subList(0, existingIndex + 1)
            } else {
                current.breadcrumb + FolderEntry(folderId, folderName)
            }
        }

        _uiState.value = current.copy(
            isLoading = true,
            error = null,
            isSearching = false,
            searchQuery = "",
            breadcrumb = newBreadcrumb
        )

        viewModelScope.launch {
            SessionManager.resetInactivityTimer()
            driveRepository.listFolder(folderId).fold(
                onSuccess = { items ->
                    _uiState.value = _uiState.value.copy(items = items, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load folder"
                    )
                }
            )
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, isSearching = true, isLoading = true, error = null)
        viewModelScope.launch {
            SessionManager.resetInactivityTimer()
            driveRepository.searchDocs(query).fold(
                onSuccess = { items ->
                    _uiState.value = _uiState.value.copy(items = items, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Search failed"
                    )
                }
            )
        }
    }

    fun clearSearch() {
        val root = _uiState.value.breadcrumb.first()
        loadFolder(root.id, root.name)
    }

    fun navigateUp() {
        val breadcrumb = _uiState.value.breadcrumb
        if (breadcrumb.size > 1) {
            val parent = breadcrumb[breadcrumb.size - 2]
            loadFolder(parent.id, parent.name)
        }
    }
}
