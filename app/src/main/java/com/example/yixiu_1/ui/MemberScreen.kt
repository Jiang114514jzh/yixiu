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
                onConfirm = { updatedDetail ->
                    scope.launch {
                        val res = NetworkClient.instance.updateVolunteerInfo(token, updatedDetail)
                        if (res.isSuccessful) {
                            Log.d("MemberDebug", "修改成功，正在刷新列表...")
                            // 刷新
                            val refreshRes = NetworkClient.instance.getVolunteerList(token, 1, 50)
                            volunteerList = refreshRes.body()?.data?.list ?: emptyList()
                            editingUser = null
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
    onConfirm: (VolunteerDetail) -> Unit
) {
    var studentNumber by remember { mutableStateOf(user.volunteerInfo?.studentNumber ?: "") }
    var majorClass by remember { mutableStateOf(user.volunteerInfo?.majorClass ?: "") }
    var grade by remember { mutableStateOf(user.volunteerInfo?.grade ?: "") }
    var contactNumber by remember { mutableStateOf(user.volunteerInfo?.contactNumber ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改志愿者信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = studentNumber, onValueChange = { studentNumber = it }, label = { Text("学号") })
                OutlinedTextField(value = majorClass, onValueChange = { majorClass = it }, label = { Text("专业班级") })
                OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("年级") })
                OutlinedTextField(value = contactNumber, onValueChange = { contactNumber = it }, label = { Text("联系电话") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val updated = user.volunteerInfo?.copy(
                    studentNumber = studentNumber,
                    majorClass = majorClass,
                    grade = grade,
                    contactNumber = contactNumber
                ) ?: VolunteerDetail(0, user.userId, studentNumber, majorClass, grade, 1, 0, contactNumber)
                onConfirm(updated)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}