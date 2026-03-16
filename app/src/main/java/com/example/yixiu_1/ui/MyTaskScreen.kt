package com.example.yixiu_1.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.filled.*

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.runtime.Composable

import com.example.yixiu_1.data.UserPreferences
import com.example.yixiu_1.RepairTaskCard

// ==================== 必须添加的引用 ====================
import com.example.yixiu_1.network.NetworkClient
import com.example.yixiu_1.network.RepairTaskItem
// =======================================================
import kotlinx.coroutines.launch
import android.util.Log
import com.example.yixiu_1.TaskDetailScreen

import com.example.yixiu_1.network.*


// MainActivity.kt
@Composable
fun MyTasksScreen(
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    // --- 1. 分页与状态管理 ---
    var tasks by remember { mutableStateOf<List<RepairTaskItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTask by remember { mutableStateOf<RepairTaskItem?>(null) }

    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    val pageSize = 10 // 固定每页 10 条

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = userPreferences.token ?: ""
    val volunteerId = userPreferences.volunteerInfo?.volunteerId ?: -1

    // --- 2. 数据加载逻辑 ---
    // 增加 page 参数
    fun loadMyTasks(page: Int) {
        if (token.isEmpty() || volunteerId == -1) {
            isLoading = false
            return
        }
        scope.launch {
            isLoading = true
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = NetworkClient.instance.getMyTaskByVolunteerId(
                    authHeader,
                    page,
                    pageSize
                )

                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data
                    if (data != null) {
                        tasks = data.list
                        totalPages = data.pages // 直接从后端返回的 pages 获取总页数
                        currentPage = data.pageNum
                    }
                } else {
                    Toast.makeText(context, "获取失败: ${response.body()?.msg}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MyTasks", "加载异常: ${e.message}")
                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // 监听页面加载或页码切换
    LaunchedEffect(currentPage) {
        loadMyTasks(currentPage)
    }

    // --- 3. UI 布局 ---
    Box(modifier = modifier.fillMaxSize()) {
        if (selectedTask != null) {
            // 详情页逻辑（包含你之前添加的审核功能）
            TaskDetailScreen(
                task = selectedTask!!,
                userPreferences = userPreferences,
                onBack = { selectedTask = null },
                onRefresh = { loadMyTasks(currentPage) } // 操作后刷新当前页
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // 上部：任务列表 (占据剩余空间)
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (tasks.isEmpty()) {
                        Text("暂无参与的任务", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tasks) { task ->
                                RepairTaskCard(task = task, onClick = { selectedTask = task })
                            }
                        }
                    }
                }

                // --- 4. 底部分页控制器 ---
                if (!isLoading && totalPages > 0) {
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
                            // 上一页
                            OutlinedButton(
                                onClick = { if (currentPage > 1) currentPage-- },
                                enabled = currentPage > 1,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("上一页")
                            }

                            // 页码状态展示
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "第 $currentPage / $totalPages 页",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                // 你要求的 total 数量显示
                                // 注意：需要从接口中保存 total 变量到 remember 状态中，这里假设 UI 需要显示总数
                                // Text("共 ${totalCount} 条", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }

                            // 下一页
                            OutlinedButton(
                                onClick = { if (currentPage < totalPages) currentPage++ },
                                enabled = currentPage < totalPages,
                                shape = RoundedCornerShape(8.dp)
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