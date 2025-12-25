package com.example.yixiu_1

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.yixiu_1.data.UserPreferences
import com.example.yixiu_1.network.EmailRegisterOrLoginRequest
import com.example.yixiu_1.network.NetworkClient
import com.example.yixiu_1.network.RepairHistoryItem
import com.example.yixiu_1.network.RepairTaskRequest
import com.example.yixiu_1.ui.ProfileScreen
import com.example.yixiu_1.ui.theme.YIXIU_1Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import kotlin.text.toIntOrNull

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
    object Profile : Screen()
    object Login : Screen()
    object AppointmentQuestionnaire : Screen()
    object RepairHistory : Screen()
    data class RepairDetail(val taskId: String) : Screen()
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
                modifier = Modifier.matchParentSize().clip(CircleShape),
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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AppContent() {
    SharedTransitionLayout {
        val context = LocalContext.current
        val userPreferences = remember { UserPreferences(context) }

        AppContentInternal(userPreferences = userPreferences, sharedTransitionScope = this)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun AppContentInternal(
    userPreferences: UserPreferences,
    sharedTransitionScope: SharedTransitionScope
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var avatarPath by remember { mutableStateOf<String?>(null) }

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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isServiceExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        val blurRadius by animateDpAsState(
            targetValue = if (drawerState.isOpen) 16.dp else 0.dp,
            animationSpec = tween(300), label = "blur"
        )
        val imageModifier = if (blurRadius > 0.dp) {
            Modifier
                .fillMaxSize()
                .blur(radiusX = blurRadius, radiusY = blurRadius)
        } else { Modifier.fillMaxSize() }

        Image(
            painter = painterResource(id = R.drawable.bg_campus_repair),
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop
        )

        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color.Transparent,
            drawerContent = {
                ModalDrawerSheet(Modifier.fillMaxWidth(0.618f)) {
                    Spacer(Modifier.height(64.dp))
                    NavigationDrawerItem(
                        label = { Text("主页") },
                        selected = currentScreen == Screen.Home,
                        onClick = { scope.launch { drawerState.close() }; currentScreen = Screen.Home }
                    )
                    ExpandableNavigationItem(
                        label = "义修服务",
                        isExpanded = isServiceExpanded,
                        onToggle = { isServiceExpanded = !isServiceExpanded },
                        subItems = listOf(
                            "预约维修" to { scope.launch { drawerState.close() }; currentScreen = Screen.AppointmentQuestionnaire },
                            "维修历史" to { scope.launch { drawerState.close() }; currentScreen = Screen.RepairHistory }
                        )
                    )
                }
            }
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith
                            fadeOut(animationSpec = tween(500))
                },
                label = "PageTransition"
            ) { screen ->
                val animatedContentScope = this

                when (screen) {
                    is Screen.Home -> {
                        Scaffold(
                            topBar = {
                                StandardTopAppBar(
                                    title = "义修校园",
                                    drawerState = drawerState,scope = scope,
                                    isLoggedIn = isLoggedIn,
                                    avatarPath = avatarPath,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope,
                                    onAvatarClick = { currentScreen = Screen.Profile }
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            // 使用 Box 来进行绝对定位
                            Box(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize()
                            ) {
                                // 计算模糊半径，复用之前定义的 blurRadius 逻辑
                                // 注意：这里需要确保能访问到外层的 blurRadius 变量。
                                // 如果访问不到，可以直接在这里重新计算：
                                val cardBlurRadius by animateDpAsState(
                                    targetValue = if (drawerState.isOpen) 10.dp else 0.dp,
                                    animationSpec = tween(300), label = "cardBlur"
                                )

                                val cardModifier = if (cardBlurRadius > 0.dp) {
                                    // 仅在侧边栏打开时应用模糊
                                    Modifier.blur(cardBlurRadius)
                                } else {
                                    Modifier
                                }
                                val configuration = LocalConfiguration.current
                                val screenHeight = configuration.screenHeightDp
                                Card(
                                    modifier = cardModifier
                                        .align(Alignment.TopCenter) // 先顶部居中
                                        // 使用 padding 来模拟 61.8% 的位置 (黄金分割点)
                                        // fillMaxHeight(0.618f) 会让它顶部的 spacer 占位，或者直接用 BiasAlignment
                                        .padding(top = (screenHeight * 0.1).dp)
                                        .padding(horizontal = 32.dp), // 水平边距
                                    colors = CardDefaults.cardColors(
                                        // 白色半透明背景 (0.8f 不透明度)
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
                                            text = "欢迎使用义修校园",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.Black, // 黑色字体
                                            fontWeight = FontWeight.Bold // 加粗
                                        )
                                    }
                                }
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
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                    is Screen.Login -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("登录") },
                                    navigationIcon = {
                                        IconButton(onClick = { currentScreen = Screen.Home }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                        }
                                    },
                                    modifier = Modifier.background(Color.White)
                                )
                            },
                            containerColor = Color.Transparent
                        ) { p -> AuthScreen(Modifier.padding(p), userPreferences) { currentScreen = Screen.Home } }
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
                                // 【关键修改】提交成功后，直接跳转到历史记录页面
                                // 这样不仅能立即看到结果，还能触发历史页面的数据刷新
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
                            // 【关键修改】使用 key 绑定 userId
                            // 作用：当 userId 发生变化（如切换账号）时，Compose 会强制销毁并重建这个页面
                            // 确保了：
                            // 1. 账号不变时，页面状态保持稳定
                            // 2. 账号变化时，绝不会显示上一个账号的残留数据，并强制重新加载
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            },
            selected = false,
            onClick = onToggle
        )
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                subItems.forEach { (subLabel, onClick) ->
                    NavigationDrawerItem(
                        label = { Text(subLabel) },
                        selected = false,
                        onClick = onClick,
                        modifier = Modifier.padding(start = 16.dp)
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
    var isLoginMode by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val roleOptions = listOf("学生" to "student", "志愿者" to "volunteer", "管理员" to "super_admin")
    var selectedRole by remember { mutableStateOf("student") }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            kotlinx.coroutines.delay(1000L)
            countdown--
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(if (isLoginMode) "登录" else "注册", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(Modifier.height(24.dp))

                Text("选择您的身份", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    roleOptions.forEach { (displayName, roleValue) ->
                        Row(modifier = Modifier.clickable { selectedRole = roleValue }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (selectedRole == roleValue),
                                onClick = { selectedRole = roleValue },
                                colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.White.copy(alpha = 0.7f))
                            )
                            Text(text = displayName, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱", color = Color.White.copy(alpha = 0.8f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it },
                        label = { Text("验证码", color = Color.White.copy(alpha = 0.8f)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.White,
                            unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
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

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val codeInt = verificationCode.toIntOrNull() ?: 0
                                // 【修复】这里将 authCode 改为 code，匹配常见的数据类定义
                                val request = EmailRegisterOrLoginRequest(email = email, role = selectedRole, verificationCode = codeInt)
                                val resp = if (isLoginMode) NetworkClient.instance.loginByEmail(request) else NetworkClient.instance.registerByEmail(request)
                                val token = (resp.body()?.data as? String) ?: (resp.body()?.data as? Map<*, *>)?.get("token") as? String

                                if (token != null) {
                                    userPreferences.token = token
                                    NetworkClient.setToken(token)
                                    userPreferences.isLoggedIn = true

                                    // 【头像逻辑】登录后获取用户信息，并使用其中的 avatar 字段更新头像
                                    try {
                                        val uResp = NetworkClient.instance.getUserInfo()
                                        // 假设 getUserInfo 返回的是 ApiResponse<UserInfo>
                                        val userInfo = uResp.body()?.data
                                        if (userInfo != null) {
                                            // 注意：这里需要根据你实际的 UserInfo 类结构进行取值
                                            // 如果是 Any 类型需要强转，如果是泛型则直接使用
                                            // 这里假设 userInfo 对象中有 avatar 属性
                                            // 且 userId 是 String 类型 (如果不是请转 .toString())
                                            // userPreferences.userId = userInfo.userId.toString()
                                            // ...

                                            // 使用反射或者直接访问（取决于你的 UserInfo 定义）
                                            // 简单起见，这里假设它就是那个对象
                                            if (userInfo is com.example.yixiu_1.network.UserInfo) {
                                                userPreferences.userId = userInfo.userId

                                                userPreferences.userEmail = userInfo.email
                                                userPreferences.nickname = userInfo.username
                                                userPreferences.userRole = userInfo.role
                                                // 更新头像
                                                userPreferences.avatarPath = userInfo.avatar
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Fetch user info failed", e)
                                    }

                                    onLoginSuccess()
                                    Toast.makeText(context, if (isLoginMode) "登录成功" else "注册成功", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "操作失败: ${resp.body()?.msg ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "错误: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally { isLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) { Text(if (isLoginMode) "登录" else "注册") }

                TextButton(onClick = { isLoginMode = !isLoginMode }) {
                    Text(if (isLoginMode) "没有账户？去注册" else "已有账户？去登录", color = Color.White)
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
                        label = { Text("设备类型 (如电脑)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                    OutlinedTextField(
                        value = deviceSystem,
                        onValueChange = { deviceSystem = it },
                        label = { Text("系统 (如Win10)") },
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
                    label = { Text("期望预约时间") },
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
                            if (description.isBlank() || location.isBlank() || contactInfo.isBlank()) {
                                Toast.makeText(context, "请填写必填项(描述、地址、联系方式)", Toast.LENGTH_SHORT).show()
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

// ===================== 5. Preview =====================
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    YIXIU_1Theme {
        SafeAppContent()
    }
}
