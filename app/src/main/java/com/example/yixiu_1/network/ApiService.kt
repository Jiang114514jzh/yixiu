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
import retrofit2.http.DELETE
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
    val isRead: Int? = null,            // 恢复为 Int
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

// 1. 新增：维修分配信息的数据类
data class RepairAssignment(
    val assignId: Int,
    val requestId: Int,
    val volunteerId: Int,
    val volunteerName: String?,
    val majorClass: String?,
    val grade: String?,
    val contactType: Int?,
    val contactNumber: String?,
    val avatar: String?,
    val isLeader: Int?,
    val assignedTime: String?,
    val status: Int?,
    val remarks: String?,
    val updateTime: String?
)

// 2. 修改：原本的 RepairTaskItem 类
data class RepairTaskItem(
    val requestId: Int,
    val userId: Int,
    val username: String,
    val realName: String?,
    val contactType: String?,
    val contactInfo: String?,
    val deviceType: String?,
    val deviceSystem: String?,
    val deviceModel: String?,
    val problemDescription: String?,
    val campus: String?,
    val repairLocation: String?,
    val appointmentTime: String?,
    val remarks: String?,
    val status: Int,
    val imgUrl: List<String>?,
    // 【核心新增字段】：接收后端的分配列表
    val repairAssignment: List<RepairAssignment>? = null,
    val repairLog: List<RepairLog>? = null,
    val createTime: String?,
    val updateTime: String?,
    val completeTime: String?        // 【重要】：完成时间
)


// 1. 新增结单日志数据类 (字段名请根据你后端的实际情况调整)
data class RepairLog(
    val logId: Int?,
    val requestId: Int,
    val volunteerId: Int,
    val volunteerName: String?,
    val logContent: String?, // 故障原因
    val repairDuration: String?,    // 解决方案
    val solutionSummary: String?,     // 备注
    val uploadTime: String?,
    val importStatus: Int?,
    val logImgUrl: List<String>?
)


// 在 ApiService.kt 中找到这部分并修改
data class RepairTaskListData(
    val total: Int,
    val list: List<RepairTaskItem>,
    val pageNum: Int,
    val pageSize: Int,
    val pages: Int // 【新增】加入总页数字段，对应后端返回的 "pages": 3
)

data class MarkReadRequest(
    val notifyId: Int
)

data class UserUpdateRequest(
    val username: String,
    val realName: String
)

data class VolunteerUpdateRequest(
    val userId: Int,
    val studentNumber: String,
    val majorClass: String,
    val grade: String,
    val contactType: Int,
    val contactNumber: String
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

    // 修改基本信息 (PUT)
    @PUT("/api/v1/users/userInfo")
    suspend fun updateBasicInfo(@Body request: UserUpdateRequest): ApiResponse<Any>

    // 修改志愿者信息 (PUT 或 POST，根据后端定义，这里默认为修改操作)
    @PUT("/api/v1/volunteer/info")
    suspend fun updateVolunteerInfo(@Body request: VolunteerUpdateRequest): ApiResponse<Any>

    @POST("/api/v1/users/loginByEmail")
    suspend fun loginByEmail(@Body body: EmailRegisterOrLoginRequest): Response<ApiResponse<Any>>

    @POST("/api/v1/admin/loginByEmail")
    suspend fun AdminloginByEmail(@Body body: EmailRegisterOrLoginRequest): Response<ApiResponse<Any>>

    // --- 报修任务相关接口 ---
    @POST("/api/v1/task/add")
    suspend fun submitRepairTask(@Body task: RepairTaskRequest): Response<ApiResponse<Any>>

    // 【新增】：获取当前用户的维修历史记录
    @GET("/api/v1/task/getByUserId")
    suspend fun getMyRepairHistory(
        @Header("Authorization") token: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<ApiResponse<RepairTaskListData>>

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
    // 在 ApiService.kt 的 interface ApiService 中
    @GET("/api/v1/task/getAll")
    suspend fun getAllTasks(
        @Header("Authorization") token: String,
        @Query("pageNum") pageNum: Int,    // 【新增】
        @Query("pageSize") pageSize: Int   // 【新增】
    ): Response<ApiResponse<RepairTaskListData>>
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
    ): Response<ApiResponse<AddCommentResponseData>>

    // 2. 添加回复 (包括回复主评论、回复子评论)
    @POST("/api/v1/community/comment/addReply")
    suspend fun addReply(
        @Header("Authorization") token: String,
        @Body request: AddReplyRequest
    ): Response<ApiResponse<AddReplyResponse>>

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

    @DELETE("/api/v1/community/post/delete")
    suspend fun deletePost(
        @Header("Authorization") token: String,
        @Query("postId") postId: Int
    ): Response<ApiResponse<Any>>

    // 发送删除通知
    @POST("/api/v1/notify/systemToUser")
    suspend fun sendDeleteNotification(
        @Header("Authorization") token: String,
        @Body request: SendDeleteNotifyRequest
    ): Response<ApiResponse<Any>>

    @GET("/api/v1/volunteer/infoListExcludeUserId")
    suspend fun getVolunteerList(
        @Header("Authorization") token: String,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): Response<ApiResponse<VolunteerPageResponse>> // 引用当前文件的类

    @PUT("/api/v1/admin/volunteerInfo")
    suspend fun updateVolunteerInfo(
        @Header("Authorization") token: String,
        @Body info: UpdateVolunteerRequest
    ): Response<ApiResponse<Any>>

    @PUT("/api/v1/task/updateStatus")
    suspend fun updateTaskStatus(
        @Header("Authorization") token: String,
        @Body request: TaskStatusUpdateRequest
    ): Response<ApiResponse<Any>>

    // 2. 在 interface ApiService 中添加接口
    @POST("/api/v1/task/addAssign")
    suspend fun addAssign(
        @Header("Authorization") token: String,
        @Body request: AddAssignRequest
    ): Response<ApiResponse<Any>>

    @GET("/api/v1/ai/allKnowledge") // 请根据真实后端路径修改
    suspend fun getKnowledgeList(@Header("Authorization") token: String): Response<KnowledgeResponse>

    @POST("/api/v1/task/addLog") // 请替换为实际的结单接口路径
    suspend fun completeRepairTask(
        @Header("Authorization") token: String,
        @Body request: CompleteTaskRequest
    ): Response<ApiResponse<Any>>

    @POST("/api/v1/ai/knowledge")
    suspend fun addKnowledge(
        @Header("Authorization") token: String,
        @Body request: AddKnowledgeRequest
    ): Response<ApiResponse<Any>>

    @POST("/api/v1/notify/comment")
    suspend fun sendCommentNotification(
        @Header("Authorization") token: String,
        @Body request: SendCommentNotifyRequest
    ): Response<ApiResponse<AddCommentResponseData>>

    @POST("/api/v1/notify/replyToReply")
    suspend fun sendReplyToReplyNotification(
        @Header("Authorization") token: String,
        @Body request: SendReplyToReplyNotifyRequest
    ): Response<ApiResponse<Any>>

    //回复评论
    @POST("/api/v1/notify/replyToComment")
    suspend fun sendReplyToCommentNotification(
        @Header("Authorization") token: String,
        @Body request: SendReplyToCommentNotifyRequest
    ): Response<ApiResponse<Any>>

    //删除知识库内容
    @DELETE("/api/v1/ai/deleteKnowledge")
    suspend fun deleteKnowledge(
        @Header("Authorization") token: String,
        @Query("knowledgeId") knowledgeId: Int
    ): Response<ApiResponse<Any>>

    //修改知识库内容
    @PUT("/api/v1/ai/updateKnowledge")
    suspend fun updateKnowledge(
        @Header("Authorization") token: String,
        @Body request: UpdateKnowledgeRequest
    ): Response<ApiResponse<Any>>

    @POST("/api/v1/task/applyToJoin")
    suspend fun addJoinRequest(
        @Header("Authorization") token: String,
        @Body request: AddJoinRequest
    )

    @PUT("/api/v1/task/approveTaskApply")
    suspend fun approveJoinRequest(
        @Header("Authorization") token: String,
        @Body request: ApproveJoinRequest
    ): Response<ApiResponse<Any>>

    @PUT("/api/v1/task/rejectTaskApply")
    suspend fun rejectJoinRequest(
        @Header("Authorization") token: String,
        @Body request: RejectJoinRequest
    ): Response<ApiResponse<Any>>

    @GET("api/v1/task/getMyTaskByVolunteerId")
    suspend fun getMyTaskByVolunteerId(
        @Header("Authorization") token: String,
        @Query("pageNum") pageNum: Int,
        @Query("pageSize") pageSize: Int
    ): Response<ApiResponse<RepairTaskListData>>

    //关注用户
    @Multipart
    @POST("/api/v1/users/follow")
    suspend fun followUser(
        @Header("Authorization") token: String,
        @Part("followeeId") followeeId: Int // 【关键】：这里必须从 @Field 改为 @Part
    ): Response<ApiResponse<Any>>

    // 取消关注用户 (完美还原 Apifox 的 form-data 格式)
    @Multipart
    @POST("/api/v1/users/cancelFollow")
    suspend fun cancelFollowUser(
        @Header("Authorization") token: String,
        @Part("followeeId") followeeId: Int // 【关键】：这里必须从 @Field 改为 @Part
    ): Response<ApiResponse<Any>>

    @GET("/api/v1/users/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String,
        @Query("userId") userId: Int
    ): Response<ApiResponse<UserProfile>>

    //获取关注列表
    @GET("/api/v1/users/followListByFilter")
    suspend fun getFollowList(
        @Header("Authorization") token: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("keyword") keyword: String = "",
        @Query("status") status: Int = 1
    ): Response<ApiResponse<FollowPage>>

    // 删除评论或回复
    @FormUrlEncoded
    @PUT("/api/v1/community/comment/delete")
    suspend fun deleteComment(
        @Header("Authorization") token: String,
        @Field("commentId") commentId: Int? = null,
        @Field("replyId") replyId: Int? = null
    ): Response<ApiResponse<Any>>

    // 2. 在 ApiService 接口中添加方法 (注意替换为你实际的返回类型 ApiResponse)
    @POST("/api/v1/task/addEvaluation")
    suspend fun addEvaluation(
        @Header("Authorization") token: String,
        @Body request: AddEvaluationRequest
    ): Response<ApiResponse<Any>> // 返回 Any 防止 Gson 解析 null 报错

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

data class VolunteerDetail(
    val volunteerId: Int,
    val userId: Int,
    val studentNumber: String?,
    val majorClass: String?,
    val grade: String?,
    val status: Int,
    val contactType: Int?,
    val contactNumber: String?
)

// 3. 用户与志愿者信息包装
data class VolunteerUserItem(
    val userId: Int,
    val username: String,
    val realName: String?,
    val email: String?,
    val avatar: String?,
    val role: String,
    val volunteerInfo: VolunteerDetail?
)

// 通知请求体
data class SendDeleteNotifyRequest(
    val title: String,
    val receiverId: Int,
    val content: String
)

// 4. 列表响应
data class VolunteerPageResponse(
    val list: List<VolunteerUserItem>,
    val total: Int,
    // 如果需要其他分页字段可以继续添加
)

data class TaskStatusUpdateRequest(
    val requestId: Int,
    val status: Int
)

// 1. 添加接取任务的请求体 (可以放在文件末尾或其他数据类聚集的地方)
data class AddAssignRequest(
    val requestId: Int,
    val volunteerId: Int,
    val remarks: String = "无" // 默认备注为空或"无"
)

// 对应 JSON 中的 "data" 里面的每一项
data class KnowledgeItem(
    val knowledgeId: Int,
    val sourceType: Int?,       // JSON中有数字 3 等
    val sourceId: String?,      // JSON中有 null 或者 "log_19"，所以必须是 String?
    val problem: String,
    val solution: String,
    val status: Int,
    val hitCount: Int?,         // JSON中有 null，所以必须是 Int?
    val createTime: String?,    // 例如 "2026-02-05T13:54:32.000+00:00"
    val updateTime: String?     // 替换了之前的 uploadTime
)

data class KnowledgeResponse(
    val code: Int,
    val msg: String,
    val data: List<KnowledgeItem>
)

// 1. 定义完成任务的请求体 (假设字段为：故障原因、解决方案、备注)
data class CompleteTaskRequest(
    val requestId: Int,
    val volunteerId: Int,
    // 下面这三个字段请替换为你“图中”实际需要的字段名
    val logContent: String,
    val repairDuration: String,
    val solutionSummary: String
)

data class AddKnowledgeRequest(
    val problem: String,
    val solution: String,
    val sourceType : Int,
    val sourceId : String
)

data class SendCommentNotifyRequest(
    val commentId: Int,
    val postId : Int,
    val commentContent : String
)
data class AddCommentResponseData(
    val commentId: Int
)

data class AddReplyResponse(
    val replyId: Int
)

data class SendReplyToReplyNotifyRequest(
    val replyId: Int,
    val commentId : Int,
    val postId : Int,
    val replyContent : String,
    val parentReplyId : String
)

data class SendReplyToCommentNotifyRequest(
    val replyId: Int,
    val commentId : Int,
    val postId : Int,
    val replyContent : String
)

data class UpdateKnowledgeRequest(
    val knowledgeId: Int,
    val problem: String,
    val solution: String,
    val status : Int
)

data class AddJoinRequest(
    val volunteerId: Int,
    val requestId: Int
)

data class ApproveJoinRequest(
    val assignId: Int
)

data class RejectJoinRequest(
    val assignId: Int,
    val reason: String
)

// 1. 定义请求体数据类
data class AddEvaluationRequest(
    val requestId: Int,
    val content: String,
    val score: Int
)

data class UpdateVolunteerRequest(
    val userId: Int,
    val realName: String?,
    val studentNumber: String?,
    val majorClass: String?,
    val grade: String?,
    val status: Int,
    val role: String?
)