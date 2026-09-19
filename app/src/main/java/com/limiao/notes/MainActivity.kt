package com.limiao.notes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.limiao.notes.data.AppData
import com.limiao.notes.data.AppRepository
import com.limiao.notes.data.MdHistory
import com.limiao.notes.ui.Accent
import com.limiao.notes.ui.AccentSoft
import com.limiao.notes.ui.Bg
import com.limiao.notes.ui.HomeScreen
import com.limiao.notes.ui.InkSoft
import com.limiao.notes.ui.LiMiaoTheme
import com.limiao.notes.ui.MdHomeScreen
import com.limiao.notes.ui.MdOpen
import com.limiao.notes.ui.MdReaderScreen
import com.limiao.notes.ui.MonthDetailScreen
import com.limiao.notes.ui.MonthsScreen
import com.limiao.notes.ui.Muted
import com.limiao.notes.ui.NavBar
import com.limiao.notes.ui.NotesScreen
import com.limiao.notes.ui.PeriodScreen
import com.limiao.notes.ui.ProfileScreen
import com.limiao.notes.ui.SettingsScreen
import com.limiao.notes.ui.TextTertiary
import com.limiao.notes.ui.currentYm
import com.limiao.notes.ui.theme.AppSkin
import com.limiao.notes.ui.theme.Skins
import com.limiao.notes.ui.components.EmptyState
import com.limiao.notes.ui.components.LTitleTopBar
import com.limiao.notes.ui.health.HealthHomeScreen
import com.limiao.notes.ui.health.HealthProfileScreen
import com.limiao.notes.ui.health.MealScreen
import com.limiao.notes.ui.health.WeightScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** 正在阅读的 .md（非空 = 显示全屏阅读器，盖住主界面） */
    private var mdOpen by mutableStateOf<MdOpen?>(null)
    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AppRepository(applicationContext)
        // 冷启动时直接收到"打开 .md"（VIEW），解析并打开
        if (intent?.action == Intent.ACTION_VIEW) intent.data?.let { openUri(it) }
        // 先用默认皮肤设置系统栏，避免启动瞬间闪一下系统默认色。
        // 数据读盘完成后，下面的 LaunchedEffect 会按真正存的皮肤再校正一次。
        applySystemBars(Skins.DEFAULT)
        setContent {
            // data 提升到这里：主题需要它来决定用哪套皮肤
            val data by repository.data.collectAsState(initial = AppData.empty())
            LiMiaoTheme(skinId = data.skinId) {
                LaunchedEffect(data.skinId) { applySystemBars(Skins.byId(data.skinId)) }
                val reading = mdOpen
                if (reading != null) {
                    // 外部打开 / 从 Markdown 首页打开 → 全屏阅读器
                    MdReaderScreen(reading, onClose = { mdOpen = null })
                } else {
                    AppRoot(
                        data = data,
                        repository = repository,
                        onOpenMdUri = { uri -> openUri(uri) },
                        onRemoveMdHistory = { h -> removeMdHistory(h) },
                    )
                }
            }
        }
    }

    /**
     * 让状态栏 / 导航栏跟随皮肤。
     *
     * 注：targetSdk 35+ 起 `statusBarColor` 在 Android 15+ 上已被系统忽略（强制 edge-to-edge），
     * 那里的状态栏颜色由页面自身的底色透过呈现 —— 页面底色同样跟随皮肤，观感一致。
     * 这里保留赋值是为了照顾 Android 14 及以下，同时统一设置状态栏图标的明暗。
     */
    @Suppress("DEPRECATION")
    private fun applySystemBars(skin: AppSkin) {
        window.statusBarColor = skin.statusBar.toArgb()
        window.navigationBarColor = skin.navBar.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = skin.lightStatusBarIcons
            isAppearanceLightNavigationBars = skin.lightStatusBarIcons
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // App 已在前台时再打开别的 .md，直接切换
        if (intent.action == Intent.ACTION_VIEW) intent.data?.let { openUri(it) }
    }

    /** 统一打开入口：持久授权 → 写历史 → 显示阅读器（SAF 选择、历史重开、外部 VIEW 都走这里） */
    private fun openUri(uri: Uri) {
        // 尽量拿到持久读权限（SAF 选择器/部分文件管理器授予；失败则仅本次可用）
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val uriStr = uri.toString()
        val name = queryDisplayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "文档.md"
        lifecycleScope.launch {
            // first() 等 DataStore 读盘完成，避免冷启动时基于空数据覆盖
            val cur = repository.data.first()
            val item = MdHistory(uriStr, name, System.currentTimeMillis())
            val merged = (listOf(item) + cur.mdHistory.filter { it.uri != uriStr }).take(50)
            repository.save(cur.copy(mdHistory = merged))
        }
        mdOpen = MdOpen(uriStr, name)
    }

    private fun removeMdHistory(h: MdHistory) {
        lifecycleScope.launch {
            val cur = repository.data.first()
            repository.save(cur.copy(mdHistory = cur.mdHistory.filter { it.uri != h.uri }))
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }
}

// ==================== 导航配置 ====================

/**
 * 底部 tab（3 个）：健康 - 花销 - 我的
 *
 * 经期、随手记 已从底部移动到侧边栏（也可从「我的」页的小程序宫格进入）。
 */
private val TABS: List<Pair<String, Pair<String, ImageVector>>> = listOf(
    "health" to ("健康" to Icons.Filled.Favorite),
    "home" to ("花销" to Icons.Filled.AccountBalanceWallet),
    "profile" to ("我的" to Icons.Filled.Person),
)

/** 侧边栏的「小程序」入口（同时也是「我的」页宫格里的快捷方式） */
private val DRAWER_APPS: List<DrawerApp> = listOf(
    DrawerApp("period", "经期", "经期记录 · 预测推算", Icons.Filled.CalendarMonth),
    DrawerApp("notes", "随手记", "短笔记 · 标签筛选", Icons.Filled.EditNote),
    DrawerApp("mdhome", "Markdown 阅读", "打开 .md · 阅读历史", Icons.Filled.Description),
)

private data class DrawerApp(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

@Composable
private fun AppRoot(
    data: AppData,
    repository: AppRepository,
    onOpenMdUri: (Uri) -> Unit,
    onRemoveMdHistory: (MdHistory) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    fun save(next: AppData) {
        scope.launch { repository.save(next) }
    }

    /** 打开一个非底部 tab 的页面（统一走这里，顺带收起侧边栏） */
    fun openPage(route: String) {
        scope.launch { drawerState.close() }
        navController.navigate(route) { launchSingleTop = true }
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
                    DRAWER_APPS.forEach { app ->
                        DrawerItem(
                            icon = app.icon,
                            title = app.title,
                            subtitle = app.subtitle,
                            onClick = { openPage(app.route) },
                        )
                    }
                    DrawerItem(
                        icon = Icons.Filled.Settings,
                        title = "设置",
                        subtitle = "数据备份 · 周期参数 · 清空",
                        onClick = { openPage("settings") },
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
            // 顶部栏：三杠按钮作为页面固定的一部分（不浮动、不随内容滚动）
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Bg)
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, "更多功能", tint = InkSoft)
                    }
                    Spacer(Modifier.weight(1f))
                }
            },
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    TABS.forEach { (route, pair) ->
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
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "health",
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                // ---- 底部 tab ----
                composable("health") {
                    HealthHomeScreen(
                        data = data,
                        onSave = ::save,
                        onOpenWeight = { navController.navigate("health_weight") { launchSingleTop = true } },
                        onOpenMeal = { navController.navigate("health_meal") { launchSingleTop = true } },
                        onOpenProfile = { navController.navigate("health_profile") { launchSingleTop = true } },
                        onOpenReport = { navController.navigate("health_report") { launchSingleTop = true } },
                        onOpenAi = { navController.navigate("health_ai") { launchSingleTop = true } },
                    )
                }
                composable("home") {
                    HomeScreen(
                        data = data,
                        onSave = ::save,
                        onOpenMonths = { navController.navigate("months") },
                        onOpenMonthDetail = { ym -> navController.navigate("month?ym=$ym") },
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        data = data,
                        onSave = ::save,
                        onOpenPeriod = { openPage("period") },
                        onOpenNotes = { openPage("notes") },
                        onOpenMdHome = { openPage("mdhome") },
                        onOpenSettings = { openPage("settings") },
                    )
                }

                // ---- 健康模块二级页 ----
                composable("health_weight") {
                    WeightScreen(
                        data = data,
                        onSave = ::save,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("health_meal") {
                    MealScreen(
                        data = data,
                        onSave = ::save,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("health_profile") {
                    HealthProfileScreen(
                        data = data,
                        onSave = ::save,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("health_report") {
                    PlaceholderPage(
                        title = "报告",
                        icon = Icons.Filled.Description,
                        desc = "周报 / 月报、体重变化与摄入分析正在做\n后续这里还会加上 AI 趋势解读",
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("health_ai") {
                    PlaceholderPage(
                        title = "AI 助手",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        desc = "接入 DeepSeek 后的对话式记录与分析\n暂时还没接，先留好入口",
                        onBack = { navController.popBackStack() },
                    )
                }

                // ---- 侧边栏 / 小程序 ----
                composable("period") { PeriodScreen(data = data, onSave = ::save) }
                composable("notes") { NotesScreen(data = data, onSave = ::save) }
                composable("settings") { SettingsScreen(data = data, onSave = ::save) }
                composable("mdhome") {
                    MdHomeScreen(
                        history = data.mdHistory,
                        onOpenUri = onOpenMdUri,
                        onRemove = onRemoveMdHistory,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("months") {
                    MonthsScreen(
                        data = data,
                        onOpenMonth = { ym -> navController.navigate("month?ym=$ym") },
                        // 返回首页：栈里有上一页就退回，没有就直接去首页（兜底）
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate("home") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                }
                composable(
                    "month?ym={ym}",
                    arguments = listOf(navArgument("ym") { type = NavType.StringType }),
                ) { entry ->
                    val ym = entry.arguments?.getString("ym") ?: currentYm()
                    MonthDetailScreen(
                        ym = ym,
                        data = data,
                        onBack = { navController.popBackStack() },
                        onSave = ::save,
                    )
                }
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

/** 未开放页面的占位（⑤ 报告 / ⑥ AI 助手）—— 居中标题顶栏 + 空状态 */
@Composable
private fun PlaceholderPage(
    title: String,
    icon: ImageVector,
    desc: String,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LTitleTopBar(title, onBack)
        EmptyState(icon = icon, title = title, desc = desc)
    }
}
