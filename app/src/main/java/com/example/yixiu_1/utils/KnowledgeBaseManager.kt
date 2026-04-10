package com.example.yixiu_1.utils

import com.example.yixiu_1.network.KnowledgeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 知识库缓存数据结构
 */

data class KbItemCache(
    val id: Int,                 // 【核心修改】把 String 改成 Int
    val question: String,
    val answer: String,
    val kbKeywords: List<String>
)

/**
 * 知识库管理器：负责缓存与高并发搜索
 */
object KnowledgeBaseManager {

    // 使用 @Volatile 保证多线程下的内存可见性，防止并发修改异常
    @Volatile
    private var kbCacheList: List<KbItemCache> = emptyList()

    // 【新增】用于测试暴露缓存大小
    fun getCacheSize(): Int {
        return kbCacheList.size
    }

    /**
     * 初始化/更新知识库缓存
     * 在获取到后端接口数据后调用此方法（需在协程中执行）
     */
    /**
     * 初始化/更新知识库缓存
     * 注意这里的参数类型已经改成了 List<KnowledgeItem>
     */
    suspend fun updateKnowledgeBase(backendDataList: List<KnowledgeItem>) {
        withContext(Dispatchers.Default) {
            val newProcessedList = backendDataList.map { data ->
                KbItemCache(
                    // 根据你提供的 JSON，你的字段是 knowledgeId, problem, solution
                    id = data.knowledgeId ?: 0,
                    question = data.problem ?: "",
                    answer = data.solution ?: "",
                    // 对 problem (问题标题) 进行分词
                    kbKeywords = ChineseSegmenter.extractKeywords(data.problem ?: "")
                )
            }
            // 原子性替换，保证线程安全
            kbCacheList = newProcessedList
        }
    }

    /**
     * 搜索最佳答案（瞬间完成，无需对整个知识库重新分词）
     */
    fun searchBestAnswer(userQuery: String): String? {
        val currentCache = kbCacheList
        // 如果知识库还没加载完，直接交给 AI 处理
        if (currentCache.isEmpty()) return null

        val userKeywords = ChineseSegmenter.extractKeywords(userQuery)
        // 如果用户发了全是无意义的词（比如“啊啊啊”），提不出关键词，也交给 AI 去理解
        if (userKeywords.isEmpty()) return null

        var bestMatch: KbItemCache? = null
        var highestScore = 0.0

        for (item in currentCache) {
            val score = TfIdfMatcher.calculateCoverageMatch(userKeywords, item.kbKeywords)
            if (score > highestScore) {
                highestScore = score
                bestMatch = item
            }
        }

        // 核心修改：大于等于阈值返回答案，否则返回 null 触发 AI 兜底
        return if (highestScore >= 0.5) bestMatch?.answer else null
    }
}

