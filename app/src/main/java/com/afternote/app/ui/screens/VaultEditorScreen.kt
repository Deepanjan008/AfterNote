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
import com.afternote.app.data.model.NoteColor
import com.afternote.app.ui.components.ColorPickerSlider
import com.afternote.app.ui.components.parseColorHex
import com.afternote.app.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultEditorScreen(
    noteId: Long,
    onBack: () -> Unit,
    vm: VaultViewModel = hiltViewModel()
) {
    LaunchedEffect(noteId) { vm.loadNote(noteId) }

    val stored by vm.currentDecrypted.collectAsStateWithLifecycle()

    var title    by remember { mutableStateOf("") }
    var content  by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(NoteColor.DEFAULT.hex) }

    LaunchedEffect(stored) {
        stored?.let {
            title    = it.title
            content  = it.content
            colorHex = it.colorHex
        }
    }

    val bg        = parseColorHex(colorHex)
    val isDark    = bg.luminance() < 0.5f
    val textColor = if (isDark) Color.White else Color.Black

    fun save() {
        vm.saveNote(title, content, colorHex, noteId)
        onBack()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(bg)) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { save() }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = textColor)
                        }
                    },
                    title   = {},
                    actions = {
                        IconButton(onClick = { save() }) {
                            Icon(Icons.Default.Check, "Save", tint = textColor)
                        }
                        if (noteId != -1L) {
                            IconButton(onClick = { vm.deleteNote(noteId); onBack() }) {
                                Icon(Icons.Default.Delete, "Delete", tint = textColor)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
                )
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
            TextField(
                value         = title,
                onValueChange = { title = it },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = {
                    Text("Title", style = MaterialTheme.typography.headlineSmall,
                        color = textColor.copy(.5f))
                },
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = textColor),
                colors    = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent
                )
            )
            TextField(
                value         = content,
                onValueChange = { content = it },
                modifier      = Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = 400.dp),
                placeholder   = { Text("Vault note…", color = textColor.copy(.4f)) },
                textStyle     = MaterialTheme.typography.bodyLarge.copy(color = textColor),
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
