package com.example.yixiu_1.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.yixiu_1.MainActivity
import com.example.yixiu_1.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "notification_channel"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "应用通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用于接收系统消息、广播和用户通知"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNewMessageNotification(sender: String) {
        // 检查通知权限
        if (!hasNotificationPermission()) {
            return
        }

        // 创建点击通知后跳转到消息中心的 Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_screen", "message_center") // 传递参数标识跳转到消息中心
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // 您需要准备一个通知图标
            .setContentTitle("新消息提醒")
            .setContentText("您有一条来自${sender}的新信息")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // 点击后自动取消通知

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // 权限被拒绝时的处理
            android.util.Log.e("NotificationHelper", "通知权限被拒绝", e)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    // 提醒匹配成功的志愿者进行维修
    fun showExpertRecommendationNotification(category: String) {
        if (!hasNotificationPermission()) {
            return
        }

        // 点击通知后跳转到任务大厅 (假设标识为 task_square)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_screen", "task_square")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0, // 这里可以使用不同的 RequestCode
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("✨ 专属任务推荐") // 使用特殊的标题吸引注意
            .setContentText("系统发现了一个您擅长的【$category】新报修单，快去看看吧！")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 推荐任务优先级设为高
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                // 使用系统时间作为ID，确保推荐通知不会覆盖其他普通通知
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "通知权限被拒绝", e)
        }
    }
}
