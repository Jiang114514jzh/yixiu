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
}

