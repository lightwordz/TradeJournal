package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryCobalt

sealed class Screen(val route: String, val title: String, val iconOutlined: ImageVector, val iconFilled: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    object Analytics : Screen("analytics", "Insights", Icons.Outlined.BarChart, Icons.Filled.BarChart)
    object Calendar : Screen("calendar", "Calendar", Icons.Outlined.CalendarToday, Icons.Filled.CalendarToday)
    object Journal : Screen("journal", "Journal", Icons.Outlined.Book, Icons.Filled.Book)
    object Profile : Screen("profile", "Settings", Icons.Outlined.Tune, Icons.Filled.Tune)
}

@Composable
fun LogTradeFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "fab_press_scale"
    )

    val gradientBrush = remember {
        Brush.verticalGradient(
            colors = listOf(PrimaryCobalt, Color(0xFF1D4ED8))
        )
    }

    Box(
        modifier = modifier
            .scale(pressScale)
            .size(56.dp)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = PrimaryCobalt,
                spotColor = PrimaryCobalt
            )
            .clip(CircleShape)
            .background(gradientBrush)
            .border(2.5.dp, MaterialTheme.colorScheme.background, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.25f))
            ) { onClick() }
            .testTag("add_trade_fab"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Log New Trade",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun TradeNavigationRail(
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    onLogTradeClick: () -> Unit
) {
    val items = listOf(
        Screen.Dashboard,
        Screen.Analytics,
        Screen.Calendar,
        Screen.Journal,
        Screen.Profile
    )

    Row(modifier = Modifier.fillMaxHeight()) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
            header = {
                Spacer(modifier = Modifier.height(8.dp))
                LogTradeFab(onClick = onLogTradeClick)
                Spacer(modifier = Modifier.height(12.dp))
            }
        ) {
            items.forEach { screen ->
                val selected = currentRoute == screen.route
                NavigationRailItem(
                    selected = selected,
                    onClick = { onTabSelected(screen.route) },
                    modifier = Modifier.testTag("nav_${screen.route}"),
                    icon = {
                        Icon(
                            imageVector = if (selected) screen.iconFilled else screen.iconOutlined,
                            contentDescription = screen.title
                        )
                    },
                    label = {
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = PrimaryCobalt,
                        selectedTextColor = PrimaryCobalt,
                        indicatorColor = PrimaryCobalt.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        VerticalDivider(
            thickness = 0.75.dp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun TradeBottomNavBar(
    currentRoute: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        Screen.Dashboard,
        Screen.Analytics,
        Screen.Calendar,
        Screen.Journal,
        Screen.Profile
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.75.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    NavItem(
                        modifier = Modifier.weight(1f),
                        screen = screen,
                        selected = currentRoute == screen.route,
                        onClick = { onTabSelected(screen.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    modifier: Modifier = Modifier,
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "nav_scale"
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) PrimaryCobalt.copy(alpha = 0.12f) else Color.Transparent,
        label = "nav_pill_color"
    )

    val activeColor = PrimaryCobalt
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxHeight()
            .minimumInteractiveComponentSize()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 6.dp, horizontal = 2.dp)
            .testTag("nav_${screen.route}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 42.dp, max = 48.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) screen.iconFilled else screen.iconOutlined,
                contentDescription = screen.title,
                tint = if (selected) activeColor else inactiveColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = screen.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) activeColor else inactiveColor
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
