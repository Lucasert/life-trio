package com.lifetrio

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifetrio.core.data.AppContainer
import com.lifetrio.ui.screens.HomeScreen
import com.lifetrio.ui.screens.LedgerScreen
import com.lifetrio.ui.screens.MemoScreen
import com.lifetrio.ui.screens.PasswordScreen
import com.lifetrio.ui.screens.PlanScreen
import com.lifetrio.ui.theme.LifeTrioTheme
import java.time.LocalDate

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LifeTrioApp).container
        setContent {
            LifeTrioTheme {
                LifeTrioApp(container)
            }
        }
    }
}

private enum class Destination(
    val route: String,
    val label: String,
    val outlinedIcon: ImageVector,
    val selectedIcon: ImageVector
) {
    Home("home", "首页", Icons.Outlined.Home, Icons.Filled.Home),
    Memo("memo", "备忘", Icons.Outlined.StickyNote2, Icons.Filled.StickyNote2),
    Ledger("ledger", "记账", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
    Plan("plan", "计划", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    Password("password", "密码", Icons.Outlined.Lock, Icons.Filled.Lock)
}

@Composable
private fun LifeTrioApp(container: AppContainer) {
    val navController = rememberNavController()
    val today = LocalDate.now()

    LaunchedEffect(today) {
        container.planRepository.generateOccurrences(today.minusDays(1), today.plusDays(31))
    }

    Scaffold(
        bottomBar = { BottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Home.route) {
                HomeScreen(container, navController, Destination.Ledger.route)
            }
            composable(Destination.Memo.route) {
                MemoScreen(container, navController, Destination.Plan.route)
            }
            composable(Destination.Ledger.route) {
                LedgerScreen(container)
            }
            composable(Destination.Plan.route) {
                PlanScreen(container)
            }
            composable(Destination.Password.route) {
                PasswordScreen(container)
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Destination.Home.route
    NavigationBar {
        Destination.entries.forEach { destination ->
            val selected = destination.route == route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (destination.route != route) {
                        navController.navigateToTab(destination.route)
                    }
                },
                icon = {
                    Icon(
                        if (selected) destination.selectedIcon else destination.outlinedIcon,
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

/**
 * Switch between bottom-tab destinations. All tab navigation — including jumps
 * triggered from inside a screen (e.g. Home's budget card) — must go through
 * here so the back stack keeps a single, consistent save/restore semantic.
 */
internal fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
