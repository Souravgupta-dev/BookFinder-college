package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String,
  val authors: String,
  val category: String,
  val isbn: String,
  val callNumber: String,
  val floor: String,
  val wing: String,
  val rack: String,
  val shelf: String,
  val status: String, // "AVAILABLE", "ISSUED", "REFERENCE"
  val totalCopies: Int,
  val availableCopies: Int,
  val coverUrl: String,
  val synopsis: String,
  val isSaved: Boolean = false,
  val isIssued: Boolean = false,
  val dueDate: String? = null,
  val currentPage: Int = 0,
  val totalPages: Int = 150,
  val readingProgressPercent: Int = 0,
  val customTags: String = "To-read"
)
