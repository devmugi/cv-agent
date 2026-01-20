package io.github.devmugi.cv.agent.ui.components

import kotlin.test.Test
import kotlin.test.assertTrue

class TopBarTest {

    @Test
    fun topBarTitleContainsBrandName() {
        // TopBar should display "<DH/> CV Agent"
        val title = buildTopBarTitle()
        assertTrue(title.contains("<DH/>"))
        assertTrue(title.contains("CV Agent"))
    }
}
