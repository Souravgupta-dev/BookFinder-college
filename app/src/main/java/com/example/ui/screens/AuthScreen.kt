package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.UserEntity
import com.example.ui.viewmodel.BookFinderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
  viewModel: BookFinderViewModel,
  onBackClick: () -> Unit
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val authMessage by viewModel.authMessage.collectAsState()
  val latestOtp by viewModel.latestOtp.collectAsState()
  val brevoApiKey by viewModel.brevoApiKey.collectAsState()

  var isSignUp by remember { mutableStateOf(false) }
  var isOtpStep by remember { mutableStateOf(false) }
  var isProfileSetupStep by remember { mutableStateOf(false) }

  var nameInput by remember { mutableStateOf("") }
  var emailInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var otpInput by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }

  // Profile Setup states
  var selectedAvatar by remember { mutableStateOf("https://lh3.googleusercontent.com/aida-public/AB6AXuCZsc-qdCapncfCR_NAShTL7eY-Wxa4na-a0cv9xCKxWIaicGwRrzRwaXbJPrjN5gyzgovqtZSog6r5V3e_f06tivsfdnqmIwTdr6_Nhm-JPthK8dxhiArBbq9Ko4ZQuGpc4wGV_hOzcbViVbHP-aAi6yN4r-mnq3LPq2gFN0n1b1D9CXCzjrViliCSCU_FsGFX97gmmlGqQ8BC5_9sFDjelgvbQGKRla10oG09oGCLSkmdAtTBQOuc") }
  var selectedInterests by remember { mutableStateOf(emptyList<String>()) }

  val presetAvatars = listOf(
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCZsc-qdCapncfCR_NAShTL7eY-Wxa4na-a0cv9xCKxWIaicGwRrzRwaXbJPrjN5gyzgovqtZSog6r5V3e_f06tivsfdnqmIwTdr6_Nhm-JPthK8dxhiArBbq9Ko4ZQuGpc4wGV_hOzcbViVbHP-aAi6yN4r-mnq3LPq2gFN0n1b1D9CXCzjrViliCSCU_FsGFX97gmmlGqQ8BC5_9sFDjelgvbQGKRla10oG09oGCLSkmdAtTBQOuc",
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80"
  )

  val allInterests = listOf("Computer Science", "Artificial Intelligence", "Fiction", "Mathematics", "Economics", "History", "Engineering", "Data Science", "Physics")

  val scrollState = rememberScrollState()
  val coroutineScope = rememberCoroutineScope()

  val emailFocus = remember { FocusRequester() }
  val passwordFocus = remember { FocusRequester() }
  val nameFocus = remember { FocusRequester() }
  val otpFocus = remember { FocusRequester() }

  val performSignIn = {
    if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
      viewModel.signIn(emailInput, passwordInput) { success ->
        if (success) {
          onBackClick()
        }
      }
    }
  }

  val performSignUp = {
    if (nameInput.isNotBlank() && emailInput.isNotBlank() && passwordInput.isNotBlank()) {
      viewModel.signUp(nameInput, emailInput, passwordInput, brevoApiKey) { success ->
        if (success) {
          isOtpStep = true
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = when {
              currentUser != null -> "User Account"
              isProfileSetupStep -> "Create Your Profile"
              isOtpStep -> "Verify Email (OTP)"
              isSignUp -> "Create Account"
              else -> "Sign In"
            },
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.surface),
      contentAlignment = Alignment.Center
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(scrollState),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          when {
            currentUser != null -> {
              // Logged in View
              Box(
                modifier = Modifier
                  .size(90.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                AsyncImage(
                  model = currentUser!!.avatarUrl.ifBlank { presetAvatars[0] },
                  contentDescription = null,
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop
                )
              }
              Text(currentUser!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
              Text(currentUser!!.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  Text("Verified Library Member", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                }
              }

              // Interests display
              Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Interests & Stacks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  currentUser!!.interests.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { interest ->
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                      Text(interest, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Return to Home Button
              Button(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Return to Library / Home", fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = { viewModel.signOut() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
              }
            }

            isProfileSetupStep -> {
              // Profile Setup Step (Avatar, Name, Interests)
              Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
              }
              Text("Set Up Your Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
              Text("Choose your avatar and reading interests to personalize your BookFinder experience.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

              // Avatar selection
              Text("Select Avatar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                presetAvatars.forEach { av ->
                  val isSelected = selectedAvatar == av
                  Box(
                    modifier = Modifier
                      .size(52.dp)
                      .clip(CircleShape)
                      .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                      .clickable { selectedAvatar = av },
                    contentAlignment = Alignment.Center
                  ) {
                    AsyncImage(model = av, contentDescription = null, modifier = Modifier.size(46.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                  }
                }
              }

              OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Display Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .focusRequester(nameFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                  viewModel.updateProfile(emailInput, if (nameInput.isBlank()) "Library Reader" else nameInput, selectedAvatar, selectedInterests.joinToString(", ")) { success ->
                    if (success) {
                      isProfileSetupStep = false
                    }
                  }
                }),
                shape = RoundedCornerShape(14.dp)
              )

              Text("Interests & Genres", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
              Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                allInterests.chunked(3).forEach { rowItems ->
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowItems.forEach { interest ->
                      val isSelected = selectedInterests.contains(interest)
                      FilterChip(
                        selected = isSelected,
                        onClick = {
                          selectedInterests = if (isSelected) selectedInterests - interest else selectedInterests + interest
                        },
                        label = { Text(interest, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                      )
                    }
                  }
                }
              }

              if (authMessage != null) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.errorContainer
                ) {
                  Text(authMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(10.dp))
                }
              }

              Button(
                onClick = {
                  viewModel.updateProfile(emailInput, if (nameInput.isBlank()) "Library Reader" else nameInput, selectedAvatar, selectedInterests.joinToString(", ")) { success ->
                    if (success) {
                      isProfileSetupStep = false
                    }
                  }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
              ) {
                Text("Complete Profile & Start Reading", fontWeight = FontWeight.Bold)
              }
            }

            isOtpStep -> {
              // OTP Verification Step
              Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
              }
              Text("Verify via Brevo OTP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
              Text("Enter the 6-digit verification code sent to $emailInput.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

              OutlinedTextField(
                value = otpInput,
                onValueChange = { otpInput = it },
                label = { Text("6-Digit OTP Code") },
                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .focusRequester(otpFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                  viewModel.verifyOtp(emailInput, otpInput) { success ->
                    if (success) {
                      isOtpStep = false
                      isProfileSetupStep = true
                    }
                  }
                }),
                shape = RoundedCornerShape(14.dp)
              )

              if (!latestOtp.isNullOrBlank()) {
                OutlinedButton(
                  onClick = { otpInput = latestOtp ?: "" },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                  Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Auto-Fill OTP Code ($latestOtp)")
                }
              }

              if (authMessage != null) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.errorContainer
                ) {
                  Text(authMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(10.dp))
                }
              }

              Button(
                onClick = {
                  viewModel.verifyOtp(emailInput, otpInput) { success ->
                    if (success) {
                      isOtpStep = false
                      isProfileSetupStep = true
                    }
                  }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
              ) {
                Text("Verify & Setup Profile", fontWeight = FontWeight.Bold)
              }

              TextButton(onClick = { isOtpStep = false }) {
                Text("Back to Sign In")
              }
            }

            else -> {
              // Sign In / Sign Up Form with gorgeous design
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
                  .padding(4.dp)
              ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                  Tab(
                    selected = !isSignUp,
                    onClick = { isSignUp = false },
                    modifier = Modifier.weight(1f).height(40.dp),
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                  ) {
                    Text("Sign In", fontWeight = FontWeight.Bold)
                  }
                  Tab(
                    selected = isSignUp,
                    onClick = { isSignUp = true },
                    modifier = Modifier.weight(1f).height(40.dp),
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                  ) {
                    Text("Sign Up", fontWeight = FontWeight.Bold)
                  }
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              if (isSignUp) {
                OutlinedTextField(
                  value = nameInput,
                  onValueChange = { nameInput = it },
                  label = { Text("Full Name") },
                  leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus),
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                  keyboardActions = KeyboardActions(onNext = {
                    emailFocus.requestFocus()
                    coroutineScope.launch { scrollState.animateScrollTo(200) }
                  }),
                  shape = RoundedCornerShape(14.dp)
                )
              }

              OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .focusRequester(emailFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = {
                  passwordFocus.requestFocus()
                  coroutineScope.launch { scrollState.animateScrollTo(350) }
                }),
                shape = RoundedCornerShape(14.dp)
              )

              OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                  IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                  }
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .focusRequester(passwordFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                  onDone = {
                    if (isSignUp) {
                      performSignUp()
                    } else {
                      performSignIn()
                    }
                  }
                ),
                shape = RoundedCornerShape(14.dp)
              )



              if (authMessage != null) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.errorContainer
                ) {
                  Text(authMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(10.dp))
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              Button(
                onClick = {
                  if (isSignUp) {
                    performSignUp()
                  } else {
                    performSignIn()
                  }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(if (isSignUp) Icons.Default.PersonAdd else Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSignUp) "Send OTP & Register" else "Sign In to Library", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
