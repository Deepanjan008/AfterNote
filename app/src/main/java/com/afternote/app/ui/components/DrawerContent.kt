package com.afternote.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawerContent(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
        Spacer(Modifier.height(24.dp))
        // App header
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.EditNote, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(10.dp))
            Text("AfterNote",
                style      = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold, fontSize = 22.sp),
                color      = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        DrawerItem(
            icon      = Icons.Outlined.Notes,
            label     = "Notes",
            selected  = currentRoute == "main",
            onClick   = { onNavigate("main"); onCloseDrawer() }
        )
        DrawerItem(
            icon      = Icons.Outlined.Archive,
            label     = "Archive",
            selected  = currentRoute == "archive",
            onClick   = { onNavigate("archive"); onCloseDrawer() }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DrawerItem(
            icon      = Icons.Outlined.Settings,
            label     = "Settings",
            selected  = currentRoute == "settings",
            onClick   = { onNavigate("settings"); onCloseDrawer() }
        )
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon    = { Icon(icon, contentDescription = label) },
        label   = { Text(label) },
        selected= selected,
        onClick = onClick,
        modifier= Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
