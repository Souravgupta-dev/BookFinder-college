package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.BookEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
  categoryName: String,
  books: List<BookEntity>,
  onBackClick: () -> Unit,
  onBookClick: (Long) -> Unit,
  onWayfinderClick: (Long) -> Unit,
  onToggleSave: (BookEntity) -> Unit
) {
  var filterQuery by remember { mutableStateOf("") }
  var availableOnly by remember { mutableStateOf(false) }
  var currentSort by remember { mutableStateOf("Relevance") }
  var showSortMenu by remember { mutableStateOf(false) }

  // Filter books matching category and search/filter criteria
  val categoryBooks = books.filter { it.category.equals(categoryName, ignoreCase = true) || categoryName == "All" }

  val filteredBooks = categoryBooks.filter { book ->
    val matchesQuery = filterQuery.isBlank() ||
      book.title.contains(filterQuery, ignoreCase = true) ||
      book.authors.contains(filterQuery, ignoreCase = true) ||
      book.callNumber.contains(filterQuery, ignoreCase = true) ||
      book.isbn.contains(filterQuery, ignoreCase = true)
    val matchesAvail = !availableOnly || book.status == "AVAILABLE"
    matchesQuery && matchesAvail
  }.sortedWith(
    when (currentSort) {
      "Title A–Z" -> compareBy { it.title }
      "Author A–Z" -> compareBy { it.authors }
      "Recently Added" -> compareByDescending { it.id }
      else -> compareByDescending { it.availableCopies }
    }
  )

  var isVisible by remember { mutableStateOf(false) }
  LaunchedEffect(filteredBooks) {
    isVisible = false
    kotlinx.coroutines.delay(30)
    isVisible = true
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
              Icon(
                imageVector = when {
                  categoryName.contains("User Added", true) -> Icons.Default.LibraryAdd
                  categoryName.contains("Computer", true) -> Icons.Default.Terminal
                  categoryName.contains("Math", true) -> Icons.Default.Calculate
                  categoryName.contains("Engineering", true) -> Icons.Default.PrecisionManufacturing
                  categoryName.contains("Science", true) -> Icons.Default.Biotech
                  categoryName.contains("Literature", true) -> Icons.Default.AutoStories
                  else -> Icons.Default.MenuBook
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${filteredBooks.size} books available",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBackClick, modifier = Modifier.testTag("category_back_btn")) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
      )
    },
    containerColor = MaterialTheme.colorScheme.surface
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Search and Filter Bar inside Category Screen
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = filterQuery,
          onValueChange = { filterQuery = it },
          placeholder = { Text("Filter $categoryName books by title, author...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
          trailingIcon = {
            if (filterQuery.isNotEmpty()) {
              IconButton(onClick = { filterQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("category_search_input"),
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
          ),
          singleLine = true
        )

        // Filter Controls Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box {
            OutlinedButton(
              onClick = { showSortMenu = !showSortMenu },
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text("Sort: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(currentSort, style = MaterialTheme.typography.labelMedium)
              Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false }
            ) {
              listOf("Relevance", "Title A–Z", "Author A–Z", "Recently Added").forEach { opt ->
                DropdownMenuItem(
                  text = { Text(opt, fontWeight = if (currentSort == opt) FontWeight.Bold else FontWeight.Normal) },
                  onClick = {
                    currentSort = opt
                    showSortMenu = false
                  }
                )
              }
            }
          }

          // Available Only Switch
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text("Available Only", style = MaterialTheme.typography.labelSmall)
              Switch(
                checked = availableOnly,
                onCheckedChange = { availableOnly = it },
                modifier = Modifier.scale(0.7f)
              )
            }
          }
        }
      }

      // Animated Results Transition & Fade-In
      AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                  initialOffsetY = { it / 15 },
                  animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
        exit = fadeOut(animationSpec = tween(150))
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .animateContentSize(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (filteredBooks.isEmpty()) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(48.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                  Text("No books found matching filters", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text("Try adjusting your search query or availability switch.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
              }
            }
          } else {
            items(filteredBooks, key = { it.id }) { book ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
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
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Box(
                      modifier = Modifier
                        .width(68.dp)
                        .height(96.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                      contentAlignment = Alignment.Center
                    ) {
                      AsyncImage(
                        model = book.coverUrl,
                        contentDescription = book.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                      )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = book.authors,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = "Call No: ${book.callNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                      )

                      Spacer(modifier = Modifier.height(6.dp))

                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (book.status == "AVAILABLE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          Icon(
                            imageVector = if (book.status == "AVAILABLE") Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (book.status == "AVAILABLE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(14.dp)
                          )
                          Text(
                            text = if (book.status == "AVAILABLE") "AVAILABLE (${book.availableCopies})" else "ISSUED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (book.status == "AVAILABLE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                          )
                        }
                      }
                    }
                  }

                  // Location Bar & Actions
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Icon(Icons.Default.PinDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                      Text(
                        text = "${book.floor} · Rack: ${book.rack}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Button(
                        onClick = { onWayfinderClick(book.id) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                      ) {
                        Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Locate", style = MaterialTheme.typography.labelMedium)
                      }

                      IconButton(
                        onClick = { onToggleSave(book) },
                        modifier = Modifier
                          .size(36.dp)
                          .clip(RoundedCornerShape(8.dp))
                          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                      ) {
                        Icon(
                          imageVector = if (book.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                          contentDescription = "Bookmark",
                          tint = if (book.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }
          item {
            Spacer(modifier = Modifier.height(64.dp))
          }
        }
      }
    }
  }
}
