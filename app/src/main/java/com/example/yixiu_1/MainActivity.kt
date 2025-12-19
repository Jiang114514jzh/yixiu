package com.example.yixiu_1

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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

// ===================== 1. MainActivity =====================
class MainActivity : ComponentActivity() {

    init {
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("MainActivity", "未捕获异常: ${thread.name}", exception)
            exception.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
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
    val file = remember(path) { path?.let { File(it).takeIf { f -> f.exists() } } }
    if (file != null) {
        AsyncImage(
            model = file,
            contentDescription = "Avatar",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
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
                                    boundsTransform = { _, _ ->
                                        tween(durationMillis = 500)
                                    }
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
                                    drawerState = drawerState,
                                    scope = scope,
                                    isLoggedIn = isLoggedIn,
                                    avatarPath = avatarPath,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedContentScope = animatedContentScope,
                                    onAvatarClick = { currentScreen = Screen.Profile }
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            Box(
                                Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("欢迎来到义修校园", style = MaterialTheme.typography.headlineMedium, color = Color.White)
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
                                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White)
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
                                onSubmissionSuccess = { currentScreen = Screen.Home }
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
                            RepairHistoryScreen(
                                userPreferences = userPreferences,
                                onNavigateToDetail = { taskId -> currentScreen = Screen.RepairDetail(taskId) },
                                modifier = Modifier.padding(innerPadding)
                            )
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var authCodeSent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 【关键修改 1】定义身份选项 (显示名称 to 后端值)
    val roleOptions = listOf("学生" to "student", "志愿者" to "volunteer", "管理员" to "super_admin")
    // 【关键修改 2】创建 role 状态，默认值为 "student"
    var selectedRole by remember { mutableStateOf("student") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoginMode) {
            Text("登录", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        } else {
            Text("注册", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }

        Spacer(Modifier.height(32.dp))

        // 【关键修改 3】添加身份选择的UI组件
        Text("选择您的身份", style = MaterialTheme.typography.titleSmall, color = Color.White)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            roleOptions.forEach { (displayName, roleValue) ->
                Row(
                    modifier = Modifier
                        .clickable { selectedRole = roleValue }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedRole == roleValue),
                        onClick = { selectedRole = roleValue },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color.White,
                            unselectedColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(text = displayName, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("邮箱") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                disabledContainerColor = Color.Gray,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.White.copy(alpha = 0.7f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                cursorColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                disabledContainerColor = Color.Gray,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.White.copy(alpha = 0.7f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                cursorColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(Modifier.height(16.dp))

        if (!isLoginMode) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { verificationCode = it },
                    label = { Text("验证码") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                        disabledContainerColor = Color.Gray,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val response = NetworkClient.instance.sendEmailVerification(email)
                                if (response.isSuccessful) {
                                    authCodeSent = true
                                    Toast.makeText(context, "验证码已发送", Toast.LENGTH_SHORT).show()
                                } else {
                                    val errorBody = response.errorBody()?.string() ?: "未知错误"
                                    Toast.makeText(context, "发送失败: $errorBody", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("发送")
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        // 修复：使用正确的参数构造请求对象
                        val request = EmailRegisterOrLoginRequest(
                            email = email,
                            role = selectedRole,
                            verificationCode = if (verificationCode.isNotEmpty()) verificationCode.toInt() else 0
                        )

                        // 修复：根据模式调用相应的方法
                        val response = if (isLoginMode) {
                            NetworkClient.instance.loginByEmail(request)
                        } else {
                            NetworkClient.instance.registerByEmail(request)
                        }

                        if (response.isSuccessful) {
                            val authResponse = response.body()
                            if (authResponse != null) {
                                // 修复：手动设置用户信息
                                userPreferences.isLoggedIn = true
                                userPreferences.token = authResponse.data?.toString() ?: ""
                                userPreferences.userEmail = email
                                userPreferences.userRole = selectedRole
                                Toast.makeText(context, if (isLoginMode) "登录成功" else "注册成功", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "响应为空", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "未知错误"
                            Toast.makeText(context, "操作失败: $errorBody", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoginMode) "登录" else "注册")
        }

        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(if (isLoginMode) "没有账户？去注册" else "已有账户？去登录", color = Color.White)
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
    var campus by remember { mutableIntStateOf(1) } // 1:东校区, 2:西校区, 3:南校区

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 定义通用输入框样式
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

    // 使用 Box 作为外层容器，处理 scaffold 的 padding
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp), // 外边距
        contentAlignment = Alignment.TopCenter
    ) {
        // 添加白色半透明卡片背景，与 RepairDetailScreen 风格一致
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()), // 滚动逻辑移动到卡片上
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f) // 白色半透明背景
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // 卡片内部内边距
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "请详细填写报修单",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black // 调整文字颜色为黑色以适应白色背景
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
                    listOf("南校区" to 1, "北校区" to 2).forEach { (name, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { campus = value }
                        ) {
                            RadioButton(
                                selected = (campus == value),
                                onClick = { campus = value },
                                // 使用默认颜色或深色，确保在白色背景上可见
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
                    listOf("QQ" to 1, "微信" to 2).forEach { (name, value) ->
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
                                // 构造完整的请求对象
                                val request = RepairTaskRequest(
                                    userId = userId,
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
                                val response = NetworkClient.instance.submitRepairTask(request)
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "提交成功", Toast.LENGTH_SHORT).show()
                                    onSubmissionSuccess()
                                } else {
                                    val errorBody = response.errorBody()?.string() ?: "提交失败"
                                    Toast.makeText(context, errorBody, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
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
        scope.launch {
            try {
                // 获取本地历史记录而不是网络请求
                history = userPreferences.getRepairHistory()
            } catch (e: Exception) {
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
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
        } else if (history.isEmpty()) {
            Text("没有维修记录", color = Color.White, style = MaterialTheme.typography.titleMedium)
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
            Text(
                "联系方式: ${item.contactType}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "时间: ${item.submissionDate}",
                style = MaterialTheme.typography.bodySmall
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
        scope.launch {
            try {
                // 从本地历史记录中查找详情
                val history = userPreferences.getRepairHistory()
                detail = history.find { it.id == taskId }
            } catch (e: Exception) {
                Toast.makeText(context, "加载详情失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
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
            Text("未找到维修详情", color = Color.White, style = MaterialTheme.typography.titleMedium)
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
                    DetailRow("任务ID:", item.id)
                    DetailRow("联系方式:", item.contactType)
                    DetailRow("联系信息:", item.contactInfo)
                    DetailRow("设备类型:", item.deviceType)
                    DetailRow("系统:", item.deviceSystem)
                    DetailRow("型号:", item.deviceModel)
                    DetailRow("描述:", item.problemDescription)
                    DetailRow("校区:", item.campus)
                    DetailRow("地址:", item.repairLocation)
                    DetailRow("预约时间:", item.appointmentTime)
                    item.remarks?.let { DetailRow("备注:", it) }
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
