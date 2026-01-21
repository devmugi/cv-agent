package io.github.devmugi.cv.agent.ui.components

import io.github.devmugi.cv.agent.domain.models.CVReference
import kotlin.test.Test
import kotlin.test.assertEquals

class ReferenceChipTest {

    @Test
    fun referenceChipDisplaysTypeOnly() {
        val reference = CVReference(id = "kotlin", type = "Skill", label = "Kotlin")
        val display = formatReferenceChipText(reference)
        assertEquals("Skill: Kotlin", display)
    }

    @Test
    fun referenceChipDisplaysExperienceWithCompany() {
        val reference = CVReference(id = "takeaway", type = "Experience", label = "Takeaway")
        val display = formatReferenceChipText(reference)
        assertEquals("Experience: Takeaway", display)
    }

    @Test
    fun referenceChipWithEmptyLabelShowsTypeOnly() {
        val reference = CVReference(id = "summary", type = "Summary", label = "")
        val display = formatReferenceChipText(reference)
        assertEquals("Summary", display)
    }
}
