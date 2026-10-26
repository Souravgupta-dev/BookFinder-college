package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchQueryDao {
  @Query("SELECT * FROM search_queries ORDER BY timestamp DESC")
  fun getAllQueries(): Flow<List<SearchQueryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuery(query: SearchQueryEntity)

  @Query("DELETE FROM search_queries WHERE id = :id")
  suspend fun deleteQueryById(id: Long)

  @Query("DELETE FROM search_queries")
  suspend fun clearAllQueries()
}
