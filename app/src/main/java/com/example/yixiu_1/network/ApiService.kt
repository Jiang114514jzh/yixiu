package com.example.yixiu_1.network

import okhttp3.MultipartBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

// 1. 【恢复】使用你原先定义的 NotifyItem (Int 和 String)
data class NotifyItem(
    val notifyId: Int,          // 恢复为 Int
    val senderId: Int,          // 恢复为 Int
    val receiverId: Int,        // 恢复为 Int
    val title: String,          // 恢复为 String (不可空)
    val content: String,        // 恢复为 String (不可空)
    val link: String?,
    val type: String,           // 恢复为 String
    val isRead: Int,            // 恢复为 Int
    val senderUsername: String?,
    val senderAvatar: String?,
    val createTime: String      // 恢复为 String
)

// 2. 【新增】分页包裹类 (用于解决 data -> list 的嵌套问题)
// 必须包含 hasNextPage 以解决代码中的报错
data class NotifyPage(
    val total: Int,             // 改为 Int 以匹配你的习惯
    val list: List<NotifyItem>, // 这里包裹你的 NotifyItem
    val pageNum: Int,
    val pageSize: Int,
    val pages: Int,
    val hasNextPage: Boolean    // 【关键】必须加上这个字段，否则 Unresolved reference
)

data class VolunteerRegisterRequest(
    val email: String,
    val role: String,
    val verificationCode: Int,
    val inviteCode: Int
)

// ==================== 数据模型更新 ====================

//1. 对应 JSON 中 data 里面的 list 的每一项
data class RepairTaskItem(
    val requestId: Int,             // 申请编号 (原 id)
    val userId: Int,
    val username: String,           // 用户账号
    val realName: String?,          // 真实姓名 (上传用户)
    val contactType: String?,
    val contactInfo: String?,
    val deviceType: String?,
    val deviceSystem: String?,
    val deviceModel: String?,
    val problemDescription: String?, // 问题描述
    val campus: String?,
    val repairLocation: String?,     // 维修地点
    val appointmentTime: String?,    // 预约时间
    val remarks: String?,
    val status: Int,                 // 状态 0-7
    val imgUrl: List<String>?,       // 图片可能是一个列表
    val createTime: String?,         // 创建时间
    val updateTime: String?,
    val completeTime: String?
)

// 2. 对应 JSON 中的 data 对象 (包含 list 和分页信息)


data class RepairTaskListData(
    val total: Int,
    val list: List<RepairTaskItem>,
    val pageNum: Int,
    val pageSize: Int
)

data class MarkReadRequest(
    val notifyId: Int
)
//============ 接口定义 ============
interface ApiService {
    // --- 登录注册相关接口 ---
    @GET("/api/v1/send/emailVerification")
    suspend fun sendEmailVerification(@Query("email") email: String): Response<ApiResponse<Any>>

    @POST("/api/v1/users/registerByEmail")
    suspend fun registerByEmail(@Body body: EmailRegisterOrLoginRequest): Response<ApiResponse<Any>>

    @PUT("/api/v1/users/userInfo")
    suspend fun updateUserInfo(
        @Body request: UpdateUserInfoRequest
    ): Response<ApiResponse<Any>>

    @POST("/api/v1/users/loginByEmail")
    suspend fun loginByEmail(@Body body: EmailRegisterOrLoginRequest): Response<ApiResponse<Any>>

    // --- 报修任务相关接口 ---
    @POST("/api/v1/task/add")
    suspend fun submitRepairTask(@Body task: RepairTaskRequest): Response<ApiResponse<Any>>

    @GET("/api/v1/users/userInfo")
    suspend fun getUserInfo(): Response<ApiResponse<UserInfo>>

    @Multipart
    @PUT("/api/v1/users/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): Response<ApiResponse<Any>>

    @GET("/api/v1/notify/list")
    suspend fun getNotifications(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): Response<ApiResponse<NotifyPage>>
    @POST("/api/v1/volunteer/registerByEmail")
    suspend fun registerVolunteer(@Body body: VolunteerRegisterRequest): Response<ApiResponse<Any>>
    @GET("/api/v1/task/getAll")
    // ✅ 正确：返回 ApiResponse 包装类
    suspend fun getAllTasks(@Header("Authorization") token: String): Response<ApiResponse<RepairTaskListData>>

    @GET("/api/v1/notify/poll")
    suspend fun pollNotifications(@Header("Authorization") token: String): Response<PollResponse>

    @PUT("/api/v1/notify/changeToRead")
    suspend fun markNotificationRead(
        @Header("Authorization") token: String, @Query("notifyId") notifyId: Int): Response<ApiResponse<Any>>

    @POST("/api/v1/community/post/modifyLike")
    suspend fun toggleLike(
        @Header("Authorization") token: String, @Query("postId") notifyId: Int): Response<ApiResponse<Any>>

    @GET("/api/v1/community/post/list")
    suspend fun getCommunityPosts(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): Response<ApiResponse<CommunityPostPage>>

    @POST("/api/v1/community/post/create")
    suspend fun createPost(
        @Body request: CreatePostRequest
    ): Response<ApiResponse<CreatePostData>>

    @Multipart
    @POST("/api/v1/community/post/uploadPostImg")
    suspend fun uploadPostImage(
        @Part parts: List<MultipartBody.Part>,
        @Part("postId") postId: RequestBody
    ): Response<ApiResponse<UploadPostImageResult>>

    @GET("/api/v1/community/post/listByFilter")
    suspend fun getCommunityPostsByFilter(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int,
        @Query("postUserId") postUserId: Int // 筛选特定用户的帖子
    ): Response<ApiResponse<CommunityPostPage>>

    @GET("/api/v1/community/post/listByFilter")
    suspend fun getCommunityPostsByFilterByPostId(
        @Query("postId") postId: Int,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 1
    ): Response<ApiResponse<CommunityPostPage>>

    // 【新增】收藏/取消收藏帖子 (根据 Apifox 截图 image_20e064.png)
    @FormUrlEncoded
    @POST("/api/v1/community/post/modifyFavorite")
    suspend fun toggleFavorite(
        @Header("Authorization") token: String,
        @Field("postId") postId: Int
    ): Response<ApiResponse<Any>>

    // 【新增】获取我的收藏列表 (根据 Apifox 截图 image_2148fe.png)
    @GET("/api/v1/community/post/getFavoritePostInfo")
    suspend fun getFavoritePosts(
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): Response<ApiResponse<FavoritePostPage>>

    // 1. 添加主评论
    @POST("/api/v1/community/comment/add")
    suspend fun addComment(
        @Header("Authorization") token: String,
        @Body request: AddCommentRequest
    ): Response<ApiResponse<Any>>

    // 2. 添加回复 (包括回复主评论、回复子评论)
    @POST("/api/v1/community/comment/addReply")
    suspend fun addReply(
        @Header("Authorization") token: String,
        @Body request: AddReplyRequest
    ): Response<ApiResponse<Any>>

    // 3. 点赞评论/回复 (预留)
    @POST("/api/v1/community/comment/modifyCommentLike")
    suspend fun modifyCommentLike(
        @Header("Authorization") token: String,
        @Body request: ModifyCommentLikeRequest
    ): Response<ApiResponse<Any>>

    // 获取帖子的评论列表
    @GET("/api/v1/community/comment/listByPostId")
    suspend fun getCommentsByPostId(
        @Query("postId") postId: Int,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("commentId") commentId: Int = 0
    ): Response<ApiResponse<CommentPage>>

    // 获取某条评论下的回复列表
    @GET("/api/v1/community/comment/replyListByCommentId")
    suspend fun getRepliesByCommentId(
        @Query("commentId") commentId: Int,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<ReplyPage>>
}

//============ Retrofit 客户端实例 ============
object NetworkClient {
    private const val BASE_URL = "http://8.148.253.180:8080"
    private var token: String? = null

    // 公开的方法，用于在登录后更新 token
    fun setToken(newToken: String?) {
        this.token = newToken
        // 重新构建 Retrofit 实例以应用新的 token
        _instance = createInstance()
    }

    // 认证拦截器，动态添加 token
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        token?.let {
            requestBuilder.header("Authorization", it)
        }
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor) // 确保认证拦截器被添加
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // 懒加载的 Retrofit 实例
    private var _instance: ApiService = createInstance()
    val instance: ApiService
        get() = _instance

    private fun createInstance(): ApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(ApiService::class.java)
    }
}

data class PollResponse(
    val type: String,   // "BROADCAST", "SYSTEM", "USER", "NONE"
    val unread: Int     // 未读数量
)

data class CommunityTag(
    val tagId: Int,
    val tagName: String
)

// 2. 单个帖子数据
data class CommunityPostItem(
    val postId: Int,
    val userId: Int,
    val username: String,
    val avatar: String?,
    val userSignature: String?,
    val title: String,
    val content: String,
    val imgUrls: List<String>?, // 图片列表可能为空
    val tags: List<CommunityTag>?, // 标签可能为空
    val status: Int,
    val likeNum: Int,
    val commentNum: Int,
    val viewNum: Int,
    val favoriteNum: Int,
    val isLiked: Int,    // 0 或 1
    val isFavorite: Int, // 0 或 1
    val createTime: String,
    val updateTime: String?
)

// 3. 帖子列表的分页包裹类 (对应 JSON 中的 data)
data class CommunityPostPage(
    val total: Int,
    val list: List<CommunityPostItem>,
    val pageNum: Int,
    val pageSize: Int,
    val pages: Int,
    val hasNextPage: Boolean
)

// 1. 发帖请求体
data class CreatePostRequest(
    val title: String,
    val content: String,
    val tagIdList: List<Int>
)

// 2. 发帖成功后返回的 Data 数据 (包含 postId)
data class CreatePostData(
    val postId: Int
)

data class UploadPostImageResult(
    val logImgUrls: List<String>
)

// 专门用于解析收藏列表的 Item
data class FavoritePostItem(
    val postId: Int,
    val postUserId: Int,       // 注意这里是 postUserId
    val postUserAvatar: String?, // 注意这里是 postUserAvatar
    val title: String?,
    // 注意：接口返回中没有 username, content, imgUrls, 也没有统计数据(likeNum等)
    val tags: List<CommunityTag>?,
    val createTime: String?
)

// 专门用于解析收藏列表的分页数据
data class FavoritePostPage(
    val total: Int,
    val list: List<FavoritePostItem>,
    val pageNum: Int,
    val pageSize: Int,
    val pages: Int,
    val hasNextPage: Boolean
)

data class AddCommentRequest(
    val postId: Int,
    val content: String
)

data class AddReplyRequest(
    val commentId: Int,
    val toUserId: Int,
    val parentReplyId: Int?, // 如果是直接回复主评论，传 null
    val content: String
)

data class ModifyCommentLikeRequest(
    val commentId: Int?,
    val replyId: Int?
)

// ================== 评论与回复的实体类 (用于 ApiService) ==================

data class PostComment(
    val commentId: Int,
    val postId: Int,
    val userId: Int,
    val username: String,
    val avatar: String?,
    val content: String,
    val createTime: String,
    val likeNum: Int = 0,
    val replyNum: Int = 0,
    val isLike: Int = 0,
    // 前端组装使用的嵌套列表，用于装载并发获取到的二级回复
    val replies: List<PostReply> = emptyList()
)

data class PostReply(
    val replyId: Int,
    val commentId: Int,
    val fromUserId: Int,
    val fromUserName: String,
    val fromUserAvatar: String?,
    val toUserId: Int,
    val toUserName: String,
    val parentReplyId: Int?,
    val content: String,
    val createTime: String,
    val likeNum: Int = 0,
    val isLike: Int = 0
)

data class CommentPage(
    val total: Int,
    val list: List<PostComment>
)

data class UpdateUserInfoRequest(
    val username: String
)
data class ReplyPage(
    val total: Int,
    val list: List<PostReply>
)