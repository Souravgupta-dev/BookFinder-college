package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun BookDetailsScreen(
  book: BookEntity?,
  onBackClick: () -> Unit,
  onWayfinderClick: (Long) -> Unit,
  onToggleSave: (BookEntity) -> Unit,
  onPdfClick: (Long) -> Unit,
  userReservations: List<BookReservationEntity> = emptyList(),
  onReserveBook: (BookEntity, String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _, _ -> },
  onCancelReservation: (Long) -> Unit = {}
) {
  var showSynopsis by remember { mutableStateOf(true) }
  var showToc by remember { mutableStateOf(false) }
  var showReserveDialog by remember { mutableStateOf(false) }
  var selectedPickupLocation by remember { mutableStateOf("Main Circulation Desk - Floor 1") }
  var studentNoteInput by remember { mutableStateOf("") }
  var reservationFeedback by remember { mutableStateOf<String?>(null) }
  var isSubmittingReservation by remember { mutableStateOf(false) }

  if (book == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Book not found", style = MaterialTheme.typography.titleMedium)
    }
    return
  }

  val activeReservation = userReservations.firstOrNull { it.bookId == book.id && it.status in listOf("PENDING", "APPROVED", "DECLINED") }

  val publisherInfo = when (book.category) {
    "Computer Science" -> if (book.id % 2 == 0L) "O'REILLY MEDIA · 2023" else "MIT PRESS · 2022"
    "Mathematics & Stats" -> "SPRINGER VERLAG · 2021"
    "Engineering & Tech" -> "MCGRAW-HILL EDUCATION · 2019"
    "Natural Sciences" -> "PEARSON EDUCATION · 2020"
    "Literature & Arts" -> "W. W. NORTON & CO. · 2018"
    "Business & Mgmt" -> "HARVARD BUSINESS PRESS · 2021"
    else -> "ACADEMIC PRESS · 2022"
  }

  val pageCountText = "${if (book.totalPages > 0) book.totalPages else 350} pp."
  val editionText = if (book.id % 2 == 0L) "4th Ed." else "3rd Ed."

  val tocParts = when (book.category) {
    "Computer Science" -> listOf(
      "Part I: Core Architecture & Syntax" to "Ch. 1 – 4",
      "Part II: Algorithms & Data Structures" to "Ch. 5 – 9",
      "Part III: System Design & Scaling" to "Ch. 10 – 14"
    )
    "Mathematics & Stats" -> listOf(
      "Part I: Foundations & Proofs" to "Ch. 1 – 3",
      "Part II: Advanced Calculations & Models" to "Ch. 4 – 8",
      "Part III: Applied Statistical Analysis" to "Ch. 9 – 12"
    )
    "Engineering & Tech" -> listOf(
      "Part I: Thermodynamic Principles" to "Ch. 1 – 4",
      "Part II: Circuit & Material Stress" to "Ch. 5 – 8",
      "Part III: Industrial Standards & Systems" to "Ch. 9 – 13"
    )
    else -> listOf(
      "Part I: Introduction & Core Theories" to "Ch. 1 – 4",
      "Part II: Advanced Concepts & Analysis" to "Ch. 5 – 9",
      "Part III: Applications & Case Studies" to "Ch. 10 – 14"
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface),
    contentPadding = PaddingValues(bottom = 96.dp)
  ) {
    // Top Utility Bar
    item {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(text = "Book Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text(text = "BookFinder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = {}) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
          }
          IconButton(onClick = { onToggleSave(book) }) {
            Icon(
              imageVector = if (book.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = "Bookmark",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
      )
    }

    // Book Overview Hero Card
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.Top
        ) {
          Box(
            modifier = Modifier
              .width(100.dp)
              .height(145.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
          ) {
            AsyncImage(
              model = book.coverUrl,
              contentDescription = book.title,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
            Box(
              modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(text = editionText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = publisherInfo,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = book.title,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = book.authors,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(book.category, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
              }
              Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(pageCountText, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
              }
            }
          }
        }

        // Classification Box
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Column {
                Text("CLASSIFICATION LABEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(book.callNumber, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
              }
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("ISBN-13", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(book.isbn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
          }
        }

        // Read PDF eBook Button Card
        Button(
          onClick = { onPdfClick(book.id) },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("read_pdf_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Read PDF eBook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Live Shelf Availability & Wayfinding Coordinates Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Status Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.secondaryContainer
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                )
                Text("AVAILABLE ON SHELF", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
              Text("${book.availableCopies} of ${book.totalCopies} on shelf", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          // Inventory Metric Pills
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
              Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${book.totalCopies}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }
            }
            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)) {
              Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Present", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("${book.availableCopies}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
              }
            }
            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
              Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Borrowed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${book.totalCopies - book.availableCopies}", style = MaterialTheme.typography.titleMedium)
              }
            }
          }

          // Spatial Coordinates Map Card
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("PHYSICAL WAYFINDING COORDINATES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(Icons.Default.Stairs, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                  Text("1 min walk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
              }

              // 2x2 Grid
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Column {
                        Text("Building / Wing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(book.wing, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                  Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Column {
                        Text("Floor Tier", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(book.floor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.ViewColumn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                      Column {
                        Text("Aisle / Stacks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(book.rack, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                  Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.TableRows, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                      Column {
                        Text("Shelf Height", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(book.shelf, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              // Spine Tag Banner
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                      Text("PRECISE SPINE TAG", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Text(book.callNumber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                  }
                  Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("Bay A · Side 2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }

          // Active Reservation Status Banner
          if (activeReservation != null) {
            val statusColor = when (activeReservation.status) {
              "APPROVED" -> MaterialTheme.colorScheme.primaryContainer
              "DECLINED" -> MaterialTheme.colorScheme.errorContainer
              else -> MaterialTheme.colorScheme.tertiaryContainer
            }
            val onStatusColor = when (activeReservation.status) {
              "APPROVED" -> MaterialTheme.colorScheme.onPrimaryContainer
              "DECLINED" -> MaterialTheme.colorScheme.onErrorContainer
              else -> MaterialTheme.colorScheme.onTertiaryContainer
            }
            val statusIcon = when (activeReservation.status) {
              "APPROVED" -> Icons.Default.CheckCircle
              "DECLINED" -> Icons.Default.Cancel
              else -> Icons.Default.HourglassTop
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = statusColor,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(statusIcon, contentDescription = null, tint = onStatusColor, modifier = Modifier.size(18.dp))
                    Text(
                      text = when (activeReservation.status) {
                        "APPROVED" -> "Reservation Approved!"
                        "DECLINED" -> "Reservation Declined"
                        else -> "Reservation Pending Approval"
                      },
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = onStatusColor
                    )
                  }
                  if (activeReservation.status == "PENDING") {
                    TextButton(
                      onClick = { onCancelReservation(activeReservation.id) },
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                      Text("Cancel", color = onStatusColor, style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }

                Text(
                  text = when (activeReservation.status) {
                    "APPROVED" -> "Your book is ready for pickup at ${activeReservation.pickupLocation}. Please bring your Student ID."
                    "DECLINED" -> "Access note: ${activeReservation.adminComment.ifBlank { "Unavailable for checkout." }}"
                    else -> "Sent to Admin portal for access approval. Pickup desk: ${activeReservation.pickupLocation}."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = onStatusColor
                )
                if (activeReservation.status == "APPROVED" && activeReservation.adminComment.isNotBlank()) {
                  Text(
                    text = "Librarian Note: \"${activeReservation.adminComment}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = onStatusColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }
          }

          // Primary Action Button
          Button(
            onClick = { onWayfinderClick(book.id) },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
          ) {
            Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Find This Book (View Shelf Wayfinder)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { onToggleSave(book) },
              modifier = Modifier
                .weight(1f)
                .height(44.dp),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (book.isSaved) "Saved in My List" else "Save to List", style = MaterialTheme.typography.labelMedium)
            }
            Button(
              onClick = { showReserveDialog = true },
              modifier = Modifier
                .weight(1f)
                .height(44.dp),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (activeReservation != null) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                contentColor = if (activeReservation != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = when (activeReservation?.status) {
                  "PENDING" -> "Pending"
                  "APPROVED" -> "Approved"
                  "DECLINED" -> "Re-Reserve"
                  else -> "Reserve / Borrow"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // Reservation Dialog
    if (showReserveDialog) {
      item {
        AlertDialog(
          onDismissRequest = { if (!isSubmittingReservation) showReserveDialog = false },
          title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              }
              Column {
                Text("Reserve Book", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Borrowing Access Request", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          },
          text = {
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                  Text(book.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                  Text(book.authors, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("Call No: ${book.callNumber} · ${book.floor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
              }

              Text("Select Pickup Location:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              val pickupOptions = listOf(
                "Main Circulation Desk - Floor 1",
                "Science & Tech Counter - Floor 2",
                "Engineering Study Desk - Floor 3",
                "24/7 Smart Pickup Locker #A"
              )
              pickupOptions.forEach { location ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (selectedPickupLocation == location) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedPickupLocation = location }
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = location,
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = if (selectedPickupLocation == location) FontWeight.Bold else FontWeight.Normal,
                      color = if (selectedPickupLocation == location) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedPickupLocation == location) {
                      Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }

              OutlinedTextField(
                value = studentNoteInput,
                onValueChange = { studentNoteInput = it },
                label = { Text("Note for Librarian (Optional)") },
                placeholder = { Text("e.g. For midterm preparation / thesis project") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                maxLines = 2
              )

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  Text(
                    text = "Request goes directly to Admin portal for access control (Accept/Decline).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                  )
                }
              }

              if (reservationFeedback != null) {
                Text(reservationFeedback ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
              }
            }
          },
          confirmButton = {
            Button(
              onClick = {
                isSubmittingReservation = true
                reservationFeedback = "Submitting request to Admin portal..."
                onReserveBook(book, selectedPickupLocation, studentNoteInput) { success, msg ->
                  isSubmittingReservation = false
                  reservationFeedback = msg
                  if (success) {
                    showReserveDialog = false
                  }
                }
              },
              enabled = !isSubmittingReservation
            ) {
              if (isSubmittingReservation) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
              }
              Text("Submit Reservation")
            }
          },
          dismissButton = {
            TextButton(
              onClick = { showReserveDialog = false },
              enabled = !isSubmittingReservation
            ) {
              Text("Cancel")
            }
          }
        )
      }
    }

    // Book Overview & Contents Accordions
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Book Overview & Contents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // Synopsis Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showSynopsis = !showSynopsis }
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Subject, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Comprehensive Synopsis", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              }
              Icon(
                imageVector = if (showSynopsis) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
              )
            }
            if (showSynopsis) {
              Text(
                text = book.synopsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
              )
            }
          }
        }

        // Table of Contents Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showToc = !showToc }
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Toc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Curated Table of Contents", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              }
              Icon(
                imageVector = if (showToc) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
              )
            }
            if (showToc) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                tocParts.forEach { (partTitle, partCh) ->
                  Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(partTitle, style = MaterialTheme.typography.labelMedium)
                      Text(partCh, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Neighboring Stacks on Shelf
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
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
              Text("Neighboring Stacks on Shelf 3", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
              Text("Aisle CS-04", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
            }
          }

          // Neighbor 1
          Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Box(modifier = Modifier.size(36.dp, 48.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                AsyncImage(model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCfDeJ5Ad_oa55o0Gr7_m0Y0FpzuU8pqpQp_M5OTLZ-sHXFtBkVtArIgsGTaVAa9OdnYg4oHw5KV-l76b7GBaY62zg1aTo4EMwcE0x_n3bH2WUlXij5GNH19ramuxI5YBUvSDtt5UfyT4blDwQ1F5Ka3nu9X8eIRgvZ4YIhwMfb4s8jsLJ393_rKbwI1hgq0pswXTE9Wew6YlGriplXb-PO-xECyROytKm9hDkVqMSVZ4WqUNoThOVC", contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("QA76.64 .D47 1994", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Design Patterns (GoF)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Gamma, Helm, Johnson", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
              Text("Nearby", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Report Missing Book
    item {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.HelpCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column {
              Text("Book missing from Shelf 3?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              Text("Request librarian search assistance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, contentColor = MaterialTheme.colorScheme.primary)) {
            Text("Report", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
  }
}
