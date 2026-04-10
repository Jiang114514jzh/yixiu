package com.example.yixiu_1.utils

import kotlin.math.sqrt

/**
 * 轻量级 TF-IDF 匹配器
 */
object TfIdfMatcher {

    /**
     * 计算两个词频 Map 之间的余弦相似度
     */
    fun calculateCosineSimilarity(queryVector: Map<String, Double>, docVector: Map<String, Double>): Double {
        var dotProduct = 0.0
        var queryNorm = 0.0
        var docNorm = 0.0

        val allWords = queryVector.keys + docVector.keys
        for (word in allWords) {
            val v1 = queryVector[word] ?: 0.0
            val v2 = docVector[word] ?: 0.0
            dotProduct += v1 * v2
            queryNorm += v1 * v1
            docNorm += v2 * v2
        }

        return if (queryNorm == 0.0 || docNorm == 0.0) 0.0 else dotProduct / (sqrt(queryNorm) * sqrt(docNorm))
    }

    /**
     * 将分词列表转为词频 Map (简单的 TF 实现)
     */
    fun getTermFrequency(tokens: List<String>): Map<String, Double> {
        val countMap = tokens.groupingBy { it }.eachCount()
        val total = tokens.size.toDouble()
        return countMap.mapValues { it.value / total }
    }
    /**
     * 新增：知识库覆盖率匹配算法 (针对长句匹配短句的场景)
     * @param userKeywords 用户提问提取出的关键词列表
     * @param kbKeywords 知识库条目提取出的关键词列表
     * @return 匹配得分 (0.0 到 1.0)
     */
    fun calculateCoverageMatch(userKeywords: List<String>, kbKeywords: List<String>): Double {
        if (kbKeywords.isEmpty()) return 0.0

        // 计算交集：用户提问中命中了几个知识库的关键词
        val matchCount = userKeywords.intersect(kbKeywords.toSet()).size

        // 核心：分母是知识库的词数，而不是用户的词数
        return matchCount.toDouble() / kbKeywords.size
    }
}

