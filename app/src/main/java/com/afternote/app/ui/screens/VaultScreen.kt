package com.afternote.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.app.ui.components.parseColorHex
import com.afternote.app.viewmodel.DecryptedVaultNote
import com.afternote.app.viewmodel.VaultUiState
import com.afternote.app.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    vm: VaultViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Secret Vault", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is VaultUiState.Ready) {
                FloatingActionButton(onClick = onNewNote) {
                    Icon(Icons.Default.Add, "New Vault Note")
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is VaultUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is VaultUiState.DecryptionError -> {
                Box(Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.LockPerson, null,
                            modifier = Modifier.size(64.dp),
                            tint     = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Text("Decryption Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(state.msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(.7f))
                    }
                }
            }

            is VaultUiState.Ready -> {
                if (state.notes.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Lock, null,
                                modifier = Modifier.size(64.dp),
                                tint     = MaterialTheme.colorScheme.onSurface.copy(.3f))
                            Spacer(Modifier.height(12.dp))
                            Text("Vault is empty",
                                color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns               = StaggeredGridCells.Fixed(2),
                        modifier              = Modifier.fillMaxSize().padding(padding),
                        contentPadding        = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing   = 8.dp
                    ) {
                        items(state.notes, key = { it.id }) { note ->
                            VaultNoteCard(
                                note     = note,
                                onClick  = { onOpenNote(note.id) },
                                onDelete = { vm.deleteNote(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultNoteCard(
    note: DecryptedVaultNote,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bg     = parseColorHex(note.colorHex)
    val isDark = bg.luminance() < 0.5f
    val text   = if (isDark) Color.White else Color.Black

    Card(
        onClick   = onClick,
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (note.title.isNotBlank()) {
                Text(note.title,
                    style    = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold, color = text),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
            }
            if (note.content.isNotBlank()) {
                Text(note.content,
                    style    = MaterialTheme.typography.bodySmall.copy(color = text.copy(.8f)),
                    maxLines = 5, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(note.updatedAt)),
                    style    = MaterialTheme.typography.labelSmall.copy(color = text.copy(.5f)),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null,
                        tint     = text.copy(.5f),
                        modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
