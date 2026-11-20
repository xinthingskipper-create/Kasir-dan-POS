package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.MainTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Adaptive scaffold container featuring a left-side slideable and auto-hiding navigation toolbar
 * across ALL device form factors (compact portrait phones, foldables, tablets, and landscape):
 * - Always anchored on the LEFT side (SlidingLeftNavRail) with responsive rail width & spacing.
 * - Respects WindowInsets.safeDrawing for cutouts and system navigation bars on all orientations.
 * - Auto-hides on downward scroll, shortly after tab selection, or after idle timeout (unless pinned).
 * - Reveals via TopAppBar menu button, left-edge slide handle pill (tap or swipe right), or upward scroll.
 */
@Composable
fun AdaptiveSlideNavigationContainer(
    tabs: List<MainTab>,
    currentTab: MainTab,
    storeName: String,
    userFullName: String,
    userRole: UserRole?,
    connectedPrinter: Boolean,
    autoHideDelayMs: Long = 3500L,
    onTabSelected: (MainTab) -> Unit,
    onPrinterClick: () -> Unit,
    onCloudReportClick: () -> Unit,
    onLogoutClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isNavVisible by remember { mutableStateOf(true) }
    var isNavPinned by remember { mutableStateOf(false) }
    var navInteractionTick by remember { mutableIntStateOf(0) }
    var accumulatedScrollDelta by remember { mutableFloatStateOf(0f) }

    // Auto-hide left navigation toolbar after inactivity when not pinned
    LaunchedEffect(isNavVisible, isNavPinned, navInteractionTick, autoHideDelayMs) {
        if (isNavVisible && !isNavPinned && autoHideDelayMs > 0L) {
            delay(autoHideDelayMs)
            isNavVisible = false
        }
    }

    // Listen to child scrollables (LazyColumn / verticalScroll) for automatic hide/reveal
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!isNavPinned && source == NestedScrollSource.UserInput) {
                    val dy = available.y
                    if (dy < -2f) {
                        accumulatedScrollDelta = if (accumulatedScrollDelta < 0f) accumulatedScrollDelta + dy else dy
                        if (accumulatedScrollDelta < -28f && isNavVisible) {
                            isNavVisible = false
                            accumulatedScrollDelta = 0f
                        }
                    } else if (dy > 2f) {
                        accumulatedScrollDelta = if (accumulatedScrollDelta > 0f) accumulatedScrollDelta + dy else dy
                        if (accumulatedScrollDelta > 40f && !isNavVisible) {
                            isNavVisible = true
                            navInteractionTick++
                            accumulatedScrollDelta = 0f
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    fun showToolbar() {
        isNavVisible = true
        navInteractionTick++
    }

    fun toggleToolbar() {
        isNavVisible = !isNavVisible
        if (isNavVisible) {
            navInteractionTick++
        }
    }

    fun handleTabSelect(tab: MainTab) {
        onTabSelected(tab)
        if (!isNavPinned) {
            coroutineScope.launch {
                delay(280L)
                isNavVisible = false
            }
        } else {
            navInteractionTick++
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactWidth = maxWidth < 600.dp
        val isExpandedWidth = maxWidth >= 840.dp
        val isShortHeight = maxHeight < 480.dp

        val railWidth: Dp = when {
            isCompactWidth -> 70.dp
            isExpandedWidth -> 86.dp
            else -> 78.dp
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CompactTopAppBar(
                    storeName = storeName,
                    userFullName = userFullName,
                    userRole = userRole,
                    connectedPrinter = connectedPrinter,
                    isWideScreen = !isCompactWidth,
                    isCompactHeight = isShortHeight,
                    isNavVisible = isNavVisible,
                    currentTabLabel = getTabIconAndLabel(currentTab, userRole).second,
                    onToggleNav = { toggleToolbar() },
                    onPrinterClick = onPrinterClick,
                    onCloudReportClick = onCloudReportClick,
                    onLogoutClick = onLogoutClick,
                    showLogoutButton = isCompactWidth || !isNavVisible
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            ) {
                // Full-width content area on all devices with bottom safe area padding
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                ) {
                    content()
                }

                // Floating Left-Edge Slide Handle when left navigation toolbar is hidden
                AnimatedVisibility(
                    visible = !isNavVisible,
                    enter = slideInHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetX = { -it }
                    ) + fadeIn(tween(180)),
                    exit = slideOutHorizontally(
                        animationSpec = tween(180),
                        targetOffsetX = { -it }
                    ) + fadeOut(tween(140)),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    FloatingEdgeSlideHandle(
                        onReveal = { showToolbar() }
                    )
                }

                // Left-side Sliding Navigation Rail across ALL screen sizes & orientations
                AnimatedVisibility(
                    visible = isNavVisible,
                    enter = slideInHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetX = { -it }
                    ) + fadeIn(tween(200)),
                    exit = slideOutHorizontally(
                        animationSpec = tween(220),
                        targetOffsetX = { -it }
                    ) + fadeOut(tween(160)),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    SlidingLeftNavRail(
                        tabs = tabs,
                        currentTab = currentTab,
                        userRole = userRole,
                        railWidth = railWidth,
                        isCompactWidth = isCompactWidth,
                        isShortHeight = isShortHeight,
                        isPinned = isNavPinned,
                        onTabSelected = { handleTabSelect(it) },
                        onTogglePin = {
                            isNavPinned = !isNavPinned
                            navInteractionTick++
                        },
                        onHide = { isNavVisible = false },
                        onLogoutClick = onLogoutClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingEdgeSlideHandle(
    onReveal: () -> Unit
) {
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .width(36.dp)
            .height(84.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulated = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulated += dragAmount
                        if (dragAccumulated > 14f) {
                            onReveal()
                            dragAccumulated = 0f
                        }
                    }
                )
            }
            .clickable(onClick = onReveal)
            .testTag("floating_nav_reveal_handle")
    ) {
        Surface(
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .width(24.dp)
                .height(64.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Buka Navigasi Toolbar",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Adaptive left-side sliding navigation rail for all device sizes (portrait phones, foldables, tablets, landscape).
 * Supports swipe-left to hide, pin toggle, adaptive item sizing & vertical scrolling for short screens, and logout.
 */
@Composable
private fun SlidingLeftNavRail(
    tabs: List<MainTab>,
    currentTab: MainTab,
    userRole: UserRole?,
    railWidth: Dp,
    isCompactWidth: Boolean,
    isShortHeight: Boolean,
    isPinned: Boolean,
    onTabSelected: (MainTab) -> Unit,
    onTogglePin: () -> Unit,
    onHide: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var horizontalDragAccumulated by remember { mutableFloatStateOf(0f) }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 6.dp,
        shadowElevation = 14.dp,
        shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .width(railWidth)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(vertical = if (isShortHeight) 4.dp else 8.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragAccumulated = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragAccumulated += dragAmount
                        if (horizontalDragAccumulated < -18f) {
                            onHide()
                            horizontalDragAccumulated = 0f
                        }
                    }
                )
            }
            .testTag("sliding_nav_rail")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = if (isShortHeight) 4.dp else 8.dp,
                    horizontal = if (isCompactWidth) 4.dp else 6.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top controls: Pin toggle + Hide slide arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = if (isShortHeight) 36.dp else 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onTogglePin)
                        .testTag("btn_pin_nav_toolbar")
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (isPinned) "Lepas Pin Toolbar" else "Pin Toolbar",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = if (isShortHeight) 36.dp else 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onHide)
                        .testTag("btn_hide_sliding_rail")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Sembunyikan Navigasi",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = if (isShortHeight) 2.dp else 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Vertically scrollable menu items adapt cleanly to both tall portrait & short landscape heights
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (isShortHeight) 4.dp else 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentTab == tab
                    val (icon, label) = getTabIconAndLabel(tab, userRole)

                    Surface(
                        onClick = { onTabSelected(tab) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (isShortHeight) 46.dp else 52.dp)
                            .testTag("nav_tab_${tab.name}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(
                                vertical = if (isShortHeight) 4.dp else 6.dp,
                                horizontal = 3.dp
                            )
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(if (isCompactWidth) 19.dp else 21.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = if (isCompactWidth) 9.sp else 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = if (isShortHeight) 2.dp else 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Logout at bottom of left navigation rail
            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = if (isShortHeight) 40.dp else 48.dp)
                    .testTag("rail_logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Keluar",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Compact, adaptive TopAppBar with left slide navigation toggle, store identity, and quick status actions.
 */
@Composable
fun CompactTopAppBar(
    storeName: String,
    userFullName: String,
    userRole: UserRole?,
    connectedPrinter: Boolean,
    isWideScreen: Boolean,
    isCompactHeight: Boolean,
    isNavVisible: Boolean,
    currentTabLabel: String,
    onToggleNav: () -> Unit,
    onPrinterClick: () -> Unit,
    onCloudReportClick: () -> Unit,
    onLogoutClick: () -> Unit,
    showLogoutButton: Boolean
) {
    val barHeight = if (isCompactHeight) 42.dp else 48.dp
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = primaryColor,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor,
                            primaryColor.copy(alpha = 0.94f),
                            primaryColor
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = if (isWideScreen) 10.dp else 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // --- SISI KIRI: TOMBOL SLIDE MENU + IDENTITAS TOKO & KASIR ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    IconButton(
                        onClick = onToggleNav,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("topbar_nav_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isNavVisible) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                            contentDescription = if (isNavVisible) "Sembunyikan Menu Navigasi" else "Tampilkan Menu Navigasi",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Mini Store Icon Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.size(if (isWideScreen) 28.dp else 24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isWideScreen) 16.dp else 14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(if (isWideScreen) 8.dp else 6.dp))

                    if (isWideScreen) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = storeName.ifBlank { "Kasir Pintar" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 180.dp)
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (userRole == UserRole.ADMIN) Color(0xFFF59E0B).copy(alpha = 0.25f)
                                else Color(0xFF38BDF8).copy(alpha = 0.22f),
                                border = BorderStroke(
                                    0.5.dp,
                                    if (userRole == UserRole.ADMIN) Color(0xFFFDE68A).copy(alpha = 0.5f)
                                    else Color(0xFFBAE6FD).copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = if (userRole == UserRole.ADMIN) "ADMIN" else "KASIR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }

                            Text(
                                text = "• $userFullName • $currentTabLabel",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = storeName.ifBlank { "Kasir Pintar" },
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$currentTabLabel • $userFullName",
                                fontSize = 9.5.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // --- SISI KANAN: STATUS BADGES & AKSI CEPAT ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (userRole == UserRole.ADMIN) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onCloudReportClick)
                                .testTag("topbar_cloud_report_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    horizontal = if (isWideScreen) 9.dp else 7.dp,
                                    vertical = if (isWideScreen) 4.5.dp else 3.5.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Web Laporan",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isWideScreen) 14.dp else 12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isWideScreen) "Web Laporan" else "Web",
                                    fontSize = if (isWideScreen) 10.5.sp else 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(
                            0.5.dp,
                            if (connectedPrinter) Color(0xFF4ADE80).copy(alpha = 0.4f)
                            else Color(0xFFF87171).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onPrinterClick)
                            .testTag("topbar_printer_indicator")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                horizontal = if (isWideScreen) 9.dp else 6.dp,
                                vertical = if (isWideScreen) 4.5.dp else 3.5.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (connectedPrinter) Color(0xFF4ADE80) else Color(0xFFFCA5A5))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Printer",
                                tint = Color.White,
                                modifier = Modifier.size(if (isWideScreen) 14.dp else 12.dp)
                            )
                            if (isWideScreen) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (connectedPrinter) "Siap" else "Off",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (connectedPrinter) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                )
                            }
                        }
                    }

                    if (showLogoutButton) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, Color(0xFFFCA5A5).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onLogoutClick)
                                .testTag("topbar_logout_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Keluar",
                                    tint = Color(0xFFFECACA),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getTabIconAndLabel(tab: MainTab, userRole: UserRole?): Pair<ImageVector, String> {
    return when (tab) {
        MainTab.POS -> Icons.Default.PointOfSale to "Kasir"
        MainTab.INVENTORY -> Icons.Default.Inventory2 to "Inventaris"
        MainTab.EXPENSES -> Icons.Default.Receipt to "Pengeluaran"
        MainTab.REPORT -> (if (userRole == UserRole.ADMIN) Icons.Default.Assessment else Icons.Default.ReceiptLong) to
                (if (userRole == UserRole.ADMIN) "Laporan" else "Riwayat")
        MainTab.USERS -> Icons.Default.People to "Staf"
        MainTab.PRINTER -> Icons.Default.Print to "Printer"
        MainTab.SETTINGS -> Icons.Default.Storefront to "Toko"
        MainTab.CLOUD_HOSTING -> Icons.Default.Cloud to "Hosting"
    }
}
