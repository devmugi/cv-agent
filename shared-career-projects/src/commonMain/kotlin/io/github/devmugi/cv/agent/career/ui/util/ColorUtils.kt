package io.github.devmugi.cv.agent.career.ui.util

import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    return try {
        val colorLong = cleanHex.toLong(16)
        when (cleanHex.length) {
            6 -> Color(0xFF000000 or colorLong)
            8 -> Color(colorLong)
            else -> Color.Gray
        }
    } catch (e: NumberFormatException) {
        Color.Gray
    }
}
