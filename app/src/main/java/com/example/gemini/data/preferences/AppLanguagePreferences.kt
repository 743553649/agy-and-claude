package com.example.gemini.data.preferences

import android.content.Context

/**
 * 界面语言偏好（P1 语言切换）。
 *
 * 为什么自己存、不用 appcompat 的 `autoStoreLocales`：后者要求在 AndroidManifest 里声明
 * `androidx.appcompat.app.AppLocalesMetadataHolderService`，而本项目的 manifest 不允许改。
 * 因此用 SharedPreferences 同步读写，由 [com.example.gemini.domain.i18n.AppLocaleController]
 * 在 Activity 启动时回设。
 *
 * 取值只有三种：`system`（跟随系统）/ `zh-CN` / `en`。未知值一律按 `system` 处理。
 */
class AppLanguagePreferences(context: Context) {

    companion object {
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_ZH_CN = "zh-CN"
        const val LANGUAGE_EN = "en"

        private const val PREFS_NAME = "app_language_prefs"
        private const val KEY_APP_LANGUAGE = "app_language"

        private val SUPPORTED = setOf(LANGUAGE_SYSTEM, LANGUAGE_ZH_CN, LANGUAGE_EN)
    }

    private val syncPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLanguageSync(): String {
        val stored = syncPrefs.getString(KEY_APP_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
        return if (stored in SUPPORTED) stored else LANGUAGE_SYSTEM
    }

    /** 写入偏好；非法值归一到 `system`。 */
    fun setLanguage(tag: String) {
        val normalized = if (tag in SUPPORTED) tag else LANGUAGE_SYSTEM
        syncPrefs.edit().putString(KEY_APP_LANGUAGE, normalized).apply()
    }
}
