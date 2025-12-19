package com.example.yixiu_1.network

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 报修任务提交请求的数据模型
 */
data class RepairTaskRequest(
    val userId: Int,
    val contactType: Int, // Changed from String to Int
    val contactInfo: String,
    val deviceType: String,
    val deviceSystem: String,
    val deviceModel: String,
    val problemDescription: String,
    val campus: Int, // 0 代表大学城校区, 1 代表白云山校区
    val repairLocation: String,

    @SerializedName("AppointmentTime")
    val appointmentTime: String,

    @SerializedName("Remarks")
    val remarks: String?
)

data class RepairHistoryItem(
    val id: String = "task_${System.currentTimeMillis()}", // 唯一的ID，用于区分不同的记录
    val contactType: String, // Kept as String for local history
    val contactInfo: String,
    val deviceType: String,
    val deviceSystem: String,
    val deviceModel: String,
    val problemDescription: String,
    val campus: String, // 直接存储UI上的文本，如 "大学城校区"
    val repairLocation: String,
    val appointmentTime: String,
    val remarks: String?,
    val submissionDate: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
)
