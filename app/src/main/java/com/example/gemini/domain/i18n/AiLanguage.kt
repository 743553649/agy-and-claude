package com.example.gemini.domain.i18n

import java.util.Locale

/**
 * AI 回复语言策略：跟随 App 语言。
 *
 * 界面为简体中文时注入中文指令；其他语言不注入（模型按其默认行为回复）。
 * 这里的措辞是唯一版本，改动前先看 docs/i18n/GLOSSARY.md 第 6 节。
 */
object AiLanguage {

    /**
     * 注入文案：解释性文字用简体中文，代码 / 标识符 / 命令 / 路径 / 提交信息保持原样，
     * 没有公认中文译法的技术名词不硬译。
     */
    private const val ZH_INSTRUCTION =
        "Always write your explanatory prose in Simplified Chinese. Keep all code, identifiers, " +
            "commands, file paths, and commit messages in their original form; do not translate " +
            "technical terms that have no well-established Chinese equivalent."

    private fun isChinese(): Boolean = Locale.getDefault().language == "zh"

    /** 追加到 Gemini 系统指令末尾的片段；空串表示不注入。 */
    fun suffix(): String = if (isChinese()) "\n\n$ZH_INSTRUCTION" else ""

    /** Claude 会话 initialize 请求的 appendSystemPrompt；null 表示不注入。 */
    fun appendSystemPromptOrNull(): String? = if (isChinese()) ZH_INSTRUCTION else null
}
