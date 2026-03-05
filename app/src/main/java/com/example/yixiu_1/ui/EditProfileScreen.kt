package com.example.yixiu_1.ui
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.yixiu_1.network.ApiService
import com.example.yixiu_1.network.UserInfo // 确保这个路径指向你的 UserInfo 类
import com.example.yixiu_1.network.UserUpdateRequest // 对应第一步定义的 Data Class
import com.example.yixiu_1.network.VolunteerUpdateRequest // 对应第一步定义的 Data Class
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    apiService: ApiService
) {
    var userInfo by remember { mutableStateOf<UserInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // 表单状态
    var username by remember { mutableStateOf("") }
    var realName by remember { mutableStateOf("") }

    // 志愿者表单状态
    var studentNumber by remember { mutableStateOf("") }
    var majorClass by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // 初始化获取数据
    LaunchedEffect(Unit) {
        try {
            val response = apiService.getUserInfo()
            if (response.isSuccessful) {
                val apiResponse = response.body()
                Log.d("EditProfile", "请求成功: ${apiResponse?.code}")
                if (apiResponse != null && apiResponse.code == 200) {
                    val userData = apiResponse.data
                    if (userData != null) {
                        userInfo = userData
                        username = userData.username ?: ""
                        realName = userData.realName ?: ""

                        // 解析嵌套的志愿者信息
                        userData.volunteerInfo?.let { vInfo ->
                            studentNumber = vInfo.studentNumber ?: ""
                            majorClass = vInfo.majorClass ?: ""
                            grade = vInfo.grade ?: ""
                            contactNumber = vInfo.contactNumber ?: ""
                        }
                    }
                }
            } else {
                Log.e("EditProfile", "响应失败: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("EditProfile", "代码运行异常: ${e.message}")
            e.printStackTrace()
        } finally {
            // 【关键点】无论请求是成功、失败还是崩了，
            // 都要关闭加载状态，否则页面永远卡在进度条
            isLoading = false
            Log.d("EditProfile", "isLoading 已设为 false")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("完善个人信息") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("基本信息", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = realName, onValueChange = { realName = it }, label = { Text("真实姓名") }, modifier = Modifier.fillMaxWidth())

                // 权限逻辑：只有非 student 身份才显示志愿者信息
                if (userInfo?.role != "student") {
                    Spacer(Modifier.height(16.dp))
                    Text("志愿者信息", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                    OutlinedTextField(value = studentNumber, onValueChange = { studentNumber = it }, label = { Text("学号") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = majorClass, onValueChange = { majorClass = it }, label = { Text("专业班级") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("年级") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contactNumber, onValueChange = { contactNumber = it }, label = { Text("联系电话") }, modifier = Modifier.fillMaxWidth())
                }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                // 1. 保存基本信息
                                apiService.updateBasicInfo(UserUpdateRequest(username, realName))

                                // 2. 如果显示了志愿者部分，则保存志愿者信息
                                if (userInfo?.role != "student") {
                                    apiService.updateVolunteerInfo(VolunteerUpdateRequest(
                                        userId = userInfo!!.userId,
                                        studentNumber = studentNumber,
                                        majorClass = majorClass,
                                        grade = grade,
                                        contactType = 0, // 默认手机
                                        contactNumber = contactNumber
                                    ))
                                }
                                onBack() // 返回并刷新
                            } catch (e: Exception) {
                                // 处理错误
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                ) {
                    Text("提交修改")
                }
            }
        }
    }
}
