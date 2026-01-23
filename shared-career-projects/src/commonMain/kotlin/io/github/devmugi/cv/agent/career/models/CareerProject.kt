package io.github.devmugi.cv.agent.career.models

import kotlinx.serialization.Serializable

@Serializable
data class CareerProject(
    val id: String,
    val name: String,
    val slug: String,
    val tagline: String? = null,
    val hero: Hero? = null,
    val meta: Meta? = null,
    val overview: Overview? = null,
    val companies: List<Company>? = null,
    val description: Description? = null,
    val challenge: Challenge? = null,
    val achievements: List<Achievement>? = null,
    val standout: Standout? = null,
    val team: Team? = null,
    val metrics: Metrics? = null,
    val technologies: Technologies? = null,
    val lifecycle: Lifecycle? = null,
    val links: List<Link>? = null,
    val media: Media? = null,
    val seo: SEO? = null,
    val relatedProjects: List<String>? = null,
    val lastUpdated: String? = null
)

@Serializable
data class Hero(
    val bannerStyle: String? = null,
    val gradientColors: List<String>? = null,
    val icon: String? = null,
    val backgroundPattern: Boolean? = null
)

@Serializable
data class Meta(
    val featured: Boolean? = null,
    val status: String? = null,
    val visibility: String? = null
)

@Serializable
data class Overview(
    val company: String? = null,
    val client: String? = null,
    val product: String? = null,
    val role: String? = null,
    val roleDetails: String? = null,
    val period: Period? = null,
    val location: String? = null,
    val launchDate: String? = null,
    val productPrice: String? = null
)

@Serializable
data class Period(
    val startDate: String? = null,
    val endDate: String? = null,
    val displayText: String? = null,
    val durationMonths: Int? = null
)

@Serializable
data class Company(
    val name: String? = null,
    val role: String? = null,
    val description: String? = null,
    val logo: String? = null,
    val website: String? = null
)

@Serializable
data class Description(
    val short: String? = null,
    val full: String? = null,
    val howItWorked: List<HowItWorked>? = null
)

@Serializable
data class HowItWorked(
    val step: Int? = null,
    val title: String? = null,
    val description: String? = null
)

@Serializable
data class Challenge(
    val context: String? = null,
    val response: String? = null,
    val details: List<String>? = null
)

@Serializable
data class Achievement(
    val id: String? = null,
    val title: String? = null,
    val category: String? = null,
    val icon: String? = null,
    val problem: String? = null,
    val solution: String? = null,
    val details: List<String>? = null,
    val impact: Impact? = null,
    val highlight: Boolean? = null
)

@Serializable
data class Impact(
    val text: String? = null,
    val metric: String? = null,
    val metricType: String? = null
)

@Serializable
data class Standout(
    val title: String? = null,
    val items: List<StandoutItem>? = null
)

@Serializable
data class StandoutItem(
    val icon: String? = null,
    val title: String? = null,
    val description: String? = null
)

@Serializable
data class Team(
    val totalSize: Int? = null,
    val structure: List<TeamStructure>? = null,
    val methodology: String? = null,
    val collaboration: String? = null
)

@Serializable
data class TeamStructure(
    val team: String? = null,
    val size: kotlinx.serialization.json.JsonElement? = null,
    val notes: String? = null,
    val myRole: Boolean? = null
)

@Serializable
data class Metrics(
    val launch: List<MetricItem>? = null,
    val quickStats: List<MetricItem>? = null
)

@Serializable
data class MetricItem(
    val icon: String? = null,
    val value: String? = null,
    val label: String? = null,
    val highlight: Boolean? = null
)

@Serializable
data class Technologies(
    val primary: List<Technology>? = null,
    val secondary: List<SecondaryTechnology>? = null,
    val tools: List<String>? = null
)

@Serializable
data class Technology(
    val name: String? = null,
    val category: String? = null,
    val proficiency: String? = null
)

@Serializable
data class SecondaryTechnology(
    val name: String? = null,
    val category: String? = null
)

@Serializable
data class Lifecycle(
    val events: List<LifecycleEvent>? = null,
    val discontinuationReason: String? = null,
    val currentStatus: String? = null
)

@Serializable
data class LifecycleEvent(
    val date: String? = null,
    val displayDate: String? = null,
    val event: String? = null,
    val status: String? = null
)

@Serializable
data class Link(
    val type: String? = null,
    val icon: String? = null,
    val label: String? = null,
    val url: String? = null,
    val source: String? = null,
    val active: Boolean? = null
)

@Serializable
data class Media(
    val screenshots: List<String>? = null,
    val videos: List<MediaVideo>? = null,
    val logos: List<String>? = null
)

@Serializable
data class MediaVideo(
    val type: String? = null,
    val url: String? = null,
    val title: String? = null
)

@Serializable
data class SEO(
    val title: String? = null,
    val description: String? = null,
    val keywords: List<String>? = null
)

// ============================================
// ProjectDataTimeline - for timeline card view
// ============================================

@Serializable
data class ProjectDataTimeline(
    val id: String,
    val name: String,
    val slug: String,
    val hero: TimelineHero? = null,
    val featured: Boolean? = null,
    val featuredBadgeText: String? = null,
    val role: TimelineRole? = null,
    val period: TimelinePeriod? = null,
    val companies: List<TimelineCompany>? = null,
    val description: TimelineDescription? = null,
    val impactBadges: List<ImpactBadge>? = null,
    val tags: List<Tag>? = null,
    val achievements: TimelineAchievements? = null,
    val standout: TimelineStandout? = null,
    val detailsUrl: String? = null,
    val sortOrder: Int? = null,
    val timelinePosition: TimelinePosition? = null
)

@Serializable
data class TimelineHero(
    val bannerStyle: String? = null,
    val gradientColors: List<String>? = null,
    val icon: String? = null,
    val iconColor: String? = null
)

@Serializable
data class TimelineRole(
    val title: String? = null,
    val shortTitle: String? = null
)

@Serializable
data class TimelinePeriod(
    val startDate: String? = null,
    val endDate: String? = null,
    val displayText: String? = null
)

@Serializable
data class TimelineCompany(
    val name: String? = null,
    val role: String? = null,
    val logo: String? = null
)

@Serializable
data class TimelineDescription(
    val short: String? = null,
    val medium: String? = null
)

@Serializable
data class ImpactBadge(
    val icon: String? = null,
    val text: String? = null,
    val highlight: Boolean? = null
)

@Serializable
data class Tag(
    val text: String? = null,
    val category: String? = null
)

@Serializable
data class TimelineAchievements(
    val preview: List<String>? = null
)

@Serializable
data class TimelineStandout(
    val title: String? = null,
    val items: List<TimelineStandoutItem>? = null
)

@Serializable
data class TimelineStandoutItem(
    val icon: String? = null,
    val title: String? = null,
    val description: String? = null
)

@Serializable
data class TimelinePosition(
    val year: Int? = null,
    val quarter: Int? = null
)

// Extension function to convert CareerProject to ProjectDataTimeline
fun CareerProject.toProjectTimelineData(): ProjectDataTimeline {
    return ProjectDataTimeline(
        id = this.id,
        name = this.name,
        slug = this.slug,
        hero = this.hero?.let {
            TimelineHero(
                bannerStyle = it.bannerStyle,
                gradientColors = it.gradientColors,
                icon = it.icon,
                iconColor = "rgba(255, 255, 255, 0.2)"
            )
        },
        featured = this.meta?.featured,
        featuredBadgeText = if (this.meta?.featured == true) "Featured" else null,
        role = this.overview?.let {
            TimelineRole(
                title = it.role,
                shortTitle = it.roleDetails
            )
        },
        period = this.overview?.period?.let {
            TimelinePeriod(
                startDate = it.startDate,
                endDate = it.endDate,
                displayText = it.displayText
            )
        },
        companies = this.companies?.map {
            TimelineCompany(
                name = it.name,
                role = it.role,
                logo = it.logo
            )
        },
        description = this.description?.let {
            TimelineDescription(
                short = it.short,
                medium = it.full?.take(300)
            )
        },
        impactBadges = this.metrics?.launch?.filter { it.highlight == true }?.take(2)?.map {
            ImpactBadge(
                icon = it.icon,
                text = "${it.value} ${it.label}",
                highlight = true
            )
        },
        tags = buildList {
            this@toProjectTimelineData.technologies?.primary?.take(6)?.forEach { tech ->
                add(Tag(text = tech.name, category = tech.category))
            }
        },
        achievements = this.achievements?.filter { it.highlight == true }?.let { highlights ->
            TimelineAchievements(
                preview = highlights.mapNotNull { it.solution }
            )
        },
        standout = this.standout?.let {
            TimelineStandout(
                title = it.title,
                items = it.items?.map { item ->
                    TimelineStandoutItem(
                        icon = item.icon,
                        title = item.title,
                        description = item.description
                    )
                }
            )
        },
        detailsUrl = "projects/${this.slug}.html",
        sortOrder = null,
        timelinePosition = this.overview?.period?.startDate?.let { startDate ->
            if (startDate.length >= 7) {
                val year = startDate.take(4).toIntOrNull()
                val month = startDate.substring(5, 7).toIntOrNull()
                val quarter = month?.let { ((it - 1) / 3) + 1 }
                if (year != null) TimelinePosition(year = year, quarter = quarter) else null
            } else null
        }
    )
}
