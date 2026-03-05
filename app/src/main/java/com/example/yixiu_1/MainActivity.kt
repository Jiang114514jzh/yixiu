package com.example.yixiu_1

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
// 在文件顶部的 import 区域添加正确的导入

import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// 在文件顶部添加这些导入
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
// UI Components
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import coil.request.ImageRequest
import com.example.yixiu_1.data.UserPreferences
import com.example.yixiu_1.network.EmailRegisterOrLoginRequest
import com.example.yixiu_1.network.RepairHistoryItem
import com.example.yixiu_1.network.RepairTaskRequest

// ==================== 必须添加的引用 ====================
import com.example.yixiu_1.network.NetworkClient
import com.example.yixiu_1.network.RepairTaskItem
// =======================================================
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

import com.example.yixiu_1.ui.ProfileScreen
import com.example.yixiu_1.ui.theme.YIXIU_1Theme
import com.example.yixiu_1.ui.CommunityScreen
import com.example.yixiu_1.ui.CreatePostScreen
import com.example.yixiu_1.ui.MyCollectionScreen
import com.example.yixiu_1.network.CommunityPostItem


import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.yixiu_1.network.*
import com.example.yixiu_1.ui.EditProfileScreen
import com.example.yixiu_1.ui.PostDetailScreen


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlin.text.toIntOrNull

val pageSize = AppConstants.DEFAULT_PAGE_SIZE
val pageNum = AppConstants.INITIAL_PAGE_NUM


// ===================== 1. MainActivity =====================
class MainActivity : ComponentActivity() {

    init {
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("MainActivity", "未捕获异常: ${thread.name}", exception)
            exception.printStackTrace()
        }
    }

    // in MainActivity.kt
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // 【关键修复】注释掉 enableEdgeToEdge() 来解决键盘动画超时问题
            // enableEdgeToEdge()

            setContent {
                YIXIU_1Theme {
                    SafeAppContent()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "启动失败", e)
        }
    }
}

// ===================== 2. 顶层类与辅助函数 =====================

sealed class Screen {
    object Home : Screen()
    object VolunteerHome : Screen()
    object Profile : Screen()
    object Login : Screen()
    object AppointmentQuestionnaire : Screen()
    object RepairHistory : Screen()
    data class RepairDetail(val taskId: String) : Screen()
    object MessageCenter : Screen()
    // 请确保在 Screen 类定义中包含：
    object TaskList : Screen()
    object MyTasks : Screen()
    object MemberManagement : Screen()
    object UserManagement : Screen()
    data class Community(val preloadedPosts: List<com.example.yixiu_1.network.CommunityPostItem>? = null) : Screen()
    object MyCollection : Screen()
    object CreatePost : Screen()
    object EditProfile : Screen()
    data class PostDetail(val postId: Int) : Screen()
}

// ===================== 3. 核心导航与动画逻辑 =====================

@Composable
private fun SafeAppContent() {
    AppContent()
}

@Composable
fun ErrorScreen(error: String) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White), contentAlignment = Alignment.Center) {
        Text(text = "错误: $error", color = Color.Red)
    }
}

@Composable
fun AvatarImage(
    path: String?,
    modifier: Modifier = Modifier,
    defaultTint: Color = Color.Gray
) {
    // 【新增】获取 Context 和 UserPreferences 以便读取 Token
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val token = userPreferences.token

    if (!path.isNullOrBlank()) {
        Box(modifier = modifier) {
            // 底层：占位图标
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                tint = defaultTint
            )
            // 上层：网络图片
            AsyncImage(
                // 【修改】使用 ImageRequest 构建器，添加 Authorization Header 和监听器
                model = ImageRequest.Builder(context)
                    .data(path)
                    .crossfade(true) // 淡入效果
                    .apply {
                        // 【关键修改】如果存在 Token，将其添加到请求头 Authorization 中
                        // 这样服务器才能通过鉴权返回图片
                        if (!token.isNullOrBlank()) {
                            addHeader("Authorization", token)
                        }
                    }
                    .listener(
                        onStart = { Log.d("AvatarImage", "开始加载头像: $path") },
                        onSuccess = { _, _ -> Log.d("AvatarImage", "头像加载成功") },
                        onError = { _, result ->
                            Log.e("AvatarImage", "头像加载失败: $path")
                            Log.e("AvatarImage", "错误原因: ${result.throwable.localizedMessage}", result.throwable)
                        }
                    )
                    .build(),
                contentDescription = "Avatar",
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Avatar",
            modifier = modifier,
            tint = defaultTint
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun StandardTopAppBar(
    title: String,
    drawerState: DrawerState,
    scope: CoroutineScope,
    isLoggedIn: Boolean,
    avatarPath: String?,
    hasUnreadMessages: Boolean = false, // 【新增参数】是否有未读消息
    onMessageClick: () -> Unit = {},    // 【新增参数】点击消息图标的回调
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onAvatarClick: () -> Unit
) {
    Surface(shadowElevation = 4.dp, color = Color.White) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                Image(
                    painter = painterResource(id = R.drawable.app_log),
                    contentDescription = "图标",
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                        .clickable {
                            scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() }
                        }
                )
            },
            actions = {
                // 【新增】消息中心入口
                // 只有登录后才显示消息入口，或者你可以选择始终显示但点击提示登录
                if (isLoggedIn) {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp) // 与头像保持一点距离
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent) // 或者稍微灰一点的背景 Color(0xFFF5F5F5)
                            .clickable(onClick = onMessageClick),
                        contentAlignment = Alignment.Center
                    ) {
                        // 信封图标
                        Icon(
                            imageVector = Icons.Default.Email, // 需要 import androidx.compose.material.icons.filled.Email
                            contentDescription = "消息中心",
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )

                        // 红点提示
                        if (hasUnreadMessages) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp) // 调整红点位置
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                        }
                    }
                }

                with(sharedTransitionScope) {
                    IconButton(onClick = onAvatarClick) {
                        AvatarImage(
                            path = if (isLoggedIn) avatarPath else null,
                            modifier = Modifier
                                .size(32.dp)
                                .sharedElement(
                                    state = rememberSharedContentState(key = "avatar"),
                                    animatedVisibilityScope = animatedContentScope,
                                    boundsTransform = { _, _ -> tween(500) }
                                )
                        )
                    }
                }
            }
        )
    }
}



@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppContent() {
    SharedTransitionLayout {
        val context = LocalContext.current
        val userPreferences = remember { UserPreferences(context) }
        val userRole = userPreferences.userRole
        val isVolunteer = userRole == "volunteer" || userRole == "super_admin"
        val isAdmin = userRole == "admin" || userRole == "super_admin"

        // 添加权限请求
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                // 权限被拒绝，可以选择引导用户去设置页面
                android.util.Log.d("Permission", "通知权限被拒绝")
            }
        }

        // 在应用启动时检查并请求通知权限
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // 权限已授予
                        android.util.Log.d("Permission", "通知权限已授予")
                    }
                    else -> {
                        // 请求权限
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

        // 【关键修改】在这里做角色判断，进行页面分发


            // 否则，显示原有的、包含所有普通用户逻辑的主页
            AppContentInternal(
                userPreferences = userPreferences,
                sharedTransitionScope = this@SharedTransitionLayout
            )

    }
}



@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun AppContentInternal(
    userPreferences: UserPreferences,
    sharedTransitionScope: SharedTransitionScope
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var avatarPath by remember { mutableStateOf<String?>(null) }
    var hasUnread by remember { mutableStateOf(false) }
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isGlobalLoading by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isApiLoading by remember { mutableStateOf(false) }

    fun sendMessage() {
        // 检查是否正在加载
        if (isApiLoading) return  // 如果正在加载，直接返回，不执行发送

        val trimmedInput = inputText.trim()
        if (trimmedInput.isEmpty()) return

        val userMessage = ChatMessage(
            content = trimmedInput,
            role = Role.USER
        )

        chatMessages = chatMessages + userMessage
        inputText = ""

        // 设置加载状态
        isApiLoading = true

        scope.launch {
            try {
                val doubaoHelper = DoubaoApiHelper()
                Log.d("DoubaoAPI", "准备发送请求: '$trimmedInput'")

                val result = doubaoHelper.sendTextMessage(trimmedInput)

                val assistantMessage = if (result.isSuccess) {
                    val response = result.getOrNull()
                    val content = try {
                        val choice = response?.choices?.firstOrNull()
                        val message = choice?.message
                        message?.content?.toString() ?: "无法获取豆包回复"
                    } catch (e: Exception) {
                        Log.e("DoubaoAPI", "解析响应失败: ${e.message}", e)
                        "解析回复时出错: ${e.message}"
                    }

                    ChatMessage(content = content, role = Role.ASSISTANT)
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e("DoubaoAPI", "API调用失败: ${exception?.message}", exception)
                    ChatMessage(content = "API调用失败: ${exception?.message}", role = Role.ASSISTANT)
                }

                chatMessages = chatMessages + assistantMessage
            } catch (e: Exception) {
                Log.e("DoubaoAPI", "发送消息时发生异常", e)
                val errorMessage = ChatMessage(content = "网络错误: ${e.message}", role = Role.ASSISTANT)
                chatMessages = chatMessages + errorMessage
            } finally {
                // 确保无论成功或失败都解除加载状态
                isApiLoading = false
            }
        }
    }

    // 在 AppContentInternal 函数内部开头
    var messageCenterRefreshTrigger by remember { mutableIntStateOf(0) } // 【新增】定义刷新触发器
// 【新增】用于通知消息中心刷新
    LaunchedEffect(isLoggedIn, currentScreen) {
        if (isLoggedIn) {
            try {
                // 调用获取消息列表接口，检查是否有 isRead == 0 的消息
                // 注意：这会拉取所有消息，如果消息量大建议后端提供专门的 /notify/unread-count 接口
                // 正确写法 (首字母大写，使用类名)
                val response = NetworkClient.instance.getNotifications(1,pageSize)
                Log.d("MessageCenter", "getNotifications 返回结果: $response")
                if (response.isSuccessful && response.body()?.code == 200) {
                    // 【修正点】这里需要先获取 data (NotifyPage)，再获取 list
                    val pageData = response.body()?.data
                    val list = pageData?.list ?: emptyList()

                    hasUnread = list.any { it.isRead == 0 }

                    // 如果你需要在这里更新 hasMoreData (虽然这里是局部逻辑，通常不需要)
                    // hasMoreData = (pageData?.pageNum ?: 1) < (pageData?.pages ?: 1)
                }
            } catch (e: Exception) {
                // 忽略错误，仅仅是不显示红点
            }
        } else {
            hasUnread = false
        }
    }
    LaunchedEffect(Unit, currentScreen) {
        isLoggedIn = userPreferences.isLoggedIn
        avatarPath = userPreferences.avatarPath

        // 【关键修复】应用启动时恢复Token，防止重启后无法提交数据
        val savedToken = userPreferences.token
        if (!savedToken.isNullOrBlank()) {
            NetworkClient.setToken(savedToken)
            Log.d("AppContent", "Token restored from preferences")
        }
    }

    var showNotificationDialog by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }

    // 长轮询逻辑
    // 长轮询逻辑
    LaunchedEffect(isLoggedIn) {
        // 【诊断 Log 1】打印 Effect 启动时间
        Log.d("PollService", "长轮询 LaunchedEffect 启动! IsLoggedIn=$isLoggedIn")

        if (isLoggedIn) {
            val token = userPreferences.token
            if (!token.isNullOrEmpty()) {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                // 在后台协程中运行
                val pollJob = launch {
                    Log.d("PollService", "轮询协程 (Job) 开始运行...")

                    while (isActive) {
                        try {
                            Log.d("PollService", ">>> 发起网络请求 (等待服务器响应)...")
                            // 挂起等待...
                            val response = NetworkClient.instance.pollNotifications(authHeader)

                            Log.d("PollService", "<<< 请求返回. HTTP Code=${response.code()}")

                            // 【修改点 1】只要 HTTP 200 就是成功，不需要判断 body.code
                            if (response.isSuccessful) {
                                // 【修改点 2】直接获取 body，它就是 PollResponse
                                val data = response.body()

                                Log.d("PollService", "服务器返回数据: $data")

                                if (data != null && data.type != "NONE") {
                                    Log.d("PollService", "条件满足，准备更新 UI...")

                                    // 活跃性检查
                                    if (!isActive) throw java.util.concurrent.CancellationException()

                                    val source = when(data.type) {
                                        "SYSTEM" -> "系统"
                                        "BROADCAST" -> "全员广播"
                                        "USER" -> "用户"
                                        else -> "新"
                                    }
                                    notificationMessage = "您有一条来自${source}的新消息\n目前有 ${data.unread} 条未读"

                                    // 更新 UI
                                    messageCenterRefreshTrigger++
                                    showNotificationDialog = true
                                    hasUnread = true

                                    // 刷新列表
                                    try {
                                        val listResponse = NetworkClient.instance.getNotifications(1,pageSize)
                                        // 注意：普通接口通常还是有 ApiResponse 包装的，这里保持原样
                                        if (listResponse.isSuccessful && listResponse.body()?.code == 200) {
                                            // 【修正点】解析两层结构 data -> list
                                            val pageData = listResponse.body()?.data
                                            val list = pageData?.list ?: emptyList()
                                            hasUnread = list.any { it.isRead == 0 }
                                    }} catch (e: Exception) {
                                        Log.e("PollService", "刷新列表失败", e)
                                    }
                                } else {
                                    Log.d("PollService", "无新消息 (Type=NONE)")
                                }
                            } else {
                                Log.w("PollService", "HTTP请求失败: Code=${response.code()}, Error=${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            if (e is java.util.concurrent.CancellationException) {
                                // 这是正常的页面销毁/重组导致的取消，不需要作为错误处理
                                Log.d("PollService", "轮询协程已停止")
                                throw e
                            } else if (e is java.net.SocketTimeoutException) {
                                Log.d("PollService", "请求超时 (正常心跳)，准备重试")
                                delay(100)
                            } else {
                                Log.e("PollService", "未捕获异常: ${e.message}")
                                delay(5000)
                            }
                        }
                        delay(1000)
                    }
                }

                // 等待 Job 结束（通常这行代码不会被执行到，除非 while 循环结束）
                pollJob.join()
            }
        }
    }


    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // ... 原有的变量定义 ...

    // 【新增】定义侧边栏展开状态
    var isServiceExpanded by remember { mutableStateOf(false) }     // 学生端
    var isTaskCenterExpanded by remember { mutableStateOf(true) }   // 志愿者端

    // 【新增】获取用户角色
    val userRole = userPreferences.userRole
    // 判断是否为志愿者或管理员
    val isVolunteer = userRole == "volunteer" || userRole == "super_admin"
    val isAdmin = userRole == "admin" || userRole == "super_admin"


    Box(modifier = Modifier.fillMaxSize()) {
        val blurRadius by animateDpAsState(
            targetValue = if (drawerState.isOpen) 16.dp else 3.dp,
            animationSpec = tween(300), label = "blur"
        )
        val imageModifier = if (blurRadius > 0.dp) {
            Modifier
                .fillMaxSize()
                .blur(radiusX = blurRadius, radiusY = blurRadius)
        } else { Modifier.fillMaxSize() }

        val backgroundImage = if (isAdmin) {
            painterResource(id = R.drawable.admin_background)
        }else{
                painterResource(id = R.drawable.bg_campus_repair)
            }

        Image(
            painter = backgroundImage,
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop
        )

        // 在 AppContentInternal 函数内的 ModalNavigationDrawer 中
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color.Transparent,
            drawerContent = {
                ModalDrawerSheet(Modifier.fillMaxWidth(0.618f)) {
                    Spacer(Modifier.height(64.dp))

                    // 通用导航项目 - 所有身份都可以看到
                    NavigationDrawerItem(
                        label = { Text("主页") },
                        selected = currentScreen == Screen.Home,
                        onClick = { scope.launch { drawerState.close() }; currentScreen = Screen.Home }
                    )

                    NavigationDrawerItem(
                        label = { Text("消息中心") },
                        selected = currentScreen == Screen.MessageCenter,
                        onClick = { scope.launch { drawerState.close() }; currentScreen = Screen.MessageCenter }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 动态侧边栏内容 - 根据身份显示不同功能
                    // 学生/普通用户功能
                    if (!isVolunteer && !isAdmin) {
                        ExpandableNavigationItem(
                            label = "义修服务",
                            isExpanded = isServiceExpanded,
                            onToggle = { isServiceExpanded = !isServiceExpanded },
                            subItems = listOf(
                                "预约维修" to {
                                    scope.launch { drawerState.close() }
                                    currentScreen = Screen.AppointmentQuestionnaire
                                },
                                "维修历史" to {
                                    scope.launch { drawerState.close() }
                                    currentScreen = Screen.RepairHistory
                                }
                            )
                        )
                    }

                    // 志愿者功能
                    if (isVolunteer) {
                        ExpandableNavigationItem(
                            label = "任务中心",
                            isExpanded = isTaskCenterExpanded,
                            onToggle = { isTaskCenterExpanded = !isTaskCenterExpanded },
                            subItems = listOf(
                                "任务列表" to {
                                    scope.launch { drawerState.close() }
                                    currentScreen = Screen.TaskList
                                },
                                "我的任务" to {
                                    scope.launch { drawerState.close() }
                                    currentScreen = Screen.MyTasks
                                }
                            )
                        )
                    }

                    // 管理员功能
                    if (isAdmin) {
                        NavigationDrawerItem(
                            label = { Text("任务中心") },
                            selected = currentScreen == Screen.TaskList,
                            onClick = { scope.launch { drawerState.close() }; currentScreen = Screen.TaskList }
                        )
                        NavigationDrawerItem(
                            label = { Text("用户管理") },
                            selected = currentScreen == Screen.UserManagement,
                            onClick = { scope.launch { drawerState.close() }; currentScreen = Screen.UserManagement }
                        )
                        NavigationDrawerItem(
                            label = { Text("成员管理") },
                            selected = currentScreen == Screen.MemberManagement,
                            onClick = { scope.launch { drawerState.close() }; currentScreen = Screen.MemberManagement }
                        )
                        NavigationDrawerItem(
                            label = { Text("义修社区") },
                            selected = currentScreen == Screen.Community(),
                            onClick = { scope.launch { drawerState.close() }; currentScreen = Screen.Community() }
                        )
                    }
                }
            }
        )
        {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith
                            fadeOut(animationSpec = tween(500))
                },
                label = "PageTransition"
            )

            { screen ->
                val animatedContentScope = this

                when (screen) {
                    is Screen.Home -> {
                        val title = when {
                            isAdmin -> "管理员中心"
                            isVolunteer -> "义修助手"
                            else -> "义修校园"
                        }

                        val welcomeText = when {
                            isAdmin -> "欢迎回来，管理员"
                            isVolunteer -> "欢迎使用义修助手"
                            else -> "欢迎使用义修校园"
                        }

                        Scaffold(
                            topBar = {
                                StandardTopAppBar(
                                    title = title,
                                    drawerState = drawerState, scope = scope,
                                    isLoggedIn = isLoggedIn,
                                    avatarPath = avatarPath,
                                    hasUnreadMessages = hasUnread,
                                    onMessageClick = { currentScreen = Screen.MessageCenter },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope,
                                    onAvatarClick = { currentScreen = Screen.Profile }
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize()
                            ) {
                                // 保留原有的模糊效果计算
                                val cardBlurRadius by animateDpAsState(
                                    targetValue = if (drawerState.isOpen) 10.dp else 0.dp,
                                    animationSpec = tween(300), label = "cardBlur"
                                )

                                val cardModifier = if (cardBlurRadius > 0.dp) {
                                    Modifier.blur(cardBlurRadius)
                                } else {
                                    Modifier
                                }

                                // 聊天界面容器
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Transparent) // 半透明白色背景
                                ) {
                                    // 聊天消息列表
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        reverseLayout = true
                                    ) {
                                        items(chatMessages.reversed()) { message ->
                                            ChatBubble(message = message)
                                        }
                                    }

                                    // 底部输入区域
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        OutlinedTextField(
                                            value = if (isApiLoading) "系统生成文本中" else inputText,  // 根据状态显示不同内容
                                            onValueChange = { if (!isApiLoading) inputText = it },      // 只有非加载状态下允许输入
                                            modifier = Modifier.weight(1f),
                                            placeholder = { Text("向义修助手提问...") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Sentences,
                                                imeAction = ImeAction.Send
                                            ),
                                            // 根据加载状态设置颜色
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White,
                                                disabledContainerColor = Color.White,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedTextColor = if (isApiLoading) Color.Gray else Color.Black,  // 加载时显示灰色文字
                                                focusedTextColor = if (isApiLoading) Color.Gray else Color.Black     // 加载时显示灰色文字
                                            ),
                                            shape = RoundedCornerShape(24.dp),
                                            keyboardActions = KeyboardActions(
                                                onSend = {
                                                    if (inputText.isNotBlank() && !isApiLoading) {  // 只有非加载状态下才允许发送
                                                        sendMessage()
                                                    }
                                                }
                                            ),
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        if (inputText.trim().isNotBlank() && !isApiLoading) {  // 只有非加载状态下才允许发送
                                                            sendMessage()
                                                        }
                                                    },
                                                    enabled = inputText.trim().isNotBlank() && !isApiLoading  // 根据输入内容和加载状态控制按钮可用性
                                                ) {
                                                    Icon(Icons.Default.Send, contentDescription = "发送")
                                                }
                                            }
                                        )

                                    }
                                }

                                // 原有的欢迎卡片（在聊天界面上方，透明度降低使其不遮挡聊天）
                                Card(
                                    modifier = cardModifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 16.dp)
                                        .padding(horizontal = 32.dp)
                                        .graphicsLayer(alpha = 0.7f), // 降低透明度，不影响聊天体验
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.8f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = welcomeText,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }


                    is Screen.TaskList -> {
                        Scaffold(
                            topBar = {
                                StandardTopAppBar(
                                    title = "任务列表",
                                    drawerState = drawerState, scope = scope,
                                    isLoggedIn = isLoggedIn,
                                    avatarPath = avatarPath,
                                    hasUnreadMessages = hasUnread,
                                    onMessageClick = { currentScreen = Screen.MessageCenter },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope,
                                    onAvatarClick = { currentScreen = Screen.Profile }
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            // 使用白色半透明背景，确保文字清晰，同时保留底部背景图
                            Surface(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize(),
                                color = Color.White.copy(alpha = 0.9f) // 90% 不透明度
                            ) {
                                TaskListScreen(userPreferences = userPreferences)
                            }
                        }
                    }
                    is Screen.MyTasks -> {
                        Scaffold(
                            topBar = {
                                StandardTopAppBar(
                                    title = "我的任务",
                                    drawerState = drawerState, scope = scope,
                                    isLoggedIn = isLoggedIn,
                                    avatarPath = avatarPath,
                                    hasUnreadMessages = hasUnread,
                                    onMessageClick = { currentScreen = Screen.MessageCenter },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope,
                                    onAvatarClick = { currentScreen = Screen.Profile }
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            Surface(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize(),
                                color = Color.White.copy(alpha = 0.9f)
                            ) {
                                MyTasksScreen()
                            }
                        }
                    }
                    is Screen.Profile -> {
                        ProfileScreen(
                            userPreferences = userPreferences,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope,
                            onNavigateToLogin = { currentScreen = Screen.Login },
                            onLogout = { currentScreen = Screen.Home },
                            onAvatarUpdated = { avatarPath = it },
                            onBack = { currentScreen = Screen.Home },
                            onNavigateToEdit = { currentScreen = Screen.EditProfile }
                        )
                    }

                    is Screen.EditProfile -> {
                        // --- 新增编辑页面分支 ---
                        EditProfileScreen(
                            apiService = NetworkClient.instance, // 使用你项目现有的网络客户端
                            onBack = { currentScreen = Screen.Profile } // 返回时回到个人中心
                        )
                    }
                    is Screen.Login -> {
                        // 【修改】移除 Scaffold，让 AuthScreen 直接显示在模糊背景之上
                        Box(modifier = Modifier.fillMaxSize()) {
                            // 让 AuthScreen 填充整个屏幕，并处理状态栏/导航栏 padding
                            AuthScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding(),
                                userPreferences = userPreferences,
                                onLoginSuccess = { currentScreen = Screen.Home }
                            )

                            // 手动添加返回按钮
                            IconButton(
                                onClick = { currentScreen = Screen.Home },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .statusBarsPadding()
                                    .padding(start = 16.dp, top = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack, // 【修复】修正参数名
                                    contentDescription = "返回",
                                    tint = Color.White // 在深色模糊背景上使用白色图标
                                )
                            }
                        }
                    }
                    Screen.AppointmentQuestionnaire -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("维修申请") },
                                    navigationIcon = {
                                        IconButton(onClick = { currentScreen = Screen.Home }) {
                                            Icon(Icons.Default.ArrowBack, "Back")
                                        }
                                    },
                                    modifier = Modifier.background(Color.White)
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            AppointmentQuestionnaireScreen(
                                modifier = Modifier.padding(innerPadding),
                                userPreferences = userPreferences,
                                onBack = { currentScreen = Screen.Home },
                                onSubmissionSuccess = { currentScreen = Screen.RepairHistory }
                            )
                        }
                    }
                    Screen.RepairHistory -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("维修历史") },
                                    navigationIcon = {
                                        IconButton(onClick = { currentScreen = Screen.Home }) {
                                            Icon(Icons.Default.ArrowBack, "Back")
                                        }
                                    },
                                    modifier = Modifier.background(Color.White)
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            key(userPreferences.userId) {
                                RepairHistoryScreen(
                                    userPreferences = userPreferences,
                                    onNavigateToDetail = { taskId -> currentScreen = Screen.RepairDetail(taskId) },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                    is Screen.RepairDetail -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("维修详情") },
                                    navigationIcon = {
                                        IconButton(onClick = { currentScreen = Screen.RepairHistory }) {
                                            Icon(Icons.Default.ArrowBack, "Back")
                                        }
                                    },
                                    modifier = Modifier.background(Color.White)
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            RepairDetailScreen(
                                userPreferences = userPreferences,
                                taskId = screen.taskId,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                    is Screen.MessageCenter -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("消息中心") },
                                    navigationIcon = {
                                        IconButton(onClick = { currentScreen = Screen.Home }) {
                                            Icon(Icons.Default.ArrowBack, "Back")
                                        }
                                    },
                                    modifier = Modifier.background(Color.White)
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            MessageCenterScreen(
                                modifier = Modifier.padding(innerPadding),
                                userPreferences = userPreferences,
                                refreshTrigger = messageCenterRefreshTrigger
                            )
                        }
                    }
                    is Screen.Community -> {
                        CommunityScreen(
                            userPreferences = userPreferences,
                            onNavigateToCreatePost = { currentScreen = Screen.CreatePost },
                            onBack = { currentScreen = Screen.Home },
                            preloadedPosts = (screen as Screen.Community).preloadedPosts,
                            // 【新增】传入跳转回调
                            onNavigateToMyCollection = { currentScreen = Screen.MyCollection },
                            onPostClick = { postId -> currentScreen = Screen.PostDetail(postId)}
                        )
                    }

                    // 【新增】我的收藏页面分支
                    is Screen.MyCollection -> {
                        MyCollectionScreen(
                            onBack = { currentScreen = Screen.Community() },
                            onPostClick = { postId -> currentScreen = Screen.PostDetail(postId) }// 返回社区
                        )
                    }
                    is Screen.CreatePost -> {
                        CreatePostScreen(
                            onBack = { currentScreen = Screen.Community() },
                            onPostSuccess = {
                                // 可以在这里刷新数据或清理状态
                                currentScreen = Screen.Community()
                            }
                        )
                    }
                    is Screen.PostDetail -> {
                        PostDetailScreen(
                            postId = screen.postId,
                            onBack = { currentScreen = Screen.Community() } // 看完详情返回社区主页
                        )
                    }
                    else -> {
                        // 如果进入了未知的 Screen，暂时显示一个空 Box 或者占位符
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }

            }
        }

        AnimatedVisibility(modifier = Modifier.align(Alignment.CenterStart), visible = drawerState.isClosed && currentScreen == Screen.Home) {
            Surface(shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp), color = Color.White.copy(alpha = 0.5f)) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Open Drawer", modifier = Modifier
                    .padding(8.dp)
                    .clickable { scope.launch { drawerState.open() } })
            }
        }
        // ========================================================
        // 【新代码】带动画的通知横幅        // ========================================================
        AnimatedVisibility(
            visible = showNotificationDialog,
            enter = slideInVertically(
                initialOffsetY = { -it }, // 从顶部-自身高度的位置开始
                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it }, // 向上滑出到-自身高度的位置
                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter) // 让横幅在顶部居中
                .statusBarsPadding() // 自动处理状态栏高度，避免覆盖
        ) {
            // 自定义卡片式通知
            Card(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧图标
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "新消息",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )

                    // 中间文字
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("新消息提醒", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notificationMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 右侧按钮
                    Column(horizontalAlignment = Alignment.End) {
                        // "立即查看" 按钮
                        Button(
                            onClick = {
                                showNotificationDialog = false
                                currentScreen = Screen.MessageCenter
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("查看")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // "忽略" 按钮
                        OutlinedButton(
                            onClick = { showNotificationDialog = false }, // 点击后，动画会自动向上收回
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("忽略")
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun TaskListScreen(
    modifier: Modifier = Modifier,
    userPreferences: UserPreferences
) {
    var isLoading by remember { mutableStateOf(true) }
    // 控制详情页显示：如果不为空，则显示详情页
    var selectedTask by remember { mutableStateOf<RepairTaskItem?>(null) }
    var tasks by remember { mutableStateOf<List<RepairTaskItem>>(emptyList()) }
    val context = LocalContext.current
    val token = remember { userPreferences.token }

    // 获取数据
    LaunchedEffect(Unit) {
        if (!token.isNullOrEmpty()) {
            isLoading = true
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                // 确保 NetworkClient 引用正确
                val response = com.example.yixiu_1.network.NetworkClient.instance.getAllTasks(authHeader)

                // 【修复】解析逻辑：先获取 ApiResponse，再获取 data，再获取 list
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data
                    tasks = data?.list ?: emptyList()
                } else {
                    Toast.makeText(context, "获取失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // 页面内容切换：列表 OR 详情
    Box(modifier = modifier.fillMaxSize()) {
        if (selectedTask != null) {
            // === 显示详情页 (如果还没有定义 TaskDetailScreen，请看下一步) ===
            // 这里临时调用，或者你需要添加 TaskDetailScreen 组件
            TaskDetailScreen(
                task = selectedTask!!,
                onBack = { selectedTask = null }
            )
        } else {
            // === 显示列表页 ===
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无维修任务", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tasks) { task ->
                        RepairTaskCard(task = task, onClick = { selectedTask = task })
                    }
                }
            }
        }
    }
}





@Composable
fun MyTasksScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Construction, // 或者其他相关图标
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "我的任务功能开发中...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }
    }
}


// ===================== 4. 组件定义 =====================

@Composable
fun ExpandableNavigationItem(
    label: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    subItems: List<Pair<String, () -> Unit>>
) {
    Column {
        NavigationDrawerItem(
            label = {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                    Text(label, modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            },
            selected = false,
            onClick = onToggle,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 24.dp)) {
                subItems.forEach { (subLabel, onClick) ->
                    NavigationDrawerItem(
                        label = { Text(subLabel) },
                        selected = false,
                        onClick = onClick,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(modifier: Modifier = Modifier, userPreferences: UserPreferences, onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    // 【关键变量】邀请码状态
    var inviteCode by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var isAutoLoggingIn by remember { mutableStateOf(!userPreferences.token.isNullOrBlank()) }

    val roleOptions = listOf("学生" to "student", "志愿者" to "volunteer", "管理员" to "admin")
    var selectedRole by remember { mutableStateOf("student") }

    LaunchedEffect(Unit) {
        val token = userPreferences.token
        if (!token.isNullOrBlank()) {
            try {
                // 发起验证请求
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                // 注意：请确保你的 ApiService.kt 中已经定义了这个 getUserInfo(token) 方法
                val response = NetworkClient.instance.getUserInfo() // 根据你现有的写法，这里可能不需要传参，看你接口怎么定义的

                // 判断登录是否依然有效
                if (response.isSuccessful && response.body()?.code == 200) {
                    // Token 有效！直接静默跳转到首页
                    onLoginSuccess()
                } else {
                    // Token 过期或无效
                    userPreferences.clear() // 清理本地的脏数据 (请确保 UserPreferences 中写了 clear() 方法)
                    Toast.makeText(context, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show()
                    isAutoLoggingIn = false // 关闭加载动画，露出底下的登录表单
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 网络异常导致无法校验时，为了让用户能看本地缓存，我们通常选择直接放行进入主页
                onLoginSuccess()
            }
        }
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            kotlinx.coroutines.delay(1000L)
            countdown--
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.8f)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(if (isLoginMode) "登录" else "注册", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
                Spacer(Modifier.height(24.dp))

                Text("选择您的身份", style = MaterialTheme.typography.titleSmall, color = Color.Black)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    roleOptions.forEach { (displayName, roleValue) ->
                        Row(modifier = Modifier
                            .clickable { selectedRole = roleValue }
                            .padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (selectedRole == roleValue),
                                onClick = { selectedRole = roleValue },
                                colors = RadioButtonDefaults.colors(selectedColor = Color.Black, unselectedColor = Color.Black.copy(alpha = 0.7f))
                            )
                            Text(text = displayName, color = Color.Black, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // 1. 邮箱输入框 (始终显示)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱", color = Color.Black.copy(alpha = 0.8f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.Black.copy(alpha = 0.5f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))

                // 2. 验证码输入框 (始终显示)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it },
                        label = { Text("验证码", color = Color.Black.copy(alpha = 0.8f)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Black,
                            unfocusedIndicatorColor = Color.Black.copy(alpha = 0.5f),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                if (email.isBlank()) {
                                    Toast.makeText(context, "请输入邮箱", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                countdown = 60
                                try {
                                    val response = NetworkClient.instance.sendEmailVerification(email)
                                    if (response.isSuccessful) Toast.makeText(context, "验证码已发送", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "发送失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "发送错误: ${e.message}", Toast.LENGTH_SHORT).show()
                                    countdown = 0
                                }
                            }
                        },
                        enabled = !isLoading && countdown == 0
                    ) { Text(if (countdown > 0) "${countdown}s" else "获取") }
                }

                // 3. 邀请码输入框 (仅在注册且选择志愿者时显示)
                // 【修复】将此部分移出验证码的 Row，放在下方单独显示
                AnimatedVisibility(visible = !isLoginMode && selectedRole == "volunteer") {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = { inviteCode = it },
                            label = { Text("志愿者邀请码", color = Color.Black.copy(alpha = 0.8f)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Black,
                                unfocusedIndicatorColor = Color.Black.copy(alpha = 0.5f),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 登录/注册按钮
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                // 逻辑分支：志愿者注册 vs 普通注册/登录
                                if (!isLoginMode && selectedRole == "volunteer") {
                                    // --- 志愿者注册逻辑 ---
                                    if (inviteCode.isBlank()) {
                                        Toast.makeText(context, "请输入志愿者邀请码", Toast.LENGTH_SHORT).show()
                                        isLoading = false
                                        return@launch
                                    }

                                    // 确保 verificationCode 也有值
                                    val vCodeInt = verificationCode.toIntOrNull()
                                    if (vCodeInt == null) {
                                        Toast.makeText(context, "请输入有效的验证码", Toast.LENGTH_SHORT).show()
                                        isLoading = false
                                        return@launch
                                    }

                                    val request = com.example.yixiu_1.network.VolunteerRegisterRequest(
                                        email = email,
                                        role = selectedRole,
                                        verificationCode = vCodeInt,
                                        inviteCode = inviteCode.toIntOrNull() ?: 0
                                    )
                                    val resp = NetworkClient.instance.registerVolunteer(request)
                                    if (resp.isSuccessful && resp.body()?.code == 200) {
                                        Toast.makeText(context, "志愿者注册成功，请登录", Toast.LENGTH_SHORT).show()
                                        isLoginMode = true // 切换回登录模式
                                    } else {
                                        Toast.makeText(context, "注册失败: ${resp.body()?.msg}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // --- 普通登录/注册逻辑 ---
                                    val codeInt = verificationCode.toIntOrNull() ?: 0
                                    val request = EmailRegisterOrLoginRequest(email = email, role = selectedRole, verificationCode = codeInt)
                                    val resp = if (isLoginMode) NetworkClient.instance.loginByEmail(request) else NetworkClient.instance.registerByEmail(request)
                                    val token = (resp.body()?.data as? String) ?: (resp.body()?.data as? Map<*, *>)?.get("token") as? String

                                    if (token != null) {
                                        userPreferences.token = token
                                        NetworkClient.setToken(token)
                                        userPreferences.isLoggedIn = true
                                        try {
                                            val uResp = NetworkClient.instance.getUserInfo()
                                            val userInfo = uResp.body()?.data
                                            if (userInfo != null && userInfo is com.example.yixiu_1.network.UserInfo) {
                                                userPreferences.userId = userInfo.userId
                                                userPreferences.userEmail = userInfo.email
                                                userPreferences.nickname = userInfo.username
                                                userPreferences.userRole = userInfo.role
                                                userPreferences.avatarPath = userInfo.avatar
                                            }
                                        } catch (e: Exception) {
                                            Log.e("Auth", "Fetch user info failed", e)
                                        }
                                        onLoginSuccess()
                                        Toast.makeText(context, if (isLoginMode) "登录成功" else "注册成功", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "操作失败: ${resp.body()?.msg ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "错误: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally { isLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) { Text(if (isLoginMode) "登录" else "注册", color = Color.White) }

                TextButton(onClick = { isLoginMode = !isLoginMode }) {
                    Text(if (isLoginMode) "没有账户？去注册" else "已有账户？去登录", color = Color.Black)
                }
            }
        }
        if (isAutoLoggingIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White) // 使用纯色挡住底下
                    .clickable(enabled = false) {}, // 防止用户在这个空隙瞎点到底下的组件
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF2196F3))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在验证登录状态...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}





@Composable
fun AppointmentQuestionnaireScreen(
    modifier: Modifier = Modifier,
    userPreferences: UserPreferences,
    onBack: () -> Unit,
    onSubmissionSuccess: () -> Unit
) {
    // 基础信息
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // 新增补充信息
    var contactInfo by remember { mutableStateOf("") } // 联系号码
    var deviceType by remember { mutableStateOf("") } // 设备类型
    var deviceSystem by remember { mutableStateOf("") } // 系统
    var deviceModel by remember { mutableStateOf("") } // 型号
    var appointmentTime by remember { mutableStateOf("") } // 预约时间
    var remarks by remember { mutableStateOf("") } // 备注

    // 选项 (使用 Int 代表类型)
    var contactType by remember { mutableIntStateOf(1) } // 1:QQ, 2:微信, 3:电话
    var campus by remember { mutableIntStateOf(0) } // 0:大学城, 1:白云山

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.White.copy(alpha = 0.9f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
        disabledContainerColor = Color.Gray,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = Color.Black,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.DarkGray
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "请详细填写报修单",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 1. 问题描述
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("问题描述 (必填)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. 校区选择
                Text("选择校区", color = Color.Black, modifier = Modifier.align(Alignment.Start))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    listOf("大学城校区" to 0, "白云山校区" to 1).forEach { (name, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { campus = value }
                        ) {
                            RadioButton(
                                selected = (campus == value),
                                onClick = { campus = value },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Text(name, color = Color.Black, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. 详细地址
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("详细地址 (如：xx公寓xxx)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. 联系方式
                Text("联系方式类型", color = Color.Black, modifier = Modifier.align(Alignment.Start))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    listOf("QQ" to 1, "微信" to 2, "电话" to 3).forEach { (name, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { contactType = value }
                        ) {
                            RadioButton(
                                selected = (contactType == value),
                                onClick = { contactType = value },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Text(name, color = Color.Black, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }

                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    label = { Text("联系号码/ID (必填)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. 设备信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = deviceType,
                        onValueChange = { deviceType = it },
                        label = { Text("设备类型 (如电脑，必填)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                    OutlinedTextField(
                        value = deviceSystem,
                        onValueChange = { deviceSystem = it },
                        label = { Text("系统 (如Win10,必填)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = { deviceModel = it },
                    label = { Text("设备型号 (选填)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 6. 预约时间
                OutlinedTextField(
                    value = appointmentTime,
                    onValueChange = { appointmentTime = it },
                    label = { Text("期望预约时间(必填)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 7. 备注
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("备注 (选填)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val userId = userPreferences.userId
                            if (userId == -1) {
                                Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            if (description.isBlank() || location.isBlank() || contactInfo.isBlank() || deviceType.isBlank() || deviceSystem.isBlank() || appointmentTime.isBlank()) {
                                Toast.makeText(context, "请填写必填项(描述、地址、联系方式、设备类型和系统、期望预约时间)", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            try {
                                // 【修复】根据 Task.kt，userId 是 Int，直接传递
                                val request = RepairTaskRequest(
                                    userId = userId, // 直接使用 Int 类型的 userId
                                    contactType = contactType,
                                    contactInfo = contactInfo,
                                    deviceType = deviceType,
                                    deviceSystem = deviceSystem,
                                    deviceModel = deviceModel,
                                    problemDescription = description,
                                    campus = campus,
                                    repairLocation = location,
                                    appointmentTime = appointmentTime,
                                    remarks = remarks.ifBlank { null }
                                )

                                // 【关键日志 1】打印将要发送的请求对象
                                Log.d("SubmitRepair", "Request Body: $request")

                                val response = NetworkClient.instance.submitRepairTask(request)

                                // 【关键日志 2】打印完整的后端响应
                                val responseBody = response.body()
                                val errorBody = response.errorBody()?.string()
                                Log.d("SubmitRepair", "Response Code: ${response.code()}")
                                Log.d("SubmitRepair", "Response Body: $responseBody")
                                Log.d("SubmitRepair", "Error Body: $errorBody")

                                if (response.isSuccessful && responseBody?.code == 200) {
                                    // 提交成功后，将记录添加到本地历史
                                    userPreferences.addRepairHistory(
                                        // 构造一个临时的 RepairHistoryItem 用于本地显示
                                        RepairHistoryItem(
                                            id = "local_${System.currentTimeMillis()}", // 临时ID
                                            problemDescription = description,
                                            repairLocation = location,
                                            campus = if (campus == 0) "大学城校区" else "白云山校区",
                                            deviceType = deviceType,
                                            deviceModel = deviceModel,
                                            deviceSystem = deviceSystem,
                                            contactType = contactType.toString(),
                                            contactInfo = contactInfo,
                                            appointmentTime = appointmentTime,
                                            submissionDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                                            remarks = remarks
                                        )
                                    )
                                    Toast.makeText(context, "提交成功", Toast.LENGTH_SHORT).show()
                                    onSubmissionSuccess()
                                } else {
                                    // 【优化】显示更详细的错误信息
                                    val errorMessage = responseBody?.msg ?: errorBody ?: "提交失败，未知错误"
                                    Toast.makeText(context, "提交失败: $errorMessage", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                // 【关键日志 3】打印网络或程序异常
                                Log.e("SubmitRepair", "Exception during submission", e)
                                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("提交申请", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun RepairHistoryScreen(
    userPreferences: UserPreferences,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var history by remember { mutableStateOf<List<RepairHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val userId = userPreferences.userId
        if (userId == -1) {
            Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val localHistory = userPreferences.getRepairHistory()
            // 按提交时间倒序排序
            history = localHistory.sortedByDescending { it.submissionDate }
        } catch (e: Exception) {
            // 如果读取失败，history 保持为空
            Log.e("RepairHistory", "Error loading local history", e)
        } finally {
            isLoading = false
        }
//        scope.launch {
//            try {
//                // 如果您在 NetworkClient 中添加了 getRepairHistory(userId)，请取消注释并使用
//                // 目前代码为了防止报错，暂时只做 Toast 提示，因为 ApiService 定义中没有该方法。
//                // 如果 ApiService 有：
//                /*
//                val response = NetworkClient.instance.getRepairHistory(userId)
//                if (response.isSuccessful) {
//                    history = response.body()?.data ?: emptyList()
//                } else {
//                    Toast.makeText(context, "加载历史记录失败", Toast.LENGTH_SHORT).show()
//                }
//                */
//                // 模拟数据 (防止报错)
//                isLoading = false
//            } catch (e: Exception) {
//                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
//            } finally {
//                isLoading = false
//            }
//        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else if (history.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "暂无报修记录",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "提交的报修将显示在这里",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { item ->
                    HistoryCard(item = item, onClick = { onNavigateToDetail(item.id) })
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: RepairHistoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "描述: ${item.problemDescription}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))

            // Task.kt 中的 RepairHistoryItem 没有 status 字段
            // 这里显示设备类型作为替代信息，或者您可以显示校区
            Text(
                "设备: ${item.deviceType} - ${item.deviceModel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))
            Text(
                // Task.kt 中使用的是 submissionDate
                "提交时间: ${item.submissionDate}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RepairDetailScreen(
    userPreferences: UserPreferences,
    taskId: String,
    modifier: Modifier = Modifier
) {
    var detail by remember { mutableStateOf<RepairHistoryItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(taskId) {
        try {
            // 【关键修改】优先从本地数据中查找对应的工单
            // 因为刚刚提交的数据保存在本地，网络接口可能还没有
            val localHistory = userPreferences.getRepairHistory()
            val localItem = localHistory.find { it.id == taskId }

            if (localItem != null) {
                detail = localItem
            } else {
                // 如果本地找不到，尝试请求网络接口 (如果已实现)
                /*
                val response = NetworkClient.instance.getRepairDetail(taskId)
                if (response.isSuccessful) {
                    detail = response.body()?.data
                }
                */
            }
        } catch (e: Exception) {
            Toast.makeText(context, "加载详情出错: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else if (detail == null) {
            Text(
                "未找到该工单详情",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            val item = detail!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        "维修工单详情",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider(Modifier.padding(vertical = 8.dp))

                    DetailRow("任务ID:", item.id)
                    DetailRow("问题描述:", item.problemDescription)
                    DetailRow("校区:", item.campus)
                    DetailRow("地址:", item.repairLocation)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("设备信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    DetailRow("类型:", item.deviceType)
                    DetailRow("型号:", item.deviceModel)
                    DetailRow("系统:", item.deviceSystem)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("联系与预约", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    DetailRow("联系方式:", "${item.contactType} - ${item.contactInfo}")
                    DetailRow("预约时间:", item.appointmentTime)

                    if (!item.remarks.isNullOrBlank()) {
                        DetailRow("备注:", item.remarks)
                    }

                    Divider(Modifier.padding(vertical = 8.dp))
                    DetailRow("提交时间:", item.submissionDate)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, highlight: Boolean = false) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
    Divider(color = Color.Black.copy(alpha = 0.1f))
}



@Composable
fun MessageCenterScreen(
    modifier: Modifier = Modifier,
    userPreferences: UserPreferences,
    refreshTrigger: Int = 0
) {
    // ================== 状态定义 ==================
    // 列表数据状态
    var allNotifications by remember { mutableStateOf<List<NotifyItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // 控制详情页显示：不为空则显示详情
    var selectedNotification by remember { mutableStateOf<NotifyItem?>(null) }

    // 分页状态
    var currentPage by remember { mutableIntStateOf(1) } // 默认第1页
    var hasMoreData by remember { mutableStateOf(false) } // 是否有下一页

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 筛选状态
    val filterOptions = listOf("全部", "系统消息", "广播消息", "用户消息")
    var selectedFilter by remember { mutableStateOf("全部") }
    var showFilterMenu by remember { mutableStateOf(false) }

    // ================== 逻辑计算 ==================

    // 本地计算筛选结果 (基于当前页数据)
    val filteredNotifications = remember(selectedFilter, allNotifications) {
        when (selectedFilter) {
            "系统消息" -> allNotifications.filter { it.type == "SYSTEM" }
            "广播消息" -> allNotifications.filter { it.type == "BROADCAST" }
            "用户消息" -> allNotifications.filter { it.type == "USER" }
            else -> allNotifications
        }
    }

    // ================== 核心函数：标记已读 ==================
    fun markAsRead(item: NotifyItem) {
        // 如果已经是已读(1)，不需要操作
        if (item.isRead == 1) return

        // 1. 乐观更新：立即在本地 UI 把它变成已读，让红点立即消失，体验更好
        allNotifications = allNotifications.map {
            if (it.notifyId == item.notifyId) it.copy(isRead = 1) else it
        }

        // 2. 发起网络请求
        scope.launch {
            try {
                val token = userPreferences.token ?: ""
                if (token.isNotEmpty()) {
                    val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                    val response = NetworkClient.instance.markNotificationRead(authHeader, item.notifyId)

                    // 如果服务器返回失败 (非200)，则回滚本地状态
                    if (!response.isSuccessful || response.body()?.code != 200) {
                        Log.e("MarkRead", "Failed to mark as read: ${response.code()}")
                        // 回滚：改回未读
                        allNotifications = allNotifications.map {
                            if (it.notifyId == item.notifyId) it.copy(isRead = 0) else it
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MarkRead", "Network error", e)
                // 网络异常回滚：改回未读
                allNotifications = allNotifications.map {
                    if (it.notifyId == item.notifyId) it.copy(isRead = 0) else it
                }
            }
        }
    }

    // ================== 核心逻辑：数据加载 ==================
    // 监听 refreshTrigger (外部触发) 和 currentPage (翻页触发)
    LaunchedEffect(refreshTrigger, currentPage) {
        if (!userPreferences.isLoggedIn) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        try {
            // 调用接口 (假设全局 pageSize = 10 或 20)
            val response = NetworkClient.instance.getNotifications(currentPage, pageSize)

            if (response.isSuccessful && response.body()?.code == 200) {
                // 【关键修正】先获取 Page 对象，再获取 List
                val pageData = response.body()?.data

                // 1. 提取列表
                val newItems = pageData?.list ?: emptyList()
                // 这里是翻页覆盖模式（点击下一页，列表刷新为新页数据），如果是无限滚动则需要 list + newItems
                allNotifications = newItems.sortedByDescending { it.createTime }

                // 2. 处理分页信息
                if (pageData != null) {
                    // 优先使用 hasNextPage，如果没有则使用 total pages 计算
                    hasMoreData = pageData.hasNextPage
                    // 备用逻辑: hasMoreData = currentPage < pageData.pages
                } else {
                    hasMoreData = false
                }
            } else {
                Log.e("MessageCenter", "API Error: ${response.code()}")
                // 如果是第一页请求失败，清空列表
                if (currentPage == 1) allNotifications = emptyList()
            }
        } catch (e: Exception) {
            Log.e("MessageCenter", "Exception", e)
            Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // ================== UI 渲染 ==================
    Box(modifier = modifier.fillMaxSize()) {
        if (selectedNotification != null) {
            // === 显示详情页 ===
            NotificationDetailScreen(
                notification = selectedNotification!!,
                onBack = { selectedNotification = null }
            )
        } else {
            // === 显示列表页 ===
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. 顶部筛选器下拉菜单
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showFilterMenu = !showFilterMenu },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(selectedFilter)
                        Icon(
                            imageVector = if (showFilterMenu) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "筛选菜单"
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        filterOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedFilter = option
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }

                // 2. 消息列表区域 (使用 weight 占据中间剩余空间)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (filteredNotifications.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("暂无消息", color = Color.Gray)
                                // 如果在非第一页没数据，提供返回第一页的按钮
                                if (currentPage > 1) {
                                    TextButton(onClick = { currentPage = 1 }) {
                                        Text("返回第一页")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredNotifications) { notification ->
                                NotificationCard(
                                    item = notification,
                                    onClick = {
                                        // 【关键】点击卡片时，先记录选中项（进入详情），同时触发标记已读
                                        selectedNotification = notification
                                        markAsRead(notification)
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. 底部翻页控制栏
                Surface(
                    shadowElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 上一页按钮
                        Button(
                            onClick = {
                                if (currentPage > 1) currentPage--
                            },
                            enabled = currentPage > 1,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("上一页")
                        }

                        // 页码显示
                        Text(
                            text = "第 $currentPage 页",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )

                        // 下一页按钮
                        Button(
                            onClick = { currentPage++ },
                            enabled = hasMoreData, // 由接口返回的 hasNextPage 控制
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("下一页")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TaskListScreen(
    userPreferences: UserPreferences
) {
    // 状态管理
    var tasks by remember { mutableStateOf<List<RepairTaskItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // 控制详情页显示：如果部位空，则显示详情页
    var selectedTask by remember { mutableStateOf<RepairTaskItem?>(null) }

    val context = LocalContext.current
    val token = remember { userPreferences.token }

    // 获取数据
    LaunchedEffect(Unit) {
        if (!token.isNullOrEmpty()) {
            isLoading = true
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                // 1. 打印 Token (确保 Token 存在)
                android.util.Log.d("API_DEBUG", "正在请求任务列表... Token长度: ${token.length}")

                val response = NetworkClient.instance.getAllTasks(authHeader)

                // 2. 打印 HTTP 状态码
                android.util.Log.d("API_DEBUG", "HTTP状态码: ${response.code()}")
                android.util.Log.d("API_DEBUG", "请求信息: ${response.raw().request.url}") // 关键：查看最终请求的完整 URL

                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data
                    android.util.Log.d("API_DEBUG", "获取成功，列表数量: ${data?.list?.size}")
                    tasks = data?.list ?: emptyList()
                } else {
                    // 3. 如果是 404，打印错误体看看服务器有没有返回具体信息
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("API_DEBUG", "请求失败! Code: ${response.code()}")
                    android.util.Log.e("API_DEBUG", "错误内容: $errorBody")

                    Toast.makeText(context, "获取失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("API_DEBUG", "发生异常: ${e.message}")
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        } else {
            android.util.Log.w("API_DEBUG", "Token为空，未发起请求")
        }
    }
    // 页面内容切换：列表 OR 详情
    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedTask != null) {
            // === 显示详情页 ===
            TaskDetailScreen(
                task = selectedTask!!,
                onBack = { selectedTask = null } // 返回列表
            )
        } else {
            // === 显示列表页 ===
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无维修任务", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tasks) { task ->
                        RepairTaskCard(task = task, onClick = { selectedTask = task })
                    }
                }
            }
        }
    }
}

@Composable
fun RepairTaskCard(task: RepairTaskItem, onClick: () -> Unit) {
    // 1. 处理状态显示逻辑
    val statusText = when (task.status) {
        0 -> "待审核"
        1 -> "审核通过"
        2 -> "已被接收"
        3 -> "已完成"
        4 -> "已取消"
        5 -> "自行解决"
        6 -> "已被拒绝"
        7 -> "已评价"
        else -> "状态${task.status}"
    }

    // 2. 处理状态颜色
    val statusColor = when(task.status) {
        0 -> Color(0xFFFFA000) // 橙色
        1, 2 -> Color(0xFF1976D2) // 蓝色
        3, 7 -> Color(0xFF43A047) // 绿色
        6 -> Color(0xFFD32F2F) // 红色
        else -> Color.Gray
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 头部：编号 + 状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 【修复变量名】 title -> requestId
                Text(
                    text = "#${task.requestId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                // 状态标签
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

            // 中间信息
            // 【修复变量名】 publisher -> realName/username
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)) {
                Text(text = "申请人: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(text = task.realName ?: task.username, style = MaterialTheme.typography.bodySmall)
            }

            // 【修复变量名】 date -> createTime
            // 在 RepairTaskCard 函数中
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)) {
                Text(text = "时间: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(text = task.createTime?.replace("T", " ")?.substringBefore("+") ?.substringBefore(".")?: "未知",
                    style = MaterialTheme.typography.bodySmall)
            }


            // 【修复变量名】 location -> repairLocation
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)) {
                Text(text = "地点: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(text = task.repairLocation ?: "未指定", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 【修复变量名】 description -> problemDescription
            Text(
                text = task.problemDescription ?: "无描述",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.DarkGray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(task: RepairTaskItem, onBack: () -> Unit) {
    BackHandler { onBack() }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFF5F5F5))) {
        androidx.compose.material3.TopAppBar(
            title = { Text("任务详情 #${task.requestId}") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )
        Column(modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("故障描述", fontWeight = FontWeight.Bold)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(task.problemDescription ?: "无")
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("详细信息", fontWeight = FontWeight.Bold)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("设备: ${task.deviceType} ${task.deviceModel}")
                    Text("系统: ${task.deviceSystem}")
                    Text("联系人: ${task.realName}")
                    Text("联系方式: ${task.contactInfo}")
                }
            }
        }
    }
}

// --- 详情页辅助组件 ---
@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
            content()
        }
    }
}

@Composable
fun DetailItem(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value ?: "-", color = Color.Black, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(2f), textAlign = TextAlign.End)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable
fun NotificationCard(item: NotifyItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：头像和未读标记
            Box(modifier = Modifier.size(48.dp)) {
                AvatarImage(
                    path = item.senderAvatar,
                    modifier = Modifier.fillMaxSize()
                )
                // 如果未读，显示一个小红点
                if (item.isRead == 0) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.Red, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 中间：标题和发送者
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = if (item.isRead == 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "来自: ${item.senderUsername ?: "系统"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧：时间
            Text(
                text = item.createTime.substringBefore("T"), // 只显示日期部分
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notification: NotifyItem,
    onBack: () -> Unit
) {
    // 拦截返回键
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消息详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 头部信息卡片
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 类型标签
                    Surface(
                        color = when (notification.type) {
                            "SYSTEM" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            "BROADCAST" -> Color(0xFFFFA000).copy(alpha = 0.1f)
                            else -> Color.Gray.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (notification.type) {
                                "SYSTEM" -> "系统通知"
                                "BROADCAST" -> "全员广播"
                                "USER" -> "用户私信"
                                else -> "通知"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when (notification.type) {
                                "SYSTEM" -> MaterialTheme.colorScheme.primary
                                "BROADCAST" -> Color(0xFFFFA000)
                                else -> Color.Gray
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = notification.createTime.replace("T", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = notification.senderUsername ?: "系统管理员",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 内容卡片
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = notification.content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        color = Color(0xFF333333)
                    )

                    // 如果有链接可以显示链接部分（此处略）
                }
            }
        }
    }
}


@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == Role.USER
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primary else Color.LightGray
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else Color.Black
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .defaultMinSize(minWidth = 60.dp)
                .padding(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            PaddingValues(12.dp).also { padding ->
                Text(
                    text = message.content,
                    modifier = Modifier.padding(padding),
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}



// ===================== 5. Preview =====================
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    YIXIU_1Theme {
        SafeAppContent()
    }
}

