package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_queries")
data class SearchQueryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val query: String,
  val timestamp: Long = System.currentTimeMillis()
)
