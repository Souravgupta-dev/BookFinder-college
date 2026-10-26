package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey val email: String,
  val passwordHash: String,
  val name: String,
  val isVerified: Boolean = false,
  val otpCode: String? = null,
  val avatarUrl: String = "https://lh3.googleusercontent.com/aida-public/AB6AXuCZsc-qdCapncfCR_NAShTL7eY-Wxa4na-a0cv9xCKxWIaicGwRrzRwaXbJPrjN5gyzgovqtZSog6r5V3e_f06tivsfdnqmIwTdr6_Nhm-JPthK8dxhiArBbq9Ko4ZQuGpc4wGV_hOzcbViVbHP-aAi6yN4r-mnq3LPq2gFN0n1b1D9CXCzjrViliCSCU_FsGFX97gmmlGqQ8BC5_9sFDjelgvbQGKRla10oG09oGCLSkmdAtTBQOuc",
  val interests: String = "Computer Science, Academic"
)
