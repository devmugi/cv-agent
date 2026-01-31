package io.github.devmugi.cv.agent.ui.screenshots

import io.github.devmugi.cv.agent.ui.components.CVAgentTopBar
import org.junit.Test

/**
 * Screenshot tests for [CVAgentTopBar] component.
 *
 * Captures visual snapshots of the top bar with and without the contact banner,
 * verifying consistent appearance across light and dark themes.
 */
class TopBarScreenshots : ScreenshotTest() {

    @Test
    fun topbar_with_contact_banner() = snapshotBothThemes("with_contact_banner") {
        CVAgentTopBar(
            onCareerClick = {},
            showContactBanner = true
        )
    }

    @Test
    fun topbar_without_contact_banner() = snapshotBothThemes("without_contact_banner") {
        CVAgentTopBar(
            onCareerClick = {},
            showContactBanner = false
        )
    }
}
