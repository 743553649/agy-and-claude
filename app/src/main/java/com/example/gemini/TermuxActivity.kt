package com.example.gemini

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.gemini.domain.i18n.AppLocaleController
import com.example.gemini.data.preferences.AuthPreferences
import com.example.gemini.theme.GeminiTheme
import com.example.gemini.theme.isSystemInDarkThemeRobust
import com.example.gemini.ui.components.LocalTerminalDialog

class TermuxActivity : AppCompatActivity() {

    private val authPreferences: AuthPreferences by lazy { AuthPreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 界面语言回设必须在 setContent 之前（TermuxActivity 有自己的 launcher alias，可冷启动）
        AppLocaleController.apply(this)
        enableEdgeToEdge()
        com.example.gemini.data.service.TermuxService.start(this)

        setContent {
            val initialThemeMode = remember { authPreferences.getThemeModeSync() }
            val themeMode by authPreferences.themeMode.collectAsState(initial = initialThemeMode)
            val isSystemDark = isSystemInDarkThemeRobust()
            val useDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemDark
            }

            GeminiTheme(darkTheme = useDarkTheme) {
                LocalTerminalDialog(
                    onDismiss = { finish() }
                )
            }
        }
    }
}
