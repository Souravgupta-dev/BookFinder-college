package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_reservations")
data class BookReservationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val bookId: Long,
  val bookTitle: String,
  val bookAuthors: String,
  val bookCoverUrl: String = "",
  val bookCallNumber: String = "",
  val userEmail: String,
  val userName: String = "Student",
  val status: String = "PENDING", // PENDING, APPROVED, DECLINED, FULFILLED, CANCELLED
  val requestDate: Long = System.currentTimeMillis(),
  val pickupLocation: String = "Main Circulation Desk - Floor 1",
  val studentNotes: String = "",
  val adminComment: String = ""
)
