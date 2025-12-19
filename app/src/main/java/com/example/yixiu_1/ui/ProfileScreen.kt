package com.example.yixiu_1.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import com.example.yixiu_1.data.UserPreferences
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    userPreferences: UserPreferences,
    onNavigateToLogin: () -> Unit,
    onLogout: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onAvatarUpdated: (String?) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isLoggedIn by remember { derivedStateOf { userPreferences.isLoggedIn } }
    var avatarPath by remember { mutableStateOf(userPreferences.avatarPath) }
    val userEmail by remember { derivedStateOf { userPreferences.userEmail } }
    var nickname by remember { mutableStateOf(userPreferences.getNicknameOrGenerate()) }
    var isEditingNickname by remember { mutableStateOf(false) }
    var editingNicknameText by remember(isEditingNickname) { mutableStateOf(nickname) }
    val maxNicknameLength = 20

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                if (inputStream == null) {
                    Toast.makeText(context, "无法读取图片文件", Toast.LENGTH_SHORT).show()
                    return@let
                }
                val avatarFile = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(avatarFile)
                try {
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    val savedPath = avatarFile.absolutePath
                    if (avatarFile.exists() && avatarFile.canRead()) {
                        userPreferences.avatarPath = savedPath
                        avatarPath = savedPath
                        onAvatarUpdated(savedPath)
                        Toast.makeText(context, "头像上传成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "头像文件保存失败", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // 清理可能创建的不完整文件
                    if (avatarFile.exists()) {
                        avatarFile.delete()
                    }
                    throw e
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "头像上传失败", e)
                Toast.makeText(context, "头像上传失败: ${e.message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    with(sharedTransitionScope) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface) // 确保白色背景
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(64.dp))

                val avatarFile = remember(avatarPath) {
                    avatarPath?.let { File(it) }?.takeIf { it.exists() && it.canRead() }
                }

                if (isLoggedIn) {
                    if (avatarFile != null) {
                        // 使用 AsyncImage，Coil 会自动处理加载和错误状态
                        AsyncImage(
                            model = avatarFile,
                            contentDescription = "用户头像",
                            modifier = Modifier
                                .size(120.dp)
                                .sharedElement(
                                    state = rememberSharedContentState(key = "avatar"),
                                    animatedVisibilityScope = animatedContentScope,
                                    boundsTransform = { _, _ ->
                                        tween(durationMillis = 500)
                                    }
                                )
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "默认头像",
                            modifier = Modifier
                                .size(120.dp)
                                .sharedElement(
                                    state = rememberSharedContentState(key = "avatar"),
                                    animatedVisibilityScope = animatedContentScope,
                                    boundsTransform = { _, _ ->
                                        tween(durationMillis = 500)
                                    }
                                ),
                            tint = Color.Gray
                        )
                    }
                } else {
                    // 【关键修复】为未登录状态的 Icon 添加了 sharedElement 修饰符
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "未登录头像",
                        modifier = Modifier
                            .size(120.dp)
                            .sharedElement( // <--- 此处是核心改动
                                state = rememberSharedContentState(key = "avatar"),
                                animatedVisibilityScope = animatedContentScope,
                                boundsTransform = { _, _ ->
                                    tween(durationMillis = 500)
                                }
                            ),
                        tint = Color.Gray
                    )
                }

                if (isLoggedIn) {
                    if (isEditingNickname) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedTextField(
                                value = editingNicknameText,
                                onValueChange = { if (it.length <= maxNicknameLength) editingNicknameText = it },
                                label = { Text("新昵称") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(0.8f),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isEditingNickname = false }) {
                                    Text("取消")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        if (editingNicknameText.isNotBlank()) {
                                            userPreferences.nickname = editingNicknameText
                                            nickname = editingNicknameText
                                            isEditingNickname = false
                                            Toast.makeText(context, "昵称已更新", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "昵称不能为空", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("保存")
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = nickname,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            TextButton(
                                onClick = { isEditingNickname = true },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Text("编辑")
                            }
                        }
                    }
                    userEmail?.let { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoggedIn) {
                    Button(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("上传头像")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // 正确的登出逻辑
                            onLogout()
                            scope.launch {
                                userPreferences.logout()
                                Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("退出登录")
                    }
                } else {
                    Button(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
                        Text("登录/注册")
                    }
                }
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
