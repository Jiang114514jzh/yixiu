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

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun addRepairHistory(item: RepairHistoryItem) {
        try {
            val history = getRepairHistory().toMutableList()
            history.add(0, item)
            val json = gson.toJson(history)
            prefs.edit().putString("repair_history", json).apply()
        } catch (e: Exception) {
            Log.e("UserPreferences", "添加历史记录失败", e)
        }
    }

    fun getRepairHistory(): List<RepairHistoryItem> {
        return try {
            val json = prefs.getString("repair_history", null)
            if (json != null) {
                val type = object : TypeToken<List<RepairHistoryItem>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("UserPreferences", "解析历史记录失败", e)
            // 如果解析失败，清除损坏的数据
            try {
                prefs.edit().remove("repair_history").apply()
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
            .apply()
    }
}