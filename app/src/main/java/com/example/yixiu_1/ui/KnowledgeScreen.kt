package com.example.yixiu_1.ui // 请替换为实际包名

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.example.yixiu_1.network.* // 请替换为实际包名

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

    LaunchedEffect(Unit) {
        fetchKnowledgeData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("知识库管理", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { fetchKnowledgeData() }) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("加载中...", color = MaterialTheme.colorScheme.outline)
                }
            } else if (knowledgeList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无知识库条目", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                        val res = NetworkClient.instance.deleteKnowledge(token, item.knowledgeId)
                                        if (res.isSuccessful && res.body()?.code == 200) {
                                            Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
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
    }

    // --- 详情弹窗 ---
    if (showDetailDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("条目详情") },
            text = {
                // 如果详情内容也可能很长，这里也加上滚动
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(label = "问题", value = selectedItem!!.problem)
                    DetailRow(label = "解决方法", value = selectedItem!!.solution)
                    DetailRow(label = "状态码", value = selectedItem!!.status.toString())
                    DetailRow(label = "创建时间", value = selectedItem!!.createTime ?: "未知")
                    DetailRow(label = "更新时间", value = selectedItem!!.updateTime ?: "未知")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailDialog = false }) { Text("关闭") }
            }
        )
    }

    // --- 【重点优化】修改弹窗 (结合立体设计与防截断滚动) ---
    if (showEditDialog && selectedItem != null) {
        var editProblem by remember { mutableStateOf(selectedItem!!.problem) }
        var editSolution by remember { mutableStateOf(selectedItem!!.solution) }
        var editStatus by remember { mutableStateOf(selectedItem!!.status.toString()) }
        var isSubmitting by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { showEditDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight() // 自适应高度
                    .padding(vertical = 24.dp), // 防止在极小屏幕上贴紧上下边缘
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                // 在 Column 这里加入 verticalScroll，完美解决内容过长显示不全的问题
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .verticalScroll(scrollState) // <- 添加滚动属性
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 头部图标与标题
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "编辑知识条目",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "ID: ${selectedItem!!.knowledgeId}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 立体输入框区域
                    StyledEditField(
                        label = "问题描述",
                        value = editProblem,
                        onValueChange = { editProblem = it },
                        icon = Icons.Default.QuestionAnswer,
                        iconColor = Color(0xFF42A5F5), // 蓝色
                        singleLine = false,
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    StyledEditField(
                        label = "解决方法",
                        value = editSolution,
                        onValueChange = { editSolution = it },
                        icon = Icons.Default.Lightbulb,
                        iconColor = Color(0xFFFFB300), // 琥珀色
                        singleLine = false,
                        minLines = 4,
                        maxLines = 8// 给解决方法更多默认展示空间
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    StyledEditField(
                        label = "状态 (数字)",
                        value = editStatus,
                        onValueChange = { editStatus = it },
                        icon = Icons.Default.SettingsSuggest,
                        iconColor = Color(0xFF66BB6A), // 绿色
                        keyboardType = KeyboardType.Number
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 按钮组
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("取消")
                        }

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
                                        val res = NetworkClient.instance.updateKnowledge(token, request)
                                        if (res.isSuccessful && res.body()?.code == 200) {
                                            Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
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
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting,
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("保存更新")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 优化的输入框组件 (已修复 Material 3 API 问题)
 */
@Composable
fun StyledEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    iconColor: Color,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE, // 👈 新增这一行：默认最大行数
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = iconColor) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = iconColor,
                focusedLabelColor = iconColor,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines, // 👈 新增这一行：将参数应用到输入框
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(16.dp)
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ID: ${item.knowledgeId}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.problem,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 状态指示器
                StatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.solution,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDetail) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("详情")
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("修改")
                }
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: Int) {
    val color = when(status) {
        1 -> Color(0xFF4CAF50) // 绿色
        else -> Color(0xFFFF9800) // 橙色
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = if (status == 1) "正常" else "状态:$status",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
    }
}