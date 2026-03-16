package com.afternote.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.app.data.model.Note
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg    = parseHex(note.colorHex)
    val isDark= bg.luminance() < 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = bg),
        elevation= CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (note.isPinned) {
                Icon(
                    imageVector   = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint          = if (isDark) Color.White.copy(alpha = .7f)
                                    else MaterialTheme.colorScheme.primary,
                    modifier      = Modifier.size(16.dp).align(Alignment.End)
                )
            }
            if (note.title.isNotBlank()) {
                Text(
                    text       = note.title,
                    style      = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isDark) Color.White else Color.Black
                    ),
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            if (note.content.isNotBlank()) {
                Text(
                    text     = note.content,
                    style    = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color.White.copy(.85f) else Color.DarkGray
                    ),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text  = SimpleDateFormat("MMM d", Locale.getDefault())
                            .format(Date(note.updatedAt)),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isDark) Color.White.copy(.5f) else Color.Gray
                )
            )
        }
    }
}

private fun parseHex(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrElse { Color.White }
