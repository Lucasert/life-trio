package com.lifetrio

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifetrio.core.data.AppContainer
import com.lifetrio.ui.components.LifeTrioTabBar
import com.lifetrio.ui.components.TabSpec
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
        val container = (application as LifeTrioApp).container
        setContent {
            LifeTrioTheme {
                LifeTrioApp(container)
            }
        }
    }
}

private enum class Destination(val route: String, val label: String, val emoji: String) {
    Home("home", "首页", "🏠"),
    Memo("memo", "备忘", "📝"),
    Ledger("ledger", "记账", "💰"),
    Plan("plan", "计划", "📅"),
    Password("password", "密码", "🔒")
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
    LifeTrioTabBar(
        tabs = Destination.entries.map { TabSpec(it.route, it.label, it.emoji) },
        selectedRoute = route,
        onSelect = { destination ->
            if (destination != route) {
                navController.navigate(destination) {
                    launchSingleTop = true
                    popUpTo(Destination.Home.route)
                }
            }
        }
    )
}
