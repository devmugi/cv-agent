package io.github.devmugi.cv.agent.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.devmugi.arcane.design.components.controls.ArcaneButton
import io.github.devmugi.arcane.design.components.controls.ArcaneButtonStyle
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

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
    ArcaneButton(
        onClick = { onClick(text) },
        style = ArcaneButtonStyle.Outlined(),
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Text(
            text = text,
            style = ArcaneTheme.typography.bodyMedium
        )
    }
}
