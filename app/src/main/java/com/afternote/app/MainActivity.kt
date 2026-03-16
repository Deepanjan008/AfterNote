package com.afternote.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.app.security.TamperDetector
import com.afternote.app.ui.navigation.AppNavigation
import com.afternote.app.ui.theme.AfterNoteTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tamperDetector: TamperDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val report = tamperDetector.scan(this)

        setContent {
            AfterNoteTheme {
                var warningDismissed by remember { mutableStateOf(false) }

                if (report.isCritical && !warningDismissed) {
                    TamperWarningDialog(
                        triggers  = report.triggers,
                        onContinue= { warningDismissed = true },
                        onWipe    = { finishAndRemoveTask() }
                    )
                } else {
                    AppNavigation(
                        onSecureModeChanged = { secure ->
                            if (secure) {
                                window.setFlags(
                                    WindowManager.LayoutParams.FLAG_SECURE,
                                    WindowManager.LayoutParams.FLAG_SECURE
                                )
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TamperWarningDialog(
    triggers: List<String>,
    onContinue: () -> Unit,
    onWipe: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        icon  = { Icon(Icons.Default.Warning, null,
            tint = MaterialTheme.colorScheme.error) },
        title = { Text("Security Alert", color = MaterialTheme.colorScheme.error) },
        text  = {
            Column {
                Text("Suspicious environment detected:")
                Spacer(Modifier.height(8.dp))
                triggers.forEach { t ->
                    Text("• $t",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vault access is potentially unsafe. " +
                    "Your notes are protected by AES-256-GCM but runtime hooking " +
                    "may expose decrypted data in memory.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) { Text("Continue anyway") }
        },
        dismissButton = {
            Button(onClick = onWipe,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Exit App") }
        }
    )
}
