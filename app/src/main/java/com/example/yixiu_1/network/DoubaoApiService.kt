package com.example.yixiu_1.network

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

import com.example.yixiu_1.network.DoubaoChatRequest
import com.example.yixiu_1.network.DoubaoMessage
import com.example.yixiu_1.network.NetworkClient

private const val DEFAULT_MODEL = "doubao-seed-1-6-lite-251015"
private const val MAX_TOKENS = 65535

/**
 * 豆包API调用相关数据模型
 */
data class DoubaoChatRequest(
    val model: String,
    val max_completion_tokens: Int,
    val messages: List<DoubaoMessage>,
    val reasoning_effort: String? = null
)

// 修改 DoubaoMessage 类的 content 字段定义
data class DoubaoMessage(
    @SerializedName("content")
    val content: Any?, // 使用 Any? 来接收字符串或数组
    val role: String?
)

data class ChatSessionData(
    // 必须确保这里的变量名和 JSON 里的 key 完全一致
    val conversationId: Int
)


data class DoubaoContent(
    val type: String,
    val text: String? = null,
    val image_url: DoubaoImageUrl? = null
)

data class DoubaoImageUrl(
    val url: String
)

data class DoubaoChatResponse(
    val id: String,
    val choices: List<DoubaoChoice>,
    val created: Long,
    val model: String,
    val system_fingerprint: String,
    val usage: DoubaoUsage,
    @SerializedName("object") val objType: String
)

data class DoubaoChoice(
    val index: Int,
    val message: DoubaoMessage,
    val finish_reason: String
)

data class DoubaoUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

/**
 * 对应后端返回的最外层包装
 */
data class HistoryResponse(
    val code: Int,
    val msg: String,
    val data: HistoryPageData? // 注意：这里 data 对应的是包含 list 的对象
)

/**
 * 对应 JSON 中的 "data" 字段，包含分页信息
 */
data class HistoryPageData(
    val total: Int,
    val list: List<HistoryMessage>?, // 这里的 list 才是我们要的消息数组
    val pageNum: Int,
    val pageSize: Int,
    val pages: Int
    // 其他字段如 startRow, endRow 等如果不使用可以不写，Gson 会自动忽略
)

/**
 * 对应 "list" 数组中的每一项
 */
data class HistoryMessage(
    val messageId: Int,
    val conversationId: Int,
    val role: String,     // "user" 或 "assistant"
    val content: String,  // 聊天内容
    val createTime: String // 时间字符串
)

data class HistoryData(
    val list: List<HistoryMessage>
)

/**
 * 1. 添加会话接口的响应
 */
data class AddSessionResponse(
    val code: Int,
    val msg: String,
    val data: SessionIdData?
)

data class SessionIdData(
    val conversationId: Int
)

/**
 * 2. 获取会话列表接口的响应 (对应你发的第二张图)
 */
data class ChatSessionResponse(
    val code: Int,
    val msg: String,
    val data: ChatSessionPageData?
)

data class ChatSessionPageData(
    val total: Int,
    val list: List<ChatSessionItem>?,
    val pageNum: Int,
    val pageSize: Int,
    val pages: Int
)

data class ChatSessionItem(
    val conversationId: Int,
    val userId: Int,
    val headline: String,
    val status: Int,
    val createTime: String
)
/**
 * 豆包API服务接口
 */
interface DoubaoApiService {
    // 1. 豆包官方接口 (使用 Bearer APIKey)
    @POST("/api/v3/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: DoubaoChatRequest
    ): Response<DoubaoChatResponse>

    // 2. 你的后端接口 - 保存聊天记录 (注入 Authorization Header)
    @POST("http://8.148.253.180:8080/api/v1/ai/addChatMessage")
    suspend fun saveChatMessage(
        @Header("Authorization") token: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<Any>

    // 3. 你的后端接口 - 获取历史记录 (注入 Authorization Header)
    @GET("http://8.148.253.180:8080/api/v1/ai/chatMessage")
    suspend fun getChatHistory(
        @Header("Authorization") token: String,
        @Query("conversationId") conversationId: Int,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<HistoryResponse>

    @POST("http://8.148.253.180:8080/api/v1/ai/addChatSession")
    suspend fun addChatSession(
        @Header("Authorization") token: String,
        @Query("headline") headline: String  // 关键修改：从 @Body Map 改为 @Query
    ): Response<AddSessionResponse>

    /**
     * 【获取会话列表接口】用于加载当前用户的所有历史会话
     */
    @GET("http://8.148.253.180:8080/api/v1/ai/chatSession")
    suspend fun getChatSessions(
        @Header("Authorization") token: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ChatSessionResponse>
}
/**
 * 豆包API客户端管理对象
 */
object DoubaoApiClient {
    private const val BASE_URL = "https://ark.cn-beijing.volces.com"

    private var apiKey: String? = "f1230c55-40af-4fe1-b037-0f5a03a46b64"

    /**
     * 设置API密钥
     */
    fun setApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    fun getApiKey(): String? {
        return apiKey
    }
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

   private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // 懒加载的Retrofit实例
    private var _instance: DoubaoApiService = createInstance()
    val instance: DoubaoApiService
        get() = _instance

    private fun createInstance(): DoubaoApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(DoubaoApiService::class.java)
    }

    /**
     * 重新创建实例以应用新的配置
     */
    fun rebuildInstance() {
        _instance = createInstance()
    }
}

/**
 * 豆包API工具类，提供便捷的调用方法
 */
class DoubaoApiHelper {
    suspend fun sendTextMessageWithContext(
        conversationId: Int,
        message: String,
        userToken: String, // 显式接收 Token
        apiService: DoubaoApiService
    ): Result<DoubaoChatResponse> {
        val apiKey = DoubaoApiClient.getApiKey() ?: return Result.failure(Exception("API Key未设置"))

        val historyMessages = mutableListOf<DoubaoMessage>()
        try {
            // 使用传入的 userToken 进行查询
            val historyRes = apiService.getChatHistory(
                token = userToken,
                conversationId = conversationId
            )

            if (historyRes.isSuccessful) {
                val rawList = historyRes.body()?.data?.list ?: emptyList()
                rawList.takeLast(6).forEach { historyItem ->
                    historyMessages.add(
                        DoubaoMessage(
                            role = historyItem.role,
                            content = listOf(DoubaoContent(type = "text", text = historyItem.content))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ChatDebug", "获取历史异常: ${e.message}")
        }

        val currentMessage = DoubaoMessage(
            role = "user",
            content = listOf(DoubaoContent(type = "text", text = message))
        )

        val request = DoubaoChatRequest(
            model = "doubao-seed-1-6-lite-251015",
            max_completion_tokens = 65535,
            messages = historyMessages + currentMessage
        )

        return try {
            val response = apiService.chatCompletion("Bearer $apiKey", request = request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 在 DoubaoApiHelper 类中添加
    suspend fun generateFirstHeadline(userMsg: String, aiMsg: String): String {
        val apiKey = DoubaoApiClient.getApiKey() ?: return "新对话"

        val prompt = "请根据以下第一轮对话，总结一个7字以内的标题，不要标点符号和空行：\n用户：$userMsg\nAI：$aiMsg"

        // 优化 1：直接传 String，并且稍微调大 tokens 防止截断
        val request = DoubaoChatRequest(
            model = "doubao-seed-1-6-lite-251015",
            max_completion_tokens = 65535,
            messages = listOf(
                DoubaoMessage(role = "user", content = prompt)
            )
        )

        return try {
            val response = DoubaoApiClient.instance.chatCompletion("Bearer $apiKey", request = request)

            val body = response.body()
            // 核心日志：打印解析后的完整数据体！
            Log.d("ChatDebug", "【标题生成】完整 Body: $body")

            if (response.isSuccessful && body != null) {
                val rawContent = body.choices.firstOrNull()?.message?.content
                Log.d("ChatDebug", "【标题生成】提取到的 rawContent: $rawContent")

                // 优化 2：安全解析 Any? 类型，防止直接 toString() 变成内存地址或带格式的字符串
                val extractedStr = when (rawContent) {
                    is String -> rawContent
                    is List<*> -> {
                        val first = rawContent.firstOrNull()
                        if (first is Map<*, *>) first["text"]?.toString() ?: ""
                        else ""
                    }
                    else -> rawContent?.toString() ?: ""
                }

                // 清理多余字符
                val cleanTitle = extractedStr.trim().replace(Regex("[\n\r\"'*\\[\\]]"), "")

                if (cleanTitle.isNotEmpty()) {
                    cleanTitle.take(7)
                } else {
                    Log.w("ChatDebug", "【标题生成】提取后标题为空，使用兜底")
                    userMsg.take(7)
                }
            } else {
                Log.e("ChatDebug", "【标题生成】Body为空或请求失败: ${response.errorBody()?.string()}")
                userMsg.take(7)
            }
        } catch (e: Exception) {
            Log.e("ChatDebug", "【标题生成】代码异常: ${e.message}")
            userMsg.take(7)
        }
    }
}

suspend fun classifyProblemWithAI(problemDescription: String): String {
    // 1. 定义固定的 5 个分类
    val validCategories = listOf("软件系统", "网络通讯", "硬件维修", "移动外设", "综合咨询")

    // 2. 精准的 Prompt 设计
    val systemPrompt = """
        你是一个校园电脑维修单分类引擎。请根据用户的报修描述，将其归类到以下5个固定类别之一：
        [软件系统, 网络通讯, 硬件维修, 移动外设, 综合咨询]。
        
        【严格要求】：
        1. 必须且只能输出这5个词中的一个。
        2. 绝对不能包含任何标点符号、解释性文字或多余字符。
        3. 如果实在无法判断，请输出“综合咨询”。
    """.trimIndent()

    val userPrompt = "用户描述：\"$problemDescription\" \n分类结果："

    val messages = listOf(
        DoubaoMessage(role = "system", content = systemPrompt),
        DoubaoMessage(role = "user", content = userPrompt)
    )

    val request = DoubaoChatRequest(
        model = "doubao-seed-1-6-lite-251015", // 使用你现有的模型
        max_completion_tokens = 10,            // 限制输出长度，防止废话
        messages = messages
    )

    return try {
        // 1. 获取你在 DoubaoApiClient 中配置好的 API Key
        val apiKey = DoubaoApiClient.getApiKey() ?: return "综合咨询"

        // 2. 使用正确的实例 (DoubaoApiClient.instance) 和正确的方法 (chatCompletion)
        val response = DoubaoApiClient.instance.chatCompletion(
            authorization = "Bearer $apiKey",
            request = request
        )

        if (response.isSuccessful) {
            // 解析返回值 (参考了你 DoubaoApiService.kt 里的解析逻辑)
            val rawContent = response.body()?.choices?.firstOrNull()?.message?.content
            val extractedStr = when (rawContent) {
                is String -> rawContent
                is List<*> -> {
                    val first = rawContent.firstOrNull()
                    if (first is Map<*, *>) first["text"]?.toString() ?: "" else ""
                }
                else -> rawContent?.toString() ?: ""
            }

            val cleanResult = extractedStr.trim().replace(Regex("[\n\r\"'*\\[\\]]"), "")

            // 3. 安全校验：验证 AI 返回的词是否在我们的固定列表中
            if (validCategories.contains(cleanResult)) {
                cleanResult
            } else {
                Log.w("AI_Classify", "AI 返回了不在列表中的分类: $cleanResult")
                "综合咨询" // 兜底分类
            }
        } else {
            Log.e("AI_Classify", "AI 请求失败: ${response.errorBody()?.string()}")
            "综合咨询"
        }
    } catch (e: Exception) {
        Log.e("AI_Classify", "AI 分类发生网络异常", e)
        "综合咨询"
    }
}