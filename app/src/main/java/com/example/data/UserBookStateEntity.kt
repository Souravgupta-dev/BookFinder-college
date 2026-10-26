package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_book_states")
data class UserBookStateEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val userEmail: String,
  val bookId: Long,
  val isSaved: Boolean = false,
  val isIssued: Boolean = false,
  val dueDate: String? = null,
  val currentPage: Int = 0,
  val totalPages: Int = 150,
  val readingProgressPercent: Int = 0,
  val customTags: String = "To-read"
)
