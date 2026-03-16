package com.afternote.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.app.data.model.Note
import com.afternote.app.ui.components.NoteCard
import com.afternote.app.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    vm: NoteViewModel = hiltViewModel()
) {
    val notes by vm.archivedNotes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Archive is empty", color = MaterialTheme.colorScheme.onSurface.copy(.5f))
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns           = StaggeredGridCells.Fixed(2),
                modifier          = Modifier.fillMaxSize().padding(padding),
                contentPadding    = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing   = 8.dp
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note        = note,
                        onClick     = { onOpenNote(note.id) },
                        onLongClick = { vm.archiveNote(note) } // long-press to un-archive
                    )
                }
            }
        }
    }
}
