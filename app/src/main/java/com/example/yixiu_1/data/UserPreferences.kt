package com.example.yixiu_1.data

import android.content.Context
import android.util.Log
import com.example.yixiu_1.network.RepairHistoryItem
import com.example.yixiu_1.network.VolunteerDetail // 【新增导入】确保导入你的数据类
import com.example.yixiu_1.network.VolunteerInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().putString("token", value).apply()

    var userId: Int
        get() = prefs.getInt("user_id", -1)
        set(value) = prefs.edit().putInt("user_id", value).apply()

    var userEmail: String?
        get() = prefs.getString("user_email", null)
        set(value) = prefs.edit().putString("user_email", value).apply()

    var nickname: String?
        get() = prefs.getString("nickname", null)
        set(value) = prefs.edit().putString("nickname", value).apply()

    var userRole: String?
        get() = prefs.getString("user_role", null)
        set(value) = prefs.edit().putString("user_role", value).apply()

    var avatarPath: String?
        get() = prefs.getString("avatar_path", null)
        set(value) = prefs.edit().putString("avatar_path", value).apply()

    // ================== 【新增】志愿者信息管理 ==================

    /**
     * 存取完整的志愿者信息对象
     */
    var volunteerInfo: VolunteerInfo?
        get() {
            val json = prefs.getString("volunteer_info", null)
            return if (json != null) {
                try {
                    gson.fromJson(json, VolunteerInfo::class.java)
                } catch (e: Exception) {
                    Log.e("UserPreferences", "解析 VolunteerDetail 失败", e)
                    null
                }
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                prefs.edit().putString("volunteer_info", gson.toJson(value)).apply()
            } else {
                // 如果传入 null，则清除该字段
                prefs.edit().remove("volunteer_info").apply()
            }
        }

    /**
     * 快捷获取 volunteerId (接取任务时直接调用 userPreferences.volunteerId 即可)
     * 如果不是志愿者或没有信息，返回 -1
     */
    val volunteerId: Int
        get() = volunteerInfo?.volunteerId ?: -1

    // =========================================================

    fun getNicknameOrGenerate(): String {
        return nickname ?: userEmail?.substringBefore('@') ?: "游客"
    }

    // 【修改】保存登录信息时，支持传入 volunteerInfo
    fun saveLoginInfo(
        token: String,
        userId: Int,
        username: String,
        userEmail: String,
        avatarPath: String?,
        role: String?,
        volunteerInfo: VolunteerInfo? = null // 【修复 1】：把参数名和你的数据类类型对齐，改为 volunteerInfo
    ) {
        this.token = token
        this.userId = userId
        this.nickname = username
        this.userEmail = userEmail
        this.avatarPath = avatarPath
        this.userRole = role
        this.volunteerInfo = volunteerInfo // 【修复 2】：现在能正确把传进来的参数赋值给本地存储了
        this.isLoggedIn = true
    }

    fun logout() {
        // 注销时直接清除所有用户相关信息
        clear()
    }

    // Key 绑定 userId，实现多账号数据隔离
    private fun getHistoryKey(): String {
        return "repair_history_$userId"
    }

    fun addRepairHistory(item: RepairHistoryItem) {
        if (userId == -1) return

        try {
            val history = getRepairHistory().toMutableList()
            history.add(0, item)
            val json = gson.toJson(history)
            prefs.edit().putString(getHistoryKey(), json).apply()
            Log.d("UserPreferences", "已保存本地报修记录: $item")
        } catch (e: Exception) {
            Log.e("UserPreferences", "添加历史记录失败", e)
        }
    }

    fun getRepairHistory(): List<RepairHistoryItem> {
        if (userId == -1) return emptyList()

        return try {
            val json = prefs.getString(getHistoryKey(), null)
            if (json != null) {
                val type = object : TypeToken<List<RepairHistoryItem>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("UserPreferences", "解析历史记录失败", e)
            try {
                prefs.edit().remove(getHistoryKey()).apply()
            } catch (e2: Exception) {
                Log.e("UserPreferences", "清除损坏数据失败", e2)
            }
            emptyList()
        }
    }

    fun clear() {
        prefs.edit()
            .remove("is_logged_in")
            .remove("token")
            .remove("user_id")
            .remove("user_email")
            .remove("user_role")
            .remove("avatar_path")
            .remove("nickname")
            .remove("volunteer_info") // 【新增】退出登录时清除志愿者信息
            .apply()
    }
}