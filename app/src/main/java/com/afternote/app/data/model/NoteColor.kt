package com.afternote.app.data.model

enum class NoteColor(val hex: String, val label: String) {
    DEFAULT  ("#FEFEFE", "Default"),
    CORAL    ("#FFCCBC", "Coral"),
    PEACH    ("#FFE0B2", "Peach"),
    BANANA   ("#FFF9C4", "Banana"),
    SAGE     ("#C8E6C9", "Sage"),
    TEAL     ("#B2DFDB", "Teal"),
    SKY      ("#BBDEFB", "Sky"),
    LAVENDER ("#E1BEE7", "Lavender"),
    PINK     ("#F8BBD0", "Pink"),
    GRAPHITE ("#CFD8DC", "Graphite"),
    MIDNIGHT ("#263238", "Midnight"),
    ESPRESSO ("#3E2723", "Espresso");

    companion object {
        fun fromHex(hex: String): NoteColor =
            entries.find { it.hex.equals(hex, ignoreCase = true) } ?: DEFAULT
    }
}
