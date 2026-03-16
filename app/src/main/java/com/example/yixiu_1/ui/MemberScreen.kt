package com.example.yixiu_1.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import coil.compose.AsyncImage
// 关键修改：统一引用 network 包
import com.example.yixiu_1.network.* import com.example.yixiu_1.data.UserPreferences
import kotlinx.coroutines.launch

@Composable
fun MemberManagementScreen(userPreferences: UserPreferences) {
    val scope = rememberCoroutineScope()
    var volunteerList by remember { mutableStateOf<List<VolunteerUserItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var editingUser by remember { mutableStateOf<VolunteerUserItem?>(null) }
    val token = userPreferences.token ?: ""

    // 初始加载
    LaunchedEffect(Unit) {
        isLoading = true
        Log.d("MemberDebug", "开始请求志愿者列表, Token: $token")
        try {
            // 修正后的 NetworkClient 调用
            val response = NetworkClient.instance.getVolunteerList(token, 1, 50)

            if (response.isSuccessful) {
                val data = response.body()?.data
                volunteerList = data?.list ?: emptyList()
                Log.d("MemberDebug", "请求成功: 获取到 ${volunteerList.size} 条数据")

                if (volunteerList.isEmpty()) {
                    Log.w("MemberDebug", "警告: 接口返回成功但列表为空，请检查后台数据或排除逻辑")
                }
            } else {
                Log.e("MemberDebug", "请求失败: ErrorCode=${response.code()}, Msg=${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("MemberDebug", "发生异常: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (volunteerList.isEmpty()) {
            Text("暂无志愿者成员数据", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(volunteerList) { user ->
                    VolunteerCard(user = user, onEdit = { editingUser = user })
                }
            }
        }

        // 编辑弹窗逻辑保持不变...
        if (editingUser != null) {
            EditVolunteerDialog(
                user = editingUser!!,
                onDismiss = { editingUser = null },
                onConfirm = { updatedRequest ->
                    scope.launch {
                        // 【Log 节点 1】：确认即将发送的 Token 和数据实体
                        Log.d("MemberDebug", "1. 准备调用更新接口")
                        Log.d("MemberDebug", "请求 Token 长度: ${token.length}")
                        Log.d("MemberDebug", "发送的请求体 RequestBody: $updatedRequest")

                        try {
                            val res = NetworkClient.instance.updateVolunteerInfo(token, updatedRequest)

                            // 【Log 节点 2】：检查 HTTP 状态码
                            Log.d("MemberDebug", "2. 更新接口响应 HTTP Code: ${res.code()}")

                            if (res.isSuccessful) {
                                // 【Log 节点 3】：检查业务状态码 (假设你的 ApiResponse 有 code 和 msg 字段)
                                val responseBody = res.body()
                                Log.d("MemberDebug", "更新接口返回 Body: $responseBody")

                                Log.d("MemberDebug", "3. 修改请求发送成功，准备拉取最新列表...")
                                // 刷新列表
                                val refreshRes = NetworkClient.instance.getVolunteerList(token, 1, 50)
                                Log.d("MemberDebug", "刷新列表接口响应 HTTP Code: ${refreshRes.code()}")

                                if (refreshRes.isSuccessful) {
                                    volunteerList = refreshRes.body()?.data?.list ?: emptyList()
                                    Log.d("MemberDebug", "4. 列表刷新完成，最新列表条数: ${volunteerList.size}")
                                    editingUser = null
                                } else {
                                    Log.e("MemberDebug", "刷新列表失败: ${refreshRes.errorBody()?.string()}")
                                }
                            } else {
                                // 【Log 节点 4】：打印详细的后端错误提示
                                val errorBodyString = res.errorBody()?.string()
                                Log.e("MemberDebug", "更新失败! 后端返回错误: $errorBodyString")
                            }
                        } catch (e: Exception) {
                            Log.e("MemberDebug", "网络请求直接抛出异常: ${e.message}", e)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun VolunteerCard(user: VolunteerUserItem, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = user.avatar,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.realName ?: user.username, style = MaterialTheme.typography.titleMedium)
                Text(text = "角色: ${user.role}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(text = "班级: ${user.volunteerInfo?.majorClass ?: "未填写"}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun EditVolunteerDialog(
    user: VolunteerUserItem,
    onDismiss: () -> Unit,
    // 【修改点 1】：回调类型改为 UpdateVolunteerRequest
    onConfirm: (UpdateVolunteerRequest) -> Unit
) {
    // 【修改点 2】：根据 UpdateVolunteerRequest 调整可编辑的状态
    // 如果 realName 为空，可以降级使用 username 作为初始值
    var realName by remember { mutableStateOf(user.realName ?: user.username ?: "") }
    var studentNumber by remember { mutableStateOf(user.volunteerInfo?.studentNumber ?: "") }
    var majorClass by remember { mutableStateOf(user.volunteerInfo?.majorClass ?: "") }
    var grade by remember { mutableStateOf(user.volunteerInfo?.grade ?: "") }
    var role by remember { mutableStateOf(user.role ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改志愿者信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 【修改点 3】：更新对应的输入框
                OutlinedTextField(value = realName, onValueChange = { realName = it }, label = { Text("真实姓名") })
                OutlinedTextField(value = studentNumber, onValueChange = { studentNumber = it }, label = { Text("学号") })
                OutlinedTextField(value = majorClass, onValueChange = { majorClass = it }, label = { Text("专业班级") })
                OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("年级") })
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("角色(注意不要将student直接修改为admin)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                // 【Log 节点 0】：检查用户输入前的数据原始状态
                Log.d("MemberDebug", "0. 点击保存，原始 user.userId: ${user.userId}, 原始 status: ${user.volunteerInfo?.status}")

                val requestBody = UpdateVolunteerRequest(
                    userId = user.userId,
                    realName = realName.takeIf { it.isNotBlank() },
                    studentNumber = studentNumber.takeIf { it.isNotBlank() },
                    majorClass = majorClass.takeIf { it.isNotBlank() },
                    grade = grade.takeIf { it.isNotBlank() },
                    status = user.volunteerInfo?.status ?: 1,
                    role = role.takeIf { it.isNotBlank() }
                )
                onConfirm(requestBody)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}