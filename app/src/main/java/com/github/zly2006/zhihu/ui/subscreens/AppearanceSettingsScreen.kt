package com.github.zly2006.zhihu.ui.subscreens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.Daily
import com.github.zly2006.zhihu.Follow
import com.github.zly2006.zhihu.Home
import com.github.zly2006.zhihu.HotList
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.OnlineHistory
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeMode
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.ColorPickerDialog
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import com.github.zly2006.zhihu.ui.components.SettingItemOverall
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch

const val START_DESTINATION_PREFERENCE_KEY = "startDestination"
const val BOTTOM_BAR_ITEMS_PREFERENCE_KEY = "bottom_bar_items"

private val topLevelDestinationsInOrder: List<Pair<String, NavDestination>> = listOf(
    Home.name to Home,
    Follow.name to Follow,
    HotList.name to HotList,
    Daily.name to Daily,
    OnlineHistory.name to OnlineHistory,
    Account.name to Account,
)

internal fun navDestinationFromName(name: String): NavDestination = topLevelDestinationsInOrder
    .firstOrNull { it.first == name }
    ?.second
    ?: Home

internal fun resolveValidStartDestinationKey(
    preferredKey: String?,
    availableKeysInOrder: List<String>,
): String = when {
    !preferredKey.isNullOrEmpty() && preferredKey in availableKeysInOrder -> preferredKey
    availableKeysInOrder.isNotEmpty() -> availableKeysInOrder.first()
    else -> Home.name
}

internal fun defaultBottomBarSelectionKeys(): Set<String> =
    linkedSetOf(Home.name, Follow.name, Daily.name)

internal fun normalizeBottomBarSelection(
    selectedKeys: Set<String>,
    enforceMinimumSelection: Boolean = false,
): Set<String> {
    val allowedKeys = topLevelDestinationsInOrder.map { it.first }.toSet()
    val normalized = selectedKeys
        .filterTo(linkedSetOf()) { it in allowedKeys }
        .ifEmpty { defaultBottomBarSelectionKeys().toMutableSet() }

    if (Home.name in normalized) {
        normalized.remove(Account.name)
    } else {
        normalized.add(Account.name)
    }

    if (enforceMinimumSelection) {
        val fillOrder =
            if (Home.name in normalized) {
                listOf(Follow.name, Daily.name, HotList.name, OnlineHistory.name)
            } else {
                listOf(Follow.name, Daily.name, HotList.name, OnlineHistory.name, Home.name)
            }
        fillOrder.forEach { key ->
            if (normalized.size < 3) {
                normalized.add(key)
            }
        }
    }

    return normalized
}

internal fun shouldShowAccountHistoryShortcut(
    selectedKeys: Set<String>,
): Boolean = OnlineHistory.name !in selectedKeys

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceSettingsScreen(
    innerPadding: PaddingValues,
    setting: String = "",
    onExit: () -> Unit = {},
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }

    val scrollState = rememberScrollState()
    val navigator = LocalNavigator.current

    val itemPositions = remember { mutableMapOf<String, Int>() }
    var scrollColumnRootY by remember { mutableIntStateOf(0) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollAnimationSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val density = LocalDensity.current

    var scrolledSetting by remember { mutableStateOf<String?>(null) }
    val selectedBottomBarItemKeys = remember {
        mutableStateOf(
            normalizeBottomBarSelection(
                preferences
                    .getStringSet(
                        BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                        defaultBottomBarSelectionKeys(),
                    )?.toSet() ?: defaultBottomBarSelectionKeys(),
                enforceMinimumSelection = true,
            ),
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            onExit()
        }
    }

    LaunchedEffect(setting, itemPositions[setting]) {
        if (setting.isNotEmpty() && scrolledSetting != setting) {
            itemPositions[setting]?.let { itemRootY ->
                scrolledSetting = setting
                kotlinx.coroutines.delay(200)
                // 收缩 LargeTopAppBar（programmatic scroll 不触发 nestedScroll）
                scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
                val targetScroll = maxOf(0, itemRootY - scrollColumnRootY)
                val maxScroll = scrollState.maxValue +
                    scrollBehavior.state.heightOffsetLimit.toInt() -
                    with(density) { 16.dp.toPx() }.toInt()
                scrollState.animateScrollTo(
                    minOf(targetScroll, maxScroll),
                    scrollAnimationSpec,
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("外观与阅读体验") },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { scrollColumnRootY = it.positionInRoot().y.toInt() }
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(vertical = 16.dp),
        ) {
            val useDynamicColor = ThemeManager.getUseDynamicColor()
            val currentThemeMode = ThemeManager.getThemeMode()

            // ── 主题 ────────────────────────────────────────────────────────────

            SettingItemGroup(
                title = "主题",
            ) {
                SettingItem(
                    title = { Text("主题模式") },
                    description = { Text("设置应用的显示主题。") },
                    settingKey = "nightMode",
                    highlightedKey = setting,
                    onPositioned = { itemPositions["nightMode"] = it },
                    bottomAction = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            val themeModes = listOf(
                                ThemeMode.SYSTEM to "自动",
                                ThemeMode.LIGHT to "亮色",
                                ThemeMode.DARK to "暗色",
                            )
                            themeModes.forEach { (mode, label) ->
                                val isSelected = currentThemeMode == mode
                                OutlinedButton(
                                    onClick = {
                                        ThemeManager.setThemeMode(context, mode)
                                        Toast.makeText(context, "已切换到$label", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                        contentColor = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    ),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    },
                )

                SettingItemWithSwitch(
                    title = { Text("使用 Material You 动态取色") },
                    description = { Text("根据系统壁纸自动提取主题色（Android 12+ 可用）。\n关闭后可以自己设定主题颜色。") },
                    checked = useDynamicColor,
                    onCheckedChange = {
                        ThemeManager.setUseDynamicColor(context, it)
                        Toast.makeText(context, "已${if (it) "启用" else "禁用"}动态取色", Toast.LENGTH_SHORT).show()
                    },
                    settingKey = "dynamicColor",
                    highlightedKey = setting,
                    onPositioned = { itemPositions["dynamicColor"] = it },
                )

                var showColorPicker by remember { mutableStateOf(false) }
                val customColor = ThemeManager.getCustomColor()

                AnimatedVisibility(visible = !useDynamicColor) {
                    SettingItem(
                        title = { Text("自定义主题色") },
                        description = { Text("点击选择您喜欢的主题颜色") },
                        onClick = { showColorPicker = true },
                        endAction = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(customColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            )
                        },
                    )
                }
                if (showColorPicker) {
                    ColorPickerDialog(
                        title = "选择主题色",
                        initialColor = customColor,
                        onDismiss = { showColorPicker = false },
                        onColorSelected = { color ->
                            ThemeManager.setCustomColor(context, color)
                            Toast.makeText(context, "主题色已保存", Toast.LENGTH_SHORT).show()
                            showColorPicker = false
                        },
                    )
                }

                var showLuotianYiColorPicker by remember { mutableStateOf(false) }
                val luotianYiColor = remember {
                    Color(preferences.getInt("luotianyi_color", 0xff_66CCFF.toInt()))
                }

                SettingItem(
                    title = { Text("唤起浏览器主题色") },
                    description = { Text("应用内浏览器的工具栏颜色") },
                    onClick = { showLuotianYiColorPicker = true },
                    endAction = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(luotianYiColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                    },
                )

                if (showLuotianYiColorPicker) {
                    ColorPickerDialog(
                        title = "选择浏览器主题色",
                        initialColor = luotianYiColor,
                        presetColors = listOf(
                            Color(0xFF66CCFF),
                            Color(0xFF2196F3),
                            Color(0xFF4CAF50),
                            Color(0xFFF44336),
                            Color(0xFFFF9800),
                            Color(0xFF9C27B0),
                        ),
                        onDismiss = { showLuotianYiColorPicker = false },
                        onColorSelected = { color ->
                            val argbColor = android.graphics.Color.argb(
                                (color.alpha * 255).toInt(),
                                (color.red * 255).toInt(),
                                (color.green * 255).toInt(),
                                (color.blue * 255).toInt(),
                            )
                            preferences.edit { putInt("luotianyi_color", argbColor) }
                            Toast.makeText(context, "浏览器主题色已保存", Toast.LENGTH_SHORT).show()
                            showLuotianYiColorPicker = false
                        },
                    )
                }

                val currentIsDarkTheme = ThemeManager.isDarkTheme()
                var showBackgroundColorPicker by remember { mutableStateOf(false) }
                val backgroundColor = ThemeManager.getBackgroundColor()

                SettingItem(
                    title = { Text("自定义背景颜色") },
                    description = { Text(if (currentIsDarkTheme) "深色模式背景色" else "浅色模式背景色") },
                    onClick = { showBackgroundColorPicker = true },
                    endAction = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(backgroundColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                    },
                )

                if (showBackgroundColorPicker) {
                    ColorPickerDialog(
                        title = "选择背景颜色",
                        initialColor = backgroundColor,
                        presetColors = listOfNotNull(
                            Color(if (currentIsDarkTheme) 0xFF121212.toInt() else 0xFFFFFFFF.toInt()),
                            MaterialTheme.colorScheme.surfaceContainer,
                            if (ThemeManager.isDarkTheme()) Color.Black else null,
                        ),
                        onDismiss = { showBackgroundColorPicker = false },
                        onColorSelected = { color ->
                            ThemeManager.setBackgroundColor(context, color, currentIsDarkTheme)
                            Toast.makeText(context, "背景颜色已保存", Toast.LENGTH_SHORT).show()
                            showBackgroundColorPicker = false
                        },
                    )
                }
            }
            // ── 阅读 ────────────────────────────────────────────────────────────
            SettingItemGroup(
                title = "阅读",
            ) {
                var fontSize by remember { mutableIntStateOf(preferences.getInt("webviewFontSize", 100)) }
                SettingItem(
                    title = { Text("字号") },
                    description = { Text("调整内容文字大小 ($fontSize%)") },
                    settingKey = "fontScale",
                    highlightedKey = setting,
                    onPositioned = { itemPositions["fontScale"] = it },
                    bottomAction = {
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = {
                                fontSize = it.toInt()
                                preferences.edit { putInt("webviewFontSize", it.toInt()) }
                            },
                            valueRange = 50f..200f,
                            steps = 14,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    },
                )

                var lineHeight by remember { mutableIntStateOf(preferences.getInt("webviewLineHeight", 160)) }
                SettingItem(
                    title = { Text("行高") },
                    description = { Text("调整内容行间距 (${lineHeight / 100f})") },
                    bottomAction = {
                        Slider(
                            value = lineHeight.toFloat(),
                            onValueChange = {
                                lineHeight = it.toInt()
                                preferences.edit { putInt("webviewLineHeight", it.toInt()) }
                            },
                            valueRange = 100f..300f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    },
                )
            }

            // ── 信息流 ──────────────────────────────────────────────────────────
            val showRefreshFab = remember { mutableStateOf(preferences.getBoolean("showRefreshFab", true)) }
            SettingItemGroup(
                title = "信息流",
            ) {
                val showFeedThumbnail = remember { mutableStateOf(preferences.getBoolean("showFeedThumbnail", true)) }
                SettingItemWithSwitch(
                    title = { Text("显示 Feed 卡片缩略图") },
                    description = { Text("在信息流卡片中显示文章缩略图。") },
                    checked = showFeedThumbnail.value,
                    onCheckedChange = {
                        showFeedThumbnail.value = it
                        preferences.edit { putBoolean("showFeedThumbnail", it) }
                    },
                )

                SettingItemWithSwitch(
                    title = { Text("显示刷新 FAB 按钮") },
                    description = { Text("在页面上显示可拖动的刷新按钮。") },
                    checked = showRefreshFab.value,
                    onCheckedChange = {
                        showRefreshFab.value = it
                        preferences.edit { putBoolean("showRefreshFab", it) }
                    },
                )

                var feedCardStyleExpanded by remember { mutableStateOf(false) }
                val feedCardStyle = remember {
                    mutableStateOf(preferences.getString("feedCardStyle", "card") ?: "card")
                }
                val feedCardStyleOptions = listOf(
                    "card" to "卡片样式",
                    "divider" to "分割线样式",
                )
                SettingItem(
                    title = { Text("信息流样式") },
                    description = { Text("卡片样式使用圆角卡片展示，分割线样式使用细线分隔条目。") },
                    endAction = {
                        ExposedDropdownMenuBox(
                            expanded = feedCardStyleExpanded,
                            onExpandedChange = { feedCardStyleExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = feedCardStyleOptions.find { it.first == feedCardStyle.value }?.second ?: "卡片样式",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = feedCardStyleExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .width(160.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = feedCardStyleExpanded,
                                onDismissRequest = { feedCardStyleExpanded = false },
                            ) {
                                feedCardStyleOptions.forEach { (mode, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            feedCardStyle.value = mode
                                            preferences.edit { putString("feedCardStyle", mode) }
                                            feedCardStyleExpanded = false
                                            Toast.makeText(context, "已设置为：$label", Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }

            // ── 回答页 ──────────────────────────────────────────────────────────
            val buttonSkipAnswer = remember { mutableStateOf(preferences.getBoolean("buttonSkipAnswer", true)) }
            SettingItemGroup(
                title = "回答页",
            ) {
                val articleUseWebview = remember { mutableStateOf(preferences.getBoolean("articleUseWebview", true)) }
                SettingItemWithSwitch(
                    title = { Text("使用 WebView 显示文章") },
                    description = { Text("关闭后使用 Compose 渲染，文本选择更好但格式支持较少。") },
                    checked = articleUseWebview.value,
                    onCheckedChange = {
                        articleUseWebview.value = it
                        preferences.edit { putBoolean("articleUseWebview", it) }
                    },
                )

                AnimatedVisibility(visible = articleUseWebview.value) {
                    var customFontName by remember {
                        mutableStateOf(preferences.getString("webviewCustomFontName", null))
                    }
                    val fontFilePicker = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument(),
                    ) { uri ->
                        if (uri == null) return@rememberLauncherForActivityResult
                        val name = uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
                        val destFile = java.io.File(context.filesDir, "custom_font")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        preferences.edit {
                            putString("webviewCustomFontName", name)
                        }
                        customFontName = name
                        Toast.makeText(context, "字体已设置，重新打开文章后生效", Toast.LENGTH_SHORT).show()
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        SettingItem(
                            title = { Text("WebView 自定义字体") },
                            description = { Text(customFontName ?: "未设置") },
                            bottomAction = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            fontFilePicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream"))
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                                        Text("选择", modifier = Modifier.padding(start = 4.dp))
                                    }
                                    if (customFontName != null) {
                                        OutlinedButton(
                                            onClick = {
                                                java.io.File(context.filesDir, "custom_font").delete()
                                                preferences.edit { remove("webviewCustomFontName") }
                                                customFontName = null
                                                Toast.makeText(context, "已清除自定义字体", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = null)
                                            Text("清除", modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                }
                            },
                        )

                        val useHardwareAcceleration = remember { mutableStateOf(preferences.getBoolean("webviewHardwareAcceleration", true)) }
                        SettingItemWithSwitch(
                            title = { Text("WebView 硬件加速") },
                            description = { Text("提高渲染性能，可能导致兼容性问题。") },
                            checked = useHardwareAcceleration.value,
                            onCheckedChange = {
                                useHardwareAcceleration.value = it
                                preferences.edit { putBoolean("webviewHardwareAcceleration", it) }
                            },
                        )
                    }
                }

                val isTitleAutoHide = remember { mutableStateOf(preferences.getBoolean("titleAutoHide", false)) }
                SettingItemWithSwitch(
                    title = { Text("自动隐藏回答标题") },
                    description = { Text("滚动时自动隐藏回答标题栏。") },
                    checked = isTitleAutoHide.value,
                    onCheckedChange = {
                        isTitleAutoHide.value = it
                        preferences.edit { putBoolean("titleAutoHide", it) }
                    },
                )

                val autoHideArticleBottomBar = remember {
                    mutableStateOf(preferences.getBoolean("autoHideArticleBottomBar", false))
                }
                SettingItemWithSwitch(
                    title = { Text("自动隐藏回答底部按钮") },
                    description = { Text("上划时隐藏回答底部操作按钮，下划时重新显示。") },
                    checked = autoHideArticleBottomBar.value,
                    onCheckedChange = {
                        autoHideArticleBottomBar.value = it
                        preferences.edit { putBoolean("autoHideArticleBottomBar", it) }
                    },
                )

                SettingItemWithSwitch(
                    title = { Text("显示跳转下一个回答按钮") },
                    description = { Text("在回答页面显示可拖动的快速跳转按钮。") },
                    checked = buttonSkipAnswer.value,
                    onCheckedChange = {
                        buttonSkipAnswer.value = it
                        preferences.edit { putBoolean("buttonSkipAnswer", it) }
                    },
                )

                val autoHideSkipAnswerButton = remember { mutableStateOf(preferences.getBoolean("autoHideSkipAnswerButton", true)) }
                AnimatedVisibility(buttonSkipAnswer.value) {
                    SettingItemWithSwitch(
                        title = { Text("滚动时自动隐藏跳转按钮") },
                        description = { Text("上划时淡出「下一个回答」按钮，下划时淡入显示。") },
                        checked = autoHideSkipAnswerButton.value,
                        onCheckedChange = {
                            autoHideSkipAnswerButton.value = it
                            preferences.edit { putBoolean("autoHideSkipAnswerButton", it) }
                        },
                    )
                }

                val pinAnswerDate = remember { mutableStateOf(preferences.getBoolean("pinAnswerDate", false)) }
                SettingItemWithSwitch(
                    title = { Text("置顶回答日期") },
                    description = { Text("将回答的发布日期和编辑日期移动到内容最前面显示。") },
                    checked = pinAnswerDate.value,
                    onCheckedChange = {
                        pinAnswerDate.value = it
                        preferences.edit { putBoolean("pinAnswerDate", it) }
                    },
                )

                var answerSwitchExpanded by remember { mutableStateOf(false) }
                val answerSwitchMode = remember {
                    mutableStateOf(preferences.getString("answerSwitchMode", "vertical") ?: "vertical")
                }
                val answerSwitchOptions = listOf(
                    "off" to "关闭",
                    "vertical" to "上下滑动",
                    "horizontal" to "左右滑动",
                )
                SettingItem(
                    title = { Text("回答切换手势") },
                    description = { Text("在回答页面通过手势切换同一问题下的其他回答。") },
                    endAction = {
                        ExposedDropdownMenuBox(
                            expanded = answerSwitchExpanded,
                            onExpandedChange = { answerSwitchExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = answerSwitchOptions.find { it.first == answerSwitchMode.value }?.second ?: "上下滑动切换",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = answerSwitchExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .width(160.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = answerSwitchExpanded,
                                onDismissRequest = { answerSwitchExpanded = false },
                            ) {
                                answerSwitchOptions.forEach { (mode, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            answerSwitchMode.value = mode
                                            preferences.edit { putString("answerSwitchMode", mode) }
                                            answerSwitchExpanded = false
                                            Toast.makeText(context, "已设置为：$label", Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }

            // ── 底部导航栏 ──────────────────────────────────────────────────────
            val allBottomBarItems = listOf(
                Home.name to "主页",
                Follow.name to "关注",
                HotList.name to "热榜",
                Daily.name to "日报",
                OnlineHistory.name to "历史",
                Account.name to "账号设置",
            )
            var startDestinationExpanded by remember { mutableStateOf(false) }
            var startDestinationKey by remember {
                mutableStateOf(
                    resolveValidStartDestinationKey(
                        preferences.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
                        allBottomBarItems.map { it.first }.filter { it in selectedBottomBarItemKeys.value },
                    ),
                )
            }

            fun persistBottomBarSelection(
                currentSet: Set<String>
            ) {
                val normalizedSet = normalizeBottomBarSelection(
                    currentSet,
                    enforceMinimumSelection = true,
                )
                val availableKeys = allBottomBarItems.map { it.first }.filter { it in normalizedSet }
                val resolvedStartDestination = resolveValidStartDestinationKey(startDestinationKey, availableKeys)
                selectedBottomBarItemKeys.value = normalizedSet
                startDestinationKey = resolvedStartDestination
                preferences.edit {
                    putStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, normalizedSet)
                    putString(START_DESTINATION_PREFERENCE_KEY, resolvedStartDestination)
                }
            }

            SettingItemGroup(
                title = "底部导航栏",
            ) {
                val startDestinationItems = allBottomBarItems.filter { it.first in selectedBottomBarItemKeys.value }

                SettingItem(
                    title = { Text("应用启动默认页面") },
                    description = { Text("仅可选择已在底部导航栏中显示的页面。") },
                    endAction = {
                        ExposedDropdownMenuBox(
                            expanded = startDestinationExpanded,
                            onExpandedChange = {
                                if (startDestinationItems.isNotEmpty()) {
                                    startDestinationExpanded = it
                                }
                            },
                        ) {
                            OutlinedTextField(
                                value = startDestinationItems.find { it.first == startDestinationKey }?.second ?: "主页",
                                onValueChange = {},
                                readOnly = true,
                                enabled = startDestinationItems.isNotEmpty(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startDestinationExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .width(160.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = startDestinationExpanded,
                                onDismissRequest = { startDestinationExpanded = false },
                            ) {
                                startDestinationItems.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            startDestinationKey = key
                                            preferences.edit { putString(START_DESTINATION_PREFERENCE_KEY, key) }
                                            startDestinationExpanded = false
                                            Toast.makeText(context, "已设置启动页：$label，重启后生效", Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                }
                            }
                        }
                    },
                )

                SettingItem(
                    title = { Text("选择要在底部栏显示的页面") },
                    description = {
                        Text("建议选择 3-5 项。")
                    },
                    bottomAction = {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            allBottomBarItems.forEach { (key, label) ->
                                val isChecked = selectedBottomBarItemKeys.value.contains(key)
                                val candidateSet = normalizeBottomBarSelection(
                                    selectedBottomBarItemKeys.value.toMutableSet().apply {
                                        if (isChecked) remove(key) else add(key)
                                    }
                                )
                                val isEnabled = key != Account.name

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isEnabled) {
                                            when {
                                                candidateSet.size < 3 -> {
                                                    Toast.makeText(context, "至少保留3项", Toast.LENGTH_SHORT).show()
                                                }

                                                candidateSet.size > 5 -> {
                                                    Toast.makeText(context, "最多选择5项", Toast.LENGTH_SHORT).show()
                                                }

                                                else -> persistBottomBarSelection(candidateSet)
                                            }
                                        }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    )
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        enabled = isEnabled,
                                    )
                                }
                            }
                        }
                    },
                )

                val tapToRefresh = remember { mutableStateOf(preferences.getBoolean("bottomBarTapScrollToTop", true)) }
                SettingItemWithSwitch(
                    title = { Text("点击底部导航栏回到顶部/刷新") },
                    description = { Text("点击底部导航栏当前页面按钮回到顶部，已在顶部时则刷新页面。双击可直接刷新。") },
                    checked = tapToRefresh.value,
                    onCheckedChange = {
                        tapToRefresh.value = it
                        preferences.edit { putBoolean("bottomBarTapScrollToTop", it) }
                    },
                )

                val autoHideBottomBar = remember { mutableStateOf(preferences.getBoolean("autoHideBottomBar", false)) }
                SettingItemWithSwitch(
                    title = { Text("滚动时自动隐藏底部导航栏") },
                    description = { Text("上划时隐藏底部导航栏，下划时重新显示。") },
                    checked = autoHideBottomBar.value,
                    onCheckedChange = {
                        autoHideBottomBar.value = it
                        preferences.edit { putBoolean("autoHideBottomBar", it) }
                    },
                )
            }

            // ── 交互 ────────────────────────────────────────────────────────────
            SettingItemGroup(
                title = "交互",
            ) {
                var shareActionExpanded by remember { mutableStateOf(false) }
                val shareActionMode = remember {
                    mutableStateOf(preferences.getString("shareActionMode", "ask") ?: "ask")
                }
                val shareActionOptions = listOf(
                    "ask" to "询问",
                    "copy" to "复制链接",
                    "share" to "Android分享",
                )
                SettingItem(
                    title = { Text("分享操作") },
                    description = { Text("点击分享按钮时的默认行为。") },
                    settingKey = "shareAction",
                    highlightedKey = setting,
                    onPositioned = { itemPositions["shareAction"] = it },
                    endAction = {
                        ExposedDropdownMenuBox(
                            expanded = shareActionExpanded,
                            onExpandedChange = { shareActionExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = shareActionOptions.find { it.first == shareActionMode.value }?.second ?: "询问",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shareActionExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .width(160.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = shareActionExpanded,
                                onDismissRequest = { shareActionExpanded = false },
                            ) {
                                shareActionOptions.forEach { (mode, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            shareActionMode.value = mode
                                            preferences.edit { putString("shareActionMode", mode) }
                                            shareActionExpanded = false
                                            Toast.makeText(context, "已设置为：$label", Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }

            // ── 搜索 ────────────────────────────────────────────────────────────
            SettingItemGroup(
                title = "搜索",
            ) {
                val showSearchHotSearch = remember { mutableStateOf(preferences.getBoolean("showSearchHotSearch", true)) }
                SettingItemWithSwitch(
                    title = { Text("搜索界面显示热搜") },
                    description = { Text("在搜索界面空白时显示知乎热搜关键词。") },
                    checked = showSearchHotSearch.value,
                    onCheckedChange = {
                        showSearchHotSearch.value = it
                        preferences.edit { putBoolean("showSearchHotSearch", it) }
                    },
                    settingKey = "showSearchHotSearch",
                    highlightedKey = setting,
                    onPositioned = { itemPositions["showSearchHotSearch"] = it },
                )
            }

            // ── 导航 ────────────────────────────────────────────────────────────
            SettingItemGroup(
                title = "技术性导航设置",
            ) {
                val useCustomNavHost = remember { mutableStateOf(preferences.getBoolean("use_custom_nav_host", true)) }
                SettingItemWithSwitch(
                    title = { Text("使用自定义导航") },
                    description = { Text("使用自定义导航替代系统默认的导航组件，可能部分提升国产手机上的操作手感，请视情况开启。") },
                    checked = useCustomNavHost.value,
                    onCheckedChange = {
                        useCustomNavHost.value = it
                        preferences.edit { putBoolean("use_custom_nav_host", it) }
                        Toast.makeText(context, "需要重启应用生效", Toast.LENGTH_SHORT).show()
                    },
                )

                val enablePredictiveBack = remember { mutableStateOf(preferences.getBoolean("enable_predictive_back", true)) }
                SettingItemWithSwitch(
                    title = { Text("启用预测性返回") },
                    description = { Text("开启 Android 14+ 的预测性返回手势动画。") },
                    checked = enablePredictiveBack.value,
                    onCheckedChange = {
                        enablePredictiveBack.value = it
                        preferences.edit { putBoolean("enable_predictive_back", it) }
                    },
                )
            }
        }
    }
}
