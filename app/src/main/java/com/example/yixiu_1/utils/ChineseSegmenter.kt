package com.example.yixiu_1.utils

/**
 * 基础分词器 (生产环境建议引入 HanLP Android 依赖)
 */
object ChineseSegmenter {
    fun tokenize(text: String): List<String> {
        // 简单清洗：去除空格和特殊字符
        val cleanText = text.replace(Regex("[^\\u4e00-\\u9fa5a-zA-Z0-9]"), "")

        // 简单逻辑：将文本切分为单字和双字组合（模拟基础特征提取）
        val tokens = mutableListOf<String>()
        for (i in cleanText.indices) {
            tokens.add(cleanText[i].toString())
            if (i < cleanText.length - 1) {
                tokens.add(cleanText.substring(i, i + 2))
            }
        }
        return tokens
    }
}
