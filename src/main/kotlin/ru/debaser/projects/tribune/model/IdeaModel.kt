package ru.debaser.projects.tribune.model

data class Vote(
    val id: Long,
    val authorId: Long,
    val ideaId: Long,
    val created: Long,
    val isUp: Boolean
)

data class IdeaModel(
    val id: Long,
    val authorId: Long,
    val created: Long,
    val content: String,
    val media: String,
    val link: String? = null,
    val likes: Set<Long> = emptySet(),
    val dislikes: Set<Long> = emptySet()
)