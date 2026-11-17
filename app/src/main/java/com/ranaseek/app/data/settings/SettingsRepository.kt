package com.ranaseek.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "rs_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class BubbleStyle { SOLID, ACRYLIC }

data class AppSettings(
    val apiKey: String = "",
    val model: String = "deepseek-flash",
    val globalPrompt: String = "",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val backgroundPath: String? = null,
    val backgroundBlur: Int = 0,
    /** 背景暗化（参考 DSH 窗口背景插件） */
    val backgroundDim: Int = 20,
    val bubbleStyle: BubbleStyle = BubbleStyle.SOLID,
    /** 助手回复是否显示气泡背景 */
    val assistantBubble: Boolean = true,
    /** 亚克力底色浓度 0-100 */
    val acrylicTint: Int = 38,
    /** 亚克力饱和度 100-200 */
    val acrylicSaturation: Int = 145,
    /** 亚克力模糊半径（px，0-30）：亚克力开启时背景磨砂程度 */
    val acrylicBlur: Int = 10
) {
    val hasApiKey: Boolean get() = apiKey.isNotBlank()
    val hasBackground: Boolean get() = !backgroundPath.isNullOrBlank()
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL = stringPreferencesKey("model")
        val GLOBAL_PROMPT = stringPreferencesKey("global_prompt")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BACKGROUND_PATH = stringPreferencesKey("background_path")
        val BACKGROUND_BLUR = intPreferencesKey("background_blur")
        val BACKGROUND_DIM = intPreferencesKey("background_dim")
        val BUBBLE_STYLE = stringPreferencesKey("bubble_style")
        val ASSISTANT_BUBBLE = booleanPreferencesKey("assistant_bubble")
        val ACRYLIC_TINT = intPreferencesKey("acrylic_tint")
        val ACRYLIC_SATURATION = intPreferencesKey("acrylic_saturation")
        val ACRYLIC_BLUR = intPreferencesKey("acrylic_blur")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            apiKey = p[Keys.API_KEY] ?: "",
            model = p[Keys.MODEL] ?: "deepseek-flash",
            globalPrompt = p[Keys.GLOBAL_PROMPT] ?: "",
            themeMode = p[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            backgroundPath = p[Keys.BACKGROUND_PATH],
            backgroundBlur = p[Keys.BACKGROUND_BLUR] ?: 0,
            backgroundDim = p[Keys.BACKGROUND_DIM] ?: 20,
            bubbleStyle = p[Keys.BUBBLE_STYLE]?.let { runCatching { BubbleStyle.valueOf(it) }.getOrNull() }
                ?: BubbleStyle.SOLID,
            assistantBubble = p[Keys.ASSISTANT_BUBBLE] ?: true,
            acrylicTint = p[Keys.ACRYLIC_TINT] ?: 38,
            acrylicSaturation = p[Keys.ACRYLIC_SATURATION] ?: 145,
            acrylicBlur = p[Keys.ACRYLIC_BLUR] ?: 10
        )
    }

    suspend fun setApiKey(value: String) = context.settingsStore.edit { it[Keys.API_KEY] = value }

    suspend fun setModel(value: String) = context.settingsStore.edit { it[Keys.MODEL] = value }

    suspend fun setGlobalPrompt(value: String) =
        context.settingsStore.edit { it[Keys.GLOBAL_PROMPT] = value }

    suspend fun setThemeMode(value: ThemeMode) =
        context.settingsStore.edit { it[Keys.THEME_MODE] = value.name }

    suspend fun setBackgroundPath(value: String?) = context.settingsStore.edit {
        if (value.isNullOrBlank()) it.remove(Keys.BACKGROUND_PATH) else it[Keys.BACKGROUND_PATH] = value
    }

    suspend fun setBackgroundBlur(value: Int) =
        context.settingsStore.edit { it[Keys.BACKGROUND_BLUR] = value.coerceIn(0, 25) }

    suspend fun setBubbleStyle(value: BubbleStyle) =
        context.settingsStore.edit { it[Keys.BUBBLE_STYLE] = value.name }

    suspend fun setAssistantBubble(value: Boolean) =
        context.settingsStore.edit { it[Keys.ASSISTANT_BUBBLE] = value }

    suspend fun setBackgroundDim(value: Int) =
        context.settingsStore.edit { it[Keys.BACKGROUND_DIM] = value.coerceIn(0, 80) }

    suspend fun setAcrylicTint(value: Int) =
        context.settingsStore.edit { it[Keys.ACRYLIC_TINT] = value.coerceIn(0, 100) }

    suspend fun setAcrylicSaturation(value: Int) =
        context.settingsStore.edit { it[Keys.ACRYLIC_SATURATION] = value.coerceIn(100, 200) }

    suspend fun setAcrylicBlur(value: Int) =
        context.settingsStore.edit { it[Keys.ACRYLIC_BLUR] = value.coerceIn(0, 30) }
}
