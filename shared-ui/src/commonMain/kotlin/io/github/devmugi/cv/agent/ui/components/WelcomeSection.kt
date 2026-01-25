package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
        horizontalAlignment = Alignment.CenterHorizontally
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

            Spacer(modifier = Modifier.height(24.dp))
        }

        itemsIndexed(suggestions) { index, topic ->
            TopicListItem(
                text = topic,
                onClick = { onSuggestionClick(topic) },
                modifier = Modifier.testTag("topic_item_$index")
            )
        }

        item {
            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

@Composable
private fun TopicListItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = ArcaneTheme.typography.bodyLarge,
            color = ArcaneTheme.colors.primary
        )
    }
}
