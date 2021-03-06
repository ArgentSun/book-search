package io.paleocrafter.booksearch.books

import io.paleocrafter.booksearch.DbMigration
import io.paleocrafter.booksearch.createOrModifyColumns
import nl.siegmann.epublib.epub.EpubReader
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.or
import javax.sql.rowset.serial.SerialBlob

object CreateBookTablesMigration : DbMigration("create-book-tables") {
    override fun apply() {
        SchemaUtils.createMissingTablesAndColumns(Books, Chapters, Images, ClassMappings)
    }
}

object AddCoverMigration : DbMigration("add-book-covers") {
    override fun apply() {
        SchemaUtils.createMissingTablesAndColumns(Books)

        val booksMissingCovers = Book.find { Books.cover.isNull() or Books.coverMime.isNull() }
        val epubReader = EpubReader()
        for (book in booksMissingCovers) {
            val epub = epubReader.readEpub(book.content.binaryStream)
            epub.coverImage.let {
                book.cover = SerialBlob(it.data)
                book.coverMime = it.mediaType.name
            }
        }
    }
}

object AddIndexingIndicatorMigration : DbMigration("add-book-indexing-indicator") {
    override fun apply() {
        Books.createOrModifyColumns(Books.indexing)
    }
}

object AddTagsTableMigration : DbMigration("add-tags-table") {
    override fun apply() {
        SchemaUtils.createMissingTablesAndColumns(BookTags)
    }
}

object RestrictedBooksMigration : DbMigration("restricted-books-schema") {
    override fun apply() {
        Books.createOrModifyColumns(Books.restricted)
    }
}

object CreateCitationsMigration : DbMigration("create-citations") {
    override fun apply() {
        Books.createOrModifyColumns(Books.citationTemplate)
        Chapters.createOrModifyColumns(Chapters.citationParameter)

        val booksMissingCitations = Book.find { Books.citationTemplate.isNull() }
        for (book in booksMissingCitations) {
            book.citationTemplate = book.title.toLowerCase()
        }

        val chaptersMissingCitations = Chapter.find { Chapters.citationParameter.isNull() }
        for (chapter in chaptersMissingCitations) {
            chapter.citationParameter = extractCitationParameter(chapter.title)
        }
    }
}

object AddSearchByDefaultMigration : DbMigration("search-by-default") {
    override fun apply() {
        Books.createOrModifyColumns(Books.searchedByDefault)
    }
}
