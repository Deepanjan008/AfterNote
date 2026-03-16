package com.afternote.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.app.data.model.Note
import com.afternote.app.data.model.NoteColor
import com.afternote.app.ui.components.ColorPickerSlider
import com.afternote.app.ui.components.parseColorHex
import com.afternote.app.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    vm: NoteViewModel = hiltViewModel()
) {
    LaunchedEffect(noteId) { vm.loadNote(noteId) }

    val stored by vm.currentNote.collectAsStateWithLifecycle()

    var title   by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(NoteColor.DEFAULT.hex) }

    // Sync with loaded note once
    LaunchedEffect(stored) {
        stored?.let {
            title    = it.title
            content  = it.content
            colorHex = it.colorHex
        }
    }

    val bg = parseColorHex(colorHex)
    val isDark = bg.luminance() < 0.5f
    val contentColor = if (isDark) Color.White else Color.Black

    fun save() {
        val note = (stored ?: Note()).copy(
            title     = title,
            content   = content,
            colorHex  = colorHex,
            updatedAt = System.currentTimeMillis()
        )
        vm.saveNote(note)
        onBack()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(bg)) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { save() }) {
                            Icon(Icons.Default.ArrowBack, "Back",
                                tint = contentColor)
                        }
                    },
                    title = {},
                    actions = {
                        IconButton(onClick = { save() }) {
                            Icon(Icons.Default.Check, "Save",
                                tint = contentColor)
                        }
                        stored?.let { n ->
                            if (n.id != 0L) {
                                IconButton(onClick = {
                                    vm.deleteNote(n); onBack()
                                }) {
                                    Icon(Icons.Default.Delete, "Delete",
                                        tint = contentColor)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = bg
                    )
                )
                // Colour slider header
                ColorPickerSlider(
                    selectedHex     = colorHex,
                    onColorSelected = { colorHex = it },
                    modifier        = Modifier.background(bg)
                )
            }
        },
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title field
            TextField(
                value         = title,
                onValueChange = { title = it },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("Title", style = MaterialTheme.typography.headlineSmall,
                    color = contentColor.copy(.5f)) },
                textStyle     = MaterialTheme.typography.headlineSmall.copy(color = contentColor),
                colors        = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent
                )
            )
            // Content field
            TextField(
                value         = content,
                onValueChange = { content = it },
                modifier      = Modifier.fillMaxWidth().weight(1f, fill = false)
                    .defaultMinSize(minHeight = 400.dp),
                placeholder   = { Text("Note…", color = contentColor.copy(.4f)) },
                textStyle     = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
                colors        = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent
                )
            )
        }
    }
}
