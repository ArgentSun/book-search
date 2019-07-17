package io.paleocrafter.booksearch

import io.paleocrafter.booksearch.auth.AddSearchSettingsMigration
import io.paleocrafter.booksearch.auth.CreateAuthTablesMigration
import io.paleocrafter.booksearch.books.AddCoverMigration
import io.paleocrafter.booksearch.books.AddIndexingIndicatorMigration
import io.paleocrafter.booksearch.books.CreateBookTablesMigration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

object DbMigrations : Table() {
    private val logger = LoggerFactory.getLogger("DbMigrations")
    private val versions = mapOf(
        0 to listOf(CreateAuthTablesMigration, CreateBookTablesMigration),
        1 to listOf(AddCoverMigration),
        2 to listOf(AddSearchSettingsMigration),
        3 to listOf(AddIndexingIndicatorMigration)
    ).toSortedMap()

    fun run(db: Database) {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(ExecutedMigrations)

            val lastVersion = ExecutedMigrations.selectAll().orderBy(ExecutedMigrations.version, SortOrder.DESC).firstOrNull()
                ?.get(ExecutedMigrations.version) ?: 0
            val executed = ExecutedMigrations.select { ExecutedMigrations.version eq lastVersion }.map { it[ExecutedMigrations.name] }
            for ((versionNumber, migrations) in versions.tailMap(lastVersion)) {
                for (migration in migrations) {
                    if (migration.name in executed) {
                        continue
                    }

                    migration.apply()
                    logger.info("Executed migration '${migration.name}' from version $versionNumber")
                    ExecutedMigrations.insert {
                        it[this.version] = versionNumber
                        it[this.name] = migration.name
                        it[this.executedOn] = DateTime.now()
                    }
                }
            }
        }
    }
}

private object ExecutedMigrations : Table() {
    val version = integer("version").primaryKey(0)
    val name = varchar("migration", 255).primaryKey(1)
    val executedOn = datetime("executed_on")
}

abstract class DbMigration(val name: String) {
    abstract fun apply()
}
