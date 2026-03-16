package com.afternote.app.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.afternote.app.ui.screens.*
import com.afternote.app.ui.theme.AfterNoteTheme
import com.afternote.app.viewmodel.SettingsViewModel

@Composable
fun AppNavigation(
    onSecureModeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val isDark    by settingsVm.isDarkMode.collectAsStateWithLifecycle()
    val themeIndex by settingsVm.themeIndex.collectAsStateWithLifecycle()

    AfterNoteTheme(themeIndex = themeIndex, darkMode = isDark) {
        NavHost(navController = navController, startDestination = NavRoutes.MAIN) {

            composable(NavRoutes.MAIN) {
                LaunchedEffect(Unit) { onSecureModeChanged(false) }
                MainScreen(
                    onOpenNote     = { id -> navController.navigate(NavRoutes.editor(id)) },
                    onNewNote      = { navController.navigate(NavRoutes.editor()) },
                    onOpenArchive  = { navController.navigate(NavRoutes.ARCHIVE) },
                    onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) }
                )
            }

            composable(
                NavRoutes.EDITOR,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { back ->
                val noteId = back.arguments?.getLong("noteId") ?: -1L
                LaunchedEffect(Unit) { onSecureModeChanged(true) }
                NoteEditorScreen(
                    noteId = noteId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.ARCHIVE) {
                LaunchedEffect(Unit) { onSecureModeChanged(false) }
                ArchiveScreen(
                    onBack     = { navController.popBackStack() },
                    onOpenNote = { id -> navController.navigate(NavRoutes.editor(id)) }
                )
            }

            composable(NavRoutes.SETTINGS) {
                LaunchedEffect(Unit) { onSecureModeChanged(false) }
                SettingsScreen(
                    onBack       = { navController.popBackStack() },
                    onOpenVault  = { navController.navigate(NavRoutes.VAULT) },
                    onOpenBackup = { navController.navigate(NavRoutes.BACKUP) }
                )
            }

            composable(NavRoutes.VAULT) {
                LaunchedEffect(Unit) { onSecureModeChanged(true) }
                VaultScreen(
                    onBack     = { navController.popBackStack() },
                    onOpenNote = { id -> navController.navigate(NavRoutes.vaultEditor(id)) },
                    onNewNote  = { navController.navigate(NavRoutes.vaultEditor()) }
                )
            }

            composable(
                NavRoutes.VAULT_EDITOR,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { back ->
                val noteId = back.arguments?.getLong("noteId") ?: -1L
                LaunchedEffect(Unit) { onSecureModeChanged(true) }
                VaultEditorScreen(
                    noteId = noteId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.BACKUP) {
                LaunchedEffect(Unit) { onSecureModeChanged(false) }
                BackupScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
