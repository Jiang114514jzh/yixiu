package com.example.yixiu_1

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.ui.graphics.TransformOrigin
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
import com.example.yixiu_1.ui.KnowledgeScreen


import android.content.Context
import androidx.compose.foundation.BorderStroke

// ==================== 必须添加的引用 ====================
import com.example.yixiu_1.network.NetworkClient
import com.example.yixiu_1.network.RepairTaskItem
// =======================================================
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException


import com.example.yixiu_1.ui.ProfileScreen
import com.example.yixiu_1.ui.theme.YIXIU_1Theme
import com.example.yixiu_1.ui.CommunityScreen
import com.example.yixiu_1.ui.CreatePostScreen
import com.example.yixiu_1.ui.MyCollectionScreen
import com.example.yixiu_1.ui.MyTasksScreen

import com.example.yixiu_1.network.CommunityPostItem


import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.yixiu_1.network.*
import com.example.yixiu_1.ui.EditProfileScreen
import com.example.yixiu_1.ui.MemberManagementScreen
import com.example.yixiu_1.ui.PostDetailScreen
import com.example.yixiu_1.utils.ChineseSegmenter
import com.example.yixiu_1.utils.TfIdfMatcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlin.text.toIntOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.BlurredEdgeTreatment
import com.example.yixiu_1.ui.OtherUserProfileScreen
import SkeletonHistoryCard
import androidx.compose.material.icons.outlined.StarBorder


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
    object KnowledgeManagement : Screen()
    data class PostDetail(val postId: Int) : Screen()
    data class OtherUserProfile(val userId: Int) : Screen()
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
    val userPreferences = UserPreferences(LocalContext.current)
    //控制模糊效果
    val blurRadius = remember { Animatable(0f) }

    var isApiLoading by remember { mutableStateOf(false) }
    var conversationId by remember { mutableIntStateOf(19) } // 初始 ID
    var isPendingNewSession by remember { mutableStateOf(false) }
    // --- 状态定义 ---
    var showHistoryByClock by remember { mutableStateOf(false) } // 控制历史小窗显示
    var sessionList by remember { mutableStateOf(listOf<ChatSessionItem>()) } // 存储从后端获取的会话列表

    // 【新增】：控制侧边悬浮按钮展开/收起的状态（默认隐藏）
    var isFabExpanded by remember { mutableStateOf(false) }

    // 正确写法
    var localKnowledgeList by remember { mutableStateOf<List<KnowledgeItem>>(emptyList()) }

    // 2. 监听页面状态的变化 (如果用 NavHost，传入 navController.currentDestination?.route)
    LaunchedEffect(currentScreen::class) {
        // 当 currentScreen 发生改变时，触发以下动画：
        // 瞬间将模糊度拉高到 16f
        blurRadius.animateTo(
            targetValue = 12f,
            animationSpec = tween(
                durationMillis = 150, // 用 150 毫秒快速但平滑地变模糊
                easing = FastOutLinearInEasing // 建议用这种加速曲线，配合页面退出的感觉
            )
        )
        // 然后在 350 毫秒内，平滑过渡回 0f (清晰)
        blurRadius.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 400,
                easing = LinearOutSlowInEasing // 使用缓出动画，让清晰的过程更自然
            )
        )
    }


    // --- A. 新建聊天按钮 ---
    SmallFloatingActionButton(
        onClick = {
            // 1. 检查当前对话是否为空（防止重复创建空会话）
            if (chatMessages.isEmpty()) {
                Toast.makeText(context, "当前已经是新对话", Toast.LENGTH_SHORT).show()
                return@SmallFloatingActionButton
            }

            // 2. 准备新建逻辑：清空 UI，标记待定状态
            chatMessages = emptyList()
            isPendingNewSession = true
            Toast.makeText(context, "已准备好，发送消息即刻开启新会话", Toast.LENGTH_SHORT).show()
        }
    ) { Icon(Icons.Default.Add, contentDescription = "New Chat") }

    // 1. 修改 loadHistorySessions，增加一个加载完成的回调 (onLoaded)
    fun loadHistorySessions(userToken: String, apiService: DoubaoApiService, onLoaded: (Int) -> Unit = {}) {
        scope.launch {
            try {
                val response = apiService.getChatSessions(userToken, pageNum = 1, pageSize = 20)
                if (response.isSuccessful) {
                    val list = response.body()?.data?.list ?: emptyList()
                    sessionList = list
                    // 拿到真实数据后，找出最大的 ID 并传给回调
                    val maxId = list.maxByOrNull { it.conversationId }?.conversationId ?: 0
                    onLoaded(maxId)
                } else {
                    onLoaded(0) // 如果失败了，默认从 0 开始
                }
            } catch (e: Exception) {
                Log.e("ChatDebug", "获取历史失败: ${e.message}")
                onLoaded(0)
            }
        }
    }

    fun loadChatHistory(id: Int) {
        val token = userPreferences.token ?: ""
        scope.launch {
            try {
                val response = DoubaoApiClient.instance.getChatHistory(token, id, 1, 50)
                if (response.isSuccessful) {
                    // 将后端返回的历史消息转换为 UI 显示的列表
                    chatMessages = response.body()?.data?.list?.map {
                        ChatMessage(content = it.content, role = if (it.role == "user") Role.USER else Role.ASSISTANT)
                    }?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("ChatDebug", "切换对话失败: ${e.message}")
            }
        }
    }

    // 2. 提前声明 startNewChat 函数 (挪到这里，解决编译顺序问题)
    fun startNewChat(
        currentId: Int,
        token: String,
        apiService: DoubaoApiService,
        onFound: (Int) -> Unit
    ) {
        scope.launch {
            var nextId = currentId
            var found = false
            while (!found) {
                nextId++
                try {
                    val response = apiService.getChatHistory(token, nextId, 1, 1)
                    if (response.isSuccessful) {
                        val list = response.body()?.data?.list
                        if (list.isNullOrEmpty()) found = true
                    } else {
                        found = true
                    }
                } catch (e: Exception) {
                    found = true
                }
                if (nextId > currentId + 100) break
            }
            if (found) onFound(nextId)
        }
    }

    // 3. 修改 LaunchedEffect，使用回调串联这两个操作
    LaunchedEffect(Unit) {
        val savedToken = userPreferences.token ?: ""
        if (savedToken.isNotBlank()) {
            NetworkClient.setToken(savedToken)

            // 先加载历史列表，加载完成后，拿着真实的 maxId 去寻找新对话
            loadHistorySessions(savedToken, DoubaoApiClient.instance) { realMaxId ->
                startNewChat(
                    currentId = realMaxId,
                    token = savedToken,
                    apiService = DoubaoApiClient.instance
                ) { newId ->
                    conversationId = newId
                    chatMessages = emptyList()
                    isPendingNewSession = true
                    Log.d("ChatDebug", "已自动进入新会话 ID: $newId")
                }
            }
        }
    }




    LaunchedEffect(Unit) {
        try {
            // 注意：这里需要根据你的 ApiService 实际路径来调用
            val response = NetworkClient.instance.getKnowledgeList("${userPreferences.token}")
            if (response.isSuccessful) {
                localKnowledgeList = response.body()?.data ?: emptyList()
                Log.d("ChatDebug", "知识库获取成功: ${localKnowledgeList.size} 条")
            } else {
                Log.e("ChatDebug", "接口报错: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("ChatDebug", "网络请求异常: ${e.message}")
        }
    }


    fun sendMessage() {
        val trimmedInput = inputText.trim()
        if (trimmedInput.isEmpty()) return

        val userToken = userPreferences.token ?: ""
        Log.d("ChatDebug", "--- Token 检查 ---")
        Log.d("ChatDebug", "原始 Token 长度: ${userToken.length}")
        Log.d("ChatDebug", "原始 Token 内容: |$userToken|") // 用竖线包围看是否有空格
        val apiService = DoubaoApiClient.instance
        val doubaoHelper = DoubaoApiHelper()

        // 1. UI 立即显示用户消息
        chatMessages = chatMessages + ChatMessage(content = trimmedInput, role = Role.USER)
        inputText = ""
        isApiLoading = true

        // 统一在主作用域启动，确保顺序执行
        scope.launch {
            try {
                var finalAiReply = ""
                var isFromLocalKB = false

                // --- 步骤 1: 本地匹配 ---
                if (localKnowledgeList.isNotEmpty()) {
                    val queryTokens = ChineseSegmenter.tokenize(trimmedInput)
                    val queryVector = TfIdfMatcher.getTermFrequency(queryTokens)
                    var maxScore = 0.0
                    var bestMatch: KnowledgeItem? = null

                    localKnowledgeList.forEach { item ->
                        val itemTokens = ChineseSegmenter.tokenize(item.problem)
                        val itemVector = TfIdfMatcher.getTermFrequency(itemTokens)
                        val score = TfIdfMatcher.calculateCosineSimilarity(queryVector, itemVector)
                        if (score > maxScore) {
                            maxScore = score
                            bestMatch = item
                        }
                    }

                    if (maxScore > 0.4 && bestMatch != null) {
                        finalAiReply = bestMatch!!.solution
                        isFromLocalKB = true
                        Log.d("ChatDebug", "命中本地库: ${bestMatch!!.problem}")
                    }
                }

                // --- 步骤 2: 请求 AI (本地未命中时) ---
                if (!isFromLocalKB) {
                    val result = withContext(Dispatchers.IO) {
                        doubaoHelper.sendTextMessageWithContext(conversationId, trimmedInput, userToken, apiService)
                    }
                    finalAiReply = if (result.isSuccess) {
                        result.getOrNull()?.choices?.firstOrNull()?.message?.content?.toString() ?: ""
                    } else {
                        "网络异常，请稍后再试"
                    }
                }

                if (finalAiReply.isEmpty()) finalAiReply = "暂无回复"

                // --- 步骤 3: 更新 UI ---
                chatMessages = chatMessages + ChatMessage(content = finalAiReply, role = Role.ASSISTANT)

                // --- 步骤 4: 【核心修复】同步保存到后端 ---
                // 使用 withContext 确保在后台执行，但逻辑上挂起等待完成
                // 在 sendMessage 的 scope.launch 内部，AI 回复之后：
                withContext(Dispatchers.IO) {
                    val authHeader = userToken

                    // 如果是新会话，我们需要在后端“激活”这个 Session
                    if (isPendingNewSession) {
                        val safeInput = if (trimmedInput.length > 8) trimmedInput.take(8) + "..." else trimmedInput

                        // 注意：这里调用 addChatSession 应该确保后端能接受你预设的 conversationId
                        // 或者，如果 addChatSession 会返回一个新的 ID，我们要以返回的为准
                        val sessionRes = apiService.addChatSession(authHeader, safeInput)

                        if (sessionRes.isSuccessful && sessionRes.body()?.data != null) {
                            // 重要：同步后端生成的真实 ID
                            conversationId = sessionRes.body()!!.data!!.conversationId
                            isPendingNewSession = false
                        }
                    }

                    // 只要有 ID，就保存。这样即便用户重进，第一段对话也会因为 ID 已经存入后端而出现在历史里。
                    if (conversationId > 0) {
                        apiService.saveChatMessage(authHeader, mapOf(
                            "conversationId" to conversationId,
                            "role" to "user",
                            "question" to trimmedInput
                        ))
                        apiService.saveChatMessage(authHeader, mapOf(
                            "conversationId" to conversationId,
                            "role" to "assistant",
                            "question" to finalAiReply
                        ))

                        // 保存完立即刷新列表，让历史记录显示出来
                        loadHistorySessions(authHeader, apiService)
                    }
                }

            } catch (e: Exception) {
                Log.e("ChatDebug", "sendMessage 异常: ${e.message}")
            } finally {
                isApiLoading = false
            }
        }
    }

    // 查找下一个可用的新对话 ID


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
        val backgroundBlur by animateDpAsState(
            targetValue = if (drawerState.isOpen) 16.dp else 3.dp,
            animationSpec = tween(300), label = "blur"
        )
        val imageModifier = if (backgroundBlur > 0.dp) {
            Modifier
                .fillMaxSize()
                .blur(radiusX = backgroundBlur, radiusY = backgroundBlur)
        } else {
            Modifier.fillMaxSize()
        }

        val backgroundImage = if (isAdmin) {
            painterResource(id = R.drawable.admin_background)
        } else {
            painterResource(id = R.drawable.bg_campus_repair)
        }
        Image(
            painter = backgroundImage,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 在 AppContentInternal 函数内的 ModalNavigationDrawer 中
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color.Transparent,
            drawerContent = {
                ModalDrawerSheet(Modifier.fillMaxWidth(0.618f)) {
                    Spacer(Modifier.height(64.dp))

                    // 1. 通用导航项目 (所有人可见)
                    NavigationDrawerItem(
                        label = { Text("主页") },
                        selected = currentScreen == Screen.Home,
                        onClick = {
                            scope.launch { drawerState.close() }; currentScreen = Screen.Home
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("消息中心") },
                        selected = currentScreen == Screen.MessageCenter,
                        onClick = {
                            scope.launch { drawerState.close() }; currentScreen =
                            Screen.MessageCenter
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("义修社区") },
                        selected = currentScreen is Screen.Community,
                        onClick = {
                            scope.launch { drawerState.close() }; currentScreen = Screen.Community()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 2. 义修服务 (仅学生/普通用户可见)
                    // 逻辑：如果不是志愿者且不是管理员，则显示
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

                    // 3. 任务中心 (志愿者 OR 管理员可见)
                    // 注意：我们将原有的志愿者折叠菜单和管理员的单项合并逻辑
                    if (isVolunteer || isAdmin) {
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

                    // 4. 管理专用项 (仅管理员可见)
                    if (isAdmin) {
//                        NavigationDrawerItem(
//                            label = { Text("用户管理") },
//                            selected = currentScreen == Screen.UserManagement,
//                            onClick = {
//                                scope.launch { drawerState.close() }; currentScreen =
//                                Screen.UserManagement
//                            }
//                        )
                        NavigationDrawerItem(
                            label = { Text("成员管理") },
                            selected = currentScreen == Screen.MemberManagement,
                            onClick = {
                                currentScreen = Screen.MemberManagement
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("知识库管理") },
                            selected = currentScreen == Screen.KnowledgeManagement,
                            onClick = {
                                currentScreen = Screen.KnowledgeManagement
                                scope.launch { drawerState.close() }
                            },
                            // 你可以根据喜好添加一个图标，比如 Book 或者 Settings，这里暂不加图标以和上方“用户管理”保持一致
                        )

                    }
                }
            }
        )
        {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = blurRadius.value.dp) // 应用切换屏幕时的动态模糊
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "PageTransition"
            ) { screen ->
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
                            )
                            {
                                // 【重点：找回定义】在这里重新定义 cardModifier，确保下方的 Card 可以引用到
                                val cardBlurRadius by animateDpAsState(
                                    targetValue = if (drawerState.isOpen) 10.dp else 0.dp,
                                    animationSpec = tween(300), label = "cardBlur"
                                )
                                val cardModifier = if (cardBlurRadius > 0.dp) {
                                    Modifier.blur(cardBlurRadius)
                                } else {
                                    Modifier
                                }

                                // 1. 底层：聊天消息列表与输入框
                                Column(modifier = Modifier.fillMaxSize()) {

                                    // A. 消息列表部分
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(), // weight(1f) 会把剩余空间全占满，把底部的输入框挤到最下面
                                        reverseLayout = true
                                    ) {
                                        items(chatMessages.reversed()) { message ->
                                            // 【修改】：用 Row 包裹气泡，增加头像显示逻辑
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                // 用户消息靠右，AI消息靠左
                                                horizontalArrangement = if (message.role == Role.USER) Arrangement.End else Arrangement.Start,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                // 如果是 AI 消息，在左侧显示 AI 头像
                                                if (message.role == Role.ASSISTANT) {
                                                    Surface(
                                                        modifier = Modifier.size(36.dp),
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primaryContainer
                                                    ) {
                                                        Icon(
                                                            Icons.Default.SmartToy,
                                                            contentDescription = "AI",
                                                            modifier = Modifier.padding(6.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }

                                                // 聊天气泡，为了防止过长的文本把头像挤没，使用 weight 限制它的最大宽度
                                                Box(modifier = Modifier.weight(1f, fill = false)) {
                                                    ChatBubble(message = message)
                                                }

                                                // 如果是用户消息，在右侧显示用户头像
                                                if (message.role == Role.USER) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    AvatarImage(
                                                        path = avatarPath,
                                                        modifier = Modifier.size(36.dp)
                                                            .clip(CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    } // <--- LazyColumn 到这里结束

                                    // 👇 【关键修复】：你的输入框代码必须粘贴在这里！👇
                                    // 它必须在 Column 的大括号内部，且在 LazyColumn 的下面
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        OutlinedTextField(
                                            value = if (isApiLoading) "系统生成文本中" else inputText,
                                            onValueChange = {
                                                if (!isApiLoading) inputText = it
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp), // 为右侧发送按钮留一点间距
                                            placeholder = { Text("向义修助手提问...") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(28.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White,
                                                disabledContainerColor = Color.White,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                                unfocusedTextColor = if (isApiLoading) Color.Gray else Color.Black,
                                                focusedTextColor = if (isApiLoading) Color.Gray else Color.Black
                                            ),
                                            // ... (键盘设置等)
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        if (inputText.trim().isNotBlank() && !isApiLoading) {
                                                            sendMessage()
                                                        }
                                                    },
                                                    enabled = inputText.trim().isNotBlank() && !isApiLoading
                                                ) {
                                                    Icon(Icons.Default.Send, contentDescription = "发送")
                                                }
                                            }
                                        )
                                    }
                                    // 👆 输入框代码结束 👆

                                } // <--- Column 到这里结束

                                // ====================================================
                                // 2. 中层：欢迎卡片 (靠上居中)
                                // ====================================================
                                // 【新增】：使用 AnimatedVisibility 和 chatMessages.isEmpty() 控制显示与隐藏
                                AnimatedVisibility(
                                    visible = chatMessages.isEmpty(),
                                    enter = fadeIn(tween(300)),
                                    exit = fadeOut(tween(300)),
                                    modifier = Modifier.align(Alignment.TopCenter)
                                ) {
                                    Card(
                                        // 注意：把原本在 Card 上的 .align(Alignment.TopCenter) 移到了上面的 AnimatedVisibility 里
                                        modifier = cardModifier
                                            .padding(top = 16.dp, start = 32.dp, end = 32.dp)
                                            .graphicsLayer(alpha = 0.7f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color.White.copy(
                                                alpha = 0.8f
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = CardDefaults.cardElevation(0.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(
                                                vertical = 24.dp,
                                                horizontal = 32.dp
                                            ), contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = welcomeText,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // 3. 悬浮按钮组：采用抽屉式折叠设计，不阻挡主视图
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd) // 依旧放在右下角
                                        .padding(end = 16.dp, bottom = 80.dp), // 向上偏移避开输入框
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // 半透明提示箭头
                                    IconButton(
                                        onClick = { isFabExpanded = !isFabExpanded },
                                        modifier = Modifier
                                            .padding(end = if (isFabExpanded) 8.dp else 0.dp)
                                            .size(36.dp)
                                            .background(Color.Gray.copy(alpha = 0.4f), CircleShape)
                                    ) {
                                        Icon(
                                            // 展开时箭头向右，收起时箭头向左
                                            imageVector = if (isFabExpanded) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                                            contentDescription = "切换操作面板",
                                            tint = Color.White
                                        )
                                    }

                                    // 使用横向展开/收缩动画包裹原有的两个功能按钮
                                    AnimatedVisibility(
                                        visible = isFabExpanded,
                                        enter = androidx.compose.animation.expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                                        exit = androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            // 时钟历史按钮
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    val token = userPreferences.token ?: ""
                                                    loadHistorySessions(token, DoubaoApiClient.instance)
                                                    showHistoryByClock = true
                                                    isFabExpanded = false // 点击后自动收起侧边栏
                                                },
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DateRange,
                                                    contentDescription = "历史"
                                                )
                                            }

                                            // 新建聊天按钮
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    val userToken = userPreferences.token ?: ""
                                                    startNewChat(
                                                        conversationId,
                                                        userToken,
                                                        DoubaoApiClient.instance
                                                    ) { newId ->
                                                        conversationId = newId
                                                        chatMessages = emptyList()
                                                        isPendingNewSession = true
                                                        isFabExpanded = false // 点击后自动收起
                                                        Toast.makeText(
                                                            context,
                                                            "已开启新对话",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "新建")
                                            }
                                        }
                                    }
                                }

                                // 4. 最顶层：全屏历史记录弹出小窗 (仅在显示时拦截点击)
                                AnimatedVisibility(
                                    visible = showHistoryByClock,
                                    enter = fadeIn() + scaleIn(
                                        initialScale = 0f,
                                        transformOrigin = TransformOrigin(1f, 1f)
                                    ) + slideInVertically { it / 2 },
                                    exit = fadeOut() + scaleOut(
                                        targetScale = 0f,
                                        transformOrigin = TransformOrigin(1f, 1f)
                                    ) + slideOutVertically { it / 2 }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .clickable { showHistoryByClock = false }, // 点击遮罩关闭
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .fillMaxHeight(0.6f)
                                                .clickable(enabled = false) { }, // 防止点击窗内关闭
                                            shape = RoundedCornerShape(16.dp),
                                            elevation = CardDefaults.cardElevation(8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    "历史对话",
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(
                                                        vertical = 8.dp
                                                    ), color = Color.Gray.copy(alpha = 0.3f)
                                                )

                                                LazyColumn {
                                                    items(sessionList) { session ->
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    conversationId =
                                                                        session.conversationId
                                                                    showHistoryByClock = false
                                                                    loadChatHistory(session.conversationId)
                                                                }
                                                                .padding(vertical = 12.dp)
                                                        ) {
                                                            Text(
                                                                text = session.headline,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                maxLines = 1
                                                            )
                                                            Text(
                                                                text = session.createTime.substring(
                                                                    0,
                                                                    10
                                                                ),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color.Gray
                                                            )
                                                        }
                                                        HorizontalDivider(
                                                            color = Color.Gray.copy(
                                                                alpha = 0.2f
                                                            )
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
                                    drawerState = drawerState,
                                    scope = scope,
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
                                color = Color.White.copy(alpha = 0.9f) // 保持与任务列表一致的风格
                            ) {
                                // 直接调用我们下面定义的 MyTasksScreen
                                MyTasksScreen(userPreferences = userPreferences)
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

                    is Screen.MemberManagement -> {
                        Scaffold(
                            topBar = {
                                // 建议复用你的 StandardTopAppBar 或者写一个简单的带返回键的 TopBar
                                TopAppBar(
                                    title = { Text("成员管理") },
                                    navigationIcon = {
                                        IconButton(onClick = { currentScreen = Screen.Home }) {
                                            Icon(
                                                Icons.Default.ArrowBack,
                                                contentDescription = "返回"
                                            )
                                        }
                                    }
                                )
                            }
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding)) {
                                // 调用上面在 MemberScreen.kt 中定义的页面
                                MemberManagementScreen(userPreferences = userPreferences)
                            }
                        }
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
                                    onNavigateToDetail = { taskId ->
                                        currentScreen = Screen.RepairDetail(taskId)
                                    },
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
                                        IconButton(onClick = {
                                            currentScreen = Screen.RepairHistory
                                        }) {
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
                                modifier = Modifier.padding(innerPadding),
                                onBack = { currentScreen = Screen.RepairHistory }
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
                            onNavigateToMyCollection = { currentScreen = Screen.MyCollection },
                            // 【新增】处理点击头像跳转
                            onUserClick = { userId ->
                                currentScreen = Screen.OtherUserProfile(userId)
                            },
                            onPostClick = { postId ->
                                currentScreen = Screen.PostDetail(postId)
                            }
                        )
                    }

                    is Screen.OtherUserProfile -> {
                        // 先获取当前的 screen 实例，方便后续引用
                        val profileScreen = screen as Screen.OtherUserProfile

                        OtherUserProfileScreen(
                            targetUserId = profileScreen.userId, // 使用解析出来的 userId
                            userPreferences = userPreferences,
                            onBack = {
                                // 【修复逻辑错误】：返回通常是回到社区主页
                                // 如果你的 Screen.Community 需要参数，请传入对应的参数（如 null）
                                currentScreen = Screen.Community(null)
                            }
                        )
                    }

                    // 【新增】我的收藏页面分支
                    is Screen.MyCollection -> {
                        MyCollectionScreen(
                            onBack = { currentScreen = Screen.Community() },
                            onPostClick = { postId ->
                                currentScreen = Screen.PostDetail(postId)
                            }// 返回社区
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
                            postId = (screen as Screen.PostDetail).postId,
                            // 【修复报错 1550】确保返回时状态赋值正确
                            onBack = {
                                // 如果 Community 不需要预加载数据，传 null
                                currentScreen = Screen.Community(null)
                            },
                            // 【修复报错 1552】现在参数已在第一步中定义，不会再报错了
                            onUserClick = { userId: Int -> // 显式声明类型以解决类型推导问题
                                currentScreen = Screen.OtherUserProfile(userId)
                            }
                        )
                    }

                    is Screen.KnowledgeManagement -> {
                        // 获取用户 token。请根据你实际的 DataStore 或 UserPreferences 结构来获取 token。
                        // 假设 userPreferences 中有 token 字段，如果没有请替换为你实际获取 token 的代码
                        val token = userPreferences.token ?: ""

                        // 调用我们刚刚写好的知识库管理页面
                        // 注意：因为 KnowledgeScreen 内部已经自带了 Scaffold 和 TopAppBar，所以这里不需要再包一层 Scaffold
                        KnowledgeScreen (
                            token = token,
                            onBack = {
                                currentScreen = Screen.Home // 点击返回按钮回到主页，你也可以改成 Screen.Profile
                            }
                        )
                    }

                    else -> {
                        // 如果进入了未知的 Screen，暂时显示一个空 Box 或者占位符
                        Box(modifier = Modifier.fillMaxSize())
                    }
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
                // 假设这里是你的 getUserInfo 网络请求成功的回调内部
                if (response.isSuccessful && response.body()?.code == 200) {
                    val userInfo = response.body()?.data

                    if (userInfo != null) {
                        // 第一步：检查 Gson 解析网络响应是否成功
                        Log.e("Debug_1", "网络返回的 volunteerInfo 对象: ${userInfo.volunteerInfo}")
                        Log.e("Debug_1", "网络返回的 volunteerId: ${userInfo.volunteerInfo?.volunteerId}")

                        // 第二步：执行保存
                        userPreferences.volunteerInfo = userInfo.volunteerInfo
                        // 如果你用的是 saveLoginInfo，就在这里调用：
                        // userPreferences.saveLoginInfo(..., volunteerDetail = userInfo.volunteerInfo)

                        // 第三步：直接暴力读取 SharedPreferences 的底层 XML，看有没有存进去
                        val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        val rawJson = sharedPrefs.getString("volunteer_info", "没存进去！")
                        Log.e("Debug_2", "底层SharedPreferences存的JSON是: $rawJson")

                        // 第四步：检查 UserPreferences 的反序列化读取
                        Log.e("Debug_3", "UserPreferences读出的 volunteerId: ${userPreferences.volunteerId}")
                    }
                }
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
                                            // 只要请求成功且拿到了用户信息，就一次性完整保存
                                            if (userInfo != null) {
                                                // ✅ 【核心修复】：直接调用我们在 UserPreferences 里写好的 saveLoginInfo 方法！
                                                userPreferences.saveLoginInfo(
                                                    token = token,
                                                    userId = userInfo.userId,
                                                    username = userInfo.username ?: "未知用户",
                                                    userEmail = userInfo.email ?: "",
                                                    avatarPath = userInfo.avatar,
                                                    role = userInfo.role,
                                                    volunteerInfo = userInfo.volunteerInfo // 把包含 volunteerId 的核心对象传进去！
                                                )
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
    // 【修改 1】：数据类型改为后端的 RepairTaskItem
    var history by remember { mutableStateOf<List<RepairTaskItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val token = userPreferences.token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" } ?: ""
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshTrigger += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 【修改 2】：从访问本地偏好改为请求网络接口
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        if (token.isBlank()) {
            Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
            isLoading = false
            return@LaunchedEffect
        }

        try {
            // 核心魔法：让协程先睡 300 毫秒，等页面切换的转场动画彻底播完
            kotlinx.coroutines.delay(300)

            val response = NetworkClient.instance.getMyRepairHistory(token, 1, 50)
            if (response.isSuccessful && response.body()?.code == 200) {
                history = response.body()?.data?.list ?: emptyList()
            } else {
                Toast.makeText(context, "加载失败: ${response.body()?.msg}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: CancellationException) {
            // 【核心修复】：如果是 Compose 正常的页面切换导致的协程取消，直接抛出，不要当成报错！
            throw e
        } catch (e: Exception) {
            // 只有真正的网络断开、解析失败等，才会走到这里
            Log.e("RepairHistory", "请求历史记录异常", e)
            Toast.makeText(context, "网络异常", Toast.LENGTH_SHORT).show()
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
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    // 【优化 2】：使用骨架屏占位，不打断用户的页面结构感知
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 渲染 4 个骨架屏卡片作为假数据展示
                        items(4) {
                            SkeletonHistoryCard()
                        }
                    }
                } else if (history.isEmpty()) {
                    // ... 暂无报修记录的代码保持不变 ...
                } else {
                    // ... 渲染真实数据的 LazyColumn 保持不变 ...
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(history, key = { it.requestId }) { item ->
                            RepairTaskHistoryCard(item = item, onClick = { onNavigateToDetail(item.requestId.toString()) })
                        }
                    }
                }
            }
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
                items(history, key = { it.requestId }) { item ->
                    // 传入 item.requestId.toString() 以匹配外部 onNavigateToDetail(String) 的签名
                    RepairTaskHistoryCard(item = item, onClick = { onNavigateToDetail(item.requestId.toString()) })
                }
            }
        }
    }
}

// 【新增】：专为网络数据结构设计的历史卡片
@Composable
fun RepairTaskHistoryCard(
    item: RepairTaskItem,
    onClick: () -> Unit
) {
    // 状态文本映射
    val statusText = when(item.status) {
        0 -> "待审核"
        1 -> "审核通过"
        2 -> "已被接收"
        3 -> "已完成"
        4 -> "已取消"
        5 -> "自行解决"
        6 -> "已被拒绝"
        7 -> "已评价"
        else -> "未知状态"
    }

    // 状态颜色映射 (你提供的规则)
    val statusColor = when(item.status) {
        0 -> Color(0xFFFFA000) // 橙色
        1, 2 -> Color(0xFF1976D2) // 蓝色
        3, 7 -> Color(0xFF43A047) // 绿色
        6 -> Color(0xFFD32F2F) // 红色
        else -> Color.Gray
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- 顶部：设备标题和状态标签 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val deviceName = if (item.deviceSystem.isNullOrBlank() && item.deviceModel.isNullOrBlank()) {
                    "未知设备"
                } else {
                    "${item.deviceSystem ?: ""} ${item.deviceModel ?: ""}".trim()
                }

                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 状态标签
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 问题描述 ---
            Text(
                text = item.problemDescription ?: "无详细描述",
                color = Color.DarkGray,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // --- 动态展示分配信息 (如果有) ---
            if (!item.repairAssignment.isNullOrEmpty()) {
                val volunteers = item.repairAssignment.mapNotNull { it.volunteerName }.joinToString("、")
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("服务志愿者: $volunteers", fontSize = 13.sp, color = Color(0xFF555555))
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // --- 动态展示维修日志信息 (如果有) ---
            if (!item.repairLog.isNullOrEmpty()) {
                // 取最新的一条日志展示
                val latestLog = item.repairLog.last()
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "最新进展: ${latestLog.logContent ?: "无"}",
                        fontSize = 13.sp,
                        color = Color(0xFF555555),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // --- 底部：时间和单号 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "单号: ${item.requestId}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = item.createTime?.replace("T", " ")?.substringBefore(".") ?: "未知时间",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
@Composable
fun RepairDetailScreen(
    userPreferences: UserPreferences,
    taskId: String,
    onBack: () -> Unit, // 【修改 1】：新增 onBack 参数，用于评价成功后退出页面
    modifier: Modifier = Modifier
) {
    var detail by remember { mutableStateOf<RepairTaskItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 提取 token 到外层，方便网络请求和评价时使用
    val token = userPreferences.token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" } ?: ""

    // 【新增】：评价弹窗相关的状态变量
    var showEvalDialog by remember { mutableStateOf(false) }
    var evalContent by remember { mutableStateOf("") }
    var evalScore by remember { mutableIntStateOf(5) } // 默认 5 星
    var isSubmittingEval by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (token.isBlank()) {
            Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val response = NetworkClient.instance.getMyRepairHistory(token, 1, 100)
            if (response.isSuccessful && response.body()?.code == 200) {
                val list = response.body()?.data?.list ?: emptyList()
                detail = list.find { it.requestId.toString() == taskId }
            } else {
                Toast.makeText(context, "获取详情失败: ${response.body()?.msg}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "网络异常，加载详情出错", Toast.LENGTH_SHORT).show()
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

            val statusText = when(item.status) {
                0 -> "待审核"
                1 -> "审核通过"
                2 -> "已被接收"
                3 -> "已完成"
                4 -> "已取消"
                5 -> "自行解决"
                6 -> "已被拒绝"
                7 -> "已评价"
                else -> "未知状态"
            }
            val statusColor = when(item.status) {
                0 -> Color(0xFFFFA000)
                1, 2 -> Color(0xFF1976D2)
                3, 7 -> Color(0xFF43A047)
                6 -> Color(0xFFD32F2F)
                else -> Color.Gray
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    // --- 头部标题与状态 ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "维修工单详情",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    // --- 基本与设备信息 ---
                    DetailRow("任务单号:", item.requestId.toString())
                    DetailRow("问题描述:", item.problemDescription)
                    DetailRow("校区:", if (item.campus == "0") "南校区" else if (item.campus == "1") "新校区" else item.campus)
                    DetailRow("详细地址:", item.repairLocation)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("设备信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("设备类型:", item.deviceType)
                    DetailRow("设备品牌/型号:", item.deviceModel)
                    DetailRow("操作系统:", item.deviceSystem)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("联系与预约", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("联系方式:", "${item.contactType ?: "未知"} - ${item.contactInfo ?: "无"}")
                    DetailRow("预约时间:", item.appointmentTime)

                    if (!item.remarks.isNullOrBlank()) {
                        DetailRow("报修备注:", item.remarks)
                    }

                    // --- 分配信息 (志愿者接单情况) ---
                    if (!item.repairAssignment.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("服务志愿者信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        item.repairAssignment.forEach { assign ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    DetailRow("志愿者:", "${assign.volunteerName ?: "未知"} ${if (assign.isLeader == 1) "(负责人)" else ""}")
                                    DetailRow("联系方式:", assign.contactNumber)
                                    DetailRow("接单时间:", assign.assignedTime?.replace("T", " ")?.substringBefore(".") ?: "未知")
                                    if (!assign.remarks.isNullOrBlank()) {
                                        DetailRow("备注:", assign.remarks)
                                    }
                                }
                            }
                        }
                    }

                    // --- 维修日志 (维修进度情况) ---
                    if (!item.repairLog.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("维修进度记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        item.repairLog.forEach { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    DetailRow("记录人:", log.volunteerName ?: "未知")
                                    DetailRow("故障原因:", log.logContent)
                                    DetailRow("解决方案:", log.repairDuration)
                                    if (!log.solutionSummary.isNullOrBlank()) {
                                        DetailRow("备注:", log.solutionSummary)
                                    }
                                    DetailRow("记录时间:", log.uploadTime?.replace("T", " ")?.substringBefore(".") ?: "未知")
                                }
                            }
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    DetailRow("提交时间:", item.createTime?.replace("T", " ")?.substringBefore("."))
                    if (item.status == 3 && item.completeTime != null) {
                        DetailRow("完成时间:", item.completeTime.replace("T", " ").substringBefore("."))
                    }

                    // 👇 【修改 2】：如果任务已完成，在卡片最底部显示评价按钮
                    if (item.status == 3) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showEvalDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("评价本次服务", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 👇 【修改 3】：评价弹窗 UI（置于 Box 最外层，覆盖在内容上方）
        if (showEvalDialog && detail != null) {
            AlertDialog(
                onDismissRequest = { if (!isSubmittingEval) showEvalDialog = false },
                title = { Text("服务评价", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("请为本次义修服务打分：")
                        Spacer(modifier = Modifier.height(8.dp))
                        // 简易星级评分条
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            for (i in 1..5) {
                                IconButton(onClick = { evalScore = i }) {
                                    Icon(
                                        imageVector = if (i <= evalScore) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Star $i",
                                        tint = if (i <= evalScore) Color(0xFFFFB300) else Color.Gray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = evalContent,
                            onValueChange = { evalContent = it },
                            label = { Text("评价内容 (选填)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSubmittingEval = true
                            scope.launch {
                                try {
                                    val request = AddEvaluationRequest(detail!!.requestId, evalContent.ifBlank { "很好" }, evalScore)
                                    val response = NetworkClient.instance.addEvaluation(token, request)
                                    if (response.isSuccessful && response.body()?.code == 200) {
                                        Toast.makeText(context, "评价成功！", Toast.LENGTH_SHORT).show()
                                        showEvalDialog = false
                                        onBack() // 自动退回历史页面（历史页面中的 ON_RESUME 将自动触发刷新）
                                    } else {
                                        Toast.makeText(context, "评价失败: ${response.body()?.msg}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "网络异常，提交失败", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSubmittingEval = false
                                }
                            }
                        },
                        enabled = !isSubmittingEval
                    ) {
                        if (isSubmittingEval) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        else Text("提交评价")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEvalDialog = false },
                        enabled = !isSubmittingEval
                    ) { Text("取消", color = Color.Gray) }
                }
            )
        }
    }
}


@Composable
fun DetailRow(label: String, value: String?, highlight: Boolean = false) { // 【关键修改 1】：把 value: String 改为 value: String?
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value ?: "无", // 【关键修改 2】：使用 ?: 处理空值，如果后端返回 null，则显示 "无"
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
        if (item.isRead == 1 || item.isRead == null) return

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
                        Log.e("MarkRead", "Failed to mark as read: ${response.body()?.data}")
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

                // 1. 提取列表并进行数据清洗
                val rawItems = pageData?.list ?: emptyList()

                // 【关键修改】：遍历拿到的数据，一旦发现 isRead 是 null，直接在本地把它变成 1（已读）
                val newItems = rawItems.map { item ->
                    if (item.isRead == null) {
                        item.copy(isRead = 1)
                    } else {
                        item
                    }
                }
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
    // === 1. 原有状态管理 ===
    var tasks by remember { mutableStateOf<List<RepairTaskItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTask by remember { mutableStateOf<RepairTaskItem?>(null) }

    // === 2. 【新增】分页状态管理 ===
    var currentPage by remember { mutableIntStateOf(1) } // 当前页，默认第一页
    var totalPages by remember { mutableIntStateOf(1) }  // 总页数
    val pageSize = 10 // 每页请求的数量

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = remember { userPreferences.token }

    // === 3. 提取获取数据逻辑，并增加分页参数 ===
    suspend fun loadTasks(page: Int) {
        if (token.isNullOrEmpty()) return
        isLoading = true
        try {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

            // 传入当前页码和每页数量
            val response = NetworkClient.instance.getAllTasks(authHeader, page, pageSize)

            if (response.isSuccessful && response.body()?.code == 200) {
                val data = response.body()?.data
                tasks = data?.list ?: emptyList()
                // 读取后端返回的总页数，如果后端没返回 pages，也可以用 (total + pageSize - 1) / pageSize 计算
                totalPages = data?.pages ?: 1
            } else {
                Toast.makeText(context, "获取失败: ${response.code()}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // === 4. 【关键】监听 currentPage 变化，页码改变时自动触发请求 ===
    LaunchedEffect(currentPage) {
        loadTasks(currentPage)
    }

    // === 5. 页面内容布局 ===
    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedTask != null) {
            // === 显示详情页 ===
            TaskDetailScreen(
                task = selectedTask!!,
                userPreferences = userPreferences,
                onBack = { selectedTask = null },
                onRefresh = {
                    // 详情页操作成功后，刷新当前页的数据
                    scope.launch { loadTasks(currentPage) }
                }
            )
        } else {
            // === 显示列表页 + 底部翻页栏 ===
            Column(modifier = Modifier.fillMaxSize()) {
                // 上半部分：列表区域 (使用 weight 占据所有剩余空间)
                Box(modifier = Modifier.weight(1f)) {
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

                // === 【新增】下半部分：底部翻页控制栏 ===
                // 只要不是在加载中，且数据不为空，或者总页数大于1时就显示翻页栏
                if (!isLoading && (tasks.isNotEmpty() || totalPages > 1)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 上一页按钮
                            OutlinedButton(
                                onClick = { if (currentPage > 1) currentPage-- },
                                enabled = currentPage > 1 // 第一页时禁用
                            ) {
                                Text("上一页")
                            }

                            // 中间页码指示器
                            Text(
                                text = "第 $currentPage 页 / 共 $totalPages 页",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )

                            // 下一页按钮
                            OutlinedButton(
                                onClick = { if (currentPage < totalPages) currentPage++ },
                                enabled = currentPage < totalPages // 最后一页时禁用
                            ) {
                                Text("下一页")
                            }
                        }
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
fun TaskDetailScreen(
    task: RepairTaskItem,
    userPreferences: UserPreferences,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val token = userPreferences.token ?: ""

    val isAdmin = userPreferences.userRole.equals("admin", ignoreCase = true) ||
            userPreferences.userRole.equals("super_admin", ignoreCase = true)

    val isVolunteer = userPreferences.userRole == "volunteer"
    val isVolunteerOrAdmin = isAdmin || isVolunteer

    // 获取当前用户的志愿者 ID
    val currentVolunteerId = userPreferences.volunteerInfo?.volunteerId

    val isPending = task.status == 0 // 待审核状态
    val isApproved = task.status == 1 // 审核通过状态（可接取）

    // 判断当前任务是否由当前志愿者接取 (从 repairAssignment 列表中查找)
    val isMyAcceptedTask = task.status == 2 && currentVolunteerId != null &&
            task.repairAssignment?.any { it.volunteerId == currentVolunteerId } == true

    // 获取接单的志愿者信息 (取列表第一个作为主要负责人展示)
    val assignedVolunteer = task.repairAssignment?.firstOrNull()

    // 【新增】：获取最新的维修结单记录
    val completionLog = task.repairLog?.lastOrNull()
    val isLeaderOfTask = task.repairAssignment?.any {
        it.volunteerId == currentVolunteerId && it.isLeader == 1
    } == true

    // 找出所有状态为 5 (申请中) 的成员
    val confirmedMembers = task.repairAssignment?.filter { it.status != 5 } ?: emptyList()
    val pendingApplicants = task.repairAssignment?.filter { it.status == 5 } ?: emptyList()

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

    val statusColor = when(task.status) {
        0 -> Color(0xFFFFA000)
        1, 2 -> Color(0xFF1976D2)
        3, 7 -> Color(0xFF43A047)
        6 -> Color(0xFFD32F2F)
        else -> Color.Gray
    }

    // 评价弹窗状态
    var showEvalDialog by remember { mutableStateOf(false) }
    var evalContent by remember { mutableStateOf("") }
    var evalScore by remember { mutableIntStateOf(5) } // 默认 5 星
    var isSubmittingEval by remember { mutableStateOf(false) }



    BackHandler { onBack() }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        bottomBar = {
            // 【核心修复】：在最外层加一个 Column 占位！
            // 这能强制 Scaffold 始终正确计算和分配底部栏的高度，解决按钮渲染不出来的系统 Bug。
            Column(modifier = Modifier.fillMaxWidth()) {

                if (isAdmin && isPending) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { handleTaskAudit(task.requestId, 6, token, scope, context, onBack, onRefresh) },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                            ) { Text("拒绝申请") }

                            Button(
                                onClick = { handleTaskAudit(task.requestId, 1, token, scope, context, onBack, onRefresh) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                            ) { Text("通过审核") }
                        }
                    }
                }

                if (isVolunteerOrAdmin && isApproved) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    handleAcceptTask(task.requestId, currentVolunteerId ?: -1, token, scope, context, onBack, onRefresh)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                            ) { Text("接取任务", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    }
                }

                if (isVolunteerOrAdmin && task.status == 2 && currentVolunteerId != null) {
                    val isConfirmedMember = task.repairAssignment?.any { it.volunteerId == currentVolunteerId && it.status != 5 } == true
                    val isPendingApplicant = task.repairAssignment?.any { it.volunteerId == currentVolunteerId && it.status == 5 } == true
                    var isApplyingLocally by remember { mutableStateOf(false) }
                    val showAsApplying = isPendingApplicant || isApplyingLocally

                    if (!isConfirmedMember) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 8.dp,
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        if (!showAsApplying) {
                                            isApplyingLocally = true
                                            handleJoinTeam(task.requestId, currentVolunteerId, token, scope, context, onRefresh)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !showAsApplying,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showAsApplying) Color(0xFFFBC02D) else Color(0xFF009688),
                                        disabledContainerColor = Color(0xFFFBC02D).copy(alpha = 0.9f),
                                        disabledContentColor = Color.White
                                    )
                                ) {
                                    if (showAsApplying) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("正在等待负责人查看申请", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.GroupAdd, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("加入义修小队", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 👇 【评价按钮】：彻底独立！只要状态是 3 就一定会渲染 👇
                if (task.status == 3 && false) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { showEvalDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)) // 橙色按钮
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("评价本次服务", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(

            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "返回任务列表 (任务 #${task.requestId})",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            // 1. 状态指示器展示
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "当前状态：$statusText",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // 2. 故障描述卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("故障描述", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFF5F5F5))
                    Text(
                        text = task.problemDescription ?: "无详细描述",
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            //待处理入队申请

            // 3. 详细信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("详细信息", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFF5F5F5))

                    DetailItem("设备型号", "${task.deviceType} ${task.deviceModel}")
                    DetailItem("操作系统", task.deviceSystem ?: "未知")
                    DetailItem("报修地点", task.repairLocation ?: "未填写")
                    DetailItem("申请人", task.realName ?: task.username)
                    DetailItem("联系方式", task.contactInfo ?: "未提供")
                    DetailItem("申请时间", task.createTime?.replace("T", " ")?.substringBefore(".") ?: "未知")
                }
            }



            // 4. 接单员信息卡片 (任务已被接收且有分配记录时显示)
            // 4. 接单信息卡片 (展示小队所有成员)
            if (!task.repairAssignment.isNullOrEmpty() && task.status >= 2) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("接单小队信息", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFF5F5F5))

                        // 1. 渲染正式成员 (status != 5)
                        val confirmedMembers = task.repairAssignment.filter { it.status != 5 }
                        confirmedMembers.forEachIndexed { index, assignment ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarImage(path = assignment.avatar, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = assignment.volunteerName ?: "未知成员", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(Modifier.width(8.dp))
                                        val isLeader = assignment.isLeader == 1
                                        Surface(
                                            color = if (isLeader) Color(0xFFFF9800).copy(alpha = 0.1f) else Color(0xFF2196F3).copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (isLeader) "负责人" else "队员",
                                                color = if (isLeader) Color(0xFFFF9800) else Color(0xFF2196F3),
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(text = "${assignment.majorClass ?: ""} ${assignment.grade ?: ""}", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            // 只有当后面还有正式成员，或者你是负责人且后面有申请人时，才画线
                            val pendingApplicants = task.repairAssignment.filter { it.status == 5 }
                            if (index < confirmedMembers.size - 1 || (isLeaderOfTask && pendingApplicants.isNotEmpty())) {
                                HorizontalDivider(color = Color(0xFFFAFAFA), thickness = 0.5.dp)
                            }
                        }

                        // 2. 渲染待审核申请 (嵌入在同一个卡片内，仅负责人可见)
                        val pendingApplicants = task.repairAssignment.filter { it.status == 5 }
                        if (isLeaderOfTask && pendingApplicants.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            pendingApplicants.forEach { applicant ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFF9C4).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarImage(path = applicant.avatar, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(applicant.volunteerName ?: "申请人", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("申请加入小队...", fontSize = 11.sp, color = Color(0xFFFBC02D))
                                    }

                                    // 同意
                                    IconButton(onClick = {
                                        handleJoinRequestAudit(applicant.assignId, true, token, scope, context, onRefresh, onBack)
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF43A047))
                                    }
                                    // 拒绝
                                    IconButton(onClick = {
                                        handleJoinRequestAudit(applicant.assignId, false, token, scope, context, onRefresh, onBack)
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Cancel, null, tint = Color(0xFFD32F2F))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. 【新增】维修结单报告展示区 (仅在任务已完成 status >= 3 且存在结单记录时显示)
            if (task.status >= 3 && completionLog != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("维修结单报告", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                        // 展示填写的表单内容
                        DetailItem("故障原因", completionLog.logContent ?: "未填写")
                        DetailItem("维修时长", completionLog.repairDuration ?: "未填写")
                        DetailItem("维修备注", completionLog.solutionSummary ?: "无")

                        // 展示完成时间 (优先使用 task 上的时间)
                        val finishTime = task.completeTime ?: completionLog.uploadTime
                        DetailItem("完成时间", finishTime?.replace("T", " ")?.substringBefore(".") ?: "未知")

                        Spacer(Modifier.height(16.dp))

                        // “导入知识库” 预留按钮，仅对管理员和志愿者开放
                        // “导入知识库” 按钮，仅对管理员和志愿者开放
                        if (isVolunteerOrAdmin) {
                            var isImporting by remember { mutableStateOf(false) }

                            OutlinedButton(
                                onClick = {
                                    // 1. 校验必填字段
                                    val logId = completionLog.logId
                                    if (logId == null) {
                                        Toast.makeText(context, "未能获取到结单日志ID，无法导入", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }

                                    // 优先使用表单填写的故障原因，若没有则用用户申请时的故障描述
                                    val problemText = completionLog.logContent?.takeIf { it.isNotBlank() }
                                        ?: task.problemDescription
                                        ?: "未知故障"

                                    val solutionText = completionLog.repairDuration?.takeIf { it.isNotBlank() }
                                    if (solutionText == null) {
                                        Toast.makeText(context, "解决方案为空，无法导入知识库", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }

                                    // 2. 发起网络请求
                                    isImporting = true
                                    scope.launch {
                                        try {
                                            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                                            // 按照要求严格构建参数：sourceType = 3，sourceId = "log_X"
                                            val request = AddKnowledgeRequest(
                                                problem = problemText,
                                                solution = solutionText,
                                                sourceType = 3,
                                                sourceId = "log_$logId"
                                            )

                                            // 调用接口 (请确保 NetworkClient.instance 中已经包含了你定义的 addKnowledge 方法)
                                            val response = NetworkClient.instance.addKnowledge(authHeader, request)

                                            if (response.isSuccessful && response.body()?.code == 200) {
                                                Toast.makeText(context, "成功导入知识库！这是第${response.body()?.data}条知识", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val msg = response.body()?.msg ?: "未知错误"
                                                Toast.makeText(context, "导入失败: $msg", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "网络异常，导入失败", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isImporting = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isImporting, // 导入中禁用按钮
                                border = BorderStroke(1.dp, Color(0xFF2196F3)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF2196F3),
                                    disabledContentColor = Color.Gray
                                )
                            ) {
                                if (isImporting) {
                                    // 显示加载动画
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color(0xFF2196F3),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("正在导入...", fontWeight = FontWeight.Medium)
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Import", modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("将此方案导入知识库", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // 6. 展示结单填写表单 (仅在“维修中”且“属于当前志愿者的任务”时显示)
            if (task.status == 2 && isLeaderOfTask && currentVolunteerId != null) {
                Spacer(Modifier.height(16.dp))
                TaskCompletionForm(
                    requestId = task.requestId,
                    volunteerId = currentVolunteerId,
                    userToken = token,
                    onCompleteSuccess = {
                        onRefresh()
                        onBack()
                    }
                )
            }

            // 底部留白，防止被导航栏遮挡
            Spacer(Modifier.height(32.dp))
        }
        if (showEvalDialog) {
            AlertDialog(
                onDismissRequest = { if (!isSubmittingEval) showEvalDialog = false },
                title = { Text("服务评价", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("请为本次义修服务打分：")
                        Spacer(modifier = Modifier.height(8.dp))
                        // 简易星级评分条
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            for (i in 1..5) {
                                IconButton(onClick = { evalScore = i }) {
                                    Icon(
                                        imageVector = if (i <= evalScore) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Star $i",
                                        tint = if (i <= evalScore) Color(0xFFFFB300) else Color.Gray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = evalContent,
                            onValueChange = { evalContent = it },
                            label = { Text("评价内容 (选填)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSubmittingEval = true
                            scope.launch {
                                try {
                                    val request = AddEvaluationRequest(task.requestId, evalContent.ifBlank { "很好" }, evalScore)
                                    val response = NetworkClient.instance.addEvaluation(token, request)
                                    if (response.isSuccessful && response.body()?.code == 200) {
                                        Toast.makeText(context, "评价成功！", Toast.LENGTH_SHORT).show()
                                        showEvalDialog = false
                                        onRefresh() // 刷新详情页或列表
                                        onBack()    // 自动退回历史页面
                                    } else {
                                        Toast.makeText(context, "评价失败: ${response.body()?.msg}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "提交评价网络异常", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSubmittingEval = false
                                }
                            }
                        },
                        enabled = !isSubmittingEval
                    ) {
                        if (isSubmittingEval) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        else Text("提交评价")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEvalDialog = false },
                        enabled = !isSubmittingEval
                    ) { Text("取消", color = Color.Gray) }
                }
            )
        }
    }
}
@Composable
fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/**
 * 审核接口调用逻辑
 */
private fun handleTaskAudit(
    requestId: Int,
    newStatus: Int,
    token: String,
    scope: CoroutineScope,
    context: Context,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
    scope.launch {
        try {
            val request = TaskStatusUpdateRequest(
                requestId = requestId,
                status = newStatus
            )
            // 请确保 NetworkClient 对应的是你新增的接口
            val response = NetworkClient.instance.updateTaskStatus(authHeader, request)
            if (response.isSuccessful && response.body()?.code == 200) {
                Toast.makeText(context, "已成功将任务设置为${if(newStatus==1)"通过" else "拒绝"}", Toast.LENGTH_SHORT).show()
                onRefresh() // 刷新列表
                onBack()    // 返回列表页
            } else {
                Log.e("AuditError","${response.body()?.msg}")
                Toast.makeText(context, "操作失败: ${response.body()?.msg}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("AuditError", "审核异常: ${e.message}")
            Toast.makeText(context, "网络异常，请稍后重试", Toast.LENGTH_SHORT).show()
        }
    }
}
//处理接受任务
private fun handleAcceptTask(
    requestId: Int,
    volunteerId: Int,
    token: String,
    scope: CoroutineScope,
    context: Context,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {


    val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

    scope.launch {
        try {
            val request = AddAssignRequest(
                requestId = requestId,
                volunteerId = volunteerId, // 将当前的 userId 作为 volunteerId 提交
                remarks = "无"
            )

            Log.e("UserId","当前的用户id是：${request.volunteerId}")

            if (volunteerId == -1) {
                Toast.makeText(context, "用户状态异常，请重新登录", Toast.LENGTH_SHORT).show()
            }

            val response = NetworkClient.instance.addAssign(authHeader, request)

            if (response.isSuccessful && response.body()?.code == 200) {
                Log.e("TaskAccept","${response.body()?.msg}")
                Toast.makeText(context, "接取成功", Toast.LENGTH_SHORT).show()
                onRefresh() // 刷新列表
                onBack()    // 自动跳回列表页面
            } else {
                Toast.makeText(context, "接取失败: ${response.body()?.msg}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("AcceptTaskError", "接取异常: ${e.message}")
            Toast.makeText(context, "网络异常，请稍后重试", Toast.LENGTH_SHORT).show()
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

@Composable
fun NotificationDetailScreen(
    notification: NotifyItem,
    onBack: () -> Unit
) {
    // 拦截系统返回键（侧滑/实体键）
    BackHandler { onBack() }

    // 【核心修改】：直接移除 Scaffold，使用 Column 作为根布局
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // 把原本 Scaffold 的背景色移到这里
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp) // 与下方卡片保持间距
                .clickable { onBack() }, // 点击触发状态改变，回到列表
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "返回消息列表",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }
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

//用于控制任务完成
@Composable
fun TaskCompletionForm(
    requestId: Int,
    volunteerId: Int,
    userToken: String,
    onCompleteSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- 定义用户需要填写的表单状态 (请根据实际情况增删) ---
    var faultReason by remember { mutableStateOf("") }
    var solution by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }

    // 【核心逻辑】只有所有必填项都不为空白时，才允许点击完成按钮
    val isFormValid = faultReason.isNotBlank() && solution.isNotBlank() && remarks.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "填写维修结单报告",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 1. 故障原因输入框
            OutlinedTextField(
                value = faultReason,
                onValueChange = { faultReason = it },
                label = { Text("维修日志") },
                placeholder = { Text("请输入本次任务内容") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 2. 解决方案输入框
            OutlinedTextField(
                value = solution,
                onValueChange = { solution = it },
                label = { Text("维修时长") },
                placeholder = { Text("请描述您的维修进行的时长") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                singleLine = false
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 3. 备注/建议输入框
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("维修方案") },
                placeholder = { Text("例如：按正常顺序贴完手机膜") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 4. 提交按钮
            Button(
                onClick = {
                    isSubmitting = true
                    scope.launch {
                        try {
                            val authHeader = if (userToken.startsWith("Bearer ")) userToken else "Bearer $userToken"
                            // 构造请求，requestId 和 volunteerId 由外部传入，对用户透明
                            val request = CompleteTaskRequest(
                                requestId = requestId,
                                volunteerId = volunteerId,
                                logContent = faultReason,
                                repairDuration = solution,
                                solutionSummary = remarks
                            )

                            // 发起网络请求 (请确保 NetworkClient.instance 中包含了 ApiService 实例)
                            val response = NetworkClient.instance.completeRepairTask(authHeader, request)

                            if (response.isSuccessful && response.body()?.code == 200) {
                                Toast.makeText(context, "结单成功！", Toast.LENGTH_SHORT).show()
                                onCompleteSuccess() // 触发成功回调（例如：刷新页面或返回上一页）
                            } else {
                                val errorMsg = response.body()?.msg ?: "未知错误"
                                val errorCode = response.body()?.code
                                Log.e("TaskDebug","出现错误：${errorMsg},${errorCode}")
                                Toast.makeText(context, "结单失败: $errorMsg", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "网络异常，请重试", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = isFormValid && !isSubmitting, // 表单不完整或正在提交时禁用按钮
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("完成任务", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 申请加入义修小队逻辑
 */
private fun handleJoinTeam(
    requestId: Int,
    volunteerId: Int,
    token: String,
    scope: CoroutineScope,
    context: Context,
    onRefresh: () -> Unit
) {
    val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
    scope.launch {
        try {
            val request = AddJoinRequest(volunteerId = volunteerId, requestId = requestId)
            // 调用接口
            NetworkClient.instance.addJoinRequest(authHeader, request)
            // 由于该接口定义的返回值为 Unit，我们直接提示成功并刷新
            Toast.makeText(context, "申请加入成功！", Toast.LENGTH_SHORT).show()
            onRefresh()
        } catch (e: Exception) {
            Log.e("JoinTeamError", "异常: ${e.message}")
            Toast.makeText(context, "操作失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }
}

// MainActivity 类级别的处理加入小队请求方法
private fun handleJoinRequestAudit(
    assignId: Int,
    isApprove: Boolean,
    token: String,
    scope: CoroutineScope,
    context: Context,
    onRefresh: () -> Unit,
    onBack: () -> Unit // 【新增参数】用于控制返回
) {
    val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
    scope.launch {
        try {
            val response = if (isApprove) {
                NetworkClient.instance.approveJoinRequest(authHeader, ApproveJoinRequest(assignId))
            } else {
                NetworkClient.instance.rejectJoinRequest(authHeader, RejectJoinRequest(assignId, "名额已满"))
            }

            if (response.isSuccessful && response.body()?.code == 200) {
                Toast.makeText(context, if (isApprove) "已通过申请" else "已拒绝申请", Toast.LENGTH_SHORT).show()

                // 【核心修改】先触发刷新，再执行返回
                onRefresh()
                onBack()
            } else {
                Toast.makeText(context, "操作失败: ${response.body()?.msg}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "网络异常", Toast.LENGTH_SHORT).show()
        }
    }
}