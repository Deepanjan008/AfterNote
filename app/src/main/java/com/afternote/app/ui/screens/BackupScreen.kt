package com.afternote.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.app.backup.BackupManager
import com.afternote.app.viewmodel.SettingsEvent
import com.afternote.app.viewmodel.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    settingsVm: SettingsViewModel   = hiltViewModel(),
    backupVm: BackupScreenVm        = hiltViewModel()
) {
    val scope  = rememberCoroutineScope()
    val keySet by settingsVm.isRecoveryKeySet.collectAsStateWithLifecycle()

    var recoveryKeyInput  by remember { mutableStateOf("") }
    var keyVisible        by remember { mutableStateOf(false) }
    var statusMessage     by remember { mutableStateOf<String?>(null) }
    var isLoading         by remember { mutableStateOf(false) }
    var recoveryKeyDialog by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isLoading = true
            val result = backupVm.backupManager.exportToUri(uri, recoveryKeyInput.trim())
            isLoading = false
            statusMessage = if (result.isSuccess) "✅ Backup exported!"
                            else "❌ Export failed: ${result.exceptionOrNull()?.message}"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isLoading = true
            val result = backupVm.backupManager.importFromUri(uri, recoveryKeyInput.trim())
            isLoading = false
            statusMessage = if (result.isSuccess) {
                val s = result.getOrNull()
                "✅ Restored ${s?.notesRestored} notes, ${s?.vaultNotesRestored} vault notes."
            } else "❌ Import failed: ${result.exceptionOrNull()?.message}"
        }
    }

    LaunchedEffect(Unit) {
        settingsVm.event.collectLatest { event ->
            if (event is SettingsEvent.ShowRecoveryKey) recoveryKeyDialog = event.key
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encrypted Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("AES-256-GCM Offline Encryption",
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your backup is encrypted with AES-256-GCM using your Recovery Key. " +
                        "Without the key, the .emb file can NEVER be decrypted — not even by the developers. " +
                        "No internet connection is used at any point.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!keySet) {
                OutlinedButton(
                    onClick  = { settingsVm.generateAndStoreRecoveryKey() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Key, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate My Recovery Key")
                }
            } else {
                Text("Recovery Key", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value         = recoveryKeyInput,
                    onValueChange = { recoveryKeyInput = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Enter Recovery Key") },
                    placeholder   = { Text("XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX-…") },
                    trailingIcon  = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(if (keyVisible) Icons.Default.VisibilityOff
                                 else Icons.Default.Visibility, null)
                        }
                    },
                    visualTransformation = if (keyVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    textStyle  = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick  = {
                        if (recoveryKeyInput.isBlank()) statusMessage = "⚠ Enter Recovery Key first."
                        else exportLauncher.launch("afternote_backup.emb")
                    },
                    modifier = Modifier.weight(1f),
                    enabled  = keySet && !isLoading
                ) {
                    Icon(Icons.Outlined.Upload, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Export .emb")
                }
                OutlinedButton(
                    onClick  = {
                        if (recoveryKeyInput.isBlank()) statusMessage = "⚠ Enter Recovery Key first."
                        else importLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.weight(1f),
                    enabled  = !isLoading
                ) {
                    Icon(Icons.Outlined.Download, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Import .emb")
                }
            }

            if (isLoading) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }

            statusMessage?.let { msg ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = if (msg.startsWith("✅"))
                        MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )) {
                    Text(msg, modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    recoveryKeyDialog?.let { key ->
        AlertDialog(
            onDismissRequest = { recoveryKeyDialog = null },
            icon  = { Icon(Icons.Default.Key, null) },
            title = { Text("Your Recovery Key", fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text("⚠ WRITE THIS DOWN NOW. It will never be shown again.",
                        color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium) {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(key, modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Without this key, your backup CANNOT be decrypted.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    recoveryKeyInput  = key
                    recoveryKeyDialog = null
                }) { Text("I've saved it — use now") }
            },
            dismissButton = {
                TextButton(onClick = { recoveryKeyDialog = null }) { Text("Close") }
            }
        )
    }
}

@HiltViewModel
class BackupScreenVm @Inject constructor(
    val backupManager: BackupManager
) : androidx.lifecycle.ViewModel()
