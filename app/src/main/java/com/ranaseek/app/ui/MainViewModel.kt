package com.ranaseek.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ranaseek.app.data.settings.AppSettings
import com.ranaseek.app.data.settings.BubbleStyle
import com.ranaseek.app.data.settings.SettingsRepository
import com.ranaseek.app.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)
    private val chatRepo = com.ranaseek.app.data.repo.ChatRepository()

    suspend fun testApiKey(key: String): Result<String> = chatRepo.listModels(key)

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setApiKey(value: String) = viewModelScope.launch { repo.setApiKey(value) }
    fun setModel(value: String) = viewModelScope.launch { repo.setModel(value) }
    fun setGlobalPrompt(value: String) = viewModelScope.launch { repo.setGlobalPrompt(value) }
    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { repo.setThemeMode(value) }
    fun setBackgroundPath(value: String?) = viewModelScope.launch { repo.setBackgroundPath(value) }
    fun setBackgroundBlur(value: Int) = viewModelScope.launch { repo.setBackgroundBlur(value) }
    fun setBubbleStyle(value: BubbleStyle) = viewModelScope.launch { repo.setBubbleStyle(value) }
    fun setAssistantBubble(value: Boolean) = viewModelScope.launch { repo.setAssistantBubble(value) }
    fun setBackgroundDim(value: Int) = viewModelScope.launch { repo.setBackgroundDim(value) }
    fun setAcrylicTint(value: Int) = viewModelScope.launch { repo.setAcrylicTint(value) }
    fun setAcrylicSaturation(value: Int) = viewModelScope.launch { repo.setAcrylicSaturation(value) }
    fun setAcrylicBlur(value: Int) = viewModelScope.launch { repo.setAcrylicBlur(value) }

    /** 选好背景图后：默认切到浅色主题（背景在浅色下更协调）。 */
    fun applyBackground(path: String) {
        viewModelScope.launch {
            repo.setBackgroundPath(path)
            if (settings.value.themeMode == ThemeMode.DARK) {
                repo.setThemeMode(ThemeMode.LIGHT)
            }
        }
    }

    fun clearBackground() = viewModelScope.launch { repo.setBackgroundPath(null) }
}
