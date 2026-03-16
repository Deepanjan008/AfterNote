package com.afternote.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.afternote.app.data.model.NoteColor

@Composable
fun ColorPickerSlider(
    selectedHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier            = modifier.fillMaxWidth(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment   = Alignment.CenterVertically
    ) {
        items(NoteColor.entries) { noteColor ->
            val bg      = parseColorHex(noteColor.hex)
            val selected= noteColor.hex.equals(selectedHex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) Color.Black else Color.Gray.copy(.4f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(noteColor.hex) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = null,
                        tint               = if (bg.luminance() < .5f) Color.White else Color.Black,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

internal fun parseColorHex(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrElse { Color.White }
