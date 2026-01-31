package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.Course
import io.github.devmugi.cv.agent.career.theme.CareerColors

private val GreenColor = Color(0xFF4CAF50)

@Composable
fun CoursesSection(
    courses: List<Course>,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.School,
            title = "Course Iterations"
        )

        Spacer(modifier = Modifier.height(12.dp))

        courses.forEachIndexed { index, course ->
            CourseCard(
                course = course,
                onLinkClick = onLinkClick
            )
            if (index < courses.lastIndex) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseCard(
    course: Course,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ArcaneSurface(
        variant = SurfaceVariant.Container,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Course header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    course.name?.let { name ->
                        Text(
                            text = name,
                            style = ArcaneTheme.typography.labelLarge,
                            color = ArcaneTheme.colors.text,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    course.role?.let { role ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = role,
                            style = ArcaneTheme.typography.bodyMedium,
                            color = CareerColors.Amber
                        )
                    }
                }
                course.period?.let { period ->
                    Text(
                        text = period,
                        style = ArcaneTheme.typography.labelSmall,
                        color = ArcaneTheme.colors.textSecondary
                    )
                }
            }

            // Description
            course.description?.let { desc ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = desc,
                    style = ArcaneTheme.typography.bodyMedium,
                    color = ArcaneTheme.colors.textSecondary
                )
            }

            // Example project badge
            course.exampleProject?.let { project ->
                Spacer(modifier = Modifier.height(12.dp))
                ExampleProjectBadge(
                    name = project.name ?: "Example Project",
                    description = project.description,
                    url = project.url,
                    onLinkClick = onLinkClick
                )
            }

            // Stats row
            val enrolled = course.students?.enrolled
            val hired = course.students?.hired
            val hiringRate = course.hiringRate

            if (enrolled != null || hired != null || hiringRate != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    enrolled?.let {
                        StatItem(
                            value = it.toString(),
                            label = "Enrolled",
                            icon = Icons.Default.Groups
                        )
                    }
                    hired?.let {
                        StatItem(
                            value = it.toString(),
                            label = "Hired",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            highlight = true
                        )
                    }
                    hiringRate?.let {
                        StatItem(
                            value = it,
                            label = "Success Rate",
                            icon = Icons.Default.School,
                            highlight = true
                        )
                    }
                }
            }

            // Topics
            course.topics?.takeIf { it.isNotEmpty() }?.let { topics ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Topics Covered",
                    style = ArcaneTheme.typography.labelSmall,
                    color = ArcaneTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    topics.forEach { topic ->
                        TopicChip(text = topic)
                    }
                }
            }

            // Student Graduation Works
            val studentsWithWorks = course.students?.list?.filter {
                !it.graduationWorkUrl.isNullOrBlank()
            }
            if (!studentsWithWorks.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Student Graduation Works",
                    style = ArcaneTheme.typography.labelSmall,
                    color = ArcaneTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    studentsWithWorks.forEach { student ->
                        StudentWorkBadge(
                            name = student.name ?: "Student",
                            url = student.graduationWorkUrl!!,
                            onLinkClick = onLinkClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExampleProjectBadge(
    name: String,
    description: String?,
    url: String?,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isClickable = !url.isNullOrBlank()
    Surface(
        modifier = modifier
            .then(
                if (isClickable) {
                    Modifier.clickable { onLinkClick(url!!) }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E3A5F),
        border = BorderStroke(1.dp, Color(0xFF64B5F6))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = Color(0xFF90CAF9),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = name,
                    style = ArcaneTheme.typography.labelSmall,
                    color = Color(0xFF90CAF9),
                    fontWeight = FontWeight.Medium
                )
                description?.let {
                    Text(
                        text = it,
                        style = ArcaneTheme.typography.labelSmall,
                        color = Color(0xFF90CAF9).copy(alpha = 0.7f)
                    )
                }
            }
            if (isClickable) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open link",
                    tint = Color(0xFF90CAF9),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun StudentWorkBadge(
    name: String,
    url: String,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onLinkClick(url) },
        shape = RoundedCornerShape(8.dp),
        color = GreenColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, GreenColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = ArcaneTheme.typography.labelSmall,
                color = GreenColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open project",
                tint = GreenColor,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (highlight) GreenColor.copy(alpha = 0.2f) else ArcaneTheme.colors.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) GreenColor else ArcaneTheme.colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
        Column {
            Text(
                text = value,
                style = ArcaneTheme.typography.labelLarge,
                color = if (highlight) GreenColor else ArcaneTheme.colors.text,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = ArcaneTheme.typography.labelSmall,
                color = ArcaneTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun TopicChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ArcaneTheme.colors.surfaceContainerLow
    ) {
        Text(
            text = text,
            style = ArcaneTheme.typography.labelSmall,
            color = ArcaneTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
