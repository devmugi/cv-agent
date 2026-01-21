package io.github.devmugi.cv.agent.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SuggestionChipsGridTest {

    @Test
    fun gridLayoutReturnsCorrectRowCount() {
        val suggestions = listOf("A", "B", "C", "D")
        val rows = calculateGridRows(suggestions, columnsPerRow = 2)
        assertEquals(2, rows)
    }

    @Test
    fun gridLayoutHandlesOddCount() {
        val suggestions = listOf("A", "B", "C")
        val rows = calculateGridRows(suggestions, columnsPerRow = 2)
        assertEquals(2, rows) // 3 items = 2 rows with 2 columns
    }

    @Test
    fun emptyListReturnsZeroRows() {
        val rows = calculateGridRows(emptyList(), columnsPerRow = 2)
        assertEquals(0, rows)
    }
}
