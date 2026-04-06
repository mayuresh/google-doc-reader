package com.docreader.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.docreader.app.data.model.DriveItem
import com.docreader.app.viewmodel.DriveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    viewModel: DriveViewModel,
    onDocSelected: (DriveItem) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchActive by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                if (!uiState.isSearching) {
                    // Breadcrumb
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        uiState.breadcrumb.forEachIndexed { index, entry ->
                            if (index > 0) Text(" › ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Text(
                                text = entry.name,
                                style = if (index == uiState.breadcrumb.lastIndex)
                                    MaterialTheme.typography.titleMedium
                                else
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                modifier = if (index < uiState.breadcrumb.lastIndex)
                                    Modifier.clickable { viewModel.loadFolder(entry.id, entry.name) }
                                else Modifier
                            )
                        }
                    }
                } else {
                    Text("Search results for \"${uiState.searchQuery}\"")
                }
            },
            navigationIcon = {
                if (uiState.breadcrumb.size > 1 || uiState.isSearching) {
                    IconButton(onClick = {
                        if (uiState.isSearching) viewModel.clearSearch()
                        else viewModel.navigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                IconButton(onClick = { searchActive = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Sign out")
                }
            }
        )

        // Search bar
        if (searchActive) {
            var query by remember { mutableStateOf("") }
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {
                    if (it.isNotBlank()) {
                        viewModel.search(it)
                        searchActive = false
                    }
                },
                active = true,
                onActiveChange = { if (!it) searchActive = false },
                placeholder = { Text("Search your docs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { searchActive = false; query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {}
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.error != null -> Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
                uiState.items.isEmpty() -> Text(
                    text = if (uiState.isSearching) "No results found." else "This folder is empty.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                else -> LazyColumn {
                    items(uiState.items, key = { it.id }) { item ->
                        DriveItemRow(item = item, onClick = {
                            if (item.isFolder) viewModel.loadFolder(item.id, item.name)
                            else if (item.isGoogleDoc) onDocSelected(item)
                        })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DriveItemRow(item: DriveItem, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.name) },
        leadingContent = {
            Icon(
                imageVector = if (item.isFolder) Icons.Default.Folder else Icons.Default.Article,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (item.isFolder)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
