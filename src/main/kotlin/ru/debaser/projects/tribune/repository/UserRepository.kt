package ru.debaser.projects.tribune.repository

import org.jetbrains.exposed.sql.*
import ru.debaser.projects.tribune.db.data.user.Users
import ru.debaser.projects.tribune.db.dbQuery
import ru.debaser.projects.tribune.model.UserModel

interface UserRepository {
    suspend fun save(user: UserModel): Long?
    suspend fun getById(id: Long): UserModel?
    suspend fun getByUsername(username: String): UserModel?
    suspend fun reader(id: Long, set: Boolean)
    suspend fun isReader(id: Long): Boolean
    suspend fun addAvatar(id: Long, avatar: String)
}

class UserRepositoryDb: UserRepository {

    override suspend fun save(user: UserModel): Long? = dbQuery {
        Users.insert {
            it[username] = user.username
            it[password] = user.password
            it[isHater] = user.isHater
            it[isPromoter] = user.isPromoter
            it[isReader] = user.isReader
            it[avatar] = user.avatar
        }[Users.id]
    }

    override suspend fun getById(id: Long): UserModel? = dbQuery {
        Users.select { Users.id eq id }.map { toUserModel(it) }.singleOrNull()
    }
    override suspend fun getByUsername(username: String): UserModel? = dbQuery {
        Users.select { Users.username eq username }.map { toUserModel(it) }.singleOrNull()
    }

    override suspend fun reader(id: Long, set: Boolean) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[isReader] = set
            }
        }
    }

    override suspend fun isReader(id: Long): Boolean = dbQuery {
        Users.select { Users.id eq id }.map { toUserModel(it) }.single().isReader
    }

    override suspend fun addAvatar(id: Long, imageId: String) {
        dbQuery { Users.update({ Users.id eq id }) { it[Users.avatar] = imageId } }
    }

    private fun toUserModel(row: ResultRow): UserModel =
        UserModel(
            id = row[Users.id],
            username = row[Users.username],
            password = row[Users.password],
            isHater = row[Users.isHater],
            isPromoter = row[Users.isPromoter],
            isReader = row[Users.isReader],
            avatar = row[Users.avatar]
        )
}