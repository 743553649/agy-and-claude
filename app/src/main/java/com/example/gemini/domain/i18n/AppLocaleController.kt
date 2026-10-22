package com.example.gemini.domain.i18n

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.gemini.data.preferences.AppLanguagePreferences

/**
 * 应用内语言切换（方案 A：appcompat）。
 *
 * - API 33+：`AppCompatDelegate.setApplicationLocales` 转调框架 `LocaleManager`，系统负责重建
 *   Activity 与资源，通知 / Toast 一并跟随。
 * - API<33：appcompat 把语言请求记在进程内并按它包装 AppCompat Activity 的资源；
 *   持久化由 [AppLanguagePreferences] 负责（manifest 禁止改动，`autoStoreLocales` 用不了）。
 *
 * 幂等保护：`appliedTag` 记住本进程已经设过的值，避免「set → recreate → onCreate 再 set」
 * 的反复重建（recreate 循环）。
 */
object AppLocaleController {

    /** 本进程已应用过的语言标签；null 表示还没应用过。 */
    @Volatile
    private var appliedTag: String? = null

    /**
     * 启动时回设偏好，必须在 `setContent` 之前调用（见 4 个 Activity 的 `onCreate`）。
     *
     * `system` 时直接返回、不碰框架值：API 33+ 上系统「应用 → 语言」自己就能管理该应用的语言，
     * 这里再写一次空列表反而会把它清掉。
     */
    fun apply(context: Context) {
        val tag = AppLanguagePreferences(context).getLanguageSync()
        if (tag == AppLanguagePreferences.LANGUAGE_SYSTEM) return
        if (appliedTag == tag) return
        AppCompatDelegate.setApplicationLocales(localesFor(tag))
        appliedTag = tag
    }

    /** 用户在设置页点选：先落盘，再应用。切换会触发 Activity 重建，界面随即用新语言。 */
    fun setLanguage(context: Context, tag: String) {
        val prefs = AppLanguagePreferences(context)
        prefs.setLanguage(tag)
        val normalized = prefs.getLanguageSync()
        appliedTag = if (normalized == AppLanguagePreferences.LANGUAGE_SYSTEM) null else normalized
        AppCompatDelegate.setApplicationLocales(localesFor(normalized))
    }

    /** 当前生效的语言标签；`system` 表示跟随系统。设置页用它显示选中态。 */
    fun currentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) AppLanguagePreferences.LANGUAGE_SYSTEM else locales.toLanguageTags()
    }

    private fun localesFor(tag: String): LocaleListCompat =
        if (tag == AppLanguagePreferences.LANGUAGE_SYSTEM) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(tag)
}
