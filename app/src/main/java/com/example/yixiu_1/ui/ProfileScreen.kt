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
import coil.request.ImageRequest
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.example.yixiu_1.data.UserPreferences
import com.example.yixiu_1.network.NetworkClient
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody


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
        uri ?: return@rememberLauncherForActivityResult

        scope.launch {
            try {
                // 1. 将用户选择的图片复制到 App 的内部缓存目录
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Toast.makeText(context, "无法读取图片", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val imageFile = File(context.cacheDir, "upload_avatar.jpg")
                Log.d("AvatarDebug", "1. 文件准备完毕: 路径=${imageFile.absolutePath}, 大小=${imageFile.length()} 字节")
                val outputStream = FileOutputStream(imageFile)
                inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }
                if (imageFile.length() > 1024 * 1024) {
                    Toast.makeText(context, "图片大小不能超过1MB，请重新选择", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                // 2. 构建上传请求
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                // 'avatar' 是后端指定的参数名，imageFile.name 是文件名
                val body = MultipartBody.Part.createFormData("avatar", imageFile.name, requestFile)
                Log.d("AvatarDebug", "2. 构建MultipartBody: filename=${imageFile.name}, mediaType=image/jpeg")
                // 3. 调用上传接口
                Log.d("AvatarUpload", "开始上传头像...")
                val response = NetworkClient.instance.uploadAvatar(body)

                if (!response.isSuccessful) {
                    Log.d("AvatarDebug", "4. 上传失败原因: ${response.errorBody()?.string()}")
                }

                if (response.isSuccessful) {
                    Toast.makeText(context, "头像上传成功", Toast.LENGTH_SHORT).show()
                    Log.d("AvatarDebug", "5. 上传成功，服务器返回Body: ${response.body()}")
                    // 4. 【关键修复】上传成功后，重新获取用户信息以拿到最新的头像 URL
                    val userResp = NetworkClient.instance.getUserInfo()
                    val userInfo = userResp.body()?.data
                    val serverAvatarUrl = userInfo?.avatar
                    Log.d("AvatarDebug", "6. getUserInfo 返回的原始 avatar 字段: '${userInfo?.avatar}'")

                    if (!serverAvatarUrl.isNullOrBlank()) {
                        // 加上时间戳破坏缓存，确保显示最新图片
                        val finalUrl = "$serverAvatarUrl?t=${System.currentTimeMillis()}"

                        // 更新状态以刷新UI
                        avatarPath = finalUrl
                        // 更新 UserPreferences 持久化保存
                        userPreferences.avatarPath = finalUrl
                        // 回调给 MainActivity 更新顶栏图标
                        onAvatarUpdated(finalUrl)
                        Log.d("AvatarUpload", "头像更新为: $finalUrl")
                    } else {
                        Log.w("AvatarUpload", "上传成功但未获取到新头像链接")
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "上传失败: $errorBody", Toast.LENGTH_SHORT).show()
                    Log.e("AvatarUpload", "上传失败: $errorBody")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "处理图片出错: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AvatarUpload", "上传异常", e)
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
                AvatarImage(
                    path = if (isLoggedIn) avatarPath else null,
                    modifier = Modifier
                        .size(120.dp)
                        .sharedElement(
                            state = rememberSharedContentState(key = "avatar"),
                            animatedVisibilityScope = animatedContentScope,
                            boundsTransform = { _, _ ->
                                tween(durationMillis = 500)
                            }
                        )
                )

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

@Composable
fun AvatarImage(
    path: String?,
    modifier: Modifier = Modifier,
    defaultTint: Color = Color.Gray
) {
    val context = LocalContext.current
    // 获取 Token 用于请求头
    val userPreferences = remember { UserPreferences(context) }
    val token = userPreferences.token

    if (!path.isNullOrBlank()) {
        Box(modifier = modifier) {
            // 1. 底层：灰色默认头像（作为加载中或失败时的占位符）
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                tint = defaultTint
            )

            // 2. 将矢量图转换为 Painter，供 AsyncImage 的 placeholder/error 使用
            val placeholderPainter = rememberVectorPainter(Icons.Default.AccountCircle)

            // 3. 上层：网络图片
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(path)
                    .crossfade(true)
                    .apply {
                        // 添加鉴权 Token
                        if (!token.isNullOrBlank()) {
                            addHeader("Authorization", token)
                        }
                    }
                    .listener(
                        onError = { _, result ->
                            Log.e("AvatarImage", "加载失败: ${result.throwable.message}")
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
        // 路径为空时直接显示图标
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Avatar",
            modifier = modifier,
            tint = defaultTint
        )
    }
}