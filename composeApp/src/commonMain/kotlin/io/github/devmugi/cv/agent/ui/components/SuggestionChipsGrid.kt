package io.github.devmugi.cv.agent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun calculateGridRows(items: List<String>, columnsPerRow: Int): Int {
    if (items.isEmpty() || columnsPerRow <= 0) return 0
    return (items.size + columnsPerRow - 1) / columnsPerRow
}

@Composable
fun SuggestionChipsGrid(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally)
            ) {
                rowItems.forEach { suggestion ->
                    SuggestionChip(
                        text = suggestion,
                        onClick = onSuggestionClick
                    )
                }
            }
        }
    }
}
