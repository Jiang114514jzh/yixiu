package com.example.yixiu_1.network

import com.google.gson.annotations.SerializedName

/**
 * 通用的网络响应体封装
 * @param T 具体的业务数据类型
 */
data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String,
    @SerializedName("data") val data: T?
)

/**
 * 用户信息数据类 - 完全匹配服务器响应
 */
data class UserInfo(
    @SerializedName("userId") val userId: Int,
    @SerializedName("username") val username: String,
    @SerializedName("realName") val realName: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("role") val role: String,
    @SerializedName("status") val status: String?,
    @SerializedName("lastLogin") val lastLogin: String?,
    @SerializedName("volunteerInfo") val volunteerInfo: VolunteerInfo?
)

data class VolunteerInfo(
    @SerializedName("volunteerId") val volunteerId: Int,
    @SerializedName("userId") val userId: Int,
    @SerializedName("studentNumber") val studentNumber: String?,
    @SerializedName("majorClass") val majorClass: String?,
    @SerializedName("grade") val grade: String?,
    @SerializedName("status") val status: Int,
    @SerializedName("contactType") val contactType: String?,
    @SerializedName("contactNumber") val contactNumber: String?,
    @SerializedName("createTime") val createTime: String?,
    @SerializedName("updateTime") val updateTime: String?
)

/**
 * 仅包含邮箱的请求体
 */
data class EmailOnlyRequest(
    val email: String
)

/**
 * 邮箱注册或登录的请求体
 */
data class EmailRegisterOrLoginRequest(
    val email: String,
    val role: String,
    val verificationCode: Int
)
