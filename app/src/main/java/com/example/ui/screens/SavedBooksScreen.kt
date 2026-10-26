package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.BookEntity
import com.example.data.BookReservationEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedBooksScreen(
  savedBooks: List<BookEntity>,
  onBookClick: (Long) -> Unit,
  onWayfinderClick: (Long) -> Unit,
  onToggleSave: (BookEntity) -> Unit,
  onImportPdfBook: (String, String, String, String) -> Unit,
  onUpdateProgress: (Long, Int, Int) -> Unit,
  onUpdateTags: (Long, String) -> Unit,
  onAuthClick: () -> Unit,
  userReservations: List<BookReservationEntity> = emptyList(),
  onCancelReservation: (Long) -> Unit = {}
) {
  var selectedTab by remember { mutableStateOf("All Saved") }
  var showWireframe by remember { mutableStateOf(true) }
  var showImportDialog by remember { mutableStateOf(false) }
  var bookTitleInput by remember { mutableStateOf("") }
  var authorInput by remember { mutableStateOf("") }
  var categoryInput by remember { mutableStateOf("Personal Documents") }
  var selectedPdfUri by remember { mutableStateOf<String?>(null) }

  var showProgressDialog by remember { mutableStateOf(false) }
  var selectedBookForProgress by remember { mutableStateOf<BookEntity?>(null) }
  var currentPageInput by remember { mutableStateOf("") }
  var totalPagesInput by remember { mutableStateOf("") }

  var showTagsDialog by remember { mutableStateOf(false) }
  var selectedBookForTags by remember { mutableStateOf<BookEntity?>(null) }
  var tagsInput by remember { mutableStateOf("") }

  val pdfPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      selectedPdfUri = uri.toString()
      showImportDialog = true
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(text = "BookFinder", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              Text(text = "Saved Books & PDF Library", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        },
        actions = {
          IconButton(
            onClick = onAuthClick,
            modifier = Modifier.testTag("saved_auth_btn")
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
          IconButton(
            onClick = { pdfPickerLauncher.launch("application/pdf") },
            modifier = Modifier.testTag("import_pdf_top_btn")
          ) {
            Icon(Icons.Default.UploadFile, contentDescription = "Import PDF", tint = MaterialTheme.colorScheme.primary)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { pdfPickerLauncher.launch("application/pdf") },
        icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
        text = { Text("Import PDF Book") },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.testTag("import_pdf_fab")
      )
    },
    containerColor = MaterialTheme.colorScheme.surface
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.surface),
      contentPadding = PaddingValues(bottom = 96.dp)
    ) {

    // Page Header & Context
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("PERSONAL LIBRARY STACKS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Saved Books & Reading List", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
          }
        }

        // Offline Sync Status Bar
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
              Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
              Column {
                Text("${savedBooks.size} Saved Books · ${savedBooks.count { it.status == "AVAILABLE" }} Available Now", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(Icons.Default.OfflinePin, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                  Text("Offline Stack Wayfinding Active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
              }
            }
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
              Text("Floor 2 Cache", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Segmented Filter Tabs
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val tabs = listOf(
          "All Saved (${savedBooks.size})",
          "Holds & Reserved (${userReservations.size})",
          "Available on Shelf",
          "Checked Out",
          "Reading List (CS301)"
        )
        tabs.forEach { tabName ->
          val isSelected = selectedTab == tabName || (tabName.startsWith("Holds") && selectedTab.startsWith("Holds")) || (tabName.startsWith("All") && selectedTab.startsWith("All"))
          FilterChip(
            selected = isSelected,
            onClick = { selectedTab = tabName },
            label = { Text(tabName) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              containerColor = MaterialTheme.colorScheme.surfaceContainer,
              labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    if (selectedTab.startsWith("Holds")) {
      if (userReservations.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
              Text("No Active Borrow Holds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text("You have not placed any borrow reservations yet. Search or browse any book and tap 'Reserve / Borrow' to send an access request to the library admin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
          }
        }
      } else {
        items(userReservations) { res ->
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
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                  ) {
                    Text(
                      text = res.bookCallNumber.ifBlank { "STACK HOLD" },
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
                      "APPROVED" -> "Approved - Ready"
                      "DECLINED" -> "Declined"
                      else -> "Pending Approval"
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

              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text("Pickup Desk: ${res.pickupLocation}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                  }
                  if (res.adminComment.isNotBlank()) {
                    Text("Librarian Note: ${res.adminComment}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                  }
                }
              }

              if (res.status == "PENDING") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                  TextButton(onClick = { onCancelReservation(res.id) }) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel Request", color = MaterialTheme.colorScheme.error)
                  }
                }
              }
            }
          }
        }
      }
    } else {

    // Offline Wayfinder Banner
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
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
              }
              Column {
                Text("Offline Wayfinder Active", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.surfaceContainerLowest, fontWeight = FontWeight.Bold)
                Text("Deep-stack subterranean coverage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.inversePrimary)
              }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
              Text("Ready", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
            }
          }

          Text(
            "Shelf coordinates and multi-wing floor layouts are saved locally. You can trace routes seamlessly even without Wi-Fi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.surfaceDim
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }

    // Route Optimization Bento Banner
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
              Icon(Icons.Default.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("Stack Route Optimizer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
              Text("~4 Min Trip", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
            }
          }
          Text("3 of your saved books are clustered on Floor 2. Walk from Rack CS-04 to MA-02 in one direct path.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Start Multi-Book Route (Floor 2)", style = MaterialTheme.typography.labelLarge)
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }

    // Saved Books List
    items(savedBooks) { book ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .clickable { onBookClick(book.id) },
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
          ) {
            Box(
              modifier = Modifier
                .width(64.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
              contentAlignment = Alignment.Center
            ) {
              AsyncImage(model = book.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }

            Column(modifier = Modifier.weight(1f)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (book.status == "AVAILABLE") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer
                ) {
                  Text(
                    text = if (book.status == "AVAILABLE") "AVAILABLE (${book.availableCopies} copies)" else "ISSUED · Due ${book.dueDate ?: "Soon"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (book.status == "AVAILABLE") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                  )
                }
                IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.MoreVert, contentDescription = null)
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(book.authors, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }

          // Coordinates Box
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  Text("${book.floor} · ${book.wing}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                  Text(book.callNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                }
              }
              Text("Rack ${book.rack} · ${book.shelf}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          // Reading Progress Tracker Component
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  Text("Reading Progress", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Text("${book.readingProgressPercent}% (p. ${book.currentPage}/${book.totalPages})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              }

              LinearProgressIndicator(
                progress = { (book.readingProgressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (book.currentPage == 0) "Not started yet" else "Progress: page ${book.currentPage} of ${book.totalPages}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                  onClick = {
                    selectedBookForProgress = book
                    currentPageInput = book.currentPage.toString()
                    totalPagesInput = book.totalPages.toString()
                    showProgressDialog = true
                  },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                  modifier = Modifier.height(28.dp)
                ) {
                  Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Update Page", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }

          // Custom Tags Component
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
            ) {
              Icon(Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
              book.customTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                  Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            TextButton(
              onClick = {
                selectedBookForTags = book
                tagsInput = book.customTags
                showTagsDialog = true
              },
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit Tags", style = MaterialTheme.typography.labelSmall)
            }
          }

          // Actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
              Text("Offline reminder active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              IconButton(
                onClick = { onToggleSave(book) },
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainer)
              ) {
                Icon(Icons.Default.BookmarkRemove, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
              }

              Button(
                onClick = { onWayfinderClick(book.id) },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp)
              ) {
                Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Locate on Shelf", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }
    }

    // Floor 2 Aisle Preview Delighter Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
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
              Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("Floor 2 Aisle CS-04 Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { showWireframe = !showWireframe }) {
              Text(if (showWireframe) "Hide Wireframe" else "Show Wireframe", style = MaterialTheme.typography.labelMedium)
            }
          }

          if (showWireframe) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("BAY CS-04 (WEST FACE)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text("2 SAVED BOOKS LOCATED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }

                Surface(modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                  Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("S3 (Eye)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {
                      Text("QA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
}

  if (showImportDialog) {
    AlertDialog(
      onDismissRequest = { showImportDialog = false },
      title = { Text("Import PDF Book") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Organize your important PDF book or related file in one place. Please add the book name and author:", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(
            value = bookTitleInput,
            onValueChange = { bookTitleInput = it },
            label = { Text("Book Name / Title *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("import_title_input")
          )
          OutlinedTextField(
            value = authorInput,
            onValueChange = { authorInput = it },
            label = { Text("Author / Creator") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("import_author_input")
          )
          OutlinedTextField(
            value = categoryInput,
            onValueChange = { categoryInput = it },
            label = { Text("Category / Stack") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (bookTitleInput.isNotBlank()) {
              onImportPdfBook(
                bookTitleInput,
                if (authorInput.isBlank()) "Unknown Author" else authorInput,
                categoryInput,
                selectedPdfUri ?: "content://pdf"
              )
              bookTitleInput = ""
              authorInput = ""
              selectedPdfUri = null
              showImportDialog = false
            }
          },
          enabled = bookTitleInput.isNotBlank()
        ) {
          Text("Save & Import")
        }
      },
      dismissButton = {
        TextButton(onClick = { showImportDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showProgressDialog && selectedBookForProgress != null) {
    val b = selectedBookForProgress!!
    AlertDialog(
      onDismissRequest = { showProgressDialog = false },
      title = { Text("Update Reading Progress") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Manually update your current page number or reading progress for '${b.title}':", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(
            value = currentPageInput,
            onValueChange = { currentPageInput = it },
            label = { Text("Current Page") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("current_page_input")
          )
          OutlinedTextField(
            value = totalPagesInput,
            onValueChange = { totalPagesInput = it },
            label = { Text("Total Pages") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("total_pages_input")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val curr = currentPageInput.toIntOrNull() ?: b.currentPage
            val tot = totalPagesInput.toIntOrNull() ?: b.totalPages
            onUpdateProgress(b.id, curr, tot)
            showProgressDialog = false
          }
        ) {
          Text("Save Progress")
        }
      },
      dismissButton = {
        TextButton(onClick = { showProgressDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showTagsDialog && selectedBookForTags != null) {
    val b = selectedBookForTags!!
    AlertDialog(
      onDismissRequest = { showTagsDialog = false },
      title = { Text("Assign Custom Tags") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Assign custom tags (e.g., 'To-read', 'Academic', 'Fiction') separated by commas:", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(
            value = tagsInput,
            onValueChange = { tagsInput = it },
            label = { Text("Tags (comma separated)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("custom_tags_input")
          )
          // Preset chip suggestions
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf("To-read", "Academic", "Fiction", "Reference", "Favorite").forEach { preset ->
              AssistChip(
                onClick = {
                  val currentList = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                  if (!currentList.contains(preset)) {
                    tagsInput = if (tagsInput.isBlank()) preset else "$tagsInput, $preset"
                  }
                },
                label = { Text(preset) }
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateTags(b.id, tagsInput)
            showTagsDialog = false
          }
        ) {
          Text("Save Tags")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTagsDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
