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
