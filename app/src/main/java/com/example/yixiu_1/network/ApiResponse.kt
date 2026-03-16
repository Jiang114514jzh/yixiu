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
    @SerializedName("contactType") val contactType: Int?,
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

/**
 * 用户详情主数据 (UserProfile)
 */
data class UserProfile(
    val userInfoVO: UserInfoVO,
    val communityStatisticDto: CommunityStatisticDto,
    val volunteerDataVO: VolunteerDataVO?,
    val role: String,
    @SerializedName("isFollow")
    val isFollow: Boolean,
    val visitedNum: Int,
    val lastLoginTime: String
)

/**
 * 用户基本信息
 */
data class UserInfoVO(
    val userId: Long,
    val username: String,
    val avatar: String,
    val userSignature: String? // JSON 中为 null，建议设为可空
)

/**
 * 社区统计数据
 */
data class CommunityStatisticDto(
    val userId: Long,
    val postNum: Int,
    val followNum: Int,
    val fansNum: Int,
    val getLikeNum: Int
)

/**
 * 志愿者相关数据
 */
data class VolunteerDataVO(
    val volunteerId: Long,
    val grade: String,
    val status: Int,
    val fixedNum: Int,
    val finishRate: Double, // 对应 0.1429
    val contactType: Int,
    val contactNumber: String
)

data class FollowUserItem(
    @SerializedName("followUserId")
    val userId: Int,
    @SerializedName("followUsername")
    val username: String,
    @SerializedName("followUserAvatar")
    val avatar: String?,
    @SerializedName("followUserSignature")
    val userSignature: String?
)

data class FollowPage(
    val total: Int,
    val list: List<FollowUserItem>,
    val pageNum: Int,
    val pageSize: Int,
    val pages: Int,
    val hasNextPage: Boolean
)