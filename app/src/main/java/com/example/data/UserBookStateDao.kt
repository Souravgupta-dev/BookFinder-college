package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserBookStateDao {
  @Query("SELECT * FROM user_book_states WHERE userEmail = :email")
  suspend fun getStatesForUser(email: String): List<UserBookStateEntity>

  @Query("SELECT * FROM user_book_states WHERE userEmail = :email AND bookId = :bookId")
  suspend fun getState(email: String, bookId: Long): UserBookStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateState(state: UserBookStateEntity)
}
