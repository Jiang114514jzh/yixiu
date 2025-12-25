package com.example.yixiu_1.data

import android.content.Context
import android.util.Log
import com.example.yixiu_1.network.RepairHistoryItem
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

    fun getNicknameOrGenerate(): String {
        return nickname ?: userEmail?.substringBefore('@') ?: "游客"
    }

    // 【新增】辅助方法：一次性保存登录信息，代码更整洁
    fun saveLoginInfo(token: String, userId: Int, username: String, userEmail: String, avatarPath: String?) {
        this.token = token
        this.userId = userId
        this.nickname = username
        this.userEmail = userEmail
        this.avatarPath = avatarPath
        this.isLoggedIn = true
    }

    fun logout() {
        // 注销时直接清除所有用户相关信息
        clear()
    }

    // 【关键修改】Key 绑定 userId，实现多账号数据隔离
    private fun getHistoryKey(): String {
        return "repair_history_$userId"
    }

    fun addRepairHistory(item: RepairHistoryItem) {
        // 如果未登录（userId 为 -1），不保存或者保存到临时 key
        if (userId == -1) return

        try {
            val history = getRepairHistory().toMutableList()
            // 将新条目添加到列表头部（最新的在最上面）
            history.add(0, item)
            val json = gson.toJson(history)
            // 使用带 userId 的 Key 保存
            prefs.edit().putString(getHistoryKey(), json).apply()
            Log.d("UserPreferences", "已保存本地报修记录: $item")
        } catch (e: Exception) {
            Log.e("UserPreferences", "添加历史记录失败", e)
        }
    }

    fun getRepairHistory(): List<RepairHistoryItem> {
        // 如果未登录，返回空列表
        if (userId == -1) return emptyList()

        return try {
            // 使用带 userId 的 Key 读取
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
            // 注意：这里我们通常不清除 repair_history_xxx，保留本地缓存记录供下次登录查看
            // 如果你希望退出登录就清空该手机上的所有记录，可以在这里遍历清除
            .apply()
    }
}
