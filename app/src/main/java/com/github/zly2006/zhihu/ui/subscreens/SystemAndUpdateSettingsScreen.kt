package com.github.zly2006.zhihu.ui.subscreens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch
import com.github.zly2006.zhihu.util.ContinuousUsageReminderManager
import com.github.zly2006.zhihu.util.ContinuousUsageReminderPolicy
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemAndUpdateSettingsScreen(
    innerPadding: PaddingValues,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("系统与更新") },
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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(vertical = 16.dp),
        ) {
            val coroutineScope = rememberCoroutineScope()

            // Github Token
            var githubToken by remember { mutableStateOf(preferences.getString("githubToken", "") ?: "") }
            var showGithubToken by remember { mutableStateOf(false) }

            SettingItemGroup {
                SettingItem(
                    title = { Text("GitHub Token") },
                    description = {
                        Text(
                            "用于访问 GitHub API 时解除限速，提高更新检查的稳定性。留空则使用匿名访问，检查更新可能会失败。",
                        )
                    },
                    bottomAction = {
                        OutlinedTextField(
                            value = githubToken,
                            onValueChange = {
                                githubToken = it
                                preferences.edit { putString("githubToken", it) }
                            },
                            visualTransformation = if (showGithubToken) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGithubToken = !showGithubToken }) {
                                    Icon(
                                        imageVector = if (showGithubToken) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            singleLine = true,
                        )
                    },
                )

                var checkNightlyUpdates by remember { mutableStateOf(preferences.getBoolean("checkNightlyUpdates", false)) }
                SettingItemWithSwitch(
                    title = { Text("检查 Nightly 版本更新") },
                    description = { Text("检查每日构建版本 (可能不稳定)") },
                    checked = checkNightlyUpdates,
                    onCheckedChange = {
                        checkNightlyUpdates = it
                        preferences.edit { putBoolean("checkNightlyUpdates", it) }
                    },
                )

                var allowTelemetry by remember { mutableStateOf(preferences.getBoolean("allowTelemetry", true)) }
                SettingItemWithSwitch(
                    title = { Text("允许发送遥测统计数据") },
                    description = { Text("仅用于统计使用人数，不包含个人隐私") },
                    checked = allowTelemetry,
                    onCheckedChange = {
                        allowTelemetry = it
                        preferences.edit { putBoolean("allowTelemetry", it) }
                    },
                )
            }

            var reminderExpanded by remember { mutableStateOf(false) }
            var reminderIntervalMinutes by remember {
                mutableIntStateOf(
                    ContinuousUsageReminderPolicy.normalizeIntervalMinutes(
                        preferences.getInt(
                            ContinuousUsageReminderManager.KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES,
                            0,
                        ),
                    ),
                )
            }
            val reminderOptions = listOf(
                0 to "关闭",
                15 to "每 15 分钟",
                30 to "每 30 分钟",
                60 to "每 1 小时",
            )

            SettingItemGroup(
                title = "防沉迷",
            ) {
                SettingItem(
                    title = { Text("防沉迷提醒") },
                    description = { Text("你已经连续浏览知乎 N 小时 M 分钟了，休息一下吧。退出后 5 分钟内重开仍视为连续使用。") },
                    endAction = {
                        ExposedDropdownMenuBox(
                            expanded = reminderExpanded,
                            onExpandedChange = { reminderExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = reminderOptions
                                    .find { it.first == reminderIntervalMinutes }
                                    ?.second ?: "关闭",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .width(160.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = reminderExpanded,
                                onDismissRequest = { reminderExpanded = false },
                            ) {
                                reminderOptions.forEach { (minutes, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            reminderIntervalMinutes = minutes
                                            preferences.edit {
                                                putInt(
                                                    ContinuousUsageReminderManager
                                                        .KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES,
                                                    minutes,
                                                )
                                            }
                                            reminderExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }

            SettingItemGroup(
                title = "交流 & 闲聊",
                footer = { Text("代码和功能反馈请前往GitHub。上边的频道用于用户交流和闲聊，开发者不一定会在线回答问题。") },
            ) {
                SettingItem(
                    title = { Text("Discord 频道") },
                    description = { Text("请在 my-other-apps/zhihu-plus-plus 频道讨论") },
                    icon = { Icon(painterResource(R.drawable.ic_discord_24dp), null) },
                    endAction = {
                        Icon(
                            Icons.Default.ArrowOutward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { luoTianYiUrlLauncher(context, "https://discord.gg/YCPFZV5XSA".toUri()) },
                )

                SettingItem(
                    title = { Text("Telegram 群组 (Hydrogen)") },
                    description = { Text("另一个知乎客户端 Hydrogen 的群组，也可以在里面讨论知乎++哦") },
                    icon = { Icon(painterResource(R.drawable.ic_telegram_24dp), null) },
                    endAction = {
                        Icon(
                            Icons.Default.ArrowOutward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { luoTianYiUrlLauncher(context, "https://t.me/+_A1Yto6EpyIyODA1".toUri()) },
                )

                SettingItem(
                    title = { Text("Github issue") },
                    description = { Text("欢迎提交 issue 讨论功能和反馈问题") },
                    icon = { Icon(painterResource(R.drawable.ic_github_24dp), null) },
                    endAction = {
                        Icon(
                            Icons.Default.ArrowOutward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { luoTianYiUrlLauncher(context, "https://github.com/zly2006/zhihu-plus-plus/issues".toUri()) },
                )
            }
        }
    }
}
