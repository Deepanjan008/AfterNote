package com.afternote.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.app.security.BiometricHelper
import com.afternote.app.security.BiometricResult
import com.afternote.app.ui.theme.themeOptions
import com.afternote.app.viewmodel.SettingsEvent
import com.afternote.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenBackup: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val isDark     by vm.isDarkMode.collectAsStateWithLifecycle()
    val appLock    by vm.isAppLockEnabled.collectAsStateWithLifecycle()
    val themeIndex by vm.themeIndex.collectAsStateWithLifecycle()

    val context   = LocalContext.current as FragmentActivity
    val haptic    = LocalHapticFeedback.current
    val biometric = remember { BiometricHelper() }

    var vaultRevealed    by remember { mutableStateOf(false) }
    var recoveryKeyDialog by remember { mutableStateOf<String?>(null) }
    var showSelfDestruct  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.event.collectLatest { event ->
            when (event) {
                is SettingsEvent.ShowRecoveryKey  -> recoveryKeyDialog = event.key
                is SettingsEvent.SelfDestructComplete -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Appearance ────────────────────────────────────────────────────
            SectionHeader("Appearance")

            ListItem(
                headlineContent = { Text(if (isDark) "Dark Mode" else "Light Mode") },
                leadingContent  = {
                    Icon(if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode, null)
                },
                trailingContent = {
                    Switch(checked = isDark, onCheckedChange = { vm.toggleDarkMode() })
                }
            )

            ListItem(
                headlineContent  = { Text("Theme") },
                supportingContent= { Text(themeOptions.getOrElse(themeIndex) { "Indigo" }) },
                leadingContent   = { Icon(Icons.Outlined.Palette, null) }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themeOptions.forEachIndexed { idx, name ->
                    FilterChip(
                        selected = themeIndex == idx,
                        onClick  = { vm.setTheme(idx) },
                        label    = { Text(name) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Security ──────────────────────────────────────────────────────
            SectionHeader("Security")

            ListItem(
                headlineContent = { Text("App Lock (Biometric / PIN)") },
                leadingContent  = { Icon(Icons.Default.Lock, null) },
                trailingContent = {
                    Switch(checked = appLock, onCheckedChange = { vm.toggleAppLock() })
                }
            )

            ListItem(
                headlineContent  = { Text("Encrypted Backup") },
                supportingContent= { Text("Export / import .emb file") },
                leadingContent   = { Icon(Icons.Outlined.Backup, null) },
                modifier         = Modifier.clickable(onClick = onOpenBackup)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Advanced (hidden vault trigger) ───────────────────────────────
            Text(
                text     = "Advanced",
                style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    .combinedClickable(
                        onClick    = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vaultRevealed = true
                        }
                    )
            )

            AnimatedVisibility(visible = vaultRevealed) {
                Column {
                    ListItem(
                        headlineContent  = { Text("Secret Vault") },
                        supportingContent= { Text("Biometric-protected private notes") },
                        leadingContent   = { Icon(Icons.Default.Shield, null,
                            tint = MaterialTheme.colorScheme.primary) },
                        modifier         = Modifier.clickable {
                            biometric.authenticate(
                                activity = context,
                                title    = "Vault Access",
                                subtitle = "Authenticate to enter the Secret Vault"
                            ) { result ->
                                if (result is BiometricResult.Success) onOpenVault()
                            }
                        }
                    )
                    ListItem(
                        headlineContent  = { Text("Self-Destruct",
                            color = MaterialTheme.colorScheme.error) },
                        supportingContent= { Text("Permanently wipe ALL data") },
                        leadingContent   = { Icon(Icons.Default.DeleteForever, null,
                            tint = MaterialTheme.colorScheme.error) },
                        modifier         = Modifier.clickable { showSelfDestruct = true }
                    )
                }
            }
        }
    }

    // Recovery key dialog
    recoveryKeyDialog?.let { key ->
        AlertDialog(
            onDismissRequest = { recoveryKeyDialog = null },
            icon  = { Icon(Icons.Default.Key, null) },
            title = { Text("Your Recovery Key", fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text("Write this down NOW. It cannot be recovered.",
                        color = MaterialTheme.colorScheme.error)
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
                }
            },
            confirmButton = {
                TextButton(onClick = { recoveryKeyDialog = null }) { Text("I've saved it") }
            }
        )
    }

    // Self-destruct confirmation
    if (showSelfDestruct) {
        AlertDialog(
            onDismissRequest = { showSelfDestruct = false },
            icon  = { Icon(Icons.Default.Warning, null,
                tint = MaterialTheme.colorScheme.error) },
            title = { Text("Self-Destruct", color = MaterialTheme.colorScheme.error) },
            text  = {
                Text("This will permanently delete ALL notes, vault notes, and settings. This action is IRREVERSIBLE.")
            },
            confirmButton = {
                Button(
                    onClick = { showSelfDestruct = false; vm.selfDestruct() },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Wipe Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showSelfDestruct = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}

private fun Modifier.clickable(onClick: () -> Unit) =
    this.then(Modifier.combinedClickable(onClick = onClick))
