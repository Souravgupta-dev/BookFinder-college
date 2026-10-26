package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.viewmodel.BookFinderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookFinderApp(viewModel: BookFinderViewModel) {
  var currentTab by remember { mutableStateOf("home") } // home, search, saved, admin
  var activeRoute by remember { mutableStateOf<String?>(null) } // details/{id}, wayfinder/{id}, filter, diagnostics
  var activeBookId by remember { mutableStateOf<Long?>(null) }

  val books by viewModel.allBooks.collectAsStateWithLifecycle()
  val savedBooks by viewModel.savedBooks.collectAsStateWithLifecycle()
  val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val filteredLibraryBooks by viewModel.filteredLibraryBooks.collectAsStateWithLifecycle()
  val librarySearchQuery by viewModel.librarySearchQuery.collectAsStateWithLifecycle()
  val librarySearchFilter by viewModel.librarySearchFilter.collectAsStateWithLifecycle()
  val categories by viewModel.allCategories.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val allReservations by viewModel.allReservations.collectAsStateWithLifecycle()
  val userReservations by viewModel.userReservations.collectAsStateWithLifecycle()

  BackHandler(enabled = activeRoute != null || currentTab != "home") {
    if (activeRoute != null) {
      if (activeRoute?.startsWith("wayfinder/") == true || activeRoute?.startsWith("pdf/") == true) {
        activeRoute = "details/$activeBookId"
      } else {
        activeRoute = null
      }
    } else if (currentTab != "home") {
      currentTab = "home"
    }
  }

  Scaffold(
    bottomBar = {
      if (activeRoute == null) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          tonalElevation = 8.dp
        ) {
          NavigationBarItem(
            selected = currentTab == "home",
            onClick = { currentTab = "home" },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
          )
          NavigationBarItem(
            selected = currentTab == "search",
            onClick = { currentTab = "search" },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
          )
          NavigationBarItem(
            selected = currentTab == "saved",
            onClick = { currentTab = "saved" },
            icon = {
              BadgedBox(
                badge = {
                  if (savedBooks.isNotEmpty()) {
                    Badge { Text("${savedBooks.size}") }
                  }
                }
              ) {
                Icon(Icons.Default.Bookmark, contentDescription = "Saved")
              }
            },
            label = { Text("Saved") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
          )
          NavigationBarItem(
            selected = currentTab == "admin",
            onClick = { currentTab = "admin" },
            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
            label = { Text("Admin") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
          )
        }
      }
    }
  ) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding)) {
      when (activeRoute) {
        "filter" -> {
          FilterModalScreen(
            onApplyFilters = { activeRoute = null },
            onResetFilters = { activeRoute = null },
            onClose = { activeRoute = null }
          )
        }
        "diagnostics" -> {
          DiagnosticsScreen(
            onBackToHome = { activeRoute = null }
          )
        }
        "auth" -> {
          AuthScreen(
            viewModel = viewModel,
            onBackClick = { activeRoute = null }
          )
        }
        "analytics" -> {
          ReadingAnalyticsScreen(
            books = books,
            currentUser = currentUser,
            onBackClick = { activeRoute = null }
          )
        }
        "study_rooms" -> {
          StudyRoomsScreen(
            onBackClick = { activeRoute = null }
          )
        }
        else -> {
          if (activeRoute?.startsWith("category/") == true) {
            val categoryName = activeRoute!!.removePrefix("category/")
            CategoryScreen(
              categoryName = categoryName,
              books = books,
              onBackClick = { activeRoute = null },
              onBookClick = { id ->
                activeBookId = id
                activeRoute = "details/$id"
              },
              onWayfinderClick = { id ->
                activeBookId = id
                activeRoute = "wayfinder/$id"
              },
              onToggleSave = { viewModel.toggleSave(it) }
            )
          } else if (activeRoute?.startsWith("details/") == true) {
            val book = books.find { it.id == activeBookId }
            BookDetailsScreen(
              book = book,
              onBackClick = { activeRoute = null },
              onWayfinderClick = { id -> activeRoute = "wayfinder/$id" },
              onToggleSave = { viewModel.toggleSave(it) },
              onPdfClick = { id -> activeRoute = "pdf/$id" },
              userReservations = userReservations,
              onReserveBook = { b, pickup, notes, cb ->
                viewModel.reserveBook(
                  book = b,
                  pickupLocation = pickup,
                  studentNotes = notes,
                  onSuccess = { cb(true, "Reservation placed! Sent to Admin for access approval.") },
                  onError = { cb(false, it) }
                )
              },
              onCancelReservation = { viewModel.cancelReservation(it) }
            )
          } else if (activeRoute?.startsWith("wayfinder/") == true) {
            val book = books.find { it.id == activeBookId }
            WayfinderScreen(
              book = book,
              onBackClick = { activeRoute = "details/$activeBookId" }
            )
          } else if (activeRoute?.startsWith("pdf/") == true) {
            val book = books.find { it.id == activeBookId }
            if (book != null) {
              PdfReaderScreen(
                book = book,
                onBackClick = { activeRoute = "details/$activeBookId" }
              )
            }
          } else {
            when (currentTab) {
              "home" -> {
                HomeScreen(
                  books = books,
                  filteredBooks = filteredLibraryBooks,
                  searchQuery = librarySearchQuery,
                  searchFilter = librarySearchFilter,
                  onSearchQueryChanged = { viewModel.setLibrarySearchQuery(it) },
                  onSearchFilterChanged = { viewModel.setLibrarySearchFilter(it) },
                  currentUser = currentUser,
                  categories = categories,
                  selectedCategory = selectedCategory,
                  onCategorySelected = { viewModel.setSelectedCategory(it) },
                  onSearchClick = { currentTab = "search" },
                  onBookClick = { id ->
                    activeBookId = id
                    activeRoute = "details/$id"
                  },
                  onCategoryClick = { catName ->
                    activeRoute = "category/$catName"
                  },
                  onSavedClick = { currentTab = "saved" },
                  onAdminClick = { currentTab = "admin" },
                  onWayfinderClick = { id ->
                    activeBookId = id
                    activeRoute = "wayfinder/$id"
                  },
                  onAuthClick = { activeRoute = "auth" },
                  onAnalyticsClick = { activeRoute = "analytics" },
                  onStudyRoomsClick = { activeRoute = "study_rooms" },
                  onToggleSave = { viewModel.toggleSave(it) }
                )
              }
              "search" -> {
                SearchScreen(
                  books = books,
                  recentSearches = recentSearches,
                  onSaveSearchQuery = { viewModel.saveSearchQuery(it) },
                  onDeleteSearchQuery = { viewModel.deleteSearchQuery(it) },
                  onClearRecentSearches = { viewModel.clearRecentSearches() },
                  onBookClick = { id ->
                    activeBookId = id
                    activeRoute = "details/$id"
                  },
                  onWayfinderClick = { id ->
                    activeBookId = id
                    activeRoute = "wayfinder/$id"
                  },
                  onFilterClick = { activeRoute = "filter" },
                  onToggleSave = { viewModel.toggleSave(it) },
                  onAuthClick = { activeRoute = "auth" }
                )
              }
              "saved" -> {
                SavedBooksScreen(
                  savedBooks = savedBooks,
                  onBookClick = { id ->
                    activeBookId = id
                    activeRoute = "details/$id"
                  },
                  onWayfinderClick = { id ->
                    activeBookId = id
                    activeRoute = "wayfinder/$id"
                  },
                  onToggleSave = { viewModel.toggleSave(it) },
                  onImportPdfBook = { title, authors, category, uri ->
                    viewModel.importPdfBook(title, authors, category, uri)
                  },
                  onUpdateProgress = { bookId, curr, tot ->
                    viewModel.updateBookProgress(bookId, curr, tot)
                  },
                  onUpdateTags = { bookId, tags ->
                    viewModel.updateBookTags(bookId, tags)
                  },
                  onAuthClick = { activeRoute = "auth" },
                  userReservations = userReservations,
                  onCancelReservation = { viewModel.cancelReservation(it) }
                )
              }
              "admin" -> {
                AdminScreen(
                  books = books,
                  reservations = allReservations,
                  onAddBook = { title, authors, cat, isbn, callNum, floor, wing, rack, shelf, status, total, avail, syn ->
                    viewModel.addBook(title, authors, cat, isbn, callNum, floor, wing, rack, shelf, status, total, avail, syn)
                  },
                  onUpdateBook = { viewModel.updateBook(it) },
                  onDeleteBook = { viewModel.deleteBook(it) },
                  onAuthClick = { activeRoute = "auth" },
                  onExtractOcrBook = { base64, mime, cb ->
                    viewModel.extractAndCatalogDocument(base64, mime, cb)
                  },
                  onApproveReservation = { id, comment ->
                    viewModel.approveReservation(id, comment)
                  },
                  onDeclineReservation = { id, reason ->
                    viewModel.declineReservation(id, reason)
                  },
                  onDeleteReservation = { viewModel.deleteReservation(it) }
                )
              }
            }
          }
        }
      }
    }
  }
}
