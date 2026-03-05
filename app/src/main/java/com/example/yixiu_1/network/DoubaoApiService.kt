package com.example.yixiu_1.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

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
 * 豆包API服务接口
 */
interface DoubaoApiService {
    /**
     * 调用豆包聊天完成API
     * @param authorization 认证令牌
     * @param request 请求体
     * @return API响应
     */
    @POST("/api/v3/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: DoubaoChatRequest
    ): Response<DoubaoChatResponse>

    /**
     * 上传图片到豆包服务（如果需要的话）
     */
    @Multipart
    @POST("/api/v1/upload")
    suspend fun uploadImage(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part
    ): Response<Any>
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

    /**
     * 发送文本消息到豆包API
     * @param message 文本消息内容
     * @param model 模型名称，默认为DEFAULT_MODEL
     * @param maxTokens 最大完成token数，默认为MAX_TOKENS
     * @return API响应结果
     */
    suspend fun sendTextMessage(
        message: String,
        model: String = DEFAULT_MODEL,
        maxTokens: Int = MAX_TOKENS
    ): Result<DoubaoChatResponse> {
        val apiKey = DoubaoApiClient.getApiKey()
            ?: return Result.failure(Exception("API Key未设置"))

        val request = DoubaoChatRequest(
            model = model,
            max_completion_tokens = maxTokens,
            messages = listOf(
                DoubaoMessage(
                    role = "user",
                    content = listOf(
                        DoubaoContent(
                            type = "text",
                            text = message
                        )
                    )
                )
            )
        )

        return try {
            val response = DoubaoApiClient.instance.chatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Result.failure(Exception("API请求失败: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 发送图文消息到豆包API
     * @param text 文字内容
     * @param imageUrl 图片URL
     * @param model 模型名称，默认为DEFAULT_MODEL
     * @param maxTokens 最大完成token数，默认为MAX_TOKENS
     * @return API响应结果
     */
    suspend fun sendImageTextMessage(
        text: String,
        imageUrl: String,
        model: String = DEFAULT_MODEL,
        maxTokens: Int = MAX_TOKENS
    ): Result<DoubaoChatResponse> {
        val apiKey = DoubaoApiClient.getApiKey()
            ?: return Result.failure(Exception("API Key未设置"))

        val request = DoubaoChatRequest(
            model = model,
            max_completion_tokens = maxTokens,
            messages = listOf(
                DoubaoMessage(
                    role = "user",
                    content = listOf(
                        DoubaoContent(
                            type = "image_url",
                            image_url = DoubaoImageUrl(url = imageUrl)
                        ),
                        DoubaoContent(
                            type = "text",
                            text = text
                        )
                    )
                )
            ),
            reasoning_effort = "medium"
        )

        return try {
            val response = DoubaoApiClient.instance.chatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("响应体为空"))
            } else {
                Result.failure(Exception("API请求失败: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
