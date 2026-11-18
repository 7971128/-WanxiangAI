package com.example.wanxiangai

object SanfenStrategy {
    data class Suggestion(val phase: String, val candidates: List<String>, val explanation: String)
    private val main = listOf("曹操", "甄姬", "露娜", "小乔")
    private val secondary = listOf("周瑜", "孙悟空", "阿轲", "阿珂")
    fun recommend(round: Int, visibleText: String, desired: String): Suggestion {
        val phase = when {
            round <= 3 -> "前期：尽量稳血并积累经济，不盲目刷新"
            round <= 5 -> "过渡期：优先寻找曹操，逐渐建立三分登场体系"
            else -> "成型期：围绕曹操—甄姬—露娜循环，小乔提供成长"
        }
        val preferred = when {
            round <= 3 -> listOf("弈星", "孙尚香", "曹操") + main + secondary
            round <= 5 -> listOf("曹操", "孙尚香", "甄姬", "小乔", "露娜") + secondary
            else -> main + secondary + listOf("唤醒", "整备")
        }
        val hits = preferred.distinct().filter { visibleText.contains(it) }
        val reason = buildString {
            append("核心循环：曹操整备触发三分登场，甄姬补登场牌，露娜反向触发整备，小乔使登场转化为成长。")
            if (round <= 3) append(" 前期不用为了成型盲目花钱。")
            if (round >= 5) append(" 成型后减少无关登场单位干扰，谨慎处理孙尚香过渡。")
            append(" 用户目标：").append(desired.take(160))
            append("。此处只是OCR命中名单，不能证明候选在商店中，更不能直接盲点。")
        }
        return Suggestion(phase, hits, reason)
    }
}
