package com.example.yixiu_1.ui

import SkeletonPostCard
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.draw.blur
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.blur
import kotlinx.coroutines.CoroutineScope
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import com.example.yixiu_1.ui.CommentActionButton

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
    onUserClick: (Int) -> Unit, // 【新增此参数】
    onPostClick: (Int) -> Unit // 【修复 1】添加缺失的跳转回调参数
) {
    // 0: 社区主页, 1: 我的
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var currentViewUserId by remember { mutableStateOf<Int?>(null) }


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
                    onPostClick = onPostClick,
                    onUserClick = onUserClick // 【向下传递给首页列表】
                )
            } else {
                // 【修复 3】传入 onPostClick
                CommunityMineContent(
                    userId = userPreferences.userId,
                    userPreferences = userPreferences,
                    onNavigateToMyCollection = onNavigateToMyCollection,
                    onPostClick = onPostClick,
                    onUserClick = onUserClick // 【向下传递给我的列表】
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
    onPostClick: (Int) -> Unit,
    onUserClick: (Int) -> Unit
) {
    var posts by remember { mutableStateOf(initialPosts ?: emptyList()) }
    var isLoading by remember { mutableStateOf(initialPosts == null) }
    var pageNum by remember { mutableIntStateOf(1) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var hasMoreData by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context)}
    val scope = rememberCoroutineScope()

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshTrigger += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(pageNum, refreshTrigger) {
        if (posts.isEmpty()) {
            isLoading = true
        }
        try {
            kotlinx.coroutines.delay(150)

            val response = NetworkClient.instance.getCommunityPosts(pageNum, 10)
            if (response.isSuccessful && response.body()?.code == 200) {
                val pageData = response.body()?.data
                posts = pageData?.list ?: emptyList()
                val total = pageData?.total ?: 0
                hasMoreData = (pageNum * 10) < total

                // 👇 【核心修复】：把滑动操作放进 scope.launch 中！
                // 这样它就不会阻塞当前的协程向下走到 finally，从而打破死锁
                if (posts.isNotEmpty()) {
                    scope.launch {
                        listState.scrollToItem(0)
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // 协程不被阻塞，顺利来到这里将 isLoading 设为 false
            // 骨架屏消失，真正的 PostList 挂载，刚才 scope 里等待的滑动瞬间执行！
            isLoading = false
        }
    }

    // 👇 【核心修改】：通过 isLoading 判断，渲染骨架屏或真实的帖子列表
    if (isLoading) {
        // 正在加载：渲染满屏的骨架卡片
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            items(5) {
                SkeletonPostCard()
            }
        }
    } else {
        // 加载完毕：直接渲染真实的 PostList
        PostList(
            posts = posts,
            isLoading = isLoading,
            emptyText = "暂无帖子",
            onDeleteSuccess = {
                pageNum = 1
                refreshTrigger += 1
            },
            onFavoriteToggle = { post ->
                val newIsFavorite = if (post.isFavorite == 1) 0 else 1
                val newFavCount = if (newIsFavorite == 1) post.favoriteNum + 1 else post.favoriteNum - 1
                posts = posts.map {
                    if (it.postId == post.postId) it.copy(isFavorite = newIsFavorite, favoriteNum = newFavCount) else it
                }
                scope.launch {
                    val success = togglePostFavorite(post, userPreferences)
                    if (!success) {
                        posts = posts.map {
                            if (it.postId == post.postId) it.copy(isFavorite = post.isFavorite, favoriteNum = post.favoriteNum) else it
                        }
                        Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onLikeToggle = { post ->
                val newIsLiked = if (post.isLiked == 1) 0 else 1
                val newLikeNum = if (newIsLiked == 1) post.likeNum + 1 else post.likeNum - 1
                posts = posts.map {
                    if (it.postId == post.postId) it.copy(isLiked = newIsLiked, likeNum = newLikeNum) else it
                }
                scope.launch {
                    val success = togglePostLike(post, userPreferences)
                    if (!success) {
                        posts = posts.map {
                            if (it.postId == post.postId) it.copy(isLiked = post.isLiked, likeNum = post.likeNum) else it
                        }
                        Toast.makeText(context, "点赞操作失败", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onUserClick = onUserClick,
            onItemClick = onPostClick,
            listFooter = {
                if (posts.isNotEmpty() && !isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .padding(bottom = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (pageNum > 1) pageNum-- },
                            enabled = pageNum > 1,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("上一页")
                        }
                        Text(
                            text = "第 $pageNum 页",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { pageNum++ },
                            enabled = hasMoreData,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("下一页")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            },
            listState = listState
        )
    }
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
                onDeleteSuccess = { /* 收藏页通常不处理删除，或在此刷新列表 */ },
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
                onItemClick = onPostClick, // 【修复 7】传入点击事件
                onUserClick = { clickedId ->
                    // 如果收藏页也要能点头像，这里可以管理状态，或者暂时传空
                },
            )
        }
    }
}


// ================== 4. 帖子详情页 ==================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit
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

    // 获取当前用户 ID 和权限状态
    val currentUserId = userPreferences.userId
    val isAdmin = userPreferences.userRole == "admin" || userPreferences.userRole == "super_admin"

    // 1. 获取评论列表的逻辑
    fun fetchCommentsAndReplies() {
        scope.launch {
            try {
                val commentsResponse = NetworkClient.instance.getCommentsByPostId(postId = postId, pageNum = 1, pageSize = 50)
                if (commentsResponse.isSuccessful && commentsResponse.body()?.code == 200) {
                    val fetchedComments = commentsResponse.body()?.data?.list ?: emptyList()
                    val commentsWithReplies = fetchedComments.map { comment ->
                        // 加上 Dispatchers.IO 防止主线程卡顿
                        async(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val repliesResponse = NetworkClient.instance.getRepliesByCommentId(commentId = comment.commentId, pageNum = 1, pageSize = 50)
                                val fetchedReplies = if (repliesResponse.isSuccessful && repliesResponse.body()?.code == 200) {
                                    repliesResponse.body()?.data?.list ?: emptyList()
                                } else emptyList()
                                comment.copy(replies = fetchedReplies)
                            } catch (e: Exception) {
                                comment.copy(replies = emptyList())
                            }
                        }
                    }.awaitAll()
                    comments = commentsWithReplies
                }
            } catch (e: Exception) {
                Log.e("Comment", "获取评论异常: ${e.message}")
            }
        }
    }

    // 2. 页面初始化加载逻辑 (修复了无限加载的问题)
    LaunchedEffect(postId) {
        isLoading = true
        try {
            val response = NetworkClient.instance.getCommunityPostsByFilterByPostId(postId)
            if (response.isSuccessful && response.body()?.code == 200) {
                val list = response.body()?.data?.list
                if (!list.isNullOrEmpty()) post = list[0]
            }
            fetchCommentsAndReplies()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // 3. 删除评论/回复的网络请求函数
    fun deleteCommentOrReply(commentId: Int?, replyId: Int?) {
        scope.launch {
            val token = userPreferences.token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" } ?: return@launch
            try {
                val res = NetworkClient.instance.deleteComment(token, commentId, replyId)
                if (res.isSuccessful && res.body()?.code == 200) {
                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                    fetchCommentsAndReplies() // 成功后刷新列表
                } else {
                    Toast.makeText(context, "删除失败: ${res.body()?.msg}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "网络异常，删除失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 4. 提交评论的逻辑
    fun submitComment() {
        if (inputText.isBlank()) return
        val currentInput = inputText
        val target = currentReplyTarget
        inputText = ""
        currentReplyTarget = ReplyTarget.ToPost(postId)
        val currentUser = userPreferences.getNicknameOrGenerate()
        val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val fakeId = (Math.random() * 10000).toInt()

        scope.launch {
            val token = userPreferences.token ?: return@launch
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

            try {
                when (target) {
                    is ReplyTarget.ToPost -> {
                        comments = comments + PostComment(commentId = fakeId, userId = currentUserId, username = currentUser, avatar = userPreferences.avatarPath, content = currentInput, createTime = now, postId = postId, replies = emptyList())
                        val request = AddCommentRequest(postId = postId, content = currentInput)
                        val response = NetworkClient.instance.addComment(authHeader, request)
                        if (response.isSuccessful && response.body()?.code == 200) {
                            Toast.makeText(context, "评论成功", Toast.LENGTH_SHORT).show()
                            val realCommentId = response.body()?.data?.commentId
                            fetchCommentsAndReplies()
                            if (realCommentId != null) {
                                post?.userId?.let { postOwnerId ->
                                    if (postOwnerId != currentUserId) {
                                        launch {
                                            try { NetworkClient.instance.sendCommentNotification(authHeader, SendCommentNotifyRequest(realCommentId, postId, currentInput)) } catch (e: Exception) {}
                                        }
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "评论失败", Toast.LENGTH_SHORT).show()
                            comments = comments.filter { it.commentId != fakeId }
                        }
                    }
                    is ReplyTarget.ToComment -> {
                        comments = comments.map { c ->
                            if (c.commentId == target.commentId) {
                                c.copy(replies = c.replies + PostReply(replyId = fakeId, commentId = target.commentId, fromUserId = currentUserId, fromUserName = currentUser, fromUserAvatar = userPreferences.avatarPath, toUserId = target.targetUserId, toUserName = target.targetUsername, parentReplyId = null, content = currentInput, createTime = now))
                            } else c
                        }
                        val request = AddReplyRequest(commentId = target.commentId, toUserId = target.targetUserId, parentReplyId = null, content = currentInput)
                        val response = NetworkClient.instance.addReply(authHeader, request)
                        if (response.isSuccessful && response.body()?.code == 200) {
                            fetchCommentsAndReplies()
                            val realReplyId = response.body()?.data?.replyId ?: return@launch
                            Toast.makeText(context, "回复成功", Toast.LENGTH_SHORT).show()
                            if (target.targetUserId != currentUserId) {
                                launch {
                                    try { NetworkClient.instance.sendReplyToCommentNotification(authHeader, SendReplyToCommentNotifyRequest(realReplyId, target.commentId, postId, currentInput)) } catch (e: Exception) {}
                                }
                            }
                        } else {
                            Toast.makeText(context, "回复失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is ReplyTarget.ToReply -> {
                        comments = comments.map { c ->
                            if (c.commentId == target.commentId) {
                                c.copy(replies = c.replies + PostReply(replyId = fakeId, commentId = target.commentId, fromUserId = currentUserId, fromUserName = currentUser, fromUserAvatar = userPreferences.avatarPath, toUserId = target.targetUserId, toUserName = target.targetUsername, parentReplyId = target.replyId, content = currentInput, createTime = now))
                            } else c
                        }
                        val request = AddReplyRequest(commentId = target.commentId, toUserId = target.targetUserId, parentReplyId = target.replyId, content = currentInput)
                        val response = NetworkClient.instance.addReply(authHeader, request)
                        if (response.isSuccessful && response.body()?.code == 200) {
                            fetchCommentsAndReplies()
                            val realReplyId = response.body()?.data?.replyId ?: return@launch
                            Toast.makeText(context, "回复成功", Toast.LENGTH_SHORT).show()
                            if (target.targetUserId != currentUserId) {
                                launch {
                                    try { NetworkClient.instance.sendReplyToReplyNotification(authHeader, SendReplyToReplyNotifyRequest(realReplyId, target.commentId, postId, currentInput, target.replyId.toString())) } catch (e: Exception) {}
                                }
                            }
                        } else {
                            Toast.makeText(context, "回复失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "网络异常，发布失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ================== 全新重构的 UI 界面 ==================
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("帖子详情", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F7) // 页面底色：高级灰
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF2196F3))
            } else if (post == null) {
                Text("未找到内容", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            } else {
                val item = post!!
                val hasGlobalDeletePermission = isAdmin || (item.userId == currentUserId)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp) // 为底部输入框留白
                ) {
                    // --- 帖子正文卡片 ---
                    item {
                        Surface(
                            color = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarImage(
                                        path = item.avatar,
                                        modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onUserClick(item.userId) }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        val rawName = item.username ?: "未知用户"
                                        val displayUsername = if (rawName.length > 7) rawName.take(7) + "..." else rawName
                                        Text(displayUsername, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF222222))

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Surface(color = Color(0xFFF0F0F0), shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = item.createTime?.take(16)?.replace("T", " ") ?: "",
                                                color = Color(0xFF777777),
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = item.title ?: "无标题",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1A1A1A),
                                    lineHeight = 30.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = item.content ?: "无内容",
                                    fontSize = 16.sp,
                                    lineHeight = 28.sp,
                                    color = Color(0xFF333333)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (!item.imgUrls.isNullOrEmpty()) {
                                    item.imgUrls.forEach { url ->
                                        AsyncImage(
                                            model = url,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.FillWidth
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- 评论区标题 ---
                    item {
                        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "共 ${comments.size + comments.sumOf { it.replies.size }} 条评论",
                                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF111111)
                            )
                        }
                    }

                    // --- 评论列表 ---
                    items(comments, key = { it.commentId }) { comment ->
                        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                            CommentItemView(
                                comment = comment,
                                currentReplyTarget = currentReplyTarget,
                                inputText = inputText,
                                hasDeletePermission = hasGlobalDeletePermission,
                                onInputTextChange = { inputText = it },
                                onReplyClick = { target ->
                                    currentReplyTarget = if (currentReplyTarget == target) ReplyTarget.ToPost(postId) else target
                                },
                                onUserClick = onUserClick,
                                onSubmit = { submitComment() },
                                onDeleteClick = { cId, rId -> deleteCommentOrReply(cId, rId) }
                            )
                        }
                    }

                    if (comments.isNotEmpty()) {
                        item { Surface(color = Color.White, modifier = Modifier.fillMaxWidth().height(40.dp)) {} }
                    }
                }

                // --- 底部输入区 ---
                if (currentReplyTarget is ReplyTarget.ToPost) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        shadowElevation = 12.dp,
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { if (it.length <= 200) inputText = it },
                                placeholder = { Text("说点什么吧...", color = Color.Gray, fontSize = 14.sp) },
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp, max = 100.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF2F3F5),
                                    unfocusedContainerColor = Color(0xFFF2F3F5),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { submitComment() },
                                enabled = inputText.isNotBlank(),
                                modifier = Modifier.size(44.dp).background(if(inputText.isNotBlank()) Color(0xFF2196F3) else Color(0xFFE0E0E0), CircleShape)
                            ) {
                                // 已经修复了这里的 tint 属性报错
                                Icon(Icons.Default.Send, contentDescription = "发送", tint = Color.White, modifier = Modifier.size(20.dp).padding(end=2.dp, top=2.dp))
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
    onDeleteSuccess: () -> Unit,
    onUserClick: (Int) -> Unit, // 【必须新增此参数】
    onItemClick: (Int) -> Unit,
    listFooter: @Composable () -> Unit = {},
    listState: LazyListState = rememberLazyListState()
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
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(posts, key = { it.postId }) { post ->
                PostCard(
                    post = post,
                    onFavoriteToggle = onFavoriteToggle,
                    onLikeToggle = onLikeToggle,
                    onDeleteSuccess = onDeleteSuccess,
                    onUserClick = onUserClick, // 【最终传递给 PostCard】
                    onClick = { onItemClick(post.postId) }
                )
            }
            item {
                listFooter()
            }
        }
    }
}

@Composable
fun PostCard(
    post: CommunityPostItem,
    onFavoriteToggle: (CommunityPostItem) -> Unit,
    onLikeToggle: (CommunityPostItem) -> Unit,
    onDeleteSuccess: () -> Unit,
    onUserClick: (Int) -> Unit, // 【必须新增此参数】
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    var clickedImageUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val userRole = userPreferences.userRole
    val isAdmin = userRole == "admin" || userRole == "super_admin"

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除此帖子吗？此操作不可撤销，且会向发帖人发送通知。") },
            confirmButton = {
                TextButton(onClick = {
                    performDelete(
                        context = context,
                        post = post,
                        scope = scope,
                        userPreferences = userPreferences,
                        onSuccess = {
                            showDeleteDialog = false
                            onDeleteSuccess()
                        }
                    )
                }) { Text("确认删除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // --- 头部 ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 【修复核心点】：绑定了点击事件，可以跳主页了！
                AvatarImage(
                    path = post.avatar,
                    modifier = Modifier.size(40.dp).clickable { onUserClick(post.userId) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(post.username ?: "未知用户", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = post.createTime?.take(16)?.replace("T", " ") ?: "",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isAdmin) {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "删除",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(post.title ?: "无标题", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(post.content ?: "暂无内容", fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, color = Color.DarkGray)

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
                                .clickable { clickedImageUrl = url },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

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

    if (clickedImageUrl != null) {
        Dialog(
            onDismissRequest = { clickedImageUrl = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { clickedImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = clickedImageUrl,
                    contentDescription = "查看大图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

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
    onPostClick: (Int) -> Unit,
    onUserClick: (Int) -> Unit
) {
    var myPosts by remember { mutableStateOf<List<CommunityPostItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 控制关注列表弹窗显示的状态
    var showFollowList by remember { mutableStateOf(false) }

    // 【新增】：平滑过渡的背景模糊动画 (300毫秒渐变)
    val blurRadius by animateDpAsState(
        targetValue = if (showFollowList) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "blurAnimation"
    )

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

    Box(modifier = Modifier.fillMaxSize()) {

        // --- 第一层：原有的主页面 ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // 【修改】：使用动画计算出的模糊半径
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
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

            // 功能入口区
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    // 我的收藏入口
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToMyCollection() },
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // 关注列表入口
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showFollowList = true },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FavoriteBorder, null, tint = Color(0xFFE91E63))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("关注列表", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Gray)
                        }
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
                items(myPosts, key = { it.postId }) { post ->
                    PostCard(
                        post = post,
                        onDeleteSuccess = { scope.launch { /* 刷新逻辑 */ } },
                        onFavoriteToggle = { /* 收藏逻辑 */ },
                        onLikeToggle = { Toast.makeText(context, "请在社区主页进行点赞操作", Toast.LENGTH_SHORT).show() },
                        onUserClick = onUserClick,
                        onClick = { onPostClick(post.postId) }
                    )
                }
            }
        }

        // --- 第二层：纯净背景覆盖层 ---
        // 【新增】：使用 AnimatedVisibility 包裹弹窗，增加渐显和从中间缩放的效果
        AnimatedVisibility(
            visible = showFollowList,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.8f),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showFollowList = false },
                contentAlignment = Alignment.Center
            ) {
                FollowListOverlay(
                    userPreferences = userPreferences,
                    onClose = { showFollowList = false },
                    onUserClick = onUserClick
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
    hasDeletePermission: Boolean,
    onInputTextChange: (String) -> Unit,
    onReplyClick: (ReplyTarget) -> Unit,
    onUserClick: (Int) -> Unit,
    onSubmit: () -> Unit,
    onDeleteClick: (commentId: Int?, replyId: Int?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp) // 拉大主评论间的呼吸感
    ) {
        // 主评论区
        Row(verticalAlignment = Alignment.Top) {
            AvatarImage(
                path = comment.avatar,
                modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onUserClick(comment.userId) }
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val commentDisplayName = if (comment.username.length > 7) comment.username.take(7) + "..." else comment.username

                // 昵称
                Text(commentDisplayName, fontWeight = FontWeight.Bold, color = Color(0xFF555555), fontSize = 14.sp)

                Spacer(modifier = Modifier.height(4.dp))

                // 评论内容
                Text(comment.content, fontSize = 15.sp, color = Color(0xFF111111), lineHeight = 22.sp)

                // 【优化 4】：时间和操作按钮同行，弱化“回复”和“删除”的视觉侵入感
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.createTime.take(16).replace("T", " "),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasDeletePermission) {
                            CommentActionButton(icon = Icons.Default.DeleteOutline, text = "删除", onClick = { onDeleteClick(comment.commentId, null) })
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        CommentActionButton(icon = Icons.Outlined.Comment, text = "", onClick = {
                            onReplyClick(ReplyTarget.ToComment(comment.commentId, comment.userId, comment.username))
                        })
                    }
                }

                // 动态弹出的内联输入框 (回复主评论)
                InlineReplyInput(
                    isVisible = currentReplyTarget == ReplyTarget.ToComment(comment.commentId, comment.userId, comment.username),
                    targetName = comment.username,
                    text = inputText,
                    onTextChange = onInputTextChange,
                    onSubmit = onSubmit
                )

                // --- 【优化 4】：嵌套回复区 (左侧引线设计) ---
                if (comment.replies.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            // 核心魔法：用 drawBehind 画一条浅灰色的左边框竖线
                            .drawBehind {
                                drawLine(
                                    color = Color(0xFFEEEEEE),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                            .padding(start = 12.dp) // 文字与引线的距离
                    ) {
                        comment.replies.forEachIndexed { index, reply ->
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = if (index == comment.replies.lastIndex) 0.dp else 16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarImage(
                                        path = reply.fromUserAvatar,
                                        modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onUserClick(reply.fromUserId) }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    val fromDisplayName = if (reply.fromUserName.length > 7) reply.fromUserName.take(7) + "..." else reply.fromUserName
                                    val toDisplayName = if (reply.toUserName.length > 7) reply.toUserName.take(7) + "..." else reply.toUserName

                                    Text(fromDisplayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF666666))
                                    Text(" 回复 ", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                                    Text(toDisplayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF666666))
                                }

                                Text(
                                    text = reply.content,
                                    fontSize = 14.sp,
                                    color = Color(0xFF333333),
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(start = 26.dp, top = 4.dp)
                                )

                                // 子回复的时间与操作
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 26.dp, top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = comment.createTime.take(16).replace("T", " "),
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (hasDeletePermission) {
                                            CommentActionButton(icon = Icons.Default.DeleteOutline, text = "删除", onClick = { onDeleteClick(null, reply.replyId) })
                                            Spacer(modifier = Modifier.width(16.dp))
                                        }
                                        CommentActionButton(icon = Icons.Outlined.Comment, text = "", onClick = {
                                            onReplyClick(ReplyTarget.ToReply(comment.commentId, reply.replyId, reply.fromUserId, reply.fromUserName))
                                        })
                                    }
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

// 【新增】：用于统一弱化操作按钮的辅助组件
@Composable
fun CommentActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF999999), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, color = Color(0xFF999999), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    targetUserId: Int,
    userPreferences: UserPreferences,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit = {}, // 【新增】用于跳转到帖子详情
    onUserClick: (Int) -> Unit = {}  // 【新增】传递用户点击事件
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowing by remember { mutableStateOf(false) }
    var isActionLoading by remember { mutableStateOf(false) } // 防止重复点击

    // 【新增】TA的帖子列表状态
    var userPosts by remember { mutableStateOf<List<CommunityPostItem>>(emptyList()) }
    var isPostsLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = userPreferences.token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" } ?: ""

    // 【修改】并发加载用户资料和TA的帖子
    LaunchedEffect(targetUserId) {
        isLoading = true
        isPostsLoading = true
        try {
            // 使用 async 并发请求，提高加载速度
            val profileDeferred = async { NetworkClient.instance.getUserProfile(token, targetUserId) }
            val postsDeferred = async { NetworkClient.instance.getCommunityPostsByFilter(pageNum = 1, pageSize = 50, postUserId = targetUserId) }

            // 1. 处理个人资料响应
            val res = profileDeferred.await()
            if (res.isSuccessful && res.body()?.code == 200) {
                profile = res.body()?.data
                isFollowing = res.body()?.data?.isFollow ?: false
            }

            // 2. 处理帖子列表响应
            val postsRes = postsDeferred.await()
            if (postsRes.isSuccessful && postsRes.body()?.code == 200) {
                userPosts = postsRes.body()?.data?.list ?: emptyList()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "加载数据异常", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
            isPostsLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户主页") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else profile?.let { data ->
            // 【修改】将 Column 替换为 LazyColumn 以优化长列表性能
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 40.dp) // 底部留白
            ) {
                // ================= 1. 头部资料卡片 =================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AvatarImage(path = data.userInfoVO.avatar, modifier = Modifier.size(80.dp).clip(CircleShape))
                            Spacer(Modifier.height(12.dp))
                            Text(data.userInfoVO.username, fontWeight = FontWeight.Bold, fontSize = 20.sp)

                            // 角色标签
                            Surface(
                                color = if (data.role == "admin") Color(0xFFFFEBEE) else Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    if (data.role == "admin") "管理员" else "普通用户",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = if (data.role == "admin") Color.Red else Color(0xFF1976D2),
                                    fontSize = 12.sp
                                )
                            }

                            Text(data.userInfoVO.userSignature ?: "这个人很懒，什么都没写~", color = Color.Gray, fontSize = 14.sp)

                            Spacer(Modifier.height(16.dp))

                            // 关注按钮逻辑 (不能关注自己)
                            if (data.userInfoVO.userId.toInt() != userPreferences.userId) {
                                Button(
                                    onClick = {
                                        if (isActionLoading) return@Button
                                        isActionLoading = true
                                        scope.launch {
                                            try {
                                                val res = if (isFollowing) {
                                                    NetworkClient.instance.cancelFollowUser(token, targetUserId)
                                                } else {
                                                    NetworkClient.instance.followUser(token, targetUserId)
                                                }

                                                if (res.isSuccessful && res.body()?.code == 200) {
                                                    isFollowing = !isFollowing
                                                    Toast.makeText(context, if (isFollowing) "关注成功" else "已取消关注", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val errorMsg = res.body()?.msg ?: "未知错误"
                                                    Toast.makeText(context, "操作失败: $errorMsg", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "操作异常，请检查网络", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isActionLoading = false
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) Color.LightGray else Color(0xFF2196F3)
                                    )
                                ) {
                                    if (isActionLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                    } else {
                                        Text(
                                            text = if (isFollowing) "取消关注" else "+ 关注",
                                            color = if (isFollowing) Color.DarkGray else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ================= 2. 统计数据模块 =================
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox("帖子", data.communityStatisticDto.postNum, Modifier.weight(1f))
                        StatBox("获赞", data.communityStatisticDto.getLikeNum, Modifier.weight(1f))
                        StatBox("粉丝", data.communityStatisticDto.fansNum, Modifier.weight(1f))
                    }
                }

                // ================= 3. 志愿者资料模块 =================
                data.volunteerDataVO?.let { vol ->
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("志愿者信息", Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                InfoRow("年级", vol.grade ?: "未知")
                                InfoRow("完成率", "${(vol.finishRate * 100).toInt()}%")
                                InfoRow("联系方式", vol.contactNumber ?: "未提供")
                            }
                        }
                    }
                }

                // ================= 4. 底部其他信息 =================
                item {
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("最后登录: ${data.lastLoginTime ?: "未知"}", color = Color.Gray, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("主页访问量: ${data.visitedNum}", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                // ================= 5. 【新增】TA的帖子展示区域 =================
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "TA的帖子",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }

                if (isPostsLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (userPosts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("暂无发布内容", color = Color.Gray)
                        }
                    }
                } else {
                    // 使用 items 循环渲染该用户的帖子
                    items(userPosts, key = { it.postId }) { post ->
                        PostCard(
                            post = post,
                            onFavoriteToggle = { /* 在别人主页一般不直接处理收藏逻辑，或者可接入你的已有逻辑 */ },
                            onLikeToggle = { Toast.makeText(context, "请在社区主页进行点赞操作", Toast.LENGTH_SHORT).show() },
                            onDeleteSuccess = { /* 管理员删除后的刷新逻辑可在这里补充 */ },
                            onUserClick = onUserClick,
                            onClick = { onPostClick(post.postId) }
                        )
                    }
                }
            }
        }
    }
}

// ============== UI 辅助组件 ==============
@Composable
fun StatBox(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF333333))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 15.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Color(0xFF333333))
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

// 3. 将你的代码放在文件底部（或者 Composable 外部）
// 3. 将你的代码放在文件底部（或者 Composable 外部）
private fun performDelete(
    context: Context, // 显式传入 context 以解决 Intent 和 Toast 报错
    post: CommunityPostItem,
    scope: CoroutineScope,
    userPreferences: UserPreferences, // 使用已有的 userPreferences 获取 token
    onSuccess: () -> Unit
) {
    // 获取 token 并确保格式正确
    val token = userPreferences.token?.let {
        if (it.startsWith("Bearer ")) it else "Bearer $it"
    } ?: ""

    scope.launch {
        try {
            // 步骤 1: 调用删帖接口
            val deleteRes = NetworkClient.instance.deletePost(token, post.postId)

            if (deleteRes.isSuccessful) {
                // 步骤 2: 发送通知（作为次要操作，不影响跳转）
                launch {
                    try {
                        NetworkClient.instance.sendDeleteNotification(
                            token,
                            SendDeleteNotifyRequest(
                                title = "帖子删除通知",
                                content = "您的帖子《${post.title}》因违反社区规定已被管理员删除。",
                                receiverId = post.userId
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("PostDelete", "发送通知失败: ${e.message}")
                    }
                }

                // 切换到主线程执行 UI 操作和跳转
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "帖子已成功删除", Toast.LENGTH_SHORT).show()

                    // 步骤 3: 这里的 onSuccess() 在调用处应该指向具体的返回/刷新逻辑
                    // 比如在 Composable 中传入的是 { onBack() }
                    onSuccess()
                }
            } else {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "删除失败: ${deleteRes.message()}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("PostDelete", "删除请求异常: ${e.message}")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "网络错误，删除失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListOverlay(
    userPreferences: UserPreferences,
    onClose: () -> Unit,
    onUserClick: (Int) -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<FollowUserItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val token = userPreferences.token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" } ?: ""

    // 当 keyword 改变时，延迟 500ms 后自动请求接口 (防抖设计)
    LaunchedEffect(keyword) {
        isLoading = true
        kotlinx.coroutines.delay(500)
        try {
            val res = NetworkClient.instance.getFollowList(token, 1, 50, keyword, 1)
            if (res.isSuccessful && res.body()?.code == 200) {
                users = res.body()?.data?.list ?: emptyList()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "获取关注列表失败", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // 主卡片 UI
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f) // 占据屏幕 85% 宽度
            .fillMaxHeight(0.7f) // 占据屏幕 70% 高度
            // 消费掉卡片内部的点击事件，防止点到卡片上导致弹窗关闭
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {},
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // 顶部标题和关闭按钮
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("我的关注", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "关闭") }
            }

            // 搜索框
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = { Text("搜索用户名...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // 用户列表展示
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (users.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("未找到相关用户", color = Color.Gray) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(users, key = { it.userId }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    onClose() // 先关闭弹窗
                                    onUserClick(user.userId) // 跳转到主页
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarImage(path = user.avatar ?: "", modifier = Modifier.size(40.dp).clip(CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(user.username, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(user.userSignature ?: "这个人很懒，什么都没写~", color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
