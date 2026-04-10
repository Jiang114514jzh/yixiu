package com.example.yixiu_1.utils

import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.seg.common.Term

/**
 * 基于 HanLP 的中文分词器
 */
object ChineseSegmenter {

    /**
     * 基础分词：保留原句的所有词汇
     */
    fun tokenize(text: String): List<String> {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return emptyList()
        // HanLP.segment 返回 Term 列表，映射出 word 即可
        return HanLP.segment(cleanText).map { it.word }
    }

    /**
     * 提取关键词：过滤掉代词、介词等无意义词汇，专供知识库匹配使用
     */
    fun extractKeywords(text: String): List<String> {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return emptyList()

        val termList: List<Term> = HanLP.segment(cleanText)
        val keywords = mutableListOf<String>()

        for (term in termList) {
            val word = term.word
            val nature = term.nature.toString()

            // 仅保留名词(n)、动词(v)、形容词(a)
            if (nature.startsWith("n") || nature.startsWith("v") || nature.startsWith("a")) {
                // 过滤掉长度为 1 的动词（如“是”、“有”），保留名词和具有实际意义的词
                if (word.length > 1 || nature.startsWith("n")) {
                    keywords.add(word)
                }
            }
        }
        return keywords
    }
}