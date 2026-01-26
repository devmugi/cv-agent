package io.github.devmugi.cv.agent.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import io.github.devmugi.arcane.design.components.controls.ArcaneButtonSize
import io.github.devmugi.arcane.design.components.controls.ArcaneButtonStyle
import io.github.devmugi.arcane.design.components.controls.ArcaneTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import compose.icons.SimpleIcons
import compose.icons.simpleicons.Github
import compose.icons.simpleicons.Linkedin
import io.github.devmugi.arcane.design.foundation.primitives.ArcaneSurface
import io.github.devmugi.arcane.design.foundation.theme.ArcaneTheme
import io.github.devmugi.cv.agent.analytics.Analytics
import io.github.devmugi.cv.agent.analytics.AnalyticsEvent

fun buildTopBarTitle(): String = "<DH/> Denys Honcharenko CV"

private val LinkedInBlue = Color(0xFF0A66C2)

@Composable
fun CVAgentTopBar(
    onCareerClick: () -> Unit = {},
    showContactBanner: Boolean = true,
    analytics: Analytics = Analytics.NOOP
) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxWidth()) {
        ArcaneSurface(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            showBorder = false
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "<DH/>",
                    style = ArcaneTheme.typography.headlineLarge,
                    color = ArcaneTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Denys Honcharenko CV",
                    style = ArcaneTheme.typography.headlineLarge,
                    color = ArcaneTheme.colors.text
                )
                Spacer(modifier = Modifier.weight(1f))
                ArcaneTextButton(
                    text = "Career",
                    onClick = {
                        analytics.logEvent(
                            AnalyticsEvent.Navigation.ScreenView(
                                screenName = AnalyticsEvent.Navigation.Screen.CAREER_TIMELINE,
                                previousScreen = AnalyticsEvent.Navigation.Screen.CHAT
                            )
                        )
                        onCareerClick()
                    },
                    style = ArcaneButtonStyle.Outlined(),
                    size = ArcaneButtonSize.Medium
                )
            }
        }

        AnimatedVisibility(
            visible = showContactBanner,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ContactBanner(
                onLinkedInClick = {
                    uriHandler.openUri("https://www.linkedin.com/in/denyshoncharenko/")
                },
                onGitHubClick = {
                    uriHandler.openUri("https://github.com/devmugi")
                },
                onEmailClick = {
                    uriHandler.openUri("mailto:aidevmugi@gmail.com")
                },
                onPhoneClick = {
                    uriHandler.openUri("tel:+32470383388")
                },
                onCVClick = {
                    uriHandler.openUri("https://devmugi.github.io/devmugi/")
                },
                onPdfClick = {
                    val pdfUrl = "https://raw.githubusercontent.com/devmugi/devmugi/" +
                        "main/cv/Denys%20Honcharenko%20CV.pdf"
                    uriHandler.openUri(pdfUrl)
                },
                analytics = analytics
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ArcaneTheme.colors.surfaceContainerLow.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun ContactBanner(
    onLinkedInClick: () -> Unit,
    onGitHubClick: () -> Unit,
    onEmailClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onCVClick: () -> Unit,
    onPdfClick: () -> Unit,
    analytics: Analytics,
    modifier: Modifier = Modifier
) {
    fun logAndOpen(linkType: AnalyticsEvent.Link.LinkType, url: String, action: () -> Unit) {
        analytics.logEvent(
            AnalyticsEvent.Link.ExternalLinkClicked(
                linkType = linkType,
                url = url
            )
        )
        action()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "\uD83D\uDFE2 Open to Work \u00B7 Belgium, Remote",
            style = ArcaneTheme.typography.titleSmall,
            color = ArcaneTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactIconButton(
                icon = SimpleIcons.Linkedin,
                contentDescription = "LinkedIn",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.LINKEDIN, "linkedin.com/in/denyshoncharenko") {
                        onLinkedInClick()
                    }
                },
                tint = LinkedInBlue
            )
            ContactIconButton(
                icon = SimpleIcons.Github,
                contentDescription = "GitHub",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.GITHUB, "github.com/devmugi") {
                        onGitHubClick()
                    }
                }
            )
            ContactIconButton(
                icon = Icons.Filled.Email,
                contentDescription = "Email",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.EMAIL, "aidevmugi@gmail.com") {
                        onEmailClick()
                    }
                }
            )
            ContactIconButton(
                icon = Icons.Filled.Phone,
                contentDescription = "Phone",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.PHONE, "+32470383388") {
                        onPhoneClick()
                    }
                }
            )
            ContactIconButton(
                icon = Icons.Filled.Language,
                contentDescription = "Website",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.CV_WEBSITE, "devmugi.github.io") {
                        onCVClick()
                    }
                }
            )
            ContactIconButton(
                icon = Icons.Filled.PictureAsPdf,
                contentDescription = "PDF CV",
                onClick = {
                    logAndOpen(AnalyticsEvent.Link.LinkType.CV_PDF, "cv.pdf") {
                        onPdfClick()
                    }
                }
            )
        }
    }
}

@Composable
private fun ContactIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ArcaneTheme.colors.textSecondary
) {
    val interactionSource = remember { MutableInteractionSource() }

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(24.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 20.dp),
                onClick = onClick
            )
    )
}
