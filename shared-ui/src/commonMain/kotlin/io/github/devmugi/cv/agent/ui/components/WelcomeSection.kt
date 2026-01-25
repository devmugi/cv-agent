package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme

@Composable
fun WelcomeSection(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag("welcome_section"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
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

            Spacer(modifier = Modifier.height(16.dp))
        }

        itemsIndexed(suggestions) { index, topic ->
            SuggestionChip(
                text = topic,
                onClick = onSuggestionClick,
                testTag = "topic_item_$index"
            )
        }

        item {
            Spacer(modifier = Modifier.height(180.dp))
        }
    }
}
