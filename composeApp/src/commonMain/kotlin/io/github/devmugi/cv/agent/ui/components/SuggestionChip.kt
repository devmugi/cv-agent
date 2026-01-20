package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

fun simulateSuggestionChipClick(text: String, onClick: (String) -> Unit) {
    onClick(text)
}

fun getSuggestionChipDisplayText(text: String): String = text

@Composable
fun SuggestionChip(
    text: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Surface(
        onClick = { onClick(text) },
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
