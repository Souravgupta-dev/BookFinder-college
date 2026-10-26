package com.example.data

import java.security.MessageDigest

object SecurityUtils {
  private const val SALT = "BookFinderSecureSalt2026!#"

  fun hashPassword(password: String): String {
    val combined = password + SALT
    val bytes = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
