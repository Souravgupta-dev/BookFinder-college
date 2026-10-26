package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BookFinderApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BookFinderViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    try {
      com.google.firebase.FirebaseApp.initializeApp(this)
    } catch (e: Exception) {
      // Firebase might be uninitialized if google-services.json is missing
    }
    setContent {
      MyApplicationTheme {
        val viewModel: BookFinderViewModel = viewModel()
        BookFinderApp(viewModel = viewModel)
      }
    }
  }
}
