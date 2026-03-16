package com.afternote.app.ui.navigation

object NavRoutes {
    const val MAIN          = "main"
    const val EDITOR        = "editor/{noteId}"
    const val ARCHIVE       = "archive"
    const val SETTINGS      = "settings"
    const val VAULT         = "vault"
    const val VAULT_EDITOR  = "vault_editor/{noteId}"
    const val BACKUP        = "backup"

    fun editor(id: Long = -1L)       = "editor/$id"
    fun vaultEditor(id: Long = -1L)  = "vault_editor/$id"
}
