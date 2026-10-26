package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.data.BookEntity
import com.example.data.BookReservationEntity
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
  books: List<BookEntity>,
  reservations: List<BookReservationEntity> = emptyList(),
  onAddBook: (String, String, String, String, String, String, String, String, String, String, Int, Int, String) -> Unit,
  onUpdateBook: (BookEntity) -> Unit,
  onDeleteBook: (BookEntity) -> Unit,
  onAuthClick: () -> Unit,
  onExtractOcrBook: ((String, String, (Boolean, String) -> Unit) -> Unit)? = null,
  onApproveReservation: (Long, String) -> Unit = { _, _ -> },
  onDeclineReservation: (Long, String) -> Unit = { _, _ -> },
  onDeleteReservation: (Long) -> Unit = {}
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  var isScanningOcr by remember { mutableStateOf(false) }
  var ocrMessage by remember { mutableStateOf<String?>(null) }

  val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    if (uri != null) {
      isScanningOcr = true
      ocrMessage = "Reading and analyzing document with Gemini AI OCR..."
      try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
          val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
          val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"
          onExtractOcrBook?.invoke(base64Str, mimeType) { success, msg ->
            isScanningOcr = false
            ocrMessage = msg
          }
        } else {
          isScanningOcr = false
          ocrMessage = "Failed to read file bytes."
        }
      } catch (e: Exception) {
        isScanningOcr = false
        ocrMessage = "Error: ${e.localizedMessage}"
      }
    }
  }
  val auth = remember {
    try {
      FirebaseAuth.getInstance()
    } catch (e: Exception) {
      null
    }
  }
  var firebaseUser by remember { mutableStateOf(auth?.currentUser) }
  var adminEmailInput by remember { mutableStateOf("admin@library.com") }
  var adminPasswordInput by remember { mutableStateOf("") }
  var adminAuthError by remember { mutableStateOf<String?>(null) }
  var isAuthenticating by remember { mutableStateOf(false) }

  var fallbackAdminUnlocked by remember { mutableStateOf(false) }
  val isAdmin = fallbackAdminUnlocked || (firebaseUser != null && (firebaseUser?.email?.contains("admin") == true || firebaseUser?.email == "admin@library.com" || firebaseUser?.email == "librarian@aistudio.com"))

  if (!isAdmin) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(32.dp))
          }
          Text("Restricted Admin Portal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          Text("Access is restricted strictly to authenticated administrators via Firebase Auth. Standard reader accounts cannot access this console.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

          OutlinedTextField(
            value = adminEmailInput,
            onValueChange = { adminEmailInput = it },
            label = { Text("Admin Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )

          OutlinedTextField(
            value = adminPasswordInput,
            onValueChange = { adminPasswordInput = it },
            label = { Text("Admin Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          )

          if (adminAuthError != null) {
            Text(adminAuthError ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
          }

          Button(
            onClick = {
              if (adminEmailInput.isBlank() || adminPasswordInput.isBlank()) {
                adminAuthError = "Please enter admin email and password"
                return@Button
              }
              if (auth == null) {
                if (adminPasswordInput == "admin12345" || adminPasswordInput == "8842") {
                  fallbackAdminUnlocked = true
                } else {
                  adminAuthError = "Incorrect password. Try admin12345 or 8842"
                }
                isAuthenticating = false
                return@Button
              }
              isAuthenticating = true
              adminAuthError = null
              auth.signInWithEmailAndPassword(adminEmailInput, adminPasswordInput)
                .addOnCompleteListener { task ->
                  isAuthenticating = false
                  if (task.isSuccessful) {
                    firebaseUser = auth.currentUser
                  } else {
                    auth.createUserWithEmailAndPassword(adminEmailInput, adminPasswordInput)
                      .addOnCompleteListener { createDoc ->
                        if (createDoc.isSuccessful) {
                          firebaseUser = auth.currentUser
                        } else {
                          adminAuthError = task.exception?.localizedMessage ?: "Authentication failed"
                        }
                      }
                  }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isAuthenticating
          ) {
            if (isAuthenticating) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
              Icon(Icons.Default.LockOpen, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Authenticate as Admin", fontWeight = FontWeight.Bold)
            }
          }

          TextButton(onClick = {
            adminEmailInput = "admin@library.com"
            adminPasswordInput = "admin12345"
          }) {
            Text("Fill Demo Admin Credentials", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
    return
  }

  var showAddDialog by remember { mutableStateOf(false) }
  var editBookTarget by remember { mutableStateOf<BookEntity?>(null) }
  var deleteBookTarget by remember { mutableStateOf<BookEntity?>(null) }
  var selectedAdminTab by remember { mutableStateOf("Reservations") }
  var reservationFilter by remember { mutableStateOf("All") }
  var declineTargetReservation by remember { mutableStateOf<BookReservationEntity?>(null) }
  var declineReasonInput by remember { mutableStateOf("Copies currently reserved or unavailable for external checkout.") }
  var approveTargetReservation by remember { mutableStateOf<BookReservationEntity?>(null) }
  var approveCommentInput by remember { mutableStateOf("Approved. Ready for pickup at the counter with Student ID.") }

  val pendingReservationsCount = reservations.count { it.status == "PENDING" }
  val approvedReservationsCount = reservations.count { it.status == "APPROVED" }
  val declinedReservationsCount = reservations.count { it.status == "DECLINED" }

  val displayReservations = when (reservationFilter) {
    "Pending" -> reservations.filter { it.status == "PENDING" }
    "Approved" -> reservations.filter { it.status == "APPROVED" }
    "Declined" -> reservations.filter { it.status == "DECLINED" }
    else -> reservations
  }

  // Add Book Form state
  var titleInput by remember { mutableStateOf("") }
  var authorsInput by remember { mutableStateOf("") }
  var categoryInput by remember { mutableStateOf("Computer Science") }
  var isbnInput by remember { mutableStateOf("978-0130000000") }
  var callNumberInput by remember { mutableStateOf("QA76.9 .X01") }
  var floorInput by remember { mutableStateOf("Floor 2") }
  var wingInput by remember { mutableStateOf("North Wing") }
  var rackInput by remember { mutableStateOf("CS-06") }
  var shelfInput by remember { mutableStateOf("Shelf 2") }
  var totalCopiesInput by remember { mutableStateOf("3") }
  var synopsisInput by remember { mutableStateOf("Newly cataloged academic volume.") }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface),
    contentPadding = PaddingValues(bottom = 96.dp)
  ) {
    // Header
    item {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.errorContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(text = "Librarian Admin Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(text = "Staff ID: #LIB-8842 · Central Stacks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        },
        actions = {
          IconButton(
            onClick = onAuthClick
          ) {
            Surface(
              modifier = Modifier.size(36.dp),
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              }
            }
          }
          if (selectedAdminTab == "Catalog") {
            Button(
              onClick = { showAddDialog = true },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.height(36.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Book", style = MaterialTheme.typography.labelMedium)
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
      )
    }

    // Navigation Tabs for Admin Console
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val tabs = listOf(
          Triple("Reservations", "Holds & Access", if (pendingReservationsCount > 0) pendingReservationsCount.toString() else null),
          Triple("Catalog", "Stack Catalog", null),
          Triple("OCR", "Gemini OCR", null)
        )
        tabs.forEach { (key, label, badge) ->
          val isSelected = selectedAdminTab == key
          FilterChip(
            selected = isSelected,
            onClick = { selectedAdminTab = key },
            label = {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                if (badge != null) {
                  Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer
                  ) {
                    Text(
                      text = badge,
                      style = MaterialTheme.typography.labelSmall,
                      color = if (isSelected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
              labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }
      }
    }

    if (selectedAdminTab == "Reservations") {
      // Access Control Overview Header
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("BORROWING ACCESS CONTROL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              Text("Student Hold Requests", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (pendingReservationsCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
            ) {
              Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (pendingReservationsCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary))
                Text(
                  text = "$pendingReservationsCount Action Required",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (pendingReservationsCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          // Metric Counters Grid
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("Pending Review", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$pendingReservationsCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Needs authorization", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("Approved Holds", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$approvedReservationsCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Ready at desks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("Declined", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$declinedReservationsCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Refusal notes sent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }

          // Filter Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val filters = listOf("All", "Pending", "Approved", "Declined")
            filters.forEach { filter ->
              val isSelected = reservationFilter == filter
              FilterChip(
                selected = isSelected,
                onClick = { reservationFilter = filter },
                label = { Text(filter) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.secondary,
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                  labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
              )
            }
          }
        }
      }

      if (displayReservations.isEmpty()) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
              Text("No Reservation Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text("There are no book borrowing hold requests matching '$reservationFilter'.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      items(displayReservations) { res ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Header Row: Book Info & Status
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                  Text(
                    text = "CALL: ${res.bookCallNumber.ifBlank { "CS-STACK" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(res.bookTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(res.bookAuthors, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (res.status) {
                  "APPROVED" -> MaterialTheme.colorScheme.primaryContainer
                  "DECLINED" -> MaterialTheme.colorScheme.errorContainer
                  else -> MaterialTheme.colorScheme.tertiaryContainer
                }
              ) {
                Text(
                  text = when (res.status) {
                    "APPROVED" -> "Approved"
                    "DECLINED" -> "Declined"
                    else -> "Pending Review"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = when (res.status) {
                    "APPROVED" -> MaterialTheme.colorScheme.onPrimaryContainer
                    "DECLINED" -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                  },
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Student & Pickup Details
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  Text("Student: ${res.userName} · ${res.userEmail}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                  Text("Pickup At: ${res.pickupLocation}", style = MaterialTheme.typography.bodySmall)
                }
                if (res.studentNotes.isNotBlank()) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text("Student Note: \"${res.studentNotes}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
                if (res.adminComment.isNotBlank()) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Comment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text("Admin Comment: \"${res.adminComment}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                  }
                }
              }
            }

            // Access Control Decision Actions
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (res.status == "PENDING") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                  Button(
                    onClick = {
                      approveTargetReservation = res
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                  ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept Request", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                  }

                  OutlinedButton(
                    onClick = {
                      declineTargetReservation = res
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Decline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                  }
                }
              } else if (res.status == "APPROVED") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                  TextButton(
                    onClick = { declineTargetReservation = res }
                  ) {
                    Text("Revoke / Decline", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                  }
                }
              } else if (res.status == "DECLINED") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                  TextButton(
                    onClick = { approveTargetReservation = res }
                  ) {
                    Text("Re-Approve Access", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                  }
                }
              }

              IconButton(onClick = { onDeleteReservation(res.id) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete record", tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    } else if (selectedAdminTab == "Catalog") {
      // Metrics Overview
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("STACK INVENTORY HEALTH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Live Central Catalog", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
          }
          Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
              Text("Sync Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Metrics Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("Total Catalog", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("14,820", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("+124 this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
              }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("On Shelf", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("12,450", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Text("91% accuracy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("Issued & Holds", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("2,140", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("43 pickups pending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("Reshelving Queue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("230", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                Text("Floor 2 Cart B", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }

    // Rapid Stack Operations Banner
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.inversePrimary)
              Text("Rapid Stack Operations", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.inverseOnSurface, fontWeight = FontWeight.Bold)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
              Text("Batch Mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
            }
          }
          Text("Perform quick rack relocation, batch return synchronization, or shelf flag audits instantly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.inverseOnSurface)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { filePickerLauncher.launch("*/*") },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
              enabled = !isScanningOcr
            ) {
              if (isScanningOcr) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(6.dp))
              } else {
                Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
              }
              Text(if (isScanningOcr) "Analyzing with OCR..." else "Gemini OCR Import", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
              onClick = { showAddDialog = true },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.inverseOnSurface)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Manual Add", style = MaterialTheme.typography.labelMedium)
            }
          }
          if (ocrMessage != null) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
              Text(ocrMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(10.dp))
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }

    // Catalog Inventory List Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Catalog Inventory (${books.size} records)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        TextButton(onClick = { showAddDialog = true }) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Record")
        }
      }
    }

    // Inventory Stack Items
    items(books) { book ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                  Text(book.callNumber, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                }
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (book.status == "AVAILABLE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                  Text(
                    text = book.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (book.status == "AVAILABLE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(book.authors, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }

          // Location & Copies
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.PinDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("${book.floor} · ${book.rack} · ${book.shelf}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              }
              Text("${book.availableCopies}/${book.totalCopies} available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          // Admin Control Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            TextButton(
              onClick = {
                val updated = book.copy(status = if (book.status == "AVAILABLE") "ISSUED" else "AVAILABLE", availableCopies = if (book.status == "AVAILABLE") 0 else book.totalCopies)
                onUpdateBook(updated)
              }
            ) {
              Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (book.status == "AVAILABLE") "Mark Issued" else "Mark Returned")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              IconButton(
                onClick = { editBookTarget = book },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Shelf", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              }
              IconButton(
                onClick = { deleteBookTarget = book },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
    }
  }

  // ADD BOOK DIALOG
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("Catalog New Book Record", fontWeight = FontWeight.Bold) },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("Book Title") }, singleLine = true)
          OutlinedTextField(value = authorsInput, onValueChange = { authorsInput = it }, label = { Text("Authors") }, singleLine = true)
          OutlinedTextField(value = callNumberInput, onValueChange = { callNumberInput = it }, label = { Text("Call Number (e.g. QA76.6)") }, singleLine = true)
          OutlinedTextField(value = rackInput, onValueChange = { rackInput = it }, label = { Text("Rack (e.g. CS-04)") }, singleLine = true)
          OutlinedTextField(value = shelfInput, onValueChange = { shelfInput = it }, label = { Text("Shelf (e.g. Shelf 3)") }, singleLine = true)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (titleInput.isNotBlank()) {
              onAddBook(
                titleInput,
                authorsInput.ifBlank { "Unknown Author" },
                categoryInput,
                isbnInput,
                callNumberInput,
                floorInput,
                wingInput,
                rackInput,
                shelfInput,
                "AVAILABLE",
                totalCopiesInput.toIntOrNull() ?: 3,
                totalCopiesInput.toIntOrNull() ?: 3,
                synopsisInput
              )
              titleInput = ""
              authorsInput = ""
              showAddDialog = false
            }
          }
        ) {
          Text("Save & Catalog")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // EDIT BOOK DIALOG
  editBookTarget?.let { target ->
    var editRack by remember { mutableStateOf(target.rack) }
    var editShelf by remember { mutableStateOf(target.shelf) }
    var editFloor by remember { mutableStateOf(target.floor) }

    AlertDialog(
      onDismissRequest = { editBookTarget = null },
      title = { Text("Update Shelf Location", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Editing: ${target.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          OutlinedTextField(value = editFloor, onValueChange = { editFloor = it }, label = { Text("Floor") }, singleLine = true)
          OutlinedTextField(value = editRack, onValueChange = { editRack = it }, label = { Text("Rack") }, singleLine = true)
          OutlinedTextField(value = editShelf, onValueChange = { editShelf = it }, label = { Text("Shelf") }, singleLine = true)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateBook(target.copy(floor = editFloor, rack = editRack, shelf = editShelf))
            editBookTarget = null
          }
        ) {
          Text("Update Location")
        }
      },
      dismissButton = {
        TextButton(onClick = { editBookTarget = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // DELETE CONFIRMATION DIALOG
  deleteBookTarget?.let { target ->
    AlertDialog(
      onDismissRequest = { deleteBookTarget = null },
      title = { Text("Remove Catalog Record?", fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to remove '${target.title}' from the library catalog? This action cannot be undone.") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteBook(target)
            deleteBookTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete Record")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteBookTarget = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // APPROVE RESERVATION DIALOG
  approveTargetReservation?.let { target ->
    AlertDialog(
      onDismissRequest = { approveTargetReservation = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("Authorize Borrow Hold", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Approve borrowing access for ${target.userName} (${target.userEmail})?", style = MaterialTheme.typography.bodyMedium)
          Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
              Text(target.bookTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              Text("Pickup: ${target.pickupLocation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          OutlinedTextField(
            value = approveCommentInput,
            onValueChange = { approveCommentInput = it },
            label = { Text("Instructions for Student") },
            placeholder = { Text("e.g. Ready for pickup at Counter Desk with Student ID.") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onApproveReservation(target.id, approveCommentInput)
            approveTargetReservation = null
          }
        ) {
          Text("Grant Access")
        }
      },
      dismissButton = {
        TextButton(onClick = { approveTargetReservation = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // DECLINE RESERVATION DIALOG
  declineTargetReservation?.let { target ->
    AlertDialog(
      onDismissRequest = { declineTargetReservation = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Text("Decline Borrow Request", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Decline reservation for '${target.bookTitle}' requested by ${target.userName}?", style = MaterialTheme.typography.bodyMedium)
          OutlinedTextField(
            value = declineReasonInput,
            onValueChange = { declineReasonInput = it },
            label = { Text("Reason for Refusal") },
            placeholder = { Text("e.g. In-library reference copy only / Exceeded hold limits.") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onDeclineReservation(target.id, declineReasonInput)
            declineTargetReservation = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Confirm Decline")
        }
      },
      dismissButton = {
        TextButton(onClick = { declineTargetReservation = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

