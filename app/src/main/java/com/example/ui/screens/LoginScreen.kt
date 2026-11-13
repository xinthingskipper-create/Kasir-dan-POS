package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.PosViewModel

@Composable
fun LoginScreen(viewModel: PosViewModel) {
    val storeProfile by viewModel.storeProfile.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val isRemembered by viewModel.rememberMe.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var username by remember {
        mutableStateOf(if (isRemembered && viewModel.savedUsername.isNotBlank()) viewModel.savedUsername else "admin")
    }
    var pin by remember {
        mutableStateOf(if (isRemembered && viewModel.savedPin.isNotBlank()) viewModel.savedPin else "1234")
    }
    var isPinVisible by remember { mutableStateOf(false) }
    var useTouchNumpad by remember { mutableStateOf(true) }

    // If remember me is active and was updated, keep synced
    LaunchedEffect(isRemembered) {
        if (isRemembered && viewModel.savedUsername.isNotBlank()) {
            username = viewModel.savedUsername
            pin = viewModel.savedPin
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth > 720.dp

        if (isLandscape) {
            // Tablet / Landscape POS Layout (Two-Pane)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pane: Branding & User Selection
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_pos_logo),
                                        contentDescription = "POS Logo",
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = storeProfile.storeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Sistem Kasir POS & Inventaris",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Quick Demo Users Grid
                            Text(
                                text = "Pilih Akun Kasir:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            allUsers.forEach { user ->
                                val isSelected = username.equals(user.username, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            username = user.username
                                            pin = user.pin
                                        }
                                        .testTag("user_item_${user.username}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (user.role == UserRole.ADMIN) Color(0xFF7C3AED) else Color(0xFF059669),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (user.role == UserRole.ADMIN) Icons.Default.Shield else Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = user.fullName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "ID: @${user.username} • PIN: ••••",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (user.role == UserRole.ADMIN) Color(0xFFF3E8FF) else Color(0xFFECFDF5)
                                        ) {
                                            Text(
                                                text = if (user.role == UserRole.ADMIN) "Admin" else "Kasir",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (user.role == UserRole.ADMIN) Color(0xFF7C3AED) else Color(0xFF059669),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Reset Action
                        TextButton(
                            onClick = { viewModel.resetDefaultUsers() },
                            modifier = Modifier.testTag("btn_reset_default_users")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pulihkan Akun Default (Admin & Kasir)", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                // Right Pane: PIN Form & NumPad
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LoginFormContent(
                            username = username,
                            onUsernameChange = { username = it },
                            pin = pin,
                            onPinChange = { pin = it },
                            isPinVisible = isPinVisible,
                            onTogglePinVisibility = { isPinVisible = !isPinVisible },
                            isRemembered = isRemembered,
                            onRememberMeChange = { viewModel.setRememberMe(it) },
                            loginError = loginError,
                            useTouchNumpad = useTouchNumpad,
                            onToggleNumpad = { useTouchNumpad = !useTouchNumpad },
                            onLogin = { viewModel.login(username, pin) },
                            onResetUsers = { viewModel.resetDefaultUsers() }
                        )
                    }
                }
            }
        } else {
            // Phone / Compact Portrait Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Logo & Store Title
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_pos_logo),
                            contentDescription = "POS Logo",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = storeProfile.storeName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Masuk ke Sistem Kasir & Inventaris",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Staff Selection Chips (Dynamic from allUsers, without displaying PINs)
                Text(
                    text = "⚡ Akses Cepat Akun Staff (Klik untuk Pilih):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("row_quick_login_staff")
                ) {
                    items(allUsers, key = { it.id }) { user ->
                        val isSelected = username.equals(user.username, ignoreCase = true)
                        val roleColor = if (user.role == UserRole.ADMIN) Color(0xFF7C3AED) else Color(0xFF059669)
                        val roleBg = if (isSelected) roleColor else if (user.role == UserRole.ADMIN) Color(0xFFF3E8FF) else Color(0xFFECFDF5)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = roleBg,
                            border = BorderStroke(1.dp, roleColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    username = user.username
                                    pin = user.pin
                                }
                                .testTag("chip_login_${user.username}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (user.role == UserRole.ADMIN) Icons.Default.Shield else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else roleColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${if (user.role == UserRole.ADMIN) "👑" else "👤"} ${user.fullName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else roleColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Form
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        LoginFormContent(
                            username = username,
                            onUsernameChange = { username = it },
                            pin = pin,
                            onPinChange = { pin = it },
                            isPinVisible = isPinVisible,
                            onTogglePinVisibility = { isPinVisible = !isPinVisible },
                            isRemembered = isRemembered,
                            onRememberMeChange = { viewModel.setRememberMe(it) },
                            loginError = loginError,
                            useTouchNumpad = useTouchNumpad,
                            onToggleNumpad = { useTouchNumpad = !useTouchNumpad },
                            onLogin = { viewModel.login(username, pin) },
                            onResetUsers = { viewModel.resetDefaultUsers() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = { viewModel.resetDefaultUsers() },
                    modifier = Modifier.testTag("btn_reset_default_users_phone")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pulihkan Akun Kasir Default (Admin & Kasir)", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
private fun LoginFormContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    isPinVisible: Boolean,
    onTogglePinVisibility: () -> Unit,
    isRemembered: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    loginError: String?,
    useTouchNumpad: Boolean,
    onToggleNumpad: () -> Unit,
    onLogin: () -> Unit,
    onResetUsers: () -> Unit
) {
    Text(
        text = "Form Masuk Kasir",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0F172A)
    )
    Text(
        text = "Gunakan username & PIN 4-digit Anda",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF64748B)
    )

    if (loginError != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFEE2E2),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = loginError,
                    color = Color(0xFF991B1B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onResetUsers() }
                        .padding(top = 2.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Klik di sini untuk pulihkan akun default",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Username Input Field
    OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text("Username") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF2563EB))
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("login_username_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF2563EB),
            unfocusedBorderColor = Color(0xFFCBD5E1)
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // PIN Input Field
    OutlinedTextField(
        value = pin,
        onValueChange = { if (it.length <= 8) onPinChange(it) },
        label = { Text("PIN Kasir (4 digit)") },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2563EB))
        },
        trailingIcon = {
            IconButton(onClick = onTogglePinVisibility) {
                Icon(
                    imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isPinVisible) "Sembunyikan PIN" else "Lihat PIN",
                    tint = Color(0xFF64748B)
                )
            }
        },
        singleLine = true,
        visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onLogin() }),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("login_pin_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF2563EB),
            unfocusedBorderColor = Color(0xFFCBD5E1)
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Remember Me & Touch Numpad Toggle
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onRememberMeChange(!isRemembered) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isRemembered,
                onCheckedChange = onRememberMeChange,
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2563EB)),
                modifier = Modifier.testTag("remember_me_checkbox")
            )
            Text(
                text = "Ingat Saya",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155)
            )
        }

        TextButton(onClick = onToggleNumpad) {
            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2563EB))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (useTouchNumpad) "Sembunyikan Keypad" else "Tampilkan Keypad",
                fontSize = 11.sp,
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    // Touch Numpad (Standard POS Touch Experience)
    if (useTouchNumpad) {
        Spacer(modifier = Modifier.height(8.dp))
        TouchNumpad(
            onDigitClick = { digit ->
                if (pin.length < 8) {
                    onPinChange(pin + digit)
                }
            },
            onClearClick = {
                onPinChange("")
            },
            onBackspaceClick = {
                if (pin.isNotEmpty()) {
                    onPinChange(pin.dropLast(1))
                }
            }
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Login Submit Button
    Button(
        onClick = onLogin,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("login_submit_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
    ) {
        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Masuk Sebagai ${username.ifBlank { "Kasir" }}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TouchNumpad(
    onDigitClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onBackspaceClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "⌫")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { key ->
                        Surface(
                            modifier = Modifier
                                 .weight(1f)
                                 .height(48.dp)
                                 .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    when (key) {
                                        "C" -> onClearClick()
                                        "⌫" -> onBackspaceClick()
                                        else -> onDigitClick(key)
                                    }
                                }
                                .testTag("numpad_key_$key"),
                            shape = RoundedCornerShape(8.dp),
                            color = when (key) {
                                "C" -> Color(0xFFFEE2E2)
                                "⌫" -> Color(0xFFE2E8F0)
                                else -> Color.White
                            },
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "⌫") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Hapus",
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (key == "C") Color(0xFFDC2626) else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
