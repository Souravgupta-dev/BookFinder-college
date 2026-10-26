package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.BookEntity
import com.example.data.SearchQueryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  books: List<BookEntity>,
  recentSearches: List<SearchQueryEntity>,
  onSaveSearchQuery: (String) -> Unit,
  onDeleteSearchQuery: (Long) -> Unit,
  onClearRecentSearches: () -> Unit,
  onBookClick: (Long) -> Unit,
  onWayfinderClick: (Long) -> Unit,
  onFilterClick: () -> Unit,
  onToggleSave: (BookEntity) -> Unit,
  onAuthClick: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var showFilterCard by remember { mutableStateOf(false) }
  var currentSort by remember { mutableStateOf("Relevance") }
  var showSortMenu by remember { mutableStateOf(false) }
  var availableOnly by remember { mutableStateOf(false) }
  var selectedCategory by remember { mutableStateOf("") }
  var floorFilterActive by remember { mutableStateOf(false) }

  val activeFilterCount = listOf(selectedCategory.isNotEmpty(), floorFilterActive, availableOnly).count { it }

  LaunchedEffect(searchQuery) {
    if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
      kotlinx.coroutines.delay(1200)
      onSaveSearchQuery(searchQuery)
    }
  }

  val filteredBooks = books.filter { book ->
    val matchesQuery = searchQuery.isBlank() ||
      book.title.contains(searchQuery, ignoreCase = true) ||
      book.authors.contains(searchQuery, ignoreCase = true) ||
      book.callNumber.contains(searchQuery, ignoreCase = true) ||
      book.isbn.contains(searchQuery, ignoreCase = true)
    val matchesCategory = selectedCategory.isBlank() || book.category.contains(selectedCategory, ignoreCase = true)
    val matchesFloor = !floorFilterActive || book.floor.contains("Floor 2", ignoreCase = true)
    val matchesAvail = !availableOnly || book.status == "AVAILABLE"
    matchesQuery && matchesCategory && matchesFloor && matchesAvail
  }

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
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "BookFinder",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Search",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          IconButton(onClick = onAuthClick) {
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
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
      )
    }

    // Search Bar & Controls
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Search Field
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search ISBN, title, or author...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
          trailingIcon = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(end = 4.dp)) {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Cancel, contentDescription = "Clear", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                }
              }
              IconButton(onClick = {}) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              }
            }
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
          ),
          singleLine = true,
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
          keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { /* perform search */ })
        )

        if (recentSearches.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Text("Recent Searches (Room DB)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              }
              TextButton(onClick = onClearRecentSearches, contentPadding = PaddingValues(0.dp)) {
                Text("Clear All", style = MaterialTheme.typography.labelSmall)
              }
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              recentSearches.forEach { item ->
                InputChip(
                  selected = searchQuery.equals(item.query, ignoreCase = true),
                  onClick = { searchQuery = item.query },
                  label = { Text(item.query) },
                  trailingIcon = {
                    Icon(
                      Icons.Default.Close,
                      contentDescription = "Delete",
                      modifier = Modifier
                        .size(14.dp)
                        .clickable { onDeleteSearchQuery(item.id) }
                    )
                  },
                  colors = InputChipDefaults.inputChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                  )
                )
              }
            }
          }
        }

        // Trending Searches Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
              Text("Trending Academic Topics", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
          }
          val trendingTopics = listOf("Artificial Intelligence", "Data Structures", "Machine Learning", "Quantum Physics", "Linear Algebra", "Compiler Design", "Economics")
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            trendingTopics.forEach { topic ->
              SuggestionChip(
                onClick = { searchQuery = topic },
                label = { Text(topic) },
                icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                  labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
              )
            }
          }
        }


        // Meta & Status Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
            )
            Text(
              text = "${filteredBooks.size} books found in Central Library",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          if (floorFilterActive) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
              Text(
                text = "Floor 2 Active",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        // Action Controls Row: Filter, Sort, Toggle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = { showFilterCard = !showFilterCard },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Filters", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.secondaryContainer,
              modifier = Modifier.size(18.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = activeFilterCount.toString(),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Box {
            OutlinedButton(
              onClick = { showSortMenu = !showSortMenu },
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
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
              Text("Available Only", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
              Switch(
                checked = availableOnly,
                onCheckedChange = { availableOnly = it },
                modifier = Modifier.scale(0.7f),
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onSecondary, checkedTrackColor = MaterialTheme.colorScheme.secondary)
              )
            }
          }
        }

        // Active Chips Strip
        if (selectedCategory.isNotEmpty() || floorFilterActive || availableOnly) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (selectedCategory.isNotEmpty()) {
              AssistChip(
                onClick = { selectedCategory = "" },
                label = { Text(selectedCategory) },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Close category", modifier = Modifier.size(14.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
              )
            }
            if (floorFilterActive) {
              AssistChip(
                onClick = { floorFilterActive = false },
                label = { Text("Floor 2") },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Close floor", modifier = Modifier.size(14.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
              )
            }
            if (availableOnly) {
              AssistChip(
                onClick = { availableOnly = false },
                label = { Text("Available Only") },
                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Close available", modifier = Modifier.size(14.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
              )
            }
            TextButton(
              onClick = {
                selectedCategory = ""
                floorFilterActive = false
                availableOnly = false
                searchQuery = ""
              }
            ) {
              Text("Reset", style = MaterialTheme.typography.labelMedium)
            }
          }
        }

        // Collapsible Filter Card Drawer
        if (showFilterCard) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(16.dp)
          ) {
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                  Text("Refine Catalog Search", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { showFilterCard = false }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.ExpandLess, contentDescription = "Close")
                }
              }

              // Filter Groups
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DISCIPLINE & CATEGORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  val cats = listOf("Computer Science", "Engineering", "Mathematics")
                  cats.forEach { cat ->
                    val isSel = selectedCategory.equals(cat, ignoreCase = true)
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                      modifier = Modifier.clickable { selectedCategory = if (isSel) "" else cat }
                    ) {
                      Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isSel) {
                          Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                        }
                        Text(cat, style = MaterialTheme.typography.labelMedium, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                      }
                    }
                  }
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LOCATION & FLOOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  val isFloor2Sel = floorFilterActive
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isFloor2Sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.clickable { floorFilterActive = !floorFilterActive }
                  ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      if (isFloor2Sel) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                      }
                      Text("Floor 2", style = MaterialTheme.typography.labelMedium, color = if (isFloor2Sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Book Results List or Empty State
    if (filteredBooks.isEmpty()) {
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.SearchOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(40.dp)
            )
          }
          Text(
            text = "No matching books found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Try adjusting your search query, checking for typos, or resetting your active filters.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
          Button(
            onClick = {
              searchQuery = ""
              selectedCategory = ""
              floorFilterActive = false
              availableOnly = false
            },
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear Search & Filters")
          }
        }
      }
    }

    items(filteredBooks) { book ->
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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
          ) {
            Box(
              modifier = Modifier
                .width(72.dp)
                .height(100.dp)
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
                text = "${book.category} · ${book.isbn}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )

              Spacer(modifier = Modifier.height(8.dp))

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
                    text = if (book.status == "AVAILABLE") "AVAILABLE (${book.availableCopies} copies)" else "ISSUED (Due ${book.dueDate ?: "Soon"})",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (book.status == "AVAILABLE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          // Exact Spatial Wayfinding Banner
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.PinDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                  text = "${book.floor} · Sec: CS · Rack: ${book.rack} · ${book.shelf}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Text(
                text = book.callNumber,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Card Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = { onWayfinderClick(book.id) },
              modifier = Modifier
                .weight(1f)
                .height(42.dp),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Find on Shelf", style = MaterialTheme.typography.labelLarge)
            }

            IconButton(
              onClick = { onToggleSave(book) },
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
              Icon(
                imageVector = if (book.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (book.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
