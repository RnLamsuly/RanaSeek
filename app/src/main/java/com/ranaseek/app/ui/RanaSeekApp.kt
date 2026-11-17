package com.ranaseek.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ranaseek.app.ui.chat.ChatScreen
import com.ranaseek.app.ui.chat.ChatViewModel
import com.ranaseek.app.ui.home.HomeScreen
import com.ranaseek.app.ui.settings.AppearanceScreen
import com.ranaseek.app.ui.settings.PersonaScreen
import com.ranaseek.app.ui.settings.SettingsScreen
import com.ranaseek.app.ui.theme.RanaSeekTheme
import com.ranaseek.app.ui.theme.RsTheme

private object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
    const val PERSONA = "persona"
}

@Composable
fun RanaSeekApp(vm: MainViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val chatVm: ChatViewModel = viewModel()
    val sessions by chatVm.sessions.collectAsStateWithLifecycle()

    RanaSeekTheme(mode = settings.themeMode) {
        SystemBarsAppearance(isDark = RsTheme.isDark)
        var route by rememberSaveable { mutableStateOf(Routes.HOME) }
        var chatPrompt by rememberSaveable { mutableStateOf("") }
        var chatDeep by rememberSaveable { mutableStateOf(false) }
        var chatWeb by rememberSaveable { mutableStateOf(false) }
        var chatImages by remember { mutableStateOf<List<String>>(emptyList()) }
        var chatFiles by remember { mutableStateOf<List<com.ranaseek.app.data.session.StoredFile>>(emptyList()) }

        Surface(modifier = Modifier.fillMaxSize()) {
            when (route) {
                Routes.CHAT -> ChatScreen(
                    settings = settings,
                    initialPrompt = chatPrompt,
                    initialDeepThinking = chatDeep,
                    initialWebSearch = chatWeb,
                    initialImages = chatImages,
                    initialFiles = chatFiles,
                    sessions = sessions,
                    onSelectSession = { id -> chatVm.openSession(id) },
                    onDeleteSession = chatVm::deleteSession,
                    onDeleteSessions = chatVm::deleteSessions,
                    onRenameSession = chatVm::renameSession,
                    onTogglePin = chatVm::togglePin,
                    onSetPinned = chatVm::setPinned,
                    onOpenSettings = { route = Routes.SETTINGS },
                    onNewChat = {
                        chatVm.newChat()
                        chatPrompt = ""
                        chatDeep = false
                        chatWeb = false
                        chatImages = emptyList()
                        chatFiles = emptyList()
                        route = Routes.HOME
                    },
                    onBackToHome = { route = Routes.HOME },
                    vm = chatVm
                )

                Routes.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onBack = { route = Routes.HOME },
                    onOpenAppearance = { route = Routes.APPEARANCE },
                    onOpenPersona = { route = Routes.PERSONA },
                    onApiKeyChange = vm::setApiKey,
                    onModelChange = vm::setModel,
                    onTestApiKey = vm::testApiKey
                )

                Routes.APPEARANCE -> AppearanceScreen(
                    settings = settings,
                    onBack = { route = Routes.SETTINGS },
                    onThemeMode = vm::setThemeMode,
                    onApplyBackground = vm::applyBackground,
                    onBlurChange = vm::setBackgroundBlur,
                    onDimChange = vm::setBackgroundDim,
                    onClearBackground = vm::clearBackground,
                    onBubbleStyle = vm::setBubbleStyle,
                    onAssistantBubble = vm::setAssistantBubble,
                    onAcrylicTint = vm::setAcrylicTint,
                    onAcrylicSaturation = vm::setAcrylicSaturation,
                    onAcrylicBlur = vm::setAcrylicBlur
                )

                Routes.PERSONA -> PersonaScreen(
                    initial = settings.globalPrompt,
                    onBack = { route = Routes.SETTINGS },
                    onSave = vm::setGlobalPrompt
                )

                else -> HomeScreen(
                    settings = settings,
                    sessions = sessions,
                    onSelectSession = { id ->
                        chatVm.openSession(id)
                        chatPrompt = ""
                        chatDeep = false
                        chatWeb = false
                        chatImages = emptyList()
                        chatFiles = emptyList()
                        route = Routes.CHAT
                    },
                    onDeleteSession = chatVm::deleteSession,
                    onDeleteSessions = chatVm::deleteSessions,
                    onRenameSession = chatVm::renameSession,
                    onTogglePin = chatVm::togglePin,
                    onSetPinned = chatVm::setPinned,
                    onNewChat = {
                        chatVm.newChat()
                        chatPrompt = ""
                        chatDeep = false
                        chatWeb = false
                        chatImages = emptyList()
                        chatFiles = emptyList()
                        route = Routes.CHAT
                    },
                    onOpenSettings = { route = Routes.SETTINGS },
                    onStartChat = { prompt, deep, web, images, files ->
                        chatVm.newChat()
                        chatPrompt = prompt
                        chatDeep = deep
                        chatWeb = web
                        chatImages = images
                        chatFiles = files
                        route = Routes.CHAT
                    },
                    uploadAttachment = chatVm::uploadAttachment,
                    transcribe = chatVm::transcribe
                )
            }
        }
    }
}

@Composable
private fun SystemBarsAppearance(isDark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }
}
