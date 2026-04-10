import android.util.Log
import com.example.yixiu_1.network.NetworkClient
import com.example.yixiu_1.network.SendDeleteNotifyRequest
import com.example.yixiu_1.network.SendUserToUserNotifyRequest

/**
 * 封装的静默查找专家并推送的函数 (含详细 Debug 日志)
 */
/**
 * 封装的静默查找专家并推送的函数 (更新为 UserToUser 接口)
 * @param senderId 发单人的 userId
 */
suspend fun silentMatchAndNotifyExperts(token: String, requestId: Int, senderId: Int,device: String,
                                        problem: String,
                                        location: String,
                                        time: String,contact: String) {
    val TAG = "NotifyExpertsDebug"
    Log.d(TAG, "========== 开始静默匹配专家流程 ==========")

    try {
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        // ==================================================
        // 步骤 1: 获取报修单拿到 skillId
        // ==================================================
        val taskRes = NetworkClient.instance.getTaskById(authHeader, requestId)
        if (!taskRes.isSuccessful || taskRes.body()?.code != 200) return

        val aiSkillId = taskRes.body()?.data?.skillId
        if (aiSkillId == null) {
            Log.w(TAG, "【拦截】skillId 为 null！AI未处理完或接口返回空。")
            return
        }

        // ==================================================
        // 步骤 2: 拿专家列表
        // ==================================================
        val skillRes = NetworkClient.instance.getSkillListBySkillId(authHeader, aiSkillId)
        if (!skillRes.isSuccessful || skillRes.body()?.code != 200) return

        val volunteerSkills = skillRes.body()?.data ?: emptyList()

        // ==================================================
        // 步骤 3: 过滤专家并发送 UserToUser 通知
        // ==================================================
        val experts = volunteerSkills.filter { it.isExpert == 1 }

        if (experts.isNotEmpty()) {

            // 为了防止用户写的描述太长导致系统通知栏显示不下，稍微截断一下
            val shortProblem = if (problem.length > 15) problem.substring(0, 15) + "..." else problem

            // 组装丰富的通知内容，并带上用于跳转的 |||requestId
            val richContent = "\uD83D\uDD27 设备：$device\n" +
                    "\uD83D\uDCDD 问题：$shortProblem\n" +
                    "\uD83D\uDCCD 地点：$location\n" +
                    "⏰ 期望时间：$time\n" +
                    "\uD83D\uDCDE 联系方式：$contact\n" +
                    "快去接单吧！\n" +
                    "\uD83C\uDD94 请求 ID：$requestId"

            experts.forEach { expert ->
                Log.d(TAG, " -> 准备向专家 (ID: ${expert.userId}) 发送通知...")

                val notifyReq = SendUserToUserNotifyRequest(
                    senderId = senderId,
                    receiverId = expert.userId, // 确保这里是你正确的专家ID字段
                    title = "✨ 专属任务推荐",
                    content = richContent
                )

                val pushRes = NetworkClient.instance.sendUserNotification(authHeader, notifyReq)

                if (pushRes.isSuccessful && pushRes.body()?.code == 200) {
                    Log.d(TAG, " -> ✅ 专家 (ID: ${expert.volunteerId}) 通知发送成功！")
                } else {
                    Log.e(TAG, " -> ❌ 专家 (ID: ${expert.volunteerId}) 通知发送失败！")
                }
            }
            Log.d(TAG, "========== 静默匹配流程结束 ==========")
        } else {
            Log.d(TAG, "该领域暂无合格专家，不发送。========== 流程结束 ==========")
        }

    } catch (e: Exception) {
        Log.e(TAG, "静默流程发生异常", e)
    }
}