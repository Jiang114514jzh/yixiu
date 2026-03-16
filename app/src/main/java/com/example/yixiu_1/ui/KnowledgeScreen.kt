package com.example.yixiu_1.ui // 请替换为你实际的包名

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.yixiu_1.network.* // 请替换为包含你 ApiService 和数据类的包名

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(token: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 状态管理
    var knowledgeList by remember { mutableStateOf<List<KnowledgeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 弹窗状态
    var selectedItem by remember { mutableStateOf<KnowledgeItem?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // 获取数据的方法
    val fetchKnowledgeData = {
        scope.launch {
            isLoading = true
            try {
                // 调用获取全部知识库接口
                val response = NetworkClient.instance.getKnowledgeList(token)
                if (response.isSuccessful && response.body()?.code == 200) {
                    knowledgeList = response.body()?.data ?: emptyList()
                } else {
                    Toast.makeText(context, "获取数据失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // 初始化时获取数据
    LaunchedEffect(Unit) {
        fetchKnowledgeData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("知识库管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(knowledgeList) { item ->
                    KnowledgeItemCard(
                        item = item,
                        onDetail = {
                            selectedItem = item
                            showDetailDialog = true
                        },
                        onEdit = {
                            selectedItem = item
                            showEditDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                try {
                                    // 调用删除接口
                                    val res = NetworkClient.instance.deleteKnowledge(token, item.knowledgeId)
                                    if (res.isSuccessful && res.body()?.code == 200) {
                                        Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                                        // 本地移除该项，避免重新请求网络
                                        knowledgeList = knowledgeList.filter { it.knowledgeId != item.knowledgeId }
                                    } else {
                                        Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // --- 详情弹窗 (居中显示) ---
    if (showDetailDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            title = { Text("知识详情") },

            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("问题: ${selectedItem!!.problem}")
                    Text("解决方法: ${selectedItem!!.solution}")
                    Text("状态 (Status): ${selectedItem!!.status}")
                    // 请确保你的 KnowledgeItem 数据类中有 createTime 和 uploadTime 字段
                    Text("创建时间: ${selectedItem!!.createTime?: "未知"}")
                    Text("上传时间: ${selectedItem!!.updateTime ?: "未知"}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // --- 修改弹窗 (居中显示) ---
    if (showEditDialog && selectedItem != null) {
        var editProblem by remember { mutableStateOf(selectedItem!!.problem) }
        var editSolution by remember { mutableStateOf(selectedItem!!.solution) }
        var editStatus by remember { mutableStateOf(selectedItem!!.status.toString()) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("修改知识库条目") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editProblem,
                        onValueChange = { editProblem = it },
                        label = { Text("问题描述") }
                    )
                    OutlinedTextField(
                        value = editSolution,
                        onValueChange = { editSolution = it },
                        label = { Text("解决方法") }
                    )
                    OutlinedTextField(
                        value = editStatus,
                        onValueChange = { editStatus = it },
                        label = { Text("状态") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        scope.launch {
                            try {
                                val statusInt = editStatus.toIntOrNull() ?: selectedItem!!.status
                                val request = UpdateKnowledgeRequest(
                                    knowledgeId = selectedItem!!.knowledgeId,
                                    problem = editProblem,
                                    solution = editSolution,
                                    status = statusInt
                                )
                                // 调用修改接口
                                val res = NetworkClient.instance.updateKnowledge(token, request)
                                if (res.isSuccessful && res.body()?.code == 200) {
                                    Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                                    // 更新本地列表数据
                                    val updatedItem = selectedItem!!.copy(
                                        problem = editProblem,
                                        solution = editSolution,
                                        status = statusInt
                                    )
                                    knowledgeList = knowledgeList.map {
                                        if (it.knowledgeId == updatedItem.knowledgeId) updatedItem else it
                                    }
                                    showEditDialog = false
                                } else {
                                    Toast.makeText(context, "修改失败", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun KnowledgeItemCard(
    item: KnowledgeItem,
    onDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // 处理问题文字截断，最多显示7个字
    val displayProblem = if (item.problem.length > 7) {
        item.problem.take(7) + "..."
    } else {
        item.problem
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayProblem,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "ID: ${item.knowledgeId}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDetail) { Text("详情") }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onEdit) { Text("修改") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}