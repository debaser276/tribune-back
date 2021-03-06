package ru.debaser.projects.tribune.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

class DatabaseFactory(private val jdbcUrl: String) {
    fun init() {
        val dbUri = URI(jdbcUrl)
        val username = dbUri.userInfo.split(":")[0]
        val password = dbUri.userInfo.split(":")[1]
        val port = dbUri.port.toString()
        val jdbcUrl = "jdbc:postgresql://${dbUri.host}:$port${dbUri.path}"
        Database.connect(hikari(jdbcUrl, username, password))
        val flyway = Flyway.configure().dataSource(jdbcUrl, username, password).load()
        flyway.migrate()
    }

    private fun hikari(jdbcUrl: String, username: String, password: String): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            this.username = username
            this.password = password
            this.jdbcUrl = jdbcUrl
            driverClassName = "org.postgresql.Driver"
        }
        return HikariDataSource(hikariConfig)
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    withContext(Dispatchers.IO) {
        transaction {
            runBlocking {
                block()
            }
        }
    }