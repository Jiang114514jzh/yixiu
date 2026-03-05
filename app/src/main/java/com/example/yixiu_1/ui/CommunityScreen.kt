package com.example.yixiu_1.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.yixiu_1.AvatarImage
import com.example.yixiu_1.data.UserPreferences
import com.example.yixiu_1.network.CommunityPostItem
import com.example.yixiu_1.network.CreatePostRequest
import com.example.yixiu_1.network.NetworkClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.yixiu_1.network.*
import com.example.yixiu_1.network.AddCommentRequest
import com.example.yixiu_1.network.AddReplyRequest
import com.example.yixiu_1.network.ModifyCommentLikeRequest

// ================== 评论系统数据模型 ==================




sealed class ReplyTarget {
    // 回复帖子本身
    data class ToPost(val postId: Int) : ReplyTarget()

    // 回复某个主评论
    data class ToComment(
        val commentId: Int,
        val targetUserId: Int,
        val targetUsername: String
    ) : ReplyTarget()

    // 嵌套回复某个子评论
    data class ToReply(
        val commentId: Int,
        val replyId: Int,
        val targetUserId: Int,
        val targetUsername: String
    ) : ReplyTarget()
}

// ================== 1. 社区主页面 ==================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun  CommunityScreen(
    userPreferences: UserPreferences,
    onNavigateToCreatePost: () -> Unit,
    onBack: () -> Unit,
    preloadedPosts: List<CommunityPostItem>? = null,
    onNavigateToMyCollection: () -> Unit,
    onPostClick: (Int) -> Unit // 【修复 1】添加缺失的跳转回调参数
) {
    // 0: 社区主页, 1: 我的
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current


    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = { Text("义修社区") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreatePost,
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建帖子")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            CommunityBottomBar(selectedTab) { selectedTab = it }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (selectedTab == 0) {
                // 【修复 2】传入 initialPosts 和 onPostClick
                CommunityHomeContent(
                    initialPosts = preloadedPosts,
                    onPostClick = onPostClick
                )
            } else {
                // 【修复 3】传入 onPostClick
                CommunityMineContent(
                    userId = userPreferences.userId,
                    userPreferences = userPreferences,
                    onNavigateToMyCollection = onNavigateToMyCollection,
                    onPostClick = onPostClick
                )
            }
        }
    }
}

suspend fun togglePostFavorite(post: CommunityPostItem, userPreferences: UserPreferences): Boolean {
    val token = userPreferences.token ?: return false
    val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
    return try {
        val response = NetworkClient.instance.toggleFavorite(authHeader, post.postId)
        response.isSuccessful && response.body()?.code == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

suspend fun togglePostLike(post: CommunityPostItem, userPreferences: UserPreferences): Boolean {
    val token = userPreferences.token ?: return false
    val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
    return try {
        // 调用上面新定义的接口
        val response = NetworkClient.instance.toggleLike(authHeader, post.postId)
        response.isSuccessful && response.body()?.code == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


@Composable
fun CommunityBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTabSelected(0) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "社区主页",
                    color = if (selectedTab == 0) Color(0xFF2196F3) else Color.Gray,
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                )
            }
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray))
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTabSelected(1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "我的",
                    color = if (selectedTab == 1) Color(0xFF2196F3) else Color.Gray,
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
@Composable
fun CommunityHomeContent(
    initialPosts: List<CommunityPostItem>? = null,
    onPostClick: (Int) -> Unit // 【修复 4】添加缺失的参数
) {
    var posts by remember { mutableStateOf(initialPosts ?: emptyList()) }
    var isLoading by remember { mutableStateOf(initialPosts == null) }
    var pageNum by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context)}
    val scope = rememberCoroutineScope()

    LaunchedEffect(pageNum) {
        if (pageNum == 1 && initialPosts != null && posts.isNotEmpty()) return@LaunchedEffect
        isLoading = true
        try {
            val response = NetworkClient.instance.getCommunityPosts(pageNum, 10)
            if (response.isSuccessful && response.body()?.code == 200) {
                posts = response.body()?.data?.list ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    PostList(
        posts = posts,
        isLoading = isLoading,
        emptyText = "暂无帖子",
        onFavoriteToggle = { post ->
            // 【注意】此处原逻辑是控制收藏 (isFavorite)，若需控制点赞，请将字段改为 isLiked 和 likeNum
            // 并且需要调用对应的点赞接口 (例如 togglePostLike)

            // 1. 计算新状态 (以收藏为例，若改点赞请调整字段)
            val newIsFavorite = if (post.isFavorite == 1) 0 else 1
            val newFavCount = if (newIsFavorite == 1) post.favoriteNum + 1 else post.favoriteNum - 1

            // 2. 乐观更新 UI
            posts = posts.map {
                if (it.postId == post.postId) {
                    it.copy(isFavorite = newIsFavorite, favoriteNum = newFavCount)
                } else {
                    it
                }
            }

            // 3. 发起网络请求
            scope.launch {
                // 请确保已定义 togglePostFavorite 或对应的点赞函数
                val success = togglePostFavorite(post, userPreferences)

                if (!success) {
                    // 4. 失败回滚
                    posts = posts.map {
                        if (it.postId == post.postId) {
                            it.copy(isFavorite = post.isFavorite, favoriteNum = post.favoriteNum)
                        } else {
                            it
                        }
                    }
                    Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onLikeToggle = { post ->
            // 1. 计算新状态
            val newIsLiked = if (post.isLiked == 1) 0 else 1
            val newLikeNum = if (newIsLiked == 1) post.likeNum + 1 else post.likeNum - 1

            // 2. 乐观更新 UI (立即改变界面，提升体验)
            posts = posts.map {
                if (it.postId == post.postId) {
                    it.copy(isLiked = newIsLiked, likeNum = newLikeNum)
                } else {
                    it
                }
            }

            // 3. 发起网络请求
            scope.launch {
                val success = togglePostLike(post, userPreferences)

                // 4. 如果失败，回滚状态
                if (!success) {
                    posts = posts.map {
                        if (it.postId == post.postId) {
                            it.copy(isLiked = post.isLiked, likeNum = post.likeNum)
                        } else {
                            it
                        }
                    }
                    Toast.makeText(context, "点赞操作失败", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onItemClick = onPostClick // 【修复 5】修正这里的传参语法，直接将函数传进去
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCollectionScreen(
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit // 【修复 6】添加参数
) {
    var posts by remember { mutableStateOf<List<CommunityPostItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val response = NetworkClient.instance.getFavoritePosts(1, 10)
            if (response.isSuccessful && response.body()?.code == 200) {
                val rawList = response.body()?.data?.list ?: emptyList()
                posts = rawList.map { favItem ->
                    CommunityPostItem(
                        postId = favItem.postId,
                        userId = favItem.postUserId,
                        username = "用户${favItem.postUserId}",
                        avatar = favItem.postUserAvatar,
                        userSignature = null,
                        title = favItem.title ?: "无标题",
                        content = "（收藏列表暂不显示详情摘要）",
                        imgUrls = emptyList(),
                        tags = favItem.tags ?: emptyList(),
                        status = 0,
                        likeNum = 0, commentNum = 0, viewNum = 0, favoriteNum = 0,
                        isLiked = 0, isFavorite = 1,
                        createTime = favItem.createTime ?: "",
                        updateTime = null
                    )
                }
            } else {
                Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "加载异常", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            PostList(
                posts = posts,
                isLoading = isLoading,
                emptyText = "暂无收藏内容",
                onFavoriteToggle = { post ->
                    val originalPosts = posts
                    posts = posts.filter { it.postId != post.postId }
                    scope.launch {
                        val success = togglePostFavorite(post, userPreferences)
                        if (!success) {
                            posts = originalPosts
                            Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onLikeToggle = { post ->
                    Toast.makeText(context, "请在社区主页进行点赞操作", Toast.LENGTH_SHORT).show()
                },
                onItemClick = onPostClick // 【修复 7】传入点击事件
            )
        }
    }
}

// ================== 4. 帖子详情页 ==================

// ================== 4. 帖子详情页 ==================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    onBack: () -> Unit
) {
    var post by remember { mutableStateOf<CommunityPostItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    // 评论系统状态
    var comments by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var currentReplyTarget by remember { mutableStateOf<ReplyTarget>(ReplyTarget.ToPost(postId)) }

    // 获取评论列表的抽离函数（方便评论成功后刷新）
    fun fetchCommentsAndReplies() {
        scope.launch {
            try {
                // 1. 获取该帖子的主评论列表
                val commentsResponse = NetworkClient.instance.getCommentsByPostId(postId = postId, pageNum = 1, pageSize = 50)
                if (commentsResponse.isSuccessful && commentsResponse.body()?.code == 200) {
                    val fetchedComments = commentsResponse.body()?.data?.list ?: emptyList()

                    // 2. 并发获取每一条主评论的嵌套回复列表 (极大提升加载速度)
                    val commentsWithReplies = fetchedComments.map { comment ->
                        async {
                            try {
                                val repliesResponse = NetworkClient.instance.getRepliesByCommentId(
                                    commentId = comment.commentId,
                                    pageNum = 1,
                                    pageSize = 50
                                )
                                val fetchedReplies = if (repliesResponse.isSuccessful && repliesResponse.body()?.code == 200) {
                                    repliesResponse.body()?.data?.list ?: emptyList()
                                } else {
                                    emptyList()
                                }
                                // 将获取到的回复塞入该评论对象中
                                comment.copy(replies = fetchedReplies)
                            } catch (e: Exception) {
                                comment.copy(replies = emptyList())
                            }
                        }
                    }.awaitAll() // 等待所有请求完成

                    // 赋值给 UI 状态
                    comments = commentsWithReplies
                }
            } catch (e: Exception) {
                Log.e("Comment", "获取评论异常: ${e.message}")
            }
        }
    }

    // 进入页面时，请求服务器获取帖子详情、评论列表及回复
    LaunchedEffect(postId) {
        isLoading = true
        try {
            // 获取帖子详情
            val response = NetworkClient.instance.getCommunityPostsByFilterByPostId(postId)
            if (response.isSuccessful && response.body()?.code == 200) {
                val list = response.body()?.data?.list
                if (!list.isNullOrEmpty()) {
                    post = list[0]
                }
            }
            // 触发获取评论
            fetchCommentsAndReplies()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // 提交评论/回复的真实网络逻辑
    fun submitComment() {
        if (inputText.isBlank()) return

        val currentInput = inputText
        val target = currentReplyTarget

        // 1. 立即清空输入框并重置状态，提升用户体验
        inputText = ""
        currentReplyTarget = ReplyTarget.ToPost(postId)

        val currentUser = userPreferences.getNicknameOrGenerate()
        val currentUserId = userPreferences.userId
        val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val fakeId = (Math.random() * 10000).toInt() // 临时假ID

        scope.launch {
            val token = userPreferences.token ?: return@launch
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

            try {
                when (target) {
                    is ReplyTarget.ToPost -> {
                        // 乐观更新
                        comments = comments + PostComment(
                            commentId = fakeId, userId = currentUserId, username = currentUser,
                            avatar = userPreferences.avatarPath, content = currentInput, createTime = now,postId = postId,
                            replies = emptyList()
                        )
                        // 网络请求
                        val request = AddCommentRequest(postId = postId, content = currentInput)
                        val response = NetworkClient.instance.addComment(authHeader, request)
                        if (response.isSuccessful && response.body()?.code == 200) {
                            Toast.makeText(context, "评论成功", Toast.LENGTH_SHORT).show()
                            fetchCommentsAndReplies() // 成功后刷新真实数据
                        } else {
                            Toast.makeText(context, "评论失败", Toast.LENGTH_SHORT).show()
                            comments = comments.filter { it.commentId != fakeId } // 失败回滚
                        }
                    }
                    is ReplyTarget.ToComment -> {
                        // 乐观更新
                        comments = comments.map { c ->
                            if (c.commentId == target.commentId) {
                                c.copy(replies = c.replies + PostReply(
                                    replyId = fakeId, commentId = target.commentId,
                                    fromUserId = currentUserId, fromUserName = currentUser, fromUserAvatar = userPreferences.avatarPath,
                                    toUserId = target.targetUserId, toUserName = target.targetUsername, parentReplyId = null,
                                    content = currentInput, createTime = now
                                ))
                            } else c
                        }
                        // 网络请求
                        val request = AddReplyRequest(commentId = target.commentId, toUserId = target.targetUserId, parentReplyId = null, content = currentInput)
                        val response = NetworkClient.instance.addReply(authHeader, request)
                        if (response.isSuccessful && response.body()?.code == 200) fetchCommentsAndReplies()
                        else Toast.makeText(context, "回复失败", Toast.LENGTH_SHORT).show()
                    }
                    is ReplyTarget.ToReply -> {
                        // 乐观更新
                        comments = comments.map { c ->
                            if (c.commentId == target.commentId) {
                                c.copy(replies = c.replies + PostReply(
                                    replyId = fakeId, commentId = target.commentId,
                                    fromUserId = currentUserId, fromUserName = currentUser, fromUserAvatar = userPreferences.avatarPath,
                                    toUserId = target.targetUserId, toUserName = target.targetUsername, parentReplyId = target.replyId,
                                    content = currentInput, createTime = now
                                ))
                            } else c
                        }
                        // 网络请求
                        val request = AddReplyRequest(commentId = target.commentId, toUserId = target.targetUserId, parentReplyId = target.replyId, content = currentInput)
                        val response = NetworkClient.instance.addReply(authHeader, request)
                        if (response.isSuccessful && response.body()?.code == 200) fetchCommentsAndReplies()
                        else Toast.makeText(context, "回复失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "网络异常，发布失败", Toast.LENGTH_SHORT).show()
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("帖子详情") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (post == null) {
                Text("未找到内容", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            } else {
                val item = post!!

                // 将整个页面改为 LazyColumn
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp) // 底部留白，防止被输入框遮挡
                ) {
                    // --- 1. 帖子正文区 ---
                    item {
                        Column(modifier = Modifier.background(Color.White).padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarImage(path = item.avatar, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(item.username ?: "未知用户", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(item.createTime?.replace("T", " ")?.substringBefore(".") ?: "", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(item.title ?: "无标题", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(item.content ?: "无内容", fontSize = 16.sp, lineHeight = 24.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (!item.imgUrls.isNullOrEmpty()) {
                                item.imgUrls.forEach { url ->
                                    AsyncImage(
                                        model = url, contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // --- 2. 评论区头部 ---
                    item {
                        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "全部评论 (${comments.size + comments.sumOf { it.replies.size }})",
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }

                    // --- 3. 评论列表 ---
                    items(comments, key = { it.commentId }) { comment ->
                        CommentItemView(
                            comment = comment,
                            currentReplyTarget = currentReplyTarget,
                            inputText = inputText,
                            onInputTextChange = { inputText = it },
                            onReplyClick = { target ->
                                currentReplyTarget = if (currentReplyTarget == target) ReplyTarget.ToPost(postId) else target
                            },
                            onSubmit = { submitComment() }
                        )
                        HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(start = 56.dp))
                    }
                }

                // --- 4. 底部悬浮的主回复框 (针对整个帖子) ---
                if (currentReplyTarget is ReplyTarget.ToPost) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { if (it.length <= 200) inputText = it },
                                placeholder = { Text("请输入评论...", color = Color.Gray) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 100.dp), // 支持多行自适应高度
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF5F5F5),
                                    unfocusedContainerColor = Color(0xFFF5F5F5),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    Text("${inputText.length}/200", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp))
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { submitComment() },
                                enabled = inputText.isNotBlank(),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Text("发表")
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun PostList(
    posts: List<CommunityPostItem>,
    isLoading: Boolean,
    emptyText: String,
    onFavoriteToggle: (CommunityPostItem) -> Unit,
    onLikeToggle: (CommunityPostItem) -> Unit,
    onItemClick: (Int) -> Unit
) {
    if (isLoading && posts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (posts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(posts, key = { it.postId }) { post ->
                // 【修复 8】使用显式命名参数，避免解析混乱
                PostCard(
                    post = post,
                    onFavoriteToggle = onFavoriteToggle,
                    onLikeToggle = onLikeToggle,
                    onClick = { onItemClick(post.postId) }
                )
            }
        }
    }
}

@Composable
fun PostCard(
    post: CommunityPostItem,
    onFavoriteToggle: (CommunityPostItem) -> Unit,
    onLikeToggle: (CommunityPostItem) -> Unit,
    onClick: () -> Unit
) {
    // 【新增】记录当前被点击查看的大图 URL，null 表示未打开
    var clickedImageUrl by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick), // 点击卡片空白处跳转详情
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // --- 头部 ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(path = post.avatar, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(post.username ?: "未知用户", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(post.createTime?.replace("T", " ")?.substringBefore(".") ?: "", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(post.title ?: "无标题", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(post.content ?: "暂无内容", fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, color = Color.DarkGray)

            // --- 图片 ---
            if (!post.imgUrls.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    post.imgUrls.take(3).forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(4.dp))
                                // 【关键修改】添加 clickable，拦截点击事件，打开大图
                                .clickable { clickedImageUrl = url },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // --- 标签 ---
            if (!post.tags.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    post.tags.forEach { tag ->
                        Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(4.dp)) {
                            Text("#${tag.tagName}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFF1976D2), fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // --- 底部互动栏 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InteractionItem(
                    icon = if (post.isLiked == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    count = post.likeNum,
                    tint = if (post.isLiked == 1) Color(0xFF2196F3) else Color.Gray,
                    onClick = { onLikeToggle(post) }
                )
                InteractionItem(
                    icon = Icons.AutoMirrored.Filled.Comment,
                    count = post.commentNum,
                    tint = Color.Gray,
                    onClick = onClick
                )
                InteractionItem(
                    icon = if (post.isFavorite == 1) Icons.Filled.Star else Icons.Outlined.Star,
                    count = post.favoriteNum,
                    tint = if (post.isFavorite == 1) Color(0xFFFFC107) else Color.Gray,
                    onClick = { onFavoriteToggle(post) }
                )
            }
        }
    }

    // ==========================================
    // 【新增】全屏大图查看器 Dialog
    // ==========================================
    if (clickedImageUrl != null) {
        Dialog(
            onDismissRequest = { clickedImageUrl = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false, // 解除 Dialog 的默认宽度限制，允许全屏
                dismissOnBackPress = true,       // 允许返回键关闭
                dismissOnClickOutside = true     // 允许点击弹窗外关闭
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black) // 黑底色沉浸式体验
                    .clickable { clickedImageUrl = null }, // 点击黑底任何地方都可以关闭
                contentAlignment = Alignment.Center
            ) {
                // 显示完整图片，按比例缩放以适应屏幕
                AsyncImage(
                    model = clickedImageUrl,
                    contentDescription = "查看大图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // 右上角提供一个直观的关闭按钮
                IconButton(
                    onClick = { clickedImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun InteractionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    tint: Color,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp) // 增加可点击区域
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = if (count > 0) "$count" else "0", fontSize = 14.sp, color = tint)
    }
}
@Composable
fun CommunityMineContent(
    userId: Int,
    userPreferences: UserPreferences,
    onNavigateToMyCollection: () -> Unit,
    onPostClick: (Int) -> Unit
) {
    var myPosts by remember { mutableStateOf<List<CommunityPostItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 统计数据
    var postCount by remember { mutableIntStateOf(0) }
    var likeCount by remember { mutableIntStateOf(0) }
    var fansCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val nickname = remember { userPreferences.getNicknameOrGenerate() }
    val avatarPath = remember { userPreferences.avatarPath }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        isLoading = true
        try {
            val response = NetworkClient.instance.getCommunityPostsByFilter(1, 100, userId)
            if (response.isSuccessful && response.body()?.code == 200) {
                val data = response.body()?.data
                if (data != null) {
                    myPosts = data.list
                    postCount = data.total
                    likeCount = myPosts.sumOf { it.likeNum }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AvatarImage(path = avatarPath, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(text = "UID: $userId", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(count = postCount, label = "帖子")
                        StatItem(count = likeCount, label = "获赞")
                        StatItem(count = fansCount, label = "粉丝")
                    }
                }
            }
        }

        // 我的收藏入口 - 添加点击事件
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // 【修改】添加跳转回调
                    .clickable { onNavigateToMyCollection() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Star, null, tint = Color(0xFFFFC107))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("我的收藏", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = "我的帖子",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        }

        if (isLoading) {
            item { Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (myPosts.isEmpty()) {
            item { Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("暂无发布内容", color = Color.Gray) } }
        } else {
            // 我的帖子列表也支持收藏/取消收藏
            items(myPosts, key = { it.postId }) { post ->
                PostCard(
                    post = post,
                    onFavoriteToggle = { targetPost ->
                        // 更新逻辑：只更新状态，不移除帖子（因为这是我的发布列表）
                        val newIsFavorite = if (targetPost.isFavorite == 1) 0 else 1
                        val newFavCount = if (newIsFavorite == 1) targetPost.favoriteNum + 1 else targetPost.favoriteNum - 1

                        myPosts = myPosts.map { if (it.postId == targetPost.postId) it.copy(isFavorite = newIsFavorite, favoriteNum = newFavCount) else it }

                        scope.launch {
                            val success = togglePostFavorite(targetPost, userPreferences)
                            if (!success) {
                                myPosts = myPosts.map { if (it.postId == targetPost.postId) targetPost else it }
                            }
                        }
                    },onLikeToggle = { likePost ->
                        Toast.makeText(context, "请在社区主页进行点赞操作", Toast.LENGTH_SHORT).show()
                    },
                    onClick = { onPostClick(post.postId) }
                )
            }
        }
    }
}

@Composable
fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
// ================== 2. 发帖页面 ==================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    onBack: () -> Unit,
    onPostSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<Int>()) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSubmitting by remember { mutableStateOf(false) }

    val tagMap = mapOf(
        1 to "求助", 2 to "经验分享", 3 to "自愿分享",
        4 to "杂谈", 5 to "精华", 6 to "其他"
    )

    // 【优化】图片选择器：改为“添加”模式而不是“覆盖”模式
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // 使用 distinct() 去重，防止重复添加
            selectedImages = (selectedImages + uris).distinct()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发布帖子") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isBlank() || content.isBlank()) {
                                Toast.makeText(context, "标题和内容不能为空", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedTags.isEmpty()) {
                                Toast.makeText(context, "请至少选择一个标签", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSubmitting = true
                            scope.launch {
                                try {
                                    // 1. 发帖
                                    Log.d("CreatePost", "正在创建帖子: $title")
                                    val postRequest = CreatePostRequest(
                                        title = title,
                                        content = content,
                                        tagIdList = selectedTags.toList()
                                    )
                                    val postResponse = NetworkClient.instance.createPost(postRequest)

                                    if (postResponse.isSuccessful && postResponse.body()?.code == 200) {
                                        val postId = postResponse.body()?.data?.postId
                                        Log.d("CreatePost", "帖子创建成功, ID: $postId")

                                        if (postId != null) {
                                            // 2. 上传图片
                                            if (selectedImages.isNotEmpty()) {
                                                uploadImages(context, postId, selectedImages)
                                            }
                                            Toast.makeText(context, "发布成功", Toast.LENGTH_SHORT).show()
                                            onPostSuccess()
                                        } else {
                                            Toast.makeText(context, "错误：PostId为空", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        val msg = postResponse.body()?.msg ?: "未知错误"
                                        Toast.makeText(context, "发布失败: $msg", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Log.e("CreatePost", "异常: ${e.message}")
                                    Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isSubmitting && title.isNotBlank() && content.isNotBlank()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("发布")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题输入
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 内容输入
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("正文内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 标签选择
            Text("选择标签 (可多选)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tagMap.forEach { (id, name) ->
                    FilterChip(
                        selected = selectedTags.contains(id),
                        onClick = {
                            selectedTags = if (selectedTags.contains(id)) selectedTags - id else selectedTags + id
                        },
                        label = { Text(name) },
                        leadingIcon = if (selectedTags.contains(id)) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 图片选择区
            Text("添加图片 (${selectedImages.size}/9)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    OutlinedButton(
                        onClick = {
                            // 调用相册
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(100.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }

                items(selectedImages) { uri ->
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImages = selectedImages - uri },
                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.White.copy(alpha=0.7f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "删除", tint = Color.Red, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }
        }
    }
}

// ================== 3. 辅助函数 ==================

/**
 * 上传多张图片 (带详细日志)
 */
suspend fun uploadImages(context: Context, postId: Int, uris: List<Uri>) {
    // 1. 准备 PostId
    val postIdBody = postId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

    Log.d("Upload", "=== 开始批量上传: PostId $postId, 共 ${uris.size} 张 ===")

    // 2. 准备文件 Parts 列表
    val parts = mutableListOf<MultipartBody.Part>()

    uris.forEachIndexed { index, uri ->
        val file = uriToFile(context, uri)
        if (file != null) {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())

            // 【关键】必须使用 "img" 作为参数名，这与 Apifox 中的参数名一致
            // 虽然是多张图片，但每张图片的 key 都是 "img"
            val part = MultipartBody.Part.createFormData("img", file.name, requestFile)
            parts.add(part)

            Log.d("Upload", "    已打包第 ${index + 1} 张: ${file.name}")
        } else {
            Log.e("Upload", "    第 ${index + 1} 张转换失败")
        }
    }

    if (parts.isEmpty()) {
        Log.e("Upload", "没有有效图片，取消上传")
        return
    }

    // 3. 发起【单次】网络请求
    try {
        Log.d("Upload", ">>> 发送单次请求，包含 ${parts.size} 个文件...")

        // 调用修改后的接口，传入 List<MultipartBody.Part>
        val response = NetworkClient.instance.uploadPostImage(parts, postIdBody)

        if (response.isSuccessful && response.body()?.code == 200) {
            val urls = response.body()?.data?.logImgUrls
            Log.d("Upload", "✅ 批量上传成功! 服务端返回: $urls")
        } else {
            Log.e("Upload", "❌ 上传失败: Code=${response.code()}, Msg=${response.body()?.msg}")
        }

    } catch (e: Exception) {
        Log.e("Upload", "❌ 网络请求异常: ${e.message}")
        e.printStackTrace()
    } finally {
        // 清理临时文件 (这里无法直接访问 file 对象了，但在实际生产中建议在 uriToFile 生成文件时记录路径统一清理)
        // 本示例为了简化逻辑暂略过批量清理，依赖系统缓存清理机制
    }
}

/**
 * Uri 转临时文件 (UUID 防重名)
 */
fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val contentResolver = context.contentResolver
        // 使用 UUID 确保文件名唯一
        val fileName = "upload_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        val tempFile = File(context.cacheDir, fileName)

        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("Upload", "uriToFile 异常: ${e.message}")
        null
    }
}

// ================== 评论区 UI 组件 ==================

@Composable
fun CommentItemView(
    comment: PostComment,
    currentReplyTarget: ReplyTarget,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onReplyClick: (ReplyTarget) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 主评论区
        Row(verticalAlignment = Alignment.Top) {
            AvatarImage(path = comment.avatar, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 昵称和时间
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(comment.username, fontWeight = FontWeight.Bold, color = Color(0xFF333333), fontSize = 14.sp)
                    Text(comment.createTime.replace("T", " ").substringBefore("."), color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 评论内容
                Text(comment.content, fontSize = 15.sp, color = Color.Black, lineHeight = 22.sp)

                // 交互行 (回复按钮)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "回复",
                        color = Color(0xFF2196F3),
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            onReplyClick(ReplyTarget.ToComment(comment.commentId, comment.userId, comment.username))
                        }.padding(4.dp)
                    )
                }

                // 动态弹出的内联输入框 (回复主评论)
                InlineReplyInput(
                    isVisible = currentReplyTarget == ReplyTarget.ToComment(comment.commentId, comment.userId, comment.username),
                    targetName = comment.username,
                    text = inputText,
                    onTextChange = onInputTextChange,
                    onSubmit = onSubmit
                )

                // --- 嵌套回复区 ---
                if (comment.replies.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        comment.replies.forEach { reply ->
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarImage(path = reply.fromUserAvatar, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))

                                    // 格式: A 回复 B
                                    Text(reply.fromUserName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF555555))

                                    // 只有当回复的对象不是层主时，才显示 "回复 B" 避免啰嗦
                                    if (reply.toUserName != comment.username) {
                                        Text(" 回复 ", fontSize = 13.sp, color = Color.Gray)
                                        Text(reply.toUserName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF555555))
                                    }

                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(reply.createTime.replace("T", " ").substringBefore("."), color = Color.Gray, fontSize = 11.sp)
                                }

                                Text(
                                    // 后端的内容可能自带了 @xxx，UI可以根据需求处理，这里直接展示
                                    text = reply.content,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(start = 32.dp, top = 4.dp)
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text(
                                        text = "回复", color = Color(0xFF2196F3), fontSize = 12.sp,
                                        modifier = Modifier.clickable {
                                            onReplyClick(ReplyTarget.ToReply(comment.commentId, reply.replyId, reply.fromUserId, reply.fromUserName))
                                        }.padding(4.dp)
                                    )
                                }

                                // 动态弹出的内联输入框 (回复子评论)
                                InlineReplyInput(
                                    isVisible = currentReplyTarget == ReplyTarget.ToReply(comment.commentId, reply.replyId, reply.fromUserId, reply.fromUserName),
                                    targetName = reply.fromUserName,
                                    text = inputText,
                                    onTextChange = onInputTextChange,
                                    onSubmit = onSubmit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 提取的内联输入框组件
@Composable
fun InlineReplyInput(
    isVisible: Boolean,
    targetName: String,
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    if (isVisible) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("回复 @$targetName...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onSubmit,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("发表", fontSize = 13.sp)
                }
            }
        }
    }
}