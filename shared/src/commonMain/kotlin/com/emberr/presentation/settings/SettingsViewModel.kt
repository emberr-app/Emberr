package com.emberr.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.backup.automatic.BackupRescheduler
import com.emberr.domain.ai.AiPurgeReport
import com.emberr.domain.ai.DisableAiFeaturesUseCase
import com.emberr.domain.cloud.api.CloudApiErrorCode
import com.emberr.domain.cloud.api.CloudApiException
import com.emberr.domain.cloud.api.CloudServerUnreachableException
import com.emberr.domain.cloud.sync.CloudEraseResult
import com.emberr.domain.cloud.sync.CloudSyncScheduler
import com.emberr.domain.selfhost.sync.SelfHostConnectionState
import com.emberr.domain.selfhost.sync.SelfHostSyncNetwork
import com.emberr.domain.selfhost.sync.SelfHostSyncScheduler
import com.emberr.domain.space.SpaceRepository
import com.emberr.domain.sync.SyncNetworkRule
import com.emberr.domain.util.format.formatByteSize
import com.emberr.domain.util.system.eraseAllAppData
import com.emberr.domain.util.system.isDebugBuild
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.home.overview.OverviewSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.settings_ai_purge_failed
import emberr.shared.generated.resources.settings_ai_purge_files_left
import emberr.shared.generated.resources.settings_ai_purge_freed
import emberr.shared.generated.resources.settings_ai_purge_nothing_to_remove
import emberr.shared.generated.resources.settings_clear_data_failed
import emberr.shared.generated.resources.settings_erase_cloud_device_not_cleared
import emberr.shared.generated.resources.settings_erase_cloud_incorrect_password
import emberr.shared.generated.resources.settings_erase_cloud_server_unreachable
import emberr.shared.generated.resources.settings_erase_cloud_signed_out
import emberr.shared.generated.resources.settings_erase_cloud_something_went_wrong
import emberr.shared.generated.resources.settings_erase_cloud_too_many_tries
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val backupRescheduler: BackupRescheduler,
    private val disableAiFeaturesUseCase: DisableAiFeaturesUseCase,
    private val appScope: CoroutineScope,
    spaceRepository: SpaceRepository,
    private val cloudEraser: suspend (password: String) -> CloudEraseResult,
    private val syncNetworkRule: SyncNetworkRule,
    private val selfHostConnectionState: SelfHostConnectionState,
    private val selfHostSyncScheduler: SelfHostSyncScheduler,
    private val cloudSyncScheduler: Lazy<CloudSyncScheduler>
) : ViewModel() {

    val spaces: StateFlow<List<SpaceEntity>> = spaceRepository.observeSpaces().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val autoBackupEnabled: StateFlow<Boolean> = settingsManager.autoBackupEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val backupFrequency: StateFlow<String> = settingsManager.backupFrequencyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Daily"
    )

    val backupDirectoryUri: StateFlow<String?> = settingsManager.backupDirectoryUriFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _syncNetwork = MutableStateFlow(syncNetworkRule.chosenNetwork())
    val syncNetwork: StateFlow<SelfHostSyncNetwork> = _syncNetwork.asStateFlow()

    fun setSyncNetwork(syncNetwork: SelfHostSyncNetwork) {
        settingsManager.saveSelfHostSyncNetwork(syncNetwork.name)
        _syncNetwork.value = syncNetwork
        if (selfHostConnectionState.isConnected.value) selfHostSyncScheduler.scheduleBackgroundSync()
        if (isDebugBuild) cloudSyncScheduler.value.rescheduleBackgroundSync()
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        settingsManager.saveAutoBackupEnabled(enabled)
    }

    fun setBackupDirectory(uriString: String) {
        settingsManager.saveBackupDirectory(uriString)
    }

    val backupTime: StateFlow<String> = settingsManager.backupTimeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "02:00"
    )

    val backupDay: StateFlow<String> = settingsManager.backupDayFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Sunday"
    )

    fun saveBackupSchedule(frequency: String, time: String, day: String) {
        settingsManager.saveBackupFrequency(frequency)
        settingsManager.saveBackupTime(time)
        settingsManager.saveBackupDay(day)
        backupRescheduler.rescheduleNow(frequency, time, day)
    }

    val themePreference: StateFlow<String> = settingsManager.themePreferenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.emberr.data.local.prefs.SyncConstants.DEFAULT_THEME_PREFERENCE
    )

    fun setThemePreference(preference: String) {
        settingsManager.saveThemePreference(preference)
    }

    val colorThemePreference: StateFlow<String> = settingsManager.colorThemePreferenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.getColorThemePreference()
    )

    fun setColorThemePreference(preference: String) {
        settingsManager.saveColorThemePreference(preference)
    }

    val themedTextAndIcons: StateFlow<Boolean> = settingsManager.themedTextAndIconsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isThemedTextAndIconsEnabled()
    )

    fun setThemedTextAndIcons(enabled: Boolean) {
        settingsManager.saveThemedTextAndIcons(enabled)
    }

    val colorThemeStrength: StateFlow<Int> = settingsManager.colorThemeStrengthFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.getColorThemeStrength()
    )

    fun setColorThemeStrength(strengthPercent: Int) {
        settingsManager.saveColorThemeStrength(strengthPercent)
    }

    val paperTextureEnabled: StateFlow<Boolean> = settingsManager.paperTextureEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isPaperTextureEnabled()
    )

    fun setPaperTextureEnabled(enabled: Boolean) {
        settingsManager.savePaperTextureEnabled(enabled)
    }

    val paperTextureStrength: StateFlow<Int> = settingsManager.paperTextureStrengthFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.getPaperTextureStrength()
    )

    fun setPaperTextureStrength(strengthPercent: Int) {
        settingsManager.savePaperTextureStrength(strengthPercent)
    }

    val paperTextureAreas: StateFlow<String> = settingsManager.paperTextureAreasFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.getPaperTextureAreas()
    )

    fun setPaperTextureAreas(areas: String) {
        settingsManager.savePaperTextureAreas(areas)
    }

    val fontSizePreference: StateFlow<String> = settingsManager.fontSizePreferenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.emberr.data.local.prefs.SyncConstants.DEFAULT_FONT_SIZE_PREFERENCE
    )

    fun setFontSizePreference(preference: String) {
        settingsManager.saveFontSizePreference(preference)
    }

    val interfaceScalePercent: StateFlow<Int> = settingsManager.interfaceScalePercentFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.getInterfaceScalePercent()
    )

    fun setInterfaceScalePercent(percent: Int) {
        settingsManager.saveInterfaceScalePercent(percent)
        showInterfaceScaleToast(percent)
    }

    val fontStylePreference: StateFlow<String> = settingsManager.fontStylePreferenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.emberr.data.local.prefs.SyncConstants.DEFAULT_FONT_STYLE_PREFERENCE
    )

    fun setFontStylePreference(preference: String) {
        settingsManager.saveFontStylePreference(preference)
    }

    val timeFormatPreference: StateFlow<String> = settingsManager.timeFormatPreferenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.getTimeFormatPreference()
    )

    fun setTimeFormatPreference(preference: String) {
        settingsManager.saveTimeFormatPreference(preference)
    }

    val firstDayOfWeekPreference: StateFlow<String> = settingsManager.firstDayOfWeekPreferenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.getFirstDayOfWeekPreference()
    )

    fun setFirstDayOfWeekPreference(preference: String) {
        settingsManager.saveFirstDayOfWeekPreference(preference)
    }

    val topBarFadeStyle: StateFlow<String> = settingsManager.topBarFadeStyleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.getTopBarFadeStyle()
    )

    fun setTopBarFadeStyle(style: String) {
        settingsManager.saveTopBarFadeStyle(style)
    }

    val topBarFadeProgressive: StateFlow<Boolean> = settingsManager.topBarFadeProgressiveFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isTopBarFadeProgressive()
    )

    fun setTopBarFadeProgressive(enabled: Boolean) {
        settingsManager.saveTopBarFadeProgressive(enabled)
    }

    val subNoteOpenMode: StateFlow<String> = settingsManager.subNoteOpenModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.emberr.data.local.prefs.SyncConstants.DEFAULT_SUBNOTE_OPEN_MODE
    )

    fun setSubNoteOpenMode(mode: String) {
        settingsManager.saveSubNoteOpenMode(mode)
    }

    val showScrollbar: StateFlow<Boolean> = settingsManager.showScrollbarFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isShowScrollbarEnabled()
    )

    fun setShowScrollbar(enabled: Boolean) {
        settingsManager.saveShowScrollbar(enabled)
    }

    val dailyNotesEnabled: StateFlow<Boolean> = settingsManager.dailyNotesEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isDailyNotesEnabled()
    )

    fun setDailyNotesEnabled(enabled: Boolean) {
        settingsManager.saveDailyNotesEnabled(enabled)
    }

    val startOnHome: StateFlow<Boolean> = settingsManager.startOnHomeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isStartOnHomeEnabled()
    )

    fun setStartOnHome(enabled: Boolean) {
        settingsManager.saveStartOnHome(enabled)
    }

    val micButtonVisible: StateFlow<Boolean> = settingsManager.micButtonVisibleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isMicButtonVisible()
    )

    fun setMicButtonVisible(visible: Boolean) {
        settingsManager.saveMicButtonVisible(visible)
    }

    val dailyQuoteVisible: StateFlow<Boolean> = settingsManager.dailyQuoteVisibleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isDailyQuoteVisible()
    )

    fun setDailyQuoteVisible(visible: Boolean) {
        settingsManager.saveDailyQuoteVisible(visible)
    }

    val hiddenOverviewSections: StateFlow<Set<String>> = settingsManager.hiddenOverviewSectionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.getHiddenOverviewSections()
    )

    fun setOverviewSectionVisible(section: OverviewSection, isVisible: Boolean) {
        val hiddenStorageKeys = settingsManager.getHiddenOverviewSections()
        settingsManager.saveHiddenOverviewSections(
            if (isVisible) hiddenStorageKeys - section.storageKey else hiddenStorageKeys + section.storageKey
        )
    }

    val customWindowFrameEnabled: StateFlow<Boolean> =
        settingsManager.customWindowFrameEnabledFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = settingsManager.isCustomWindowFrameEnabled()
        )

    fun setCustomWindowFrameEnabled(enabled: Boolean) {
        settingsManager.saveCustomWindowFrameEnabled(enabled)
    }

    val autoHideTitleBar: StateFlow<Boolean> = settingsManager.autoHideTitleBarFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isAutoHideTitleBarEnabled()
    )

    fun setAutoHideTitleBar(enabled: Boolean) {
        settingsManager.saveAutoHideTitleBar(enabled)
    }

    val aiFeaturesDisabled: StateFlow<Boolean> = settingsManager.aiFeaturesDisabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = settingsManager.isAiFeaturesDisabled()
    )

    private val _isPurgingAiData = MutableStateFlow(false)
    val isPurgingAiData: StateFlow<Boolean> = _isPurgingAiData.asStateFlow()

    private val _aiPurgeResultMessage = MutableStateFlow<String?>(null)
    val aiPurgeResultMessage: StateFlow<String?> = _aiPurgeResultMessage.asStateFlow()

    fun consumeAiPurgeResultMessage() {
        _aiPurgeResultMessage.value = null
    }

    fun setAiFeaturesDisabled(disabled: Boolean) {
        if (!disabled) {
            disableAiFeaturesUseCase.enableAiFeatures()
            return
        }
        if (_isPurgingAiData.value) return

        _isPurgingAiData.value = true
        appScope.launch {
            try {
                val report = disableAiFeaturesUseCase.execute()
                _aiPurgeResultMessage.value = describePurgeResult(report)
            } catch (e: Exception) {
                _aiPurgeResultMessage.value = getString(Res.string.settings_ai_purge_failed, e.message.toString())
            } finally {
                _isPurgingAiData.value = false
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            if (!eraseAllAppData()) {
                showNativeToast(getString(Res.string.settings_clear_data_failed))
            }
        }
    }

    private val _isErasingCloudData = MutableStateFlow(false)
    val isErasingCloudData: StateFlow<Boolean> = _isErasingCloudData.asStateFlow()

    private val _cloudEraseErrorMessage = MutableStateFlow<String?>(null)
    val cloudEraseErrorMessage: StateFlow<String?> = _cloudEraseErrorMessage.asStateFlow()

    fun consumeCloudEraseErrorMessage() {
        _cloudEraseErrorMessage.value = null
    }

    fun eraseCloudDataAndThisDevice(password: String) {
        if (_isErasingCloudData.value || password.isEmpty()) return
        _isErasingCloudData.value = true
        _cloudEraseErrorMessage.value = null
        appScope.launch {
            val result = try {
                cloudEraser(password)
            } catch (error: Exception) {
                CloudEraseResult.NothingErased(error)
            }
            _cloudEraseErrorMessage.value = cloudEraseErrorMessage(result)
            if (result != CloudEraseResult.Erased) _isErasingCloudData.value = false
        }
    }

    private suspend fun cloudEraseErrorMessage(result: CloudEraseResult): String? = when (result) {
        CloudEraseResult.Erased -> null
        CloudEraseResult.ThisDeviceNotCleared ->
            getString(Res.string.settings_erase_cloud_device_not_cleared)
        is CloudEraseResult.NothingErased -> when (val error = result.error) {
            is CloudApiException -> when (error.code) {
                CloudApiErrorCode.INVALID_PASSWORD -> getString(Res.string.settings_erase_cloud_incorrect_password)
                CloudApiErrorCode.TOO_MANY_REQUESTS -> getString(Res.string.settings_erase_cloud_too_many_tries)
                CloudApiErrorCode.INVALID_TOKEN -> getString(Res.string.settings_erase_cloud_signed_out)
                else -> getString(Res.string.settings_erase_cloud_something_went_wrong)
            }
            is CloudServerUnreachableException ->
                getString(Res.string.settings_erase_cloud_server_unreachable, settingsManager.getCloudServerAddress())
            else -> getString(Res.string.settings_erase_cloud_something_went_wrong)
        }
    }

    private suspend fun describePurgeResult(report: AiPurgeReport): String {
        val blockedPaths = report.undeletablePaths + report.survivingPaths
        return when {
            blockedPaths.isNotEmpty() ->
                getPluralString(
                    Res.plurals.settings_ai_purge_files_left,
                    blockedPaths.size,
                    formatByteSize(report.bytesFreed),
                    blockedPaths.size,
                    blockedPaths.joinToString(", ") { it.substringAfterLast('/') }
                )
            report.bytesFreed > 0L -> getString(Res.string.settings_ai_purge_freed, formatByteSize(report.bytesFreed))
            else -> getString(Res.string.settings_ai_purge_nothing_to_remove)
        }
    }
}