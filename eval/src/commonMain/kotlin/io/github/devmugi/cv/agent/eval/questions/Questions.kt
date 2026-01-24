package io.github.devmugi.cv.agent.eval.questions

/**
 * Simple single-turn questions for evaluation.
 */
object SimpleQuestions {
    val questions = listOf(
        Question("Q1", "What is Denys's current job title and location?", "personal info recall"),
        Question("Q2", "What programming languages does Denys know?", "skills extraction"),
        Question("Q3", "Tell me about the McDonald's project", "featured project detail"),
        Question("Q4", "What was Denys's role at GEOSATIS?", "featured project role"),
        Question("Q5", "Has Denys worked with Kotlin Multiplatform?", "technology pattern matching"),
        Question("Q6", "What is the Lesara project about?", "non-featured project"),
        Question("Q7", "List all companies Denys has worked for", "cross-project aggregation"),
        Question("Q8", "What IoT experience does Denys have?", "skill and project correlation"),
        Question("Q9", "Tell me about the Adidas GMR smart insole", "featured project technical depth"),
        Question("Q10", "What teaching or training experience does Denys have?", "teaching and training")
    )
}

/**
 * Multi-turn conversations for evaluation.
 */
object Conversations {
    val conversations = listOf(
        Conversation(
            "Conv1",
            "project deep dive",
            listOf(
                "What projects has Denys worked on?",
                "Tell me more about the GEOSATIS project",
                "What technologies were used in that project?"
            )
        ),
        Conversation(
            "Conv2",
            "technology focus",
            listOf(
                "Does Denys have experience with backend development?",
                "What backend frameworks has he used?",
                "Can you give an example project where he did backend work?"
            )
        ),
        Conversation(
            "Conv3",
            "career progression",
            listOf(
                "How long has Denys been working in software development?",
                "What was his first Android project?",
                "How has his role evolved over time?"
            )
        ),
        Conversation(
            "Conv4",
            "comparison query",
            listOf(
                "Compare the McDonald's and GEOSATIS projects",
                "Which one was longer?",
                "What skills did Denys develop in each?"
            )
        ),
        Conversation(
            "Conv5",
            "non-featured project probe",
            listOf(
                "Has Denys worked in e-commerce?",
                "Tell me about the Lesara project",
                "What was the tech stack?"
            )
        ),
        Conversation(
            "Conv6",
            "skill verification",
            listOf(
                "Can Denys build Android TV apps?",
                "What project involved Android TV?",
                "What were the challenges in that project?"
            )
        ),
        Conversation(
            "Conv7",
            "team and leadership",
            listOf(
                "Has Denys led teams before?",
                "How large were the teams?",
                "What was his leadership style?"
            )
        ),
        Conversation(
            "Conv8",
            "client work",
            listOf(
                "Has Denys worked with enterprise clients?",
                "Name some of the biggest clients",
                "What was the most challenging client project?"
            )
        ),
        Conversation(
            "Conv9",
            "education and teaching",
            listOf(
                "Has Denys trained other developers?",
                "What was the Android School program?",
                "What topics did he teach?"
            )
        ),
        Conversation(
            "Conv10",
            "recent work",
            listOf(
                "What is Denys working on currently?",
                "How long has he been at GEOSATIS?",
                "What's unique about the victim protection app?"
            )
        )
    )
}

/**
 * McDonald's project deep-dive questions for evaluation.
 */
object McdonaldsQuestions {
    val questions = listOf(
        // Basic Info (3)
        Question("MCD1", "What was Denys's role on the McDonald's project?", "role"),
        Question("MCD2", "How long did Denys work on the McDonald's app?", "duration"),
        Question("MCD3", "What companies were involved in the McDonald's project chain?", "company structure"),

        // Scale & Impact (3)
        Question("MCD4", "How many users does the McDonald's Global App serve?", "metrics"),
        Question("MCD5", "How many countries is the McDonald's app available in?", "metrics"),
        Question("MCD6", "How many payment providers did Denys help integrate?", "metrics"),

        // Feature Streams (3)
        Question("MCD7", "What feature streams did Denys work on at McDonald's?", "feature streams"),
        Question("MCD8", "What is MDS in the context of the McDonald's project?", "delivery"),
        Question("MCD9", "What payment providers were integrated in the McDonald's app?", "payments"),

        // Technical Details (4)
        Question("MCD10", "What delivery tracking provider was used in the McDonald's app?", "technical"),
        Question("MCD11", "How many order status states exist in the McDonald's delivery system?", "technical"),
        Question("MCD12", "What is the Order 2.0 API in the McDonald's project?", "technical"),
        Question("MCD13", "What markets did Denys work with directly for the delivery feature?", "markets"),

        // KMM & Architecture (3)
        Question("MCD14", "How was Kotlin Multiplatform used in the McDonald's project?", "kmm"),
        Question("MCD15", "What is the mcm library in the McDonald's project?", "architecture"),
        Question("MCD16", "What ViewModel pattern was used for KMM in the McDonald's app?", "architecture"),

        // Leadership & Process (4)
        Question("MCD17", "What was the Android Talks initiative at McDonald's?", "leadership"),
        Question("MCD18", "What is the gmal-lite-rc channel about?", "process"),
        Question("MCD19", "What estimation method was used in the McDonald's project?", "process"),
        Question("MCD20", "What was the ViewBinding migration approach at McDonald's?", "technical")
    )
}

/**
 * A single evaluation question.
 */
data class Question(
    val id: String,
    val text: String,
    val category: String
)

/**
 * A multi-turn conversation for evaluation.
 */
data class Conversation(
    val id: String,
    val description: String,
    val turns: List<String>
)
