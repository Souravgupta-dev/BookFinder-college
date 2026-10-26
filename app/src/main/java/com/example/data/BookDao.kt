package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
  @Query("SELECT * FROM books")
  fun getAllBooks(): Flow<List<BookEntity>>

  @Query("SELECT * FROM books WHERE isSaved = 1")
  fun getSavedBooks(): Flow<List<BookEntity>>

  @Query("SELECT * FROM books WHERE id = :bookId")
  suspend fun getBookById(bookId: Long): BookEntity?

  @Query("SELECT DISTINCT category FROM books")
  fun getAllCategories(): Flow<List<String>>

  @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR authors LIKE '%' || :query || '%' OR synopsis LIKE '%' || :query || '%' OR callNumber LIKE '%' || :query || '%' OR isbn LIKE '%' || :query || '%'")
  fun searchBooks(query: String): Flow<List<BookEntity>>

  @Query("""
    SELECT * FROM books WHERE 
    (:filter = 'Title' AND title LIKE '%' || :query || '%') OR
    (:filter = 'Author' AND authors LIKE '%' || :query || '%') OR
    (:filter = 'OCR Content' AND synopsis LIKE '%' || :query || '%') OR
    (:filter = 'All' AND (title LIKE '%' || :query || '%' OR authors LIKE '%' || :query || '%' OR synopsis LIKE '%' || :query || '%' OR callNumber LIKE '%' || :query || '%' OR isbn LIKE '%' || :query || '%'))
  """)
  fun searchBooksByFilter(query: String, filter: String): Flow<List<BookEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBook(book: BookEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(books: List<BookEntity>)

  @Update
  suspend fun updateBook(book: BookEntity)

  @Delete
  suspend fun deleteBook(book: BookEntity)

  @Query("DELETE FROM books")
  suspend fun deleteAll()
}
