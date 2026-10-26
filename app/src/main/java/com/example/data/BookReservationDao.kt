package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookReservationDao {
  @Query("SELECT * FROM book_reservations ORDER BY requestDate DESC")
  fun getAllReservations(): Flow<List<BookReservationEntity>>

  @Query("SELECT * FROM book_reservations WHERE userEmail = :userEmail ORDER BY requestDate DESC")
  fun getReservationsByUser(userEmail: String): Flow<List<BookReservationEntity>>

  @Query("SELECT * FROM book_reservations WHERE bookId = :bookId ORDER BY requestDate DESC")
  fun getReservationsForBook(bookId: Long): Flow<List<BookReservationEntity>>

  @Query("SELECT * FROM book_reservations WHERE id = :id")
  suspend fun getReservationById(id: Long): BookReservationEntity?

  @Query("SELECT * FROM book_reservations WHERE bookId = :bookId AND userEmail = :userEmail AND status IN ('PENDING', 'APPROVED') LIMIT 1")
  fun getActiveReservationForUser(bookId: Long, userEmail: String): Flow<BookReservationEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReservation(reservation: BookReservationEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(reservations: List<BookReservationEntity>)

  @Update
  suspend fun updateReservation(reservation: BookReservationEntity)

  @Query("UPDATE book_reservations SET status = :status, adminComment = :adminComment WHERE id = :id")
  suspend fun updateStatus(id: Long, status: String, adminComment: String)

  @Delete
  suspend fun deleteReservation(reservation: BookReservationEntity)

  @Query("DELETE FROM book_reservations WHERE id = :id")
  suspend fun deleteById(id: Long)
}
