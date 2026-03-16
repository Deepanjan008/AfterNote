package com.afternote.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.app.data.model.Note
import com.afternote.app.ui.components.DrawerContent
import com.afternote.app.ui.components.NoteCard
import com.afternote.app.viewmodel.NoteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    onOpenArchive: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: NoteViewModel = hiltViewModel()
) {
    val notes       by vm.notes.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val drawerState  = rememberDrawerState(DrawerValue.Closed)
    val scope        = rememberCoroutineScope()
    val haptic       = LocalHapticFeedback.current

    var showFabLabel by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var showNoteMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            DrawerContent(
                currentRoute  = "main",
                onNavigate    = { route ->
                    when (route) {
                        "archive"  -> { scope.launch { drawerState.close() }; onOpenArchive() }
                        "settings" -> { scope.launch { drawerState.close() }; onOpenSettings() }
                    }
                },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text("AfterNote",
                                style = MaterialTheme.typography.titleLarge
                                    .copy(fontWeight = FontWeight.Bold))
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Outlined.Settings, "Settings")
                            }
                        }
                    )
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { vm.setSearchQuery(it) },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        placeholder   = { Text("Search notes…") },
                        leadingIcon   = { Icon(Icons.Default.Search, null) },
                        trailingIcon  = if (searchQuery.isNotEmpty()) {{
                            IconButton(onClick = { vm.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }} else null,
                        singleLine    = true,
                        shape         = MaterialTheme.shapes.extraLarge
                    )
                }
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (showFabLabel) {
                        Surface(
                            shape    = MaterialTheme.shapes.small,
                            color    = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text     = "Add Note",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style    = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick        = onNewNote,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary,
                        modifier       = Modifier.combinedClickable(
                            onClick    = onNewNote,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showFabLabel = !showFabLabel
                            }
                        )
                    ) {
                        Icon(Icons.Default.Add, "Add Note")
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Start
        ) { padding ->
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.NoteAdd, null,
                            modifier = Modifier.size(72.dp),
                            tint     = MaterialTheme.colorScheme.onSurface.copy(.3f))
                        Spacer(Modifier.height(16.dp))
                        Text("No notes yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                        Text("Tap + to create your first note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(.4f))
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
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note        = note,
                            onClick     = { onOpenNote(note.id) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedNote = note
                                showNoteMenu = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showNoteMenu && selectedNote != null) {
        AlertDialog(
            onDismissRequest = { showNoteMenu = false },
            title = { Text(selectedNote!!.title.ifBlank { "Untitled" }, maxLines = 1) },
            text  = {
                Column {
                    TextButton(
                        onClick  = { vm.pinNote(selectedNote!!); showNoteMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (selectedNote!!.isPinned) Icons.Default.PushPin
                             else Icons.Outlined.PushPin, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedNote!!.isPinned) "Unpin" else "Pin")
                    }
                    TextButton(
                        onClick  = { vm.archiveNote(selectedNote!!); showNoteMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Archive, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Archive")
                    }
                    TextButton(
                        onClick  = { vm.deleteNote(selectedNote!!); showNoteMenu = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Delete, null,
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton  = {},
            dismissButton  = {
                TextButton(onClick = { showNoteMenu = false }) { Text("Cancel") }
            }
        )
    }
}
