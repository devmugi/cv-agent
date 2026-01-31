package io.github.devmugi.cv.agent.career.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.primitives.SurfaceVariant
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.career.models.Link
import io.github.devmugi.cv.agent.career.theme.CareerColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinksSection(
    links: List<Link>,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier) {
        SectionHeader(
            icon = Icons.Default.Link,
            title = "Links"
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            links.filter { it.active == true }.forEach { link ->
                LinkCard(
                    icon = getLinkIcon(link.type),
                    label = link.label ?: "",
                    highlight = link.type == "document",
                    onClick = {
                        link.url?.let { url ->
                            try {
                                uriHandler.openUri(url)
                            } catch (_: Exception) {
                                // Handle URL open error silently
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LinkCard(
    icon: ImageVector,
    label: String,
    highlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (highlight) CareerColors.Amber else ArcaneTheme.colors.surfaceContainerLow

    ArcaneSurface(
        variant = SurfaceVariant.Container,
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CareerColors.Amber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = ArcaneTheme.typography.bodyMedium,
                color = ArcaneTheme.colors.text
            )
        }
    }
}

private fun getLinkIcon(linkType: String?): ImageVector {
    return when (linkType) {
        "company" -> Icons.Default.Business
        "document" -> Icons.Default.Description
        "article" -> Icons.Default.Article
        else -> Icons.Default.Link
    }
}
