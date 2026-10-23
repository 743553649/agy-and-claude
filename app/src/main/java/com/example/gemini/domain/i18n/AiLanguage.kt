package com.example.gemini.domain.i18n

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

/**
 * AI 回复语言策略：跟随 App 语言。
 *
 * 界面为简体中文时注入中文指令；其他语言不注入（模型按其默认行为回复）。
 * 这里的措辞是唯一版本，改动前先看 docs/i18n/GLOSSARY.md 第 6 节。
 *
 * 语言来源：优先读 appcompat 记录的「应用语言」——API 33+ 来自框架 LocaleManager，
 * API<33 来自 appcompat 的进程内请求值（即 `AppCompatDelegate.setApplicationLocales` 的入参）；
 * 没有被显式设置（跟随系统）时才回落到 `Locale.getDefault()`。
 * 仍不直接用 `Locale.getDefault()` 判定，原因只剩「区分不了显式设置与跟随系统」这一点：
 * 2026-10-23 真机实测（API<33）已确认切换应用语言时 `Locale.getDefault()` **确实会跟着变**
 * （证据见 docs/i18n/HANDOFF.md 第 4 节「已真机验证②」），因此原先「API<33 下 appcompat
 * 只包装 Activity 资源、静态默认值可能不同步」的顾虑不再成立；读 appcompat 的 API 更精确，
 * 逻辑保持不动。
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

    private fun currentLocale(): Locale =
        AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()

    private fun isChinese(): Boolean = currentLocale().language == "zh"

    /** 追加到 Gemini 系统指令末尾的片段；空串表示不注入。 */
    fun suffix(): String = if (isChinese()) "\n\n$ZH_INSTRUCTION" else ""

    /** Claude 会话 initialize 请求的 appendSystemPrompt；null 表示不注入。 */
    fun appendSystemPromptOrNull(): String? = if (isChinese()) ZH_INSTRUCTION else null
}
