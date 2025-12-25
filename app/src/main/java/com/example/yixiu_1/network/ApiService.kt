package com.example.yixiu_1.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

//============ 接口定义 ============
interface ApiService {
    // --- 登录注册相关接口 ---
    @GET("/api/v1/send/emailVerification")
    suspend fun sendEmailVerification(@Query("email") email: String): Response<ApiResponse<Any>>

    @POST("/api/v1/users/registerByEmail")
    suspend fun registerByEmail(@Body body: EmailRegisterOrLoginRequest): Response<ApiResponse<Any>>

    @POST("/api/v1/users/loginByEmail")
    suspend fun loginByEmail(@Body body: EmailRegisterOrLoginRequest): Response<ApiResponse<Any>>

    // --- 报修任务相关接口 ---
    @POST("/api/v1/task/add")
    suspend fun submitRepairTask(@Body task: RepairTaskRequest): Response<ApiResponse<Any>>

    @GET("/api/v1/users/userInfo")
    suspend fun getUserInfo(): Response<ApiResponse<UserInfo>>
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
        .readTimeout(20, TimeUnit.SECONDS)
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