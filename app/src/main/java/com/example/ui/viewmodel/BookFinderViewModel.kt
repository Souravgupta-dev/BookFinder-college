package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class BookFinderViewModel(application: Application) : AndroidViewModel(application) {
  private val db = LibraryDatabase.getDatabase(application)
  private val dao = db.bookDao()
  private val userDao = db.userDao()
  private val searchQueryDao = db.searchQueryDao()
  private val userBookStateDao = db.userBookStateDao()
  private val bookReservationDao = db.bookReservationDao()
  private val resendApi = ResendApiService.create()
  private val brevoApi = BrevoApiService.create()

  private val _currentUser = MutableStateFlow<UserEntity?>(null)
  val currentUser: StateFlow<UserEntity?> = _currentUser

  private val _latestOtp = MutableStateFlow<String?>(null)
  val latestOtp: StateFlow<String?> = _latestOtp

  private val currentEmail: String
    get() = _currentUser.value?.email ?: "guest@device"

  val allBooks: StateFlow<List<BookEntity>> = combine(
    dao.getAllBooks(),
    _currentUser
  ) { books, user ->
    val email = user?.email ?: "guest@device"
    val states = userBookStateDao.getStatesForUser(email).associateBy { it.bookId }
    books.map { book ->
      val state = states[book.id]
      if (state != null) {
        book.copy(
          isSaved = state.isSaved,
          isIssued = state.isIssued,
          dueDate = state.dueDate,
          currentPage = state.currentPage,
          totalPages = state.totalPages,
          readingProgressPercent = state.readingProgressPercent,
          customTags = state.customTags
        )
      } else {
        book
      }
    }
  }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val savedBooks: StateFlow<List<BookEntity>> = allBooks.map { books ->
    books.filter { it.isSaved }
  }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val recentSearches: StateFlow<List<SearchQueryEntity>> = searchQueryDao.getAllQueries()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  fun saveSearchQuery(query: String) {
    if (query.isNotBlank()) {
      viewModelScope.launch {
        searchQueryDao.insertQuery(SearchQueryEntity(query = query.trim()))
      }
    }
  }

  fun deleteSearchQuery(id: Long) {
    viewModelScope.launch {
      searchQueryDao.deleteQueryById(id)
    }
  }

  fun clearRecentSearches() {
    viewModelScope.launch {
      searchQueryDao.clearAllQueries()
    }
  }

  private val _authMessage = MutableStateFlow<String?>(null)
  val authMessage: StateFlow<String?> = _authMessage

  private val _brevoApiKey = MutableStateFlow(
    com.example.BuildConfig.BREVO_API_KEY.ifBlank { "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" }
  )
  val brevoApiKey: StateFlow<String> = _brevoApiKey

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  private val _selectedCategory = MutableStateFlow<String?>(null)
  val selectedCategory: StateFlow<String?> = _selectedCategory

  private val _selectedFloor = MutableStateFlow<String?>(null)
  val selectedFloor: StateFlow<String?> = _selectedFloor

  private val _availableOnly = MutableStateFlow(false)
  val availableOnly: StateFlow<Boolean> = _availableOnly

  fun setBrevoApiKey(key: String) {
    _brevoApiKey.value = key
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setSelectedCategory(category: String?) {
    _selectedCategory.value = category
  }

  fun setSelectedFloor(floor: String?) {
    _selectedFloor.value = floor
  }

  fun setAvailableOnly(available: Boolean) {
    _availableOnly.value = available
  }

  fun toggleSave(book: BookEntity) {
    viewModelScope.launch {
      val email = currentEmail
      val existingState = userBookStateDao.getState(email, book.id)
      val newIsSaved = !(existingState?.isSaved ?: book.isSaved)
      val newState = UserBookStateEntity(
        id = existingState?.id ?: 0L,
        userEmail = email,
        bookId = book.id,
        isSaved = newIsSaved,
        isIssued = existingState?.isIssued ?: book.isIssued,
        dueDate = existingState?.dueDate ?: book.dueDate,
        currentPage = existingState?.currentPage ?: book.currentPage,
        totalPages = existingState?.totalPages ?: book.totalPages,
        readingProgressPercent = existingState?.readingProgressPercent ?: book.readingProgressPercent,
        customTags = existingState?.customTags ?: book.customTags
      )
      userBookStateDao.insertOrUpdateState(newState)
    }
  }

  fun signUp(name: String, email: String, pass: String, apiKey: String, onResult: (Boolean) -> Unit) {
    if (name.isBlank() || email.isBlank() || pass.isBlank()) {
      _authMessage.value = "Please fill in all fields"
      onResult(false)
      return
    }
    if (pass.length < 8 || !pass.any { it.isDigit() } || !pass.any { it.isLetter() }) {
      _authMessage.value = "Password must be at least 8 chars with letters & numbers"
      onResult(false)
      return
    }
    viewModelScope.launch {
      try {
        val existing = userDao.getUserByEmail(email)
        if (existing != null && existing.isVerified) {
          _authMessage.value = "Email already registered and verified. Please sign in."
          onResult(false)
          return@launch
        }

        val otp = String.format("%06d", Random.nextInt(100000, 999999))
        _latestOtp.value = otp
        val newUser = UserEntity(
          email = email,
          passwordHash = SecurityUtils.hashPassword(pass),
          name = name,
          isVerified = false,
          otpCode = otp
        )
        userDao.insertUser(newUser)

        // Send OTP via Brevo or Resend API based on API key prefix
        var deliveryMsg = "OTP generated successfully!"
        Log.i("EmailDebug", "Initiating email API request for email: $email, name: $name")
        Log.d("EmailDebug", "API Key present: ${apiKey.isNotBlank()}, length: ${apiKey.length}, startsWith xkeysib: ${apiKey.startsWith("xkeysib-")}")

        if (apiKey.isNotBlank()) {
          try {
            if (apiKey.startsWith("xkeysib-")) {
              val emailReq = BrevoEmailRequest(
                sender = BrevoSender(name = "BookFinder Library", email = email),
                to = listOf(BrevoRecipient(email = email, name = name)),
                subject = "Your BookFinder Verification OTP",
                htmlContent = "<html><body><h2>Welcome to BookFinder, $name!</h2><p>Your verification code is: <b>$otp</b></p></body></html>"
              )
              Log.d("EmailDebug", "Sending Brevo HTTP POST request to https://api.brevo.com/v3/smtp/email")
              val response = brevoApi.sendEmail(apiKey, emailReq)
              Log.i("EmailDebug", "Brevo API HTTP response received — Code: ${response.code()}, isSuccessful: ${response.isSuccessful}")

              if (response.isSuccessful) {
                deliveryMsg = "OTP sent successfully via Brevo email!"
              } else {
                val err = response.errorBody()?.string() ?: "Unknown Brevo error"
                Log.e("EmailDebug", "Brevo error: $err")
                deliveryMsg = if (response.code() == 401) {
                  if (err.contains("unrecognised IP address")) {
                    "Brevo IP Block: Authorize IP 108.177.6.5 in Brevo. OTP Code: $otp"
                  } else {
                    "Brevo 401 Unauthorized: Check Brevo API Key. OTP Code: $otp"
                  }
                } else {
                  "Brevo error (${response.code()}): $err. Code: $otp"
                }
              }
            } else {
              val emailReq = ResendEmailRequest(
                from = "BookFinder <onboarding@resend.dev>",
                to = listOf(email),
                subject = "Your BookFinder Verification OTP",
                html = "<html><body><h2>Welcome to BookFinder, $name!</h2><p>Your verification code is: <b>$otp</b></p></body></html>"
              )
              Log.d("EmailDebug", "Sending Resend HTTP POST request to https://api.resend.com/emails")
              val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
              val response = resendApi.sendEmail(authHeader, emailReq)
              Log.i("EmailDebug", "Resend API HTTP response received — Code: ${response.code()}, isSuccessful: ${response.isSuccessful}")

              if (response.isSuccessful) {
                deliveryMsg = "OTP sent successfully via Resend email!"
              } else {
                val err = response.errorBody()?.string() ?: "Unknown Resend error"
                Log.e("EmailDebug", "Resend error: $err")
                deliveryMsg = if (response.code() == 401) {
                  "Resend 401 Unauthorized: API key is invalid or expired. OTP Code: $otp"
                } else {
                  "Resend error (${response.code()}): $err. Code: $otp"
                }
              }
            }
          } catch (e: Exception) {
            Log.e("EmailDebug", "Exception during email dispatch", e)
            deliveryMsg = "Email network error: ${e.localizedMessage}. Code: $otp"
          }
        } else {
          Log.w("EmailDebug", "Email API key is blank. Skipping API call. OTP Code: $otp")
          deliveryMsg = "OTP generated! Code: $otp"
        }

        _authMessage.value = "$deliveryMsg (Use Auto-Fill below)"
        onResult(true)
      } catch (e: Exception) {
        _authMessage.value = "Sign Up error: ${e.localizedMessage}"
        onResult(false)
      }
    }
  }

  fun verifyOtp(email: String, otp: String, onResult: (Boolean) -> Unit) {
    viewModelScope.launch {
      val user = userDao.getUserByEmail(email)
      if (user != null && user.otpCode == otp) {
        val verifiedUser = user.copy(isVerified = true, otpCode = null)
        userDao.insertUser(verifiedUser)
        _currentUser.value = verifiedUser
        _authMessage.value = null
        onResult(true)
      } else {
        _authMessage.value = "Invalid OTP code. Please check."
        onResult(false)
      }
    }
  }

  fun signIn(email: String, pass: String, onResult: (Boolean) -> Unit) {
    if (email.isBlank() || pass.isBlank()) {
      _authMessage.value = "Please enter email and password"
      onResult(false)
      return
    }
    viewModelScope.launch {
      val user = userDao.getUserByEmail(email)
      if (user == null) {
        _authMessage.value = "No account found with this email. Please sign up."
        onResult(false)
      } else if (!user.isVerified) {
        _authMessage.value = "Account not verified via OTP yet."
        onResult(false)
      } else if (user.passwordHash != SecurityUtils.hashPassword(pass)) {
        _authMessage.value = "Incorrect password."
        onResult(false)
      } else {
        _currentUser.value = user
        _authMessage.value = null
        onResult(true)
      }
    }
  }

  fun signOut() {
    _currentUser.value = null
    _authMessage.value = null
  }

  fun updateProfile(email: String, name: String, avatarUrl: String, interests: String, onResult: (Boolean) -> Unit) {
    viewModelScope.launch {
      val user = userDao.getUserByEmail(email)
      if (user != null) {
        val updated = user.copy(name = name, avatarUrl = avatarUrl, interests = interests)
        userDao.insertUser(updated)
        _currentUser.value = updated
        _authMessage.value = "Profile updated successfully!"
        onResult(true)
      } else {
        onResult(false)
      }
    }
  }

  suspend fun getBook(id: Long): BookEntity? {
    return dao.getBookById(id)
  }

  fun addBook(
    title: String,
    authors: String,
    category: String,
    isbn: String,
    callNumber: String,
    floor: String,
    wing: String,
    rack: String,
    shelf: String,
    status: String,
    totalCopies: Int,
    availableCopies: Int,
    synopsis: String
  ) {
    viewModelScope.launch {
      val newBook = BookEntity(
        title = title,
        authors = authors,
        category = category,
        isbn = isbn,
        callNumber = callNumber,
        floor = floor,
        wing = wing,
        rack = rack,
        shelf = shelf,
        status = status,
        totalCopies = totalCopies,
        availableCopies = availableCopies,
        coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCZsc-qdCapncfCR_NAShTL7eY-Wxa4na-a0cv9xCKxWIaicGwRrzRwaXbJPrjN5gyzgovqtZSog6r5V3e_f06tivsfdnqmIwTdr6_Nhm-JPthK8dxhiArBbq9Ko4ZQuGpc4wGV_hOzcbViVbHP-aAi6yN4r-mnq3LPq2gFN0n1b1D9CXCzjrViliCSCU_FsGFX97gmmlGqQ8BC5_9sFDjelgvbQGKRla10oG09oGCLSkmdAtTBQOuc",
        synopsis = synopsis
      )
      val bookId = dao.insertBook(newBook)
      try {
        val firestore = FirebaseFirestore.getInstance()
        val bookMap = hashMapOf(
          "id" to bookId,
          "title" to title,
          "authors" to authors,
          "category" to category,
          "isbn" to isbn,
          "callNumber" to callNumber,
          "floor" to floor,
          "wing" to wing,
          "rack" to rack,
          "shelf" to shelf,
          "status" to status,
          "totalCopies" to totalCopies,
          "availableCopies" to availableCopies,
          "synopsis" to synopsis
        )
        firestore.collection("universal_books").document(bookId.toString()).set(bookMap)
      } catch (e: Exception) {
        // Fallback
      }
    }
  }

  fun updateBook(book: BookEntity) {
    viewModelScope.launch {
      dao.updateBook(book)
    }
  }

  fun importPdfBook(
    title: String,
    authors: String,
    category: String,
    pdfUri: String
  ) {
    viewModelScope.launch {
      val newBook = BookEntity(
        title = title,
        authors = authors,
        category = category,
        isbn = "PDF-${System.currentTimeMillis() % 100000}",
        callNumber = "PDF · $category",
        floor = "Floor 1",
        wing = "Digital Stacks",
        rack = "PDF-01",
        shelf = "Shelf 1",
        status = "AVAILABLE",
        totalCopies = 1,
        availableCopies = 1,
        coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCZsc-qdCapncfCR_NAShTL7eY-Wxa4na-a0cv9xCKxWIaicGwRrzRwaXbJPrjN5gyzgovqtZSog6r5V3e_f06tivsfdnqmIwTdr6_Nhm-JPthK8dxhiArBbq9Ko4ZQuGpc4wGV_hOzcbViVbHP-aAi6yN4r-mnq3LPq2gFN0n1b1D9CXCzjrViliCSCU_FsGFX97gmmlGqQ8BC5_9sFDjelgvbQGKRla10oG09oGCLSkmdAtTBQOuc",
        synopsis = "Imported PDF document ($pdfUri). Organized in personal digital library.",
        isSaved = true
      )
      val bookId = dao.insertBook(newBook)
      // Save state for user
      toggleSave(newBook.copy(id = bookId, isSaved = true))
    }
  }

  fun deleteBook(book: BookEntity) {
    viewModelScope.launch {
      dao.deleteBook(book)
      try {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("universal_books").document(book.id.toString()).delete()
      } catch (e: Exception) {
        // Fallback
      }
    }
  }

  fun updateBookProgress(bookId: Long, currentPage: Int, totalPages: Int) {
    viewModelScope.launch {
      val email = currentEmail
      val book = dao.getBookById(bookId) ?: return@launch
      val existingState = userBookStateDao.getState(email, bookId)
      val validTotal = if (totalPages > 0) totalPages else book.totalPages
      val validCurrent = currentPage.coerceIn(0, validTotal)
      val percent = ((validCurrent.toFloat() / validTotal) * 100).toInt().coerceIn(0, 100)

      val newState = UserBookStateEntity(
        id = existingState?.id ?: 0L,
        userEmail = email,
        bookId = bookId,
        isSaved = existingState?.isSaved ?: book.isSaved,
        isIssued = existingState?.isIssued ?: book.isIssued,
        dueDate = existingState?.dueDate ?: book.dueDate,
        currentPage = validCurrent,
        totalPages = validTotal,
        readingProgressPercent = percent,
        customTags = existingState?.customTags ?: book.customTags
      )
      userBookStateDao.insertOrUpdateState(newState)
    }
  }

  fun updateBookTags(bookId: Long, customTags: String) {
    viewModelScope.launch {
      val email = currentEmail
      val book = dao.getBookById(bookId) ?: return@launch
      val existingState = userBookStateDao.getState(email, bookId)

      val newState = UserBookStateEntity(
        id = existingState?.id ?: 0L,
        userEmail = email,
        bookId = bookId,
        isSaved = existingState?.isSaved ?: book.isSaved,
        isIssued = existingState?.isIssued ?: book.isIssued,
        dueDate = existingState?.dueDate ?: book.dueDate,
        currentPage = existingState?.currentPage ?: book.currentPage,
        totalPages = existingState?.totalPages ?: book.totalPages,
        readingProgressPercent = existingState?.readingProgressPercent ?: book.readingProgressPercent,
        customTags = customTags.trim()
      )
      userBookStateDao.insertOrUpdateState(newState)
    }
  }

  private val geminiApi = GeminiApiService.create()

  fun extractAndCatalogDocument(base64Data: String, mimeType: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val apiKey = com.example.BuildConfig.GEMINI_API_KEY.ifBlank { "" }
      if (apiKey.isBlank()) {
        onResult(false, "Gemini API Key missing in Secrets panel.")
        return@launch
      }
      try {
        val request = GeminiRequest(
          contents = listOf(
            GeminiContent(
              parts = listOf(
                GeminiPart(inlineData = InlineData(mimeType = mimeType, data = base64Data)),
                GeminiPart(text = "Analyze this book document or image. Extract the details separated by '|' in exact order: Title | Author(s) | Category (Choose from: Computer Science, Mathematics & Stats, Engineering & Tech, Natural Sciences, Literature & Arts, Business & Mgmt) | ISBN | Call Number | Synopsis. Provide ONLY the separated values.")
              )
            )
          )
        )
        val response = geminiApi.generateContent(apiKey, request)
        if (response.isSuccessful) {
          val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
          val parts = text.split("|").map { it.trim() }
          if (parts.size >= 6) {
            val title = parts[0]
            val authors = parts[1]
            val category = parts[2].ifBlank { "Computer Science" }
            val isbn = parts[3].ifBlank { "978-${System.currentTimeMillis() % 1000000000}" }
            val callNumber = parts[4].ifBlank { "QA76.9 .AI8" }
            val synopsis = parts[5]

            val newBook = BookEntity(
              title = title,
              authors = authors,
              category = category,
              isbn = isbn,
              callNumber = callNumber,
              floor = "Floor 2",
              wing = "AI Library Wing",
              rack = "AI-01",
              shelf = "Shelf 1",
              status = "AVAILABLE",
              totalCopies = 3,
              availableCopies = 3,
              coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCZsc-qdCapncfCR_NAShTL7eY-Wxa4na-a0cv9xCKxWIaicGwRrzRwaXbJPrjN5gyzgovqtZSog6r5V3e_f06tivsfdnqmIwTdr6_Nhm-JPthK8dxhiArBbq9Ko4ZQuGpc4wGV_hOzcbViVbHP-aAi6yN4r-mnq3LPq2gFN0n1b1D9CXCzjrViliCSCU_FsGFX97gmmlGqQ8BC5_9sFDjelgvbQGKRla10oG09oGCLSkmdAtTBQOuc",
              synopsis = synopsis,
              isSaved = true
            )
            val bookId = dao.insertBook(newBook)
            toggleSave(newBook.copy(id = bookId, isSaved = true))
            onResult(true, "Successfully extracted and cataloged: $title")
          } else {
            val fallbackTitle = text.take(50).ifBlank { "AI Scanned Publication" }
            val newBook = BookEntity(
              title = fallbackTitle,
              authors = "Extracted Author",
              category = "Computer Science",
              isbn = "978-${System.currentTimeMillis() % 1000000000}",
              callNumber = "QA76.9 .AI9",
              floor = "Floor 2",
              wing = "AI Library Wing",
              rack = "AI-02",
              shelf = "Shelf 2",
              status = "AVAILABLE",
              totalCopies = 3,
              availableCopies = 3,
              coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCZsc-qdCapncfCR_NAShTL7eY-Wxa4na-a0cv9xCKxWIaicGwRrzRwaXbJPrjN5gyzgovqtZSog6r5V3e_f06tivsfdnqmIwTdr6_Nhm-JPthK8dxhiArBbq9Ko4ZQuGpc4wGV_hOzcbViVbHP-aAi6yN4r-mnq3LPq2gFN0n1b1D9CXCzjrViliCSCU_FsGFX97gmmlGqQ8BC5_9sFDjelgvbQGKRla10oG09oGCLSkmdAtTBQOuc",
              synopsis = text,
              isSaved = true
            )
            val bookId = dao.insertBook(newBook)
            toggleSave(newBook.copy(id = bookId, isSaved = true))
            onResult(true, "Cataloged with AI summary: $fallbackTitle")
          }
        } else {
          onResult(false, "Gemini API error: ${response.code()}")
        }
      } catch (e: Exception) {
        onResult(false, "OCR Exception: ${e.localizedMessage}")
      }
    }
  }

  private val _librarySearchQuery = MutableStateFlow("")
  val librarySearchQuery: StateFlow<String> = _librarySearchQuery

  private val _librarySearchFilter = MutableStateFlow("All")
  val librarySearchFilter: StateFlow<String> = _librarySearchFilter

  fun setLibrarySearchQuery(query: String) {
    _librarySearchQuery.value = query
  }

  fun setLibrarySearchFilter(filter: String) {
    _librarySearchFilter.value = filter
  }

  val allCategories: StateFlow<List<String>> = dao.getAllCategories()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val filteredLibraryBooks: StateFlow<List<BookEntity>> = combine(
    allBooks,
    _librarySearchQuery,
    _librarySearchFilter,
    _selectedCategory
  ) { books, query, filter, category ->
    books.filter { book ->
      val matchesCategory = category == null || category == "All" || book.category.equals(category, ignoreCase = true)
      val matchesQuery = if (query.isBlank()) {
        true
      } else {
        val q = query.lowercase()
        when (filter) {
          "Title" -> book.title.lowercase().contains(q)
          "Author" -> book.authors.lowercase().contains(q)
          "OCR Content" -> book.synopsis.lowercase().contains(q)
          else -> book.title.lowercase().contains(q) ||
                  book.authors.lowercase().contains(q) ||
                  book.synopsis.lowercase().contains(q) ||
                  book.callNumber.lowercase().contains(q) ||
                  book.isbn.lowercase().contains(q)
        }
      }
      matchesCategory && matchesQuery
    }
  }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val allReservations: StateFlow<List<BookReservationEntity>> = bookReservationDao.getAllReservations()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val userReservations: StateFlow<List<BookReservationEntity>> = combine(
    bookReservationDao.getAllReservations(),
    _currentUser
  ) { reservations, user ->
    val email = user?.email ?: "student@university.edu"
    reservations.filter { it.userEmail.equals(email, ignoreCase = true) || it.userEmail == "guest@device" }
  }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  fun reserveBook(
    book: BookEntity,
    pickupLocation: String = "Main Circulation Desk - Floor 1",
    studentNotes: String = "",
    onSuccess: (Long) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val user = _currentUser.value
        val email = user?.email ?: "student@university.edu"
        val name = if (!user?.name.isNullOrBlank()) user!!.name else "Student Reader (${email.substringBefore("@")})"

        val newReservation = BookReservationEntity(
          bookId = book.id,
          bookTitle = book.title,
          bookAuthors = book.authors,
          bookCoverUrl = book.coverUrl,
          bookCallNumber = book.callNumber,
          userEmail = email,
          userName = name,
          status = "PENDING",
          requestDate = System.currentTimeMillis(),
          pickupLocation = pickupLocation,
          studentNotes = studentNotes,
          adminComment = ""
        )

        val id = bookReservationDao.insertReservation(newReservation)
        onSuccess(id)
      } catch (e: Exception) {
        onError("Reservation failed: ${e.localizedMessage ?: "Unknown error"}")
      }
    }
  }

  fun approveReservation(
    reservationId: Long,
    adminComment: String = "Approved by Admin. Ready for pickup at the counter with Student ID."
  ) {
    viewModelScope.launch {
      bookReservationDao.updateStatus(reservationId, "APPROVED", adminComment)
    }
  }

  fun declineReservation(
    reservationId: Long,
    reason: String = "Copies currently reserved or unavailable for external checkout."
  ) {
    viewModelScope.launch {
      bookReservationDao.updateStatus(reservationId, "DECLINED", reason)
    }
  }

  fun cancelReservation(reservationId: Long) {
    viewModelScope.launch {
      bookReservationDao.updateStatus(reservationId, "CANCELLED", "Cancelled by student.")
    }
  }

  fun deleteReservation(reservationId: Long) {
    viewModelScope.launch {
      bookReservationDao.deleteById(reservationId)
    }
  }
}
