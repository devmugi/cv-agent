package io.github.devmugi.cv.agent.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CVAgentShapes = Shapes(
    small = RoundedCornerShape(8.dp),      // Chips
    medium = RoundedCornerShape(16.dp),    // Cards, message bubbles
    large = RoundedCornerShape(24.dp)      // Dialogs, sheets
)
