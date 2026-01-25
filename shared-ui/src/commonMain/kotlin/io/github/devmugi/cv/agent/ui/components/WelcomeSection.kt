package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import io.github.devmugi.arcane.design.components.feedback.ArcaneEmptyState
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeSection(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ArcaneEmptyState(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("welcome_section")
    ) {
        Text(
            text = "Welcome!",
            style = ArcaneTheme.typography.displaySmall,
            color = ArcaneTheme.colors.text
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "I'm Denys's AI assistant. Ask me anything about his professional experience, skills, or projects.",
            style = ArcaneTheme.typography.bodyLarge,
            color = ArcaneTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        SuggestionChipsGrid(
            suggestions = suggestions,
            onSuggestionClick = onSuggestionClick
        )
    }
}
