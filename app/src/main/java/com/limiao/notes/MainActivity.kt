package com.limiao.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.limiao.notes.data.AppData
import com.limiao.notes.data.AppRepository
import com.limiao.notes.ui.Bg
import com.limiao.notes.ui.HomeScreen
import com.limiao.notes.ui.LiMiaoTheme
import com.limiao.notes.ui.Muted
import com.limiao.notes.ui.NotesScreen
import com.limiao.notes.ui.PeriodScreen
import com.limiao.notes.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiMiaoTheme {
                AppRoot(AppRepository(applicationContext))
            }
        }
    }
}

private val TAB_ICONS: List<Pair<String, Pair<String, ImageVector>>> = listOf(
    "home" to ("花销" to Icons.Filled.AccountBalanceWallet),
    "period" to ("经期" to Icons.Filled.CalendarMonth),
    "notes" to ("随手记" to Icons.Filled.EditNote),
)

@Composable
private fun AppRoot(repository: AppRepository) {
    val data by repository.data.collectAsState(initial = AppData.empty())
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    fun save(next: AppData) {
        scope.launch { repository.save(next) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "更多",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                    HorizontalDivider()
                    DrawerItem(
                        icon = Icons.Filled.Settings,
                        title = "设置",
                        subtitle = "数据备份 · 周期参数 · 清空",
                        onClick = {
                            navController.navigate("settings") { launchSingleTop = true }
                            scope.launch { drawerState.close() }
                        },
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "黎喵记录 · 数据只存在本机",
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 24.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    TAB_ICONS.forEach { (route, pair) ->
                        val (label, icon) = pair
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, label) },
                            label = { Text(label) },
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { scope.launch { drawerState.open() } },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Muted,
                ) {
                    Icon(Icons.Filled.Menu, "更多功能")
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                composable("home") { HomeScreen(data = data, onSave = ::save) }
                composable("period") { PeriodScreen(data = data, onSave = ::save) }
                composable("notes") { NotesScreen(data = data, onSave = ::save) }
                composable("settings") { SettingsScreen(data = data, onSave = ::save) }
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, title) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
    )
}
