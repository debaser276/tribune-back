package ru.debaser.projects.tribune.service

import ru.debaser.projects.tribune.dto.IdeaResponseDto
import ru.debaser.projects.tribune.exception.DatabaseException
import ru.debaser.projects.tribune.exception.IdeaNotFoundException
import ru.debaser.projects.tribune.model.IdeaModel
import ru.debaser.projects.tribune.model.VoteModel
import ru.debaser.projects.tribune.repository.IdeaRepository
import ru.debaser.projects.tribune.repository.VoteRepository

class IdeaService (
    private val ideaRepo: IdeaRepository,
    private val voteRepo: VoteRepository
) {
    suspend fun postIdea(idea: IdeaModel): Long =
        ideaRepo.postIdea(idea) ?: throw DatabaseException()

    suspend fun getById(id: Long): IdeaModel =
        ideaRepo.getById(id) ?: throw IdeaNotFoundException()

    suspend fun like(ideaId: Long, userId: Long): IdeaResponseDto {
        ideaRepo.like(ideaId, userId)
        voteRepo.addVote(userId, ideaId, true)
        return IdeaResponseDto.fromModel(getById(ideaId))
    }
}