package com.emberr.presentation.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.data.local.prefs.SyncConstants
import com.emberr.domain.backup.manual.BackupContentChoice
import com.emberr.domain.selfhost.sync.SelfHostSyncNetwork
import com.emberr.domain.sync.SyncPairingData
import com.emberr.domain.sync.SyncServerStatus
import com.emberr.domain.util.system.AppPermission
import com.emberr.domain.util.system.appVersionName
import com.emberr.domain.util.system.isDebugBuild
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.restartApplication
import com.emberr.domain.util.language.AppLanguage
import com.emberr.domain.util.language.appLanguageChangeNeedsRestart
import com.emberr.domain.util.language.chooseAppLanguage
import com.emberr.domain.util.language.chosenAppLanguage
import com.emberr.presentation.shared.editor.ActiveEditorRegistry
import kotlinx.coroutines.launch
import com.emberr.domain.util.system.rememberAppPermissionCoordinator
import com.emberr.presentation.settings.about.AboutScreen
import com.emberr.presentation.settings.about.OpenSourceLicensesScreen
import com.emberr.presentation.settings.about.ThirdPartyServicesScreen
import com.emberr.presentation.settings.importer.ImportScreen
import com.emberr.presentation.settings.cloud.CloudAccountScreen
import com.emberr.presentation.settings.cloud.PasswordField
import com.emberr.presentation.settings.selfhost.SelfHostSetupScreen
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.LocalSitsOnCard
import com.emberr.presentation.whatsnew.EmberrUpdateDialog
import com.emberr.presentation.shared.components.EmberrVerticalScrollbar
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrSearchSheetBody
import com.emberr.presentation.shared.components.EmberrChoiceValueMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrSwitch
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.SmallRoundedCornerShape
import com.emberr.presentation.shared.components.emberrControlContainerColor
import com.emberr.presentation.shared.components.SelectedOptionBackground
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.TopHeaderBarButtonSize
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.presentation.sync.SyncPairingDialog
import com.emberr.presentation.sync.SyncScannerDialog
import com.emberr.presentation.sync.SyncViewModel
import com.emberr.presentation.sync.text
import com.emberr.domain.util.system.showNativeToast
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.emberr.ui.theme.FontSizePreference
import com.emberr.ui.theme.FontStylePreference
import com.emberr.ui.theme.LARGEST_INTERFACE_SCALE_PERCENT
import com.emberr.ui.theme.SMALLEST_INTERFACE_SCALE_PERCENT
import com.emberr.ui.theme.interfaceScalePercentMovedBySteps
import com.emberr.ui.theme.colorThemePreferenceNamed
import com.emberr.ui.theme.fontFamilyFor
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import com.emberr.domain.util.time.TimeFormatPreference
import com.emberr.domain.util.time.FirstDayOfWeekPreference
import com.emberr.domain.util.time.formatTimeOfDay
import com.emberr.domain.util.time.weekdayName
import kotlinx.datetime.DayOfWeek
import com.emberr.domain.util.time.timeFormatPreferenceNamed
import com.emberr.domain.util.time.firstDayOfWeekPreferenceNamed
import com.emberr.presentation.home.overview.OverviewSection
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_back
import emberr.shared.generated.resources.common_cancel
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.common_delete
import emberr.shared.generated.resources.common_off
import emberr.shared.generated.resources.common_on
import emberr.shared.generated.resources.common_percent_value
import emberr.shared.generated.resources.common_save
import emberr.shared.generated.resources.common_version_label
import emberr.shared.generated.resources.settings_about_emberr
import emberr.shared.generated.resources.settings_ai_off_hint
import emberr.shared.generated.resources.settings_ai_on_hint
import emberr.shared.generated.resources.settings_ai_removing_data
import emberr.shared.generated.resources.settings_auto_hide_title_bar
import emberr.shared.generated.resources.settings_auto_hide_title_bar_hint
import emberr.shared.generated.resources.settings_automatic_backups
import emberr.shared.generated.resources.settings_backup_day
import emberr.shared.generated.resources.settings_backup_frequency_daily
import emberr.shared.generated.resources.settings_backup_frequency_weekly
import emberr.shared.generated.resources.settings_backup_location
import emberr.shared.generated.resources.settings_backup_location_change
import emberr.shared.generated.resources.settings_backup_time
import emberr.shared.generated.resources.settings_clear_all_data
import emberr.shared.generated.resources.settings_clear_all_data_desktop_hint
import emberr.shared.generated.resources.settings_clear_all_data_dialog_message
import emberr.shared.generated.resources.settings_clear_all_data_dialog_title
import emberr.shared.generated.resources.settings_cloud_blocked_by_lan_sync
import emberr.shared.generated.resources.settings_cloud_blocked_by_self_host
import emberr.shared.generated.resources.settings_custom_title_bar
import emberr.shared.generated.resources.settings_custom_title_bar_hint
import emberr.shared.generated.resources.settings_daily_notes
import emberr.shared.generated.resources.settings_daily_notes_hint
import emberr.shared.generated.resources.settings_daily_quote
import emberr.shared.generated.resources.settings_daily_quote_hint
import emberr.shared.generated.resources.settings_emberr_cloud
import emberr.shared.generated.resources.settings_erase
import emberr.shared.generated.resources.settings_erase_cloud_dialog_message
import emberr.shared.generated.resources.settings_erase_cloud_password
import emberr.shared.generated.resources.settings_erasing
import emberr.shared.generated.resources.settings_export_backup
import emberr.shared.generated.resources.settings_faq
import emberr.shared.generated.resources.settings_first_day_of_week
import emberr.shared.generated.resources.settings_first_day_of_week_desktop_hint
import emberr.shared.generated.resources.settings_font_size
import emberr.shared.generated.resources.settings_font_size_default
import emberr.shared.generated.resources.settings_font_size_extra_large
import emberr.shared.generated.resources.settings_font_size_extra_small
import emberr.shared.generated.resources.settings_font_size_large
import emberr.shared.generated.resources.settings_font_size_small
import emberr.shared.generated.resources.settings_font_style
import emberr.shared.generated.resources.settings_language
import emberr.shared.generated.resources.settings_search_languages
import emberr.shared.generated.resources.settings_language_restart_message
import emberr.shared.generated.resources.settings_group_ai
import emberr.shared.generated.resources.settings_group_appearance
import emberr.shared.generated.resources.settings_group_danger_zone
import emberr.shared.generated.resources.settings_group_data_storage
import emberr.shared.generated.resources.settings_group_date_time
import emberr.shared.generated.resources.settings_group_desktop_window
import emberr.shared.generated.resources.settings_group_editor
import emberr.shared.generated.resources.settings_group_lan_sync
import emberr.shared.generated.resources.settings_group_layout
import emberr.shared.generated.resources.settings_group_need_help
import emberr.shared.generated.resources.settings_group_overview
import emberr.shared.generated.resources.settings_group_reminders
import emberr.shared.generated.resources.settings_group_sync_backup
import emberr.shared.generated.resources.settings_import
import emberr.shared.generated.resources.settings_interface_scale
import emberr.shared.generated.resources.settings_interface_scale_bigger
import emberr.shared.generated.resources.settings_interface_scale_hint
import emberr.shared.generated.resources.settings_interface_scale_reset
import emberr.shared.generated.resources.settings_interface_scale_smaller
import emberr.shared.generated.resources.settings_keyboard_toolbar
import emberr.shared.generated.resources.settings_lan_sync_blocked_by_cloud
import emberr.shared.generated.resources.settings_lan_sync_blocked_by_self_host
import emberr.shared.generated.resources.settings_lan_sync_paired_desktop
import emberr.shared.generated.resources.settings_mic_button
import emberr.shared.generated.resources.settings_open_home_first
import emberr.shared.generated.resources.settings_overview_sidebar_footnote
import emberr.shared.generated.resources.settings_pair_mobile_device
import emberr.shared.generated.resources.settings_pair_with_desktop
import emberr.shared.generated.resources.settings_privacy_policy
import emberr.shared.generated.resources.settings_restart_later
import emberr.shared.generated.resources.settings_restart_now
import emberr.shared.generated.resources.settings_restart_to_apply_message
import emberr.shared.generated.resources.settings_restart_to_apply_title
import emberr.shared.generated.resources.settings_select_backup_day
import emberr.shared.generated.resources.settings_self_host
import emberr.shared.generated.resources.settings_self_host_blocked_by_cloud
import emberr.shared.generated.resources.settings_self_host_blocked_by_lan_sync
import emberr.shared.generated.resources.settings_show_scrollbar
import emberr.shared.generated.resources.settings_show_scrollbar_hint
import emberr.shared.generated.resources.settings_subnote_opening
import emberr.shared.generated.resources.settings_sync_network_both
import emberr.shared.generated.resources.settings_sync_network_data_only
import emberr.shared.generated.resources.settings_sync_network_hint
import emberr.shared.generated.resources.settings_sync_network_wifi_only
import emberr.shared.generated.resources.settings_sync_now
import emberr.shared.generated.resources.settings_sync_port
import emberr.shared.generated.resources.settings_sync_port_hint
import emberr.shared.generated.resources.settings_theme
import emberr.shared.generated.resources.settings_time_format
import emberr.shared.generated.resources.settings_title
import emberr.shared.generated.resources.settings_top_bar_fade
import emberr.shared.generated.resources.settings_top_bar_fade_progressive
import emberr.shared.generated.resources.settings_top_bar_fade_subtitle
import emberr.shared.generated.resources.settings_turn_off
import emberr.shared.generated.resources.settings_turn_off_ai_dialog_message
import emberr.shared.generated.resources.settings_turn_off_ai_dialog_title
import emberr.shared.generated.resources.settings_turn_off_ai_features
import emberr.shared.generated.resources.settings_unpair
import emberr.shared.generated.resources.settings_unpair_dialog_message_desktop
import emberr.shared.generated.resources.settings_unpair_dialog_message_phone
import emberr.shared.generated.resources.settings_unpair_dialog_title
import emberr.shared.generated.resources.settings_unpair_from_desktop
import emberr.shared.generated.resources.settings_unpair_from_mobile_device
import emberr.shared.generated.resources.settings_unrestricted_background
import emberr.shared.generated.resources.settings_whats_new
import emberr.shared.generated.resources.astroid
import emberr.shared.generated.resources.badge_plus
import emberr.shared.generated.resources.badge_question_mark
import emberr.shared.generated.resources.bell
import emberr.shared.generated.resources.calendar
import emberr.shared.generated.resources.calendar_clock
import emberr.shared.generated.resources.daily
import emberr.shared.generated.resources.download
import emberr.shared.generated.resources.chevron_left
import emberr.shared.generated.resources.chevron_right
import emberr.shared.generated.resources.files
import emberr.shared.generated.resources.folder_input
import emberr.shared.generated.resources.folder_sync
import emberr.shared.generated.resources.house
import emberr.shared.generated.resources.info
import emberr.shared.generated.resources.keyboard
import emberr.shared.generated.resources.lock
import emberr.shared.generated.resources.message_lan_sync_could_not_start
import emberr.shared.generated.resources.message_lan_sync_port_in_use
import emberr.shared.generated.resources.microphone
import emberr.shared.generated.resources.quote_down2
import emberr.shared.generated.resources.palette
import emberr.shared.generated.resources.globe
import emberr.shared.generated.resources.qr_code
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.scan_line
import emberr.shared.generated.resources.share
import emberr.shared.generated.resources.shield_alert
import emberr.shared.generated.resources.sidebar
import emberr.shared.generated.resources.scale
import emberr.shared.generated.resources.minus
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.sliders_horizontal
import emberr.shared.generated.resources.text_type
import emberr.shared.generated.resources.clock_circle
import emberr.shared.generated.resources.timer_reset
import emberr.shared.generated.resources.triangle_alert
import com.emberr.presentation.shared.SubNoteOpenMode
import com.emberr.presentation.shared.TopBarFadeStyle
import com.emberr.presentation.shared.components.orHighContrastLine
import com.emberr.presentation.shared.components.highContrastBorder
import com.emberr.presentation.shared.LanguageChangeTransition
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.emberr.presentation.home.overview.label

private const val PRIVACY_POLICY_URL = "https://github.com/emberr-app/Emberr/blob/main/PRIVACY.md"
private const val LANGUAGE_NAMES_ROLLED_PAST = 8
private const val LANGUAGE_NAMES_SHOWN_BELOW = 2
private val SettingsCardShape = RoundedCornerShape(18.dp)
private val SettingsRowMinHeight = 62.dp
private val SettingsRowSidePadding = 16.dp
private val SettingsPaneMaxWidth = 760.dp
private val LanguageMenuMaxHeight = 520.dp
private const val SUB_PAGE_FADE_MILLIS = 150

internal fun desktopSettingsSidePadding(availableWidth: Dp): Dp =
    if (isDesktopPlatform) ((availableWidth - SettingsPaneMaxWidth) / 2).coerceAtLeast(0.dp) else 0.dp

private val BackupFrequencyLabels = mapOf(
    "Daily" to Res.string.settings_backup_frequency_daily,
    "Weekly" to Res.string.settings_backup_frequency_weekly
)
private val SyncNetworkLabels = listOf(
    SelfHostSyncNetwork.WIFI_AND_MOBILE_DATA to Res.string.settings_sync_network_both,
    SelfHostSyncNetwork.WIFI_ONLY to Res.string.settings_sync_network_wifi_only,
    SelfHostSyncNetwork.MOBILE_DATA_ONLY to Res.string.settings_sync_network_data_only
)
private val FontSizeOptions = listOf(
    FontSizePreference.EXTRA_SMALL.name to Res.string.settings_font_size_extra_small,
    FontSizePreference.SMALL.name to Res.string.settings_font_size_small,
    FontSizePreference.DEFAULT.name to Res.string.settings_font_size_default,
    FontSizePreference.LARGE.name to Res.string.settings_font_size_large,
    FontSizePreference.EXTRA_LARGE.name to Res.string.settings_font_size_extra_large
)

private class SettingsCategory(
    val title: String,
    val content: @Composable () -> Unit
)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onExportReady: (choice: BackupContentChoice) -> Unit = {},
    onRequestBackupFolder: () -> Unit = {},
    onNavigateToSelfHostSetup: () -> Unit = {},
    onNavigateToEmberrCloud: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToImportNotes: () -> Unit = {},
    onNavigateToKeyboardToolbarSettings: () -> Unit = {},
    onNavigateToColorThemeSettings: () -> Unit = {},
    desktopSubPage: SettingsSubPage? = null,
    onDesktopSubPageChange: (SettingsSubPage?) -> Unit = {},
    showBackButton: Boolean = true,
    viewModel: SettingsViewModel = koinViewModel(),
    syncViewModel: SyncViewModel = koinViewModel()
) {
    val isPaired by syncViewModel.isPaired.collectAsState()
    val isSelfHostConnected by syncViewModel.isSelfHostConnected.collectAsState()
    val isSignedInToCloud by syncViewModel.isSignedInToCloud.collectAsState()
    val clearAllDataErasesCloudData = isDebugBuild && isSignedInToCloud
    val isErasingCloudData by viewModel.isErasingCloudData.collectAsState()
    val cloudEraseErrorMessage by viewModel.cloudEraseErrorMessage.collectAsState()
    val syncStatus by syncViewModel.syncStatus.collectAsState()
    val syncStatusText = syncStatus.text()
    val serverStatus = syncViewModel.serverStatus?.collectAsState()?.value
    val serverUnavailableReason = (serverStatus as? SyncServerStatus.Unavailable)?.let { unavailable ->
        when {
            unavailable.isPortAlreadyTaken -> stringResource(Res.string.message_lan_sync_port_in_use, unavailable.port)
            unavailable.errorDetail != null -> unavailable.errorDetail
            else -> stringResource(Res.string.message_lan_sync_could_not_start, unavailable.port)
        }
    }
    var showPairingDialog by remember { mutableStateOf(false) }
    var activePairingData by remember { mutableStateOf<SyncPairingData?>(null) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showUnpairConfirmation by remember { mutableStateOf(false) }
    var syncPortInput by remember { mutableStateOf(syncViewModel.getSyncPort().toString()) }

    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val backupFrequency by viewModel.backupFrequency.collectAsState()
    val syncNetwork by viewModel.syncNetwork.collectAsState()
    val backupDirectoryUri by viewModel.backupDirectoryUri.collectAsState()

    val backupTime by viewModel.backupTime.collectAsState()
    val backupDay by viewModel.backupDay.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }

    val colorThemePreference by viewModel.colorThemePreference.collectAsState()

    val fontSizePreference by viewModel.fontSizePreference.collectAsState()
    val interfaceScalePercent by viewModel.interfaceScalePercent.collectAsState()
    val fontStylePreference by viewModel.fontStylePreference.collectAsState()

    val timeFormatPreference by viewModel.timeFormatPreference.collectAsState()

    var chosenLanguage by remember { mutableStateOf(chosenAppLanguage()) }
    var showLanguageRestartNotice by remember { mutableStateOf(false) }
    val languageLabels = AppLanguage.entries.associateWith { language -> language.label() }

    val firstDayOfWeekPreference by viewModel.firstDayOfWeekPreference.collectAsState()

    val topBarFadeStyle by viewModel.topBarFadeStyle.collectAsState()
    val topBarFadeProgressive by viewModel.topBarFadeProgressive.collectAsState()
    var showTopBarFadeStyleSheet by remember { mutableStateOf(false) }

    val subNoteOpenMode by viewModel.subNoteOpenMode.collectAsState()

    val showScrollbar by viewModel.showScrollbar.collectAsState()
    val dailyNotesEnabled by viewModel.dailyNotesEnabled.collectAsState()
    val startOnHome by viewModel.startOnHome.collectAsState()
    val micButtonVisible by viewModel.micButtonVisible.collectAsState()
    val dailyQuoteVisible by viewModel.dailyQuoteVisible.collectAsState()
    val hiddenOverviewSections by viewModel.hiddenOverviewSections.collectAsState()
    val customWindowFrameEnabled by viewModel.customWindowFrameEnabled.collectAsState()
    val autoHideTitleBar by viewModel.autoHideTitleBar.collectAsState()

    val aiFeaturesDisabled by viewModel.aiFeaturesDisabled.collectAsState()
    val isPurgingAiData by viewModel.isPurgingAiData.collectAsState()
    val aiPurgeResultMessage by viewModel.aiPurgeResultMessage.collectAsState()
    var showDisableAiConfirmation by remember { mutableStateOf(false) }
    var showClearAllDataConfirmation by remember { mutableStateOf(false) }
    val spaces by viewModel.spaces.collectAsState()
    var showExportSpacesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(aiPurgeResultMessage) {
        aiPurgeResultMessage?.let { message ->
            showNativeToast(message)
            viewModel.consumeAiPurgeResultMessage()
        }
    }

    LaunchedEffect(syncStatus) {
        if (syncStatus.shouldShowAsMessage) {
            showNativeToast(syncStatusText)
            syncViewModel.resetSyncStatus()
        }
    }

    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    val desktopListScrollState = rememberScrollState()
    val desktopTopEdgeFadeAlpha by remember(desktopSubPage) {
        derivedStateOf { if (desktopSubPage == null && desktopListScrollState.value > 0) 1f else 0f }
    }
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    val categories = buildList {
        if (!isDesktopPlatform) {
            add(
                SettingsCategory(title = "Reminders") {
                    RemindersSettingsSection()
                }
            )
        }

        add(
            SettingsCategory(title = DATA_CATEGORY_TITLE) {
                DataSettingsSection(
                    autoBackupEnabled = autoBackupEnabled,
                    backupFrequency = backupFrequency,
                    backupTime = backupTime,
                    backupDay = backupDay,
                    onExportClick = { showExportSpacesSheet = true },
                    onImportNotesClick = onNavigateToImportNotes,
                    onAutoBackupChange = { isChecked ->
                        when {
                            !isChecked -> viewModel.setAutoBackupEnabled(false)
                            backupDirectoryUri == null -> onRequestBackupFolder()
                            else -> viewModel.setAutoBackupEnabled(true)
                        }
                    },
                    onBackupFrequencySelected = { frequency ->
                        viewModel.saveBackupSchedule(frequency, backupTime, backupDay)
                    },
                    onPickBackupDay = { showDayPicker = true },
                    onPickBackupTime = { showTimePicker = true },
                    onChooseBackupFolder = onRequestBackupFolder
                )
            }
        )

        add(
            SettingsCategory(title = SYNC_CATEGORY_TITLE) {
                SyncSettingsSection(
                    isPaired = isPaired,
                    isSelfHostConnected = isSelfHostConnected,
                    isSignedInToCloud = isSignedInToCloud,
                    syncNetwork = syncNetwork,
                    onSyncNetworkSelected = viewModel::setSyncNetwork,
                    syncStatus = syncStatusText,
                    serverUnavailableReason = serverUnavailableReason,
                    syncPortInput = syncPortInput,
                    onSyncPortChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }.take(5)
                        syncPortInput = digitsOnly
                        val port = digitsOnly.toIntOrNull()
                        if (port != null && port in 1024..65535) {
                            syncViewModel.setSyncPort(port)
                        }
                    },
                    onOpenSelfHostSetup = onNavigateToSelfHostSetup,
                    onOpenEmberrCloud = onNavigateToEmberrCloud,
                    onSyncNow = { syncViewModel.triggerManualSync() },
                    onPairMobileDevice = {
                        syncViewModel.startServerForPairing()
                        activePairingData = syncViewModel.generatePairingData()
                        showPairingDialog = true
                    },
                    onPairWithDesktop = { showScannerDialog = true },
                    onUnpair = { showUnpairConfirmation = true }
                )
            }
        )

        add(
            SettingsCategory(title = "Security") {
                SecretStorageSettingsSection()
                AppLockSettingsSection()
            }
        )

        if (!isDesktopPlatform) {
            add(
                SettingsCategory(title = "Editor") {
                    EditorSettingsSection(onKeyboardToolbarClick = onNavigateToKeyboardToolbarSettings)
                }
            )
        }

        add(
            SettingsCategory(title = "Appearance") {
                val selectedFontStyle = runCatching { FontStylePreference.valueOf(fontStylePreference) }
                    .getOrDefault(FontStylePreference.POPPINS)
                val selectedTimeFormat = timeFormatPreferenceNamed(timeFormatPreference)
                val selectedFirstDayOfWeek = firstDayOfWeekPreferenceNamed(firstDayOfWeekPreference)
                val selectedSubNoteOpenMode = runCatching { SubNoteOpenMode.valueOf(subNoteOpenMode) }
                    .getOrDefault(SubNoteOpenMode.SIDE_PANEL)

                AppearanceSettingsSection(
                    languageLabel = chosenLanguage.label(),
                    colorThemeLabel = colorThemePreferenceNamed(colorThemePreference).label(),
                    fontSizeLabel = fontSizeLabelFor(fontSizePreference),
                    interfaceScalePercent = interfaceScalePercent,
                    fontStyleLabel = selectedFontStyle.label(),
                    timeFormatLabel = selectedTimeFormat.label(),
                    firstDayOfWeekLabel = selectedFirstDayOfWeek.label(),
                    subNoteOpenModeLabel = selectedSubNoteOpenMode.label(),
                    topBarFadeStyleLabel = topBarFadeStyleFor(topBarFadeStyle).label(topBarFadeProgressive),
                    showScrollbar = showScrollbar,
                    dailyNotesEnabled = dailyNotesEnabled,
                    startOnHome = startOnHome,
                    micButtonVisible = micButtonVisible,
                    dailyQuoteVisible = dailyQuoteVisible,
                    hiddenOverviewSections = hiddenOverviewSections,
                    customWindowFrameEnabled = customWindowFrameEnabled,
                    autoHideTitleBar = autoHideTitleBar,
                    languageChoices = AppLanguage.entries.map { language ->
                        val languageLabel = languageLabels.getValue(language)
                        SettingsChoice(
                            label = languageLabel,
                            isSelected = language == chosenLanguage,
                            subtitle = language.nameInCurrentLanguage()
                        ) {
                            if (language != chosenLanguage) {
                                val otherLanguageNames = AppLanguage.entries
                                    .filter { other -> other != AppLanguage.SYSTEM && other != chosenLanguage && other != language }
                                    .map { other -> languageLabels.getValue(other) }
                                val namesRolledPast = listOf(languageLabels.getValue(chosenLanguage)) +
                                    otherLanguageNames.take(LANGUAGE_NAMES_ROLLED_PAST)
                                val namesBelow = otherLanguageNames.drop(LANGUAGE_NAMES_ROLLED_PAST).take(LANGUAGE_NAMES_SHOWN_BELOW)
                                chosenLanguage = language
                                if (appLanguageChangeNeedsRestart) {
                                    chooseAppLanguage(language)
                                    showLanguageRestartNotice = true
                                } else {
                                    LanguageChangeTransition.changeLanguage(languageLabel, namesRolledPast, namesBelow) { chooseAppLanguage(language) }
                                }
                            }
                        }
                    },
                    fontSizeChoices = FontSizeOptions.map { (preferenceName, labelResource) ->
                        SettingsChoice(label = stringResource(labelResource), isSelected = preferenceName == fontSizePreference) {
                            viewModel.setFontSizePreference(preferenceName)
                        }
                    },
                    fontStyleChoices = FontStylePreference.entries.map { option ->
                        SettingsChoice(
                            label = option.label(),
                            isSelected = option == selectedFontStyle,
                            labelFontFamily = fontFamilyFor(option)
                        ) {
                            viewModel.setFontStylePreference(option.name)
                        }
                    },
                    timeFormatChoices = TimeFormatPreference.entries.map { option ->
                        SettingsChoice(label = option.label(), isSelected = option == selectedTimeFormat) {
                            viewModel.setTimeFormatPreference(option.name)
                        }
                    },
                    firstDayOfWeekChoices = FirstDayOfWeekPreference.entries.map { option ->
                        SettingsChoice(label = option.label(), isSelected = option == selectedFirstDayOfWeek) {
                            viewModel.setFirstDayOfWeekPreference(option.name)
                        }
                    },
                    subNoteOpenModeChoices = SubNoteOpenMode.entries.map { option ->
                        SettingsChoice(label = option.label(), isSelected = option == selectedSubNoteOpenMode) {
                            viewModel.setSubNoteOpenMode(option.name)
                        }
                    },
                    topBarFadeStyleChoices = TopBarFadeStyle.entries.filter { isDebugBuild || it != TopBarFadeStyle.FALLBACK }.map { option ->
                        SettingsChoice(label = option.label(topBarFadeProgressive), isSelected = option == topBarFadeStyleFor(topBarFadeStyle)) {
                            viewModel.setTopBarFadeStyle(option.name)
                        }
                    },
                    topBarFadeProgressiveToggle = if (
                        topBarFadeStyleFor(topBarFadeStyle) == TopBarFadeStyle.NONE ||
                        topBarFadeStyleFor(topBarFadeStyle).isAlwaysFlatOnThisPlatform
                    ) {
                        null
                    } else {
                        SettingsMenuToggle(label = stringResource(Res.string.settings_top_bar_fade_progressive), isOn = topBarFadeProgressive) {
                            viewModel.setTopBarFadeProgressive(!topBarFadeProgressive)
                        }
                    },
                    onColorThemeClick = onNavigateToColorThemeSettings,
                    onInterfaceScaleChange = { viewModel.setInterfaceScalePercent(it) },
                    onTopBarFadeStyleClick = { showTopBarFadeStyleSheet = true },
                    onShowScrollbarChange = { viewModel.setShowScrollbar(it) },
                    onDailyNotesEnabledChange = { viewModel.setDailyNotesEnabled(it) },
                    onStartOnHomeChange = { viewModel.setStartOnHome(it) },
                    onMicButtonVisibleChange = { viewModel.setMicButtonVisible(it) },
                    onDailyQuoteVisibleChange = { viewModel.setDailyQuoteVisible(it) },
                    onOverviewSectionVisibleChange = { section, isVisible ->
                        viewModel.setOverviewSectionVisible(section, isVisible)
                    },
                    onCustomWindowFrameChange = { viewModel.setCustomWindowFrameEnabled(it) },
                    onAutoHideTitleBarChange = { viewModel.setAutoHideTitleBar(it) }
                )
            }
        )

        add(
            SettingsCategory(title = "AI") {
                AiSettingsSection(
                    aiFeaturesDisabled = aiFeaturesDisabled,
                    isPurgingAiData = isPurgingAiData,
                    onDisableRequested = { showDisableAiConfirmation = true },
                    onEnableRequested = { viewModel.setAiFeaturesDisabled(false) }
                )
            }
        )

        add(
            SettingsCategory(title = HELP_CATEGORY_TITLE) {
                Column {
                    HelpSettingsSection(onAboutClick = onNavigateToAbout)
                    AppUpdateSettingsSection()
                }
            }
        )

        add(
            SettingsCategory(title = "Danger Zone") {
                DangerZoneSettingsSection(onClearAllData = { showClearAllDataConfirmation = true })
            }
        )

        add(SettingsCategory(title = "Footer") { SettingsFooter() })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val surfaceModifier = Modifier
            .fillMaxSize()
            .hazeSource(state = hazeState)
            .background(MaterialTheme.colorScheme.background)

        if (isDesktopPlatform) {
            DesktopSettingsPanes(
                categories = categories,
                contentTopPadding = topBarHeightDp,
                subPage = desktopSubPage,
                onSubPageChange = onDesktopSubPageChange,
                subPageContent = { page ->
                    val goBack = { onDesktopSubPageChange(page.parentPage) }
                    when (page) {
                        SettingsSubPage.SELF_HOST -> SelfHostSetupScreen(onNavigateBack = goBack, showTopBar = false)
                        SettingsSubPage.EMBERR_CLOUD -> if (isDebugBuild) {
                            CloudAccountScreen(onNavigateBack = goBack, showTopBar = false)
                        }
                        SettingsSubPage.IMPORT -> ImportScreen(
                            onNavigateBack = goBack,
                            showTopBar = false
                        )
                        SettingsSubPage.ABOUT -> AboutScreen(
                            onNavigateBack = goBack,
                            onOpenSourceLicensesClick = { onDesktopSubPageChange(SettingsSubPage.OPEN_SOURCE_LICENSES) },
                            onThirdPartyServicesClick = { onDesktopSubPageChange(SettingsSubPage.THIRD_PARTY_SERVICES) },
                            showTopBar = false
                        )
                        SettingsSubPage.COLOR_THEME -> ColorThemeSettingsScreen(onNavigateBack = goBack, showTopBar = false)
                        SettingsSubPage.OPEN_SOURCE_LICENSES -> OpenSourceLicensesScreen(onNavigateBack = goBack, showTopBar = false)
                        SettingsSubPage.THIRD_PARTY_SERVICES -> ThirdPartyServicesScreen(onNavigateBack = goBack, showTopBar = false)
                    }
                },
                listScrollState = desktopListScrollState,
                modifier = surfaceModifier
            )
        } else {
            MobileSettingsList(
                categories = categories,
                listState = listState,
                contentTopPadding = topBarHeightDp + 8.dp,
                modifier = surfaceModifier
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates -> topBarHeightPx = coordinates.size.height.toFloat() }
        ) {
            EmberrTopHeaderBar(
                title = stringResource(Res.string.settings_title),
                hazeState = hazeState,
                topEdgeFadeAlpha = if (isDesktopPlatform) desktopTopEdgeFadeAlpha else 1f,
                contentPadding = topHeaderBarPadding(bottom = 16.dp),
                showBackButton = showBackButton,
                backTooltip = stringResource(Res.string.common_back),
                onBackClick = {
                    val openSubPage = desktopSubPage
                    if (openSubPage != null) onDesktopSubPageChange(openSubPage.parentPage) else onNavigateBack()
                }
            )
        }
    }

    if (showTimePicker) {
        val timeParts = backupTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 2
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
        }

        com.emberr.presentation.shared.components.MinimalTimePickerDialog(
            expanded = showTimePicker,
            initialTimestamp = cal.timeInMillis,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                val formattedTime = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                viewModel.saveBackupSchedule(backupFrequency, formattedTime, backupDay)
            }
        )
    }

    if (showDayPicker) {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        var selectedIndex by remember { mutableIntStateOf(days.indexOf(backupDay).coerceAtLeast(0)) }

        val wheelContent = @Composable {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                com.emberr.presentation.shared.components.WheelPicker(
                    items = days.map(::backupDayLabel),
                    selectedIndex = selectedIndex,
                    onItemSelected = { selectedIndex = it },
                    itemHeight = if (isDesktopPlatform) 40.dp else 44.dp
                )
            }
        }

        if (isDesktopPlatform) {
            com.emberr.presentation.shared.components.EmberrDesktopMenu(
                expanded = showDayPicker,
                onDismissRequest = { showDayPicker = false }
            ) {
                Column(modifier = Modifier.width(280.dp).wrapContentHeight()) {
                    wheelContent()
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f).orHighContrastLine()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EmberrButtonSecondary(text = stringResource(Res.string.common_cancel), onClick = { showDayPicker = false }, modifier = Modifier.weight(1f))
                        EmberrButtonPrimary(text = stringResource(Res.string.common_save), onClick = { viewModel.saveBackupSchedule(backupFrequency, backupTime, days[selectedIndex]); showDayPicker = false }, modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            EmberrBottomSheet(
                expanded = showDayPicker,
                onDismiss = { showDayPicker = false },
                title = stringResource(Res.string.settings_select_backup_day)
            ) {
                wheelContent()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmberrButtonSecondary(text = stringResource(Res.string.common_cancel), onClick = { showDayPicker = false }, modifier = Modifier.weight(1f))
                    EmberrButtonPrimary(text = stringResource(Res.string.common_save), onClick = { viewModel.saveBackupSchedule(backupFrequency, backupTime, days[selectedIndex]); showDayPicker = false }, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (showExportSpacesSheet) {
        ExportSpacesSheet(
            spaces = spaces,
            onExportClick = onExportReady,
            onDismiss = { showExportSpacesSheet = false }
        )
    }

    if (showLanguageRestartNotice) {
        RestartToApplyDialog(
            message = stringResource(Res.string.settings_language_restart_message),
            onDismiss = { showLanguageRestartNotice = false }
        )
    }

    if (showTopBarFadeStyleSheet) {
        EmberrBottomSheet(
            expanded = true,
            onDismiss = { showTopBarFadeStyleSheet = false },
            title = stringResource(Res.string.settings_top_bar_fade),
            subtitle = stringResource(Res.string.settings_top_bar_fade_subtitle),
            contentHorizontalPadding = 0.dp
        ) {
            val selectedFadeStyle = topBarFadeStyleFor(topBarFadeStyle)

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                TopBarFadeStyle.entries.filter { isDebugBuild || it != TopBarFadeStyle.FALLBACK }.forEach { option ->
                    EmberrBottomSheetOption(
                        label = option.label(topBarFadeProgressive),
                        isSelected = option == selectedFadeStyle,
                        onClick = {
                            viewModel.setTopBarFadeStyle(option.name)
                            showTopBarFadeStyleSheet = false
                        }
                    )
                }

                if (selectedFadeStyle != TopBarFadeStyle.NONE && !selectedFadeStyle.isAlwaysFlatOnThisPlatform) {
                    EmberrBottomSheetOption(
                        label = stringResource(Res.string.settings_top_bar_fade_progressive),
                        onClick = { viewModel.setTopBarFadeProgressive(!topBarFadeProgressive) },
                        trailing = { EmberrSwitch(isOn = topBarFadeProgressive) }
                    )
                }

                EmberrButtonPrimary(
                    text = stringResource(Res.string.common_close),
                    onClick = { showTopBarFadeStyleSheet = false },
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                )
            }
        }
    }

    if (showDisableAiConfirmation) {
        EmberrAlertDialog(
            onDismissRequest = { showDisableAiConfirmation = false },
            title = stringResource(Res.string.settings_turn_off_ai_dialog_title)
        ) {
            Text(
                text = stringResource(Res.string.settings_turn_off_ai_dialog_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmberrButtonSecondary(
                    text = stringResource(Res.string.common_cancel),
                    onClick = { showDisableAiConfirmation = false },
                    modifier = Modifier.weight(1f)
                )
                EmberrButtonPrimary(
                    text = stringResource(Res.string.settings_turn_off),
                    onClick = {
                        showDisableAiConfirmation = false
                        viewModel.setAiFeaturesDisabled(true)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showClearAllDataConfirmation && clearAllDataErasesCloudData) {
        EraseCloudDataDialog(
            isErasing = isErasingCloudData,
            errorMessage = cloudEraseErrorMessage,
            onDismiss = {
                showClearAllDataConfirmation = false
                viewModel.consumeCloudEraseErrorMessage()
            },
            onErase = viewModel::eraseCloudDataAndThisDevice
        )
    }

    if (showClearAllDataConfirmation && !clearAllDataErasesCloudData) {
        EmberrAlertDialog(
            onDismissRequest = { showClearAllDataConfirmation = false },
            title = stringResource(Res.string.settings_clear_all_data_dialog_title)
        ) {
            Text(
                text = stringResource(Res.string.settings_clear_all_data_dialog_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmberrButtonSecondary(
                    text = stringResource(Res.string.common_cancel),
                    onClick = { showClearAllDataConfirmation = false },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        showClearAllDataConfirmation = false
                        viewModel.clearAllData()
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = EmberrShadowElevation.None)
                ) {
                    Text(stringResource(Res.string.common_delete), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    if (showPairingDialog && activePairingData != null) {
        SyncPairingDialog(
            pairingData = activePairingData,
            onDismiss = {
                showPairingDialog = false
                syncViewModel.stopServerUnlessPaired()
            }
        )
    }

    if (showScannerDialog) {
        SyncScannerDialog(
            onDismiss = { showScannerDialog = false },
            onScanned = { pairingData ->
                showScannerDialog = false
                syncViewModel.applyScannedPairing(pairingData)
            }
        )
    }

    if (showUnpairConfirmation) {
        EmberrAlertDialog(
            onDismissRequest = { showUnpairConfirmation = false },
            title = stringResource(Res.string.settings_unpair_dialog_title)
        ) {
            Text(
                text = if (isDesktopPlatform) {
                    stringResource(Res.string.settings_unpair_dialog_message_desktop)
                } else {
                    stringResource(Res.string.settings_unpair_dialog_message_phone)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmberrButtonSecondary(
                    text = stringResource(Res.string.common_cancel),
                    onClick = { showUnpairConfirmation = false },
                    modifier = Modifier.weight(1f)
                )
                EmberrButtonPrimary(
                    text = stringResource(Res.string.settings_unpair),
                    onClick = {
                        showUnpairConfirmation = false
                        syncViewModel.unpair()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MobileSettingsList(
    categories: List<SettingsCategory>,
    listState: LazyListState,
    contentTopPadding: Dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(top = contentTopPadding, bottom = 56.dp)
    ) {
        categories.forEach { category ->
            item(key = category.title) { category.content() }
        }
    }
}

@Composable
private fun DesktopSettingsPanes(
    categories: List<SettingsCategory>,
    contentTopPadding: Dp,
    subPage: SettingsSubPage?,
    onSubPageChange: (SettingsSubPage?) -> Unit,
    subPageContent: @Composable (SettingsSubPage) -> Unit,
    listScrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = subPage,
        transitionSpec = {
            fadeIn(tween(SUB_PAGE_FADE_MILLIS)) togetherWith fadeOut(tween(SUB_PAGE_FADE_MILLIS))
        },
        modifier = modifier
    ) { shownSubPage ->
        if (shownSubPage == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(listScrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = SettingsPaneMaxWidth)
                            .fillMaxWidth()
                            .padding(top = contentTopPadding, bottom = 48.dp)
                    ) {
                        categories.forEach { category ->
                            key(category.title) { category.content() }
                        }
                    }
                }

                EmberrVerticalScrollbar(
                    scrollState = listScrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = contentTopPadding + 8.dp, bottom = 8.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = contentTopPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(modifier = Modifier.widthIn(max = SettingsPaneMaxWidth).fillMaxSize()) {
                    SettingsSubPageBackButton(onClick = { onSubPageChange(shownSubPage.parentPage) })
                    Box(modifier = Modifier.weight(1f)) { subPageContent(shownSubPage) }
                }
            }
        }
    }
}

@Composable
private fun SettingsSubPageBackButton(onClick: () -> Unit) {
    Box(modifier = Modifier.padding(start = 16.dp, top = 20.dp)) {
        TopBarIconButton(
            icon = painterResource(Res.drawable.chevron_left),
            contentDescription = stringResource(Res.string.common_back),
            bgColor = Color.Transparent,
            tint = MaterialTheme.colorScheme.primary,
            size = TopHeaderBarButtonSize,
            tooltip = stringResource(Res.string.common_back),
            onClick = onClick
        )
    }
}

private fun backupDayLabel(savedDay: String): String =
    DayOfWeek.entries.firstOrNull { it.name.equals(savedDay, ignoreCase = true) }?.let(::weekdayName) ?: savedDay

@Composable
private fun RemindersSettingsSection() {
    SettingsGroup(title = stringResource(Res.string.settings_group_reminders)) {
        val permissionCoordinator = rememberAppPermissionCoordinator()
        val isUnrestricted = permissionCoordinator.isGranted(AppPermission.UnrestrictedBackground)

        SettingsActionRow(
            icon = painterResource(Res.drawable.timer_reset),
            title = stringResource(Res.string.settings_unrestricted_background),
            trailingLabel = if (isUnrestricted) stringResource(Res.string.common_on) else stringResource(Res.string.common_off),
            onClick = { permissionCoordinator.request(AppPermission.UnrestrictedBackground) }
        )
    }
}

@Composable
private fun DataSettingsSection(
    autoBackupEnabled: Boolean,
    backupFrequency: String,
    backupTime: String,
    backupDay: String,
    onExportClick: () -> Unit,
    onImportNotesClick: () -> Unit,
    onAutoBackupChange: (Boolean) -> Unit,
    onBackupFrequencySelected: (String) -> Unit,
    onPickBackupDay: () -> Unit,
    onPickBackupTime: () -> Unit,
    onChooseBackupFolder: () -> Unit
) {
    SettingsGroup(title = stringResource(Res.string.settings_group_data_storage)) {
        SettingsActionRow(
            icon = painterResource(Res.drawable.download),
            title = stringResource(Res.string.settings_import),
            onClick = onImportNotesClick
        )
        SettingsDivider()
        SettingsActionRow(
            icon = painterResource(Res.drawable.share),
            title = stringResource(Res.string.settings_export_backup),
            onClick = onExportClick
        )

        if (!isDesktopPlatform) {
            SettingsDivider()
            SettingsToggleRow(
                icon = painterResource(Res.drawable.folder_sync),
                title = stringResource(Res.string.settings_automatic_backups),
                isChecked = autoBackupEnabled,
                onCheckedChange = onAutoBackupChange
            )

            AnimatedVisibility(
                visible = autoBackupEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSegmentedOptions(
                        options = BackupFrequencyLabels.keys.toList(),
                        selectedOption = backupFrequency,
                        optionLabel = { frequency -> stringResource(BackupFrequencyLabels.getValue(frequency)) },
                        onOptionSelected = onBackupFrequencySelected,
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    )

                    if (backupFrequency == "Weekly") {
                        SettingsDivider()
                        SettingsActionRow(
                            icon = painterResource(Res.drawable.calendar_clock),
                            title = stringResource(Res.string.settings_backup_day),
                            trailingLabel = backupDayLabel(backupDay),
                            onClick = onPickBackupDay
                        )
                    }

                    SettingsDivider()
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.timer_reset),
                        title = stringResource(Res.string.settings_backup_time),
                        trailingLabel = backupTimeLabel(backupTime, LocalUsesTwentyFourHourClock.current),
                        onClick = onPickBackupTime
                    )

                    SettingsDivider()
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.folder_input),
                        title = stringResource(Res.string.settings_backup_location),
                        trailingLabel = stringResource(Res.string.settings_backup_location_change),
                        onClick = onChooseBackupFolder
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncSettingsSection(
    isPaired: Boolean,
    isSelfHostConnected: Boolean,
    isSignedInToCloud: Boolean,
    syncNetwork: SelfHostSyncNetwork,
    onSyncNetworkSelected: (SelfHostSyncNetwork) -> Unit,
    syncStatus: String,
    serverUnavailableReason: String?,
    syncPortInput: String,
    onSyncPortChange: (String) -> Unit,
    onOpenSelfHostSetup: () -> Unit,
    onOpenEmberrCloud: () -> Unit,
    onSyncNow: () -> Unit,
    onPairMobileDevice: () -> Unit,
    onPairWithDesktop: () -> Unit,
    onUnpair: () -> Unit
) {
    SettingsGroup(title = stringResource(Res.string.settings_group_sync_backup)) {
        SettingsActionRow(
            icon = painterResource(Res.drawable.refresh_cw),
            title = stringResource(Res.string.settings_self_host),
            subtitle = when {
                isPaired -> stringResource(Res.string.settings_self_host_blocked_by_lan_sync)
                isSignedInToCloud -> stringResource(Res.string.settings_self_host_blocked_by_cloud)
                else -> null
            },
            isEnabled = !isPaired && !isSignedInToCloud,
            onClick = onOpenSelfHostSetup
        )
        if (isDebugBuild) {
            SettingsDivider()
            SettingsActionRow(
                icon = painterResource(Res.drawable.lock),
                title = stringResource(Res.string.settings_emberr_cloud),
                subtitle = when {
                    isSignedInToCloud -> null
                    isSelfHostConnected -> stringResource(Res.string.settings_cloud_blocked_by_self_host)
                    isPaired -> stringResource(Res.string.settings_cloud_blocked_by_lan_sync)
                    else -> null
                },
                isEnabled = isSignedInToCloud || (!isSelfHostConnected && !isPaired),
                onClick = onOpenEmberrCloud
            )
        }
        if (!isDesktopPlatform) {
            SettingsDivider()
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SettingsSegmentedOptions(
                    options = SyncNetworkLabels.map { it.first },
                    selectedOption = syncNetwork,
                    optionLabel = { network -> stringResource(SyncNetworkLabels.first { it.first == network }.second) },
                    onOptionSelected = onSyncNetworkSelected
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(Res.string.settings_sync_network_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }

    SettingsGroup(title = stringResource(Res.string.settings_group_lan_sync)) {
        val lanSyncBlockedReason = when {
            isSelfHostConnected -> stringResource(Res.string.settings_lan_sync_blocked_by_self_host)
            isSignedInToCloud -> stringResource(Res.string.settings_lan_sync_blocked_by_cloud)
            else -> null
        }

        if (isDesktopPlatform && serverUnavailableReason != null) {
            SettingsWarningNotice(message = serverUnavailableReason)
        }

        when {
            isPaired -> {
                if (isDesktopPlatform) {
                    Text(
                        text = stringResource(Res.string.settings_lan_sync_paired_desktop),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
                    )
                } else {
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.refresh_cw),
                        title = stringResource(Res.string.settings_sync_now),
                        trailingLabel = syncStatus,
                        onClick = onSyncNow
                    )
                }

                SettingsDivider()
                SettingsActionRow(
                    icon = rememberVectorPainter(Icons.Default.LinkOff),
                    title = if (isDesktopPlatform) stringResource(Res.string.settings_unpair_from_mobile_device) else stringResource(Res.string.settings_unpair_from_desktop),
                    isDestructive = true,
                    onClick = onUnpair
                )
            }

            isDesktopPlatform -> {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = stringResource(Res.string.settings_sync_port),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    EmberrTextField(
                        value = syncPortInput,
                        onValueChange = onSyncPortChange,
                        placeholder = "8080",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.settings_sync_port_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
                SettingsDivider()
                SettingsActionRow(
                    icon = painterResource(Res.drawable.qr_code),
                    title = stringResource(Res.string.settings_pair_mobile_device),
                    subtitle = lanSyncBlockedReason,
                    isEnabled = !isSelfHostConnected && !isSignedInToCloud,
                    onClick = onPairMobileDevice
                )
            }

            else -> {
                SettingsActionRow(
                    icon = painterResource(Res.drawable.scan_line),
                    title = stringResource(Res.string.settings_pair_with_desktop),
                    subtitle = lanSyncBlockedReason,
                    isEnabled = !isSelfHostConnected && !isSignedInToCloud,
                    onClick = onPairWithDesktop
                )
            }
        }
    }
}

@Composable
private fun AppearanceSettingsSection(
    languageLabel: String,
    colorThemeLabel: String,
    fontSizeLabel: String,
    interfaceScalePercent: Int,
    fontStyleLabel: String,
    timeFormatLabel: String,
    firstDayOfWeekLabel: String,
    subNoteOpenModeLabel: String,
    topBarFadeStyleLabel: String,
    showScrollbar: Boolean,
    dailyNotesEnabled: Boolean,
    startOnHome: Boolean,
    micButtonVisible: Boolean,
    dailyQuoteVisible: Boolean,
    hiddenOverviewSections: Set<String>,
    customWindowFrameEnabled: Boolean,
    autoHideTitleBar: Boolean,
    languageChoices: List<SettingsChoice>,
    fontSizeChoices: List<SettingsChoice>,
    fontStyleChoices: List<SettingsChoice>,
    timeFormatChoices: List<SettingsChoice>,
    firstDayOfWeekChoices: List<SettingsChoice>,
    subNoteOpenModeChoices: List<SettingsChoice>,
    topBarFadeStyleChoices: List<SettingsChoice>,
    topBarFadeProgressiveToggle: SettingsMenuToggle?,
    onColorThemeClick: () -> Unit,
    onInterfaceScaleChange: (Int) -> Unit,
    onTopBarFadeStyleClick: () -> Unit,
    onShowScrollbarChange: (Boolean) -> Unit,
    onDailyNotesEnabledChange: (Boolean) -> Unit,
    onStartOnHomeChange: (Boolean) -> Unit,
    onMicButtonVisibleChange: (Boolean) -> Unit,
    onDailyQuoteVisibleChange: (Boolean) -> Unit,
    onOverviewSectionVisibleChange: (OverviewSection, Boolean) -> Unit,
    onCustomWindowFrameChange: (Boolean) -> Unit,
    onAutoHideTitleBarChange: (Boolean) -> Unit
) {
    SettingsGroup(title = stringResource(Res.string.settings_group_appearance)) {
        SettingsChoiceRow(
            icon = painterResource(Res.drawable.globe),
            title = stringResource(Res.string.settings_language),
            value = languageLabel,
            choices = languageChoices,
            searchPlaceholder = stringResource(Res.string.settings_search_languages),
            desktopMenuMaxHeight = LanguageMenuMaxHeight
        )
        SettingsDivider()
        SettingsActionRow(
            icon = painterResource(Res.drawable.palette),
            title = stringResource(Res.string.settings_theme),
            trailingLabel = colorThemeLabel,
            onClick = onColorThemeClick
        )
        SettingsDivider()
        if (isDesktopPlatform) {
            SettingsChoiceRow(
                icon = painterResource(Res.drawable.palette),
                title = stringResource(Res.string.settings_top_bar_fade),
                value = topBarFadeStyleLabel,
                choices = topBarFadeStyleChoices,
                menuToggle = topBarFadeProgressiveToggle
            )
        } else {
            SettingsActionRow(
                icon = painterResource(Res.drawable.palette),
                title = stringResource(Res.string.settings_top_bar_fade),
                trailingLabel = topBarFadeStyleLabel,
                onClick = onTopBarFadeStyleClick
            )
        }
        SettingsDivider()
        InterfaceScaleStepperRow(
            savedPercent = interfaceScalePercent,
            onPercentChosen = onInterfaceScaleChange
        )
        SettingsDivider()
        SettingsChoiceRow(
            icon = painterResource(Res.drawable.sliders_horizontal),
            title = stringResource(Res.string.settings_font_size),
            value = fontSizeLabel,
            choices = fontSizeChoices
        )
        SettingsDivider()
        SettingsChoiceRow(
            icon = painterResource(Res.drawable.text_type),
            title = stringResource(Res.string.settings_font_style),
            value = fontStyleLabel,
            choices = fontStyleChoices
        )
    }

    SettingsGroup(title = stringResource(Res.string.settings_group_date_time)) {
        SettingsChoiceRow(
            icon = painterResource(Res.drawable.clock_circle),
            title = stringResource(Res.string.settings_time_format),
            value = timeFormatLabel,
            choices = timeFormatChoices
        )
        SettingsDivider()
        SettingsChoiceRow(
            icon = painterResource(Res.drawable.calendar),
            title = stringResource(Res.string.settings_first_day_of_week),
            value = firstDayOfWeekLabel,
            choices = firstDayOfWeekChoices,
            subtitle = if (isDesktopPlatform) stringResource(Res.string.settings_first_day_of_week_desktop_hint) else null
        )
    }

    SettingsGroup(title = stringResource(Res.string.settings_group_layout)) {
        SettingsToggleRow(
            icon = painterResource(Res.drawable.daily),
            title = stringResource(Res.string.settings_daily_notes),
            isChecked = dailyNotesEnabled,
            subtitle = if (isDesktopPlatform) stringResource(Res.string.settings_daily_notes_hint) else null,
            onCheckedChange = onDailyNotesEnabledChange
        )

        if (dailyNotesEnabled) {
            SettingsDivider()
            SettingsToggleRow(
                icon = painterResource(Res.drawable.quote_down2),
                title = stringResource(Res.string.settings_daily_quote),
                isChecked = dailyQuoteVisible,
                subtitle = stringResource(Res.string.settings_daily_quote_hint),
                onCheckedChange = onDailyQuoteVisibleChange
            )
        }

        if (!isDesktopPlatform && dailyNotesEnabled) {
            SettingsDivider()
            SettingsToggleRow(
                icon = painterResource(Res.drawable.house),
                title = stringResource(Res.string.settings_open_home_first),
                isChecked = startOnHome,
                onCheckedChange = onStartOnHomeChange
            )
        }

        if (!isDesktopPlatform) {
            SettingsDivider()
            SettingsToggleRow(
                icon = painterResource(Res.drawable.microphone),
                title = stringResource(Res.string.settings_mic_button),
                isChecked = micButtonVisible,
                onCheckedChange = onMicButtonVisibleChange
            )
        }
    }

    SettingsGroup(title = stringResource(Res.string.settings_group_overview)) {
        OverviewSection.entries.forEachIndexed { index, section ->
            if (index > 0) SettingsDivider()
            SettingsToggleRow(
                icon = painterResource(section.icon),
                title = section.label(),
                isChecked = section.storageKey !in hiddenOverviewSections,
                onCheckedChange = { isVisible -> onOverviewSectionVisibleChange(section, isVisible) }
            )
        }

        if (isDesktopPlatform) {
            SettingsFootnote(text = stringResource(Res.string.settings_overview_sidebar_footnote))
        }
    }

    if (isDesktopPlatform) {
        SettingsGroup(title = stringResource(Res.string.settings_group_desktop_window)) {
            SettingsChoiceRow(
                icon = painterResource(Res.drawable.sidebar),
                title = stringResource(Res.string.settings_subnote_opening),
                value = subNoteOpenModeLabel,
                choices = subNoteOpenModeChoices
            )

            SettingsDivider()
            SettingsToggleRow(
                icon = painterResource(Res.drawable.sidebar),
                title = stringResource(Res.string.settings_show_scrollbar),
                isChecked = showScrollbar,
                subtitle = stringResource(Res.string.settings_show_scrollbar_hint),
                onCheckedChange = onShowScrollbarChange
            )

            SettingsDivider()

            var showCustomWindowFrameNotice by remember { mutableStateOf(false) }

            SettingsToggleRow(
                icon = painterResource(Res.drawable.sidebar),
                title = stringResource(Res.string.settings_custom_title_bar),
                isChecked = customWindowFrameEnabled,
                subtitle = stringResource(Res.string.settings_custom_title_bar_hint),
                onCheckedChange = { enabled ->
                    onCustomWindowFrameChange(enabled)
                    showCustomWindowFrameNotice = true
                }
            )

            if (customWindowFrameEnabled) {
                SettingsDivider()
                SettingsToggleRow(
                    icon = painterResource(Res.drawable.sidebar),
                    title = stringResource(Res.string.settings_auto_hide_title_bar),
                    isChecked = autoHideTitleBar,
                    subtitle = stringResource(Res.string.settings_auto_hide_title_bar_hint),
                    onCheckedChange = onAutoHideTitleBarChange
                )
            }

            if (showCustomWindowFrameNotice) {
                RestartToApplyDialog(
                    message = stringResource(Res.string.settings_restart_to_apply_message),
                    onDismiss = { showCustomWindowFrameNotice = false }
                )
            }
        }
    }
}

@Composable
private fun RestartToApplyDialog(message: String, onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    EmberrAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_restart_to_apply_title)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmberrButtonSecondary(
                text = stringResource(Res.string.settings_restart_later),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            EmberrButtonPrimary(
                text = stringResource(Res.string.settings_restart_now),
                onClick = {
                    coroutineScope.launch {
                        ActiveEditorRegistry.flushAllPending()
                        restartApplication()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AiSettingsSection(
    aiFeaturesDisabled: Boolean,
    isPurgingAiData: Boolean,
    onDisableRequested: () -> Unit,
    onEnableRequested: () -> Unit
) {
    SettingsGroup(title = stringResource(Res.string.settings_group_ai)) {
        SettingsToggleRow(
            icon = painterResource(Res.drawable.astroid),
            title = stringResource(Res.string.settings_turn_off_ai_features),
            isChecked = aiFeaturesDisabled,
            subtitle = when {
                isPurgingAiData -> stringResource(Res.string.settings_ai_removing_data)
                !isDesktopPlatform -> null
                aiFeaturesDisabled -> stringResource(Res.string.settings_ai_off_hint)
                else -> stringResource(Res.string.settings_ai_on_hint)
            },
            onCheckedChange = { isChecked ->
                if (!isPurgingAiData) {
                    if (isChecked) onDisableRequested() else onEnableRequested()
                }
            }
        )
    }
}

@Composable
private fun EditorSettingsSection(onKeyboardToolbarClick: () -> Unit) {
    SettingsGroup(title = stringResource(Res.string.settings_group_editor)) {
        SettingsActionRow(
            icon = painterResource(Res.drawable.keyboard),
            title = stringResource(Res.string.settings_keyboard_toolbar),
            onClick = onKeyboardToolbarClick
        )
    }
}

@Composable
private fun HelpSettingsSection(onAboutClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var isWhatsNewOpen by remember { mutableStateOf(false) }
    SettingsGroup(title = stringResource(Res.string.settings_group_need_help)) {
        SettingsActionRow(
            icon = painterResource(Res.drawable.badge_question_mark),
            title = stringResource(Res.string.settings_faq),
            isEnabled = false,
            onClick = {}
        )
        SettingsDivider()
        SettingsActionRow(
            icon = painterResource(Res.drawable.badge_plus),
            title = stringResource(Res.string.settings_whats_new),
            onClick = { isWhatsNewOpen = true }
        )
        PlayTestingSettingsRow()
        SettingsDivider()
        SettingsActionRow(
            icon = painterResource(Res.drawable.shield_alert),
            title = stringResource(Res.string.settings_privacy_policy),
            onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) }
        )
        SettingsDivider()
        SettingsActionRow(
            icon = painterResource(Res.drawable.info),
            title = stringResource(Res.string.settings_about_emberr),
            trailingLabel = appVersionName?.let { stringResource(Res.string.common_version_label, it) },
            onClick = onAboutClick
        )
    }

    if (isWhatsNewOpen) {
        EmberrUpdateDialog(onDismissRequest = { isWhatsNewOpen = false })
    }
}

@Composable
private fun DangerZoneSettingsSection(onClearAllData: () -> Unit) {
    SettingsGroup(title = stringResource(Res.string.settings_group_danger_zone)) {
        SettingsActionRow(
            icon = painterResource(Res.drawable.triangle_alert),
            title = stringResource(Res.string.settings_clear_all_data),
            subtitle = if (isDesktopPlatform) {
                stringResource(Res.string.settings_clear_all_data_desktop_hint)
            } else {
                null
            },
            isDestructive = true,
            onClick = onClearAllData
        )
    }
}

@Composable
private fun EraseCloudDataDialog(
    isErasing: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onErase: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    EmberrAlertDialog(
        onDismissRequest = { if (!isErasing) onDismiss() },
        title = stringResource(Res.string.settings_clear_all_data_dialog_title)
    ) {
        Text(
            text = stringResource(Res.string.settings_erase_cloud_dialog_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(16.dp))
        PasswordField(
            password = password,
            onPasswordChanged = { password = it },
            placeholder = stringResource(Res.string.settings_erase_cloud_password),
            onSubmit = { onErase(password) }
        )
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmberrButtonSecondary(
                text = stringResource(Res.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !isErasing
            )
            Button(
                onClick = { onErase(password) },
                enabled = !isErasing && password.isNotEmpty(),
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = EmberrShadowElevation.None)
            ) {
                Text(
                    text = if (isErasing) stringResource(Res.string.settings_erasing) else stringResource(Res.string.settings_erase),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun SettingsDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f).orHighContrastLine()
    )
}

@Composable
fun SettingsGroup(
    title: String,
    cardColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp)
    ) {
        SettingsGroupTitle(title = title)

        CompositionLocalProvider(LocalSitsOnCard provides true) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SettingsCardShape)
                    .background(cardColor)
                    .highContrastBorder(SettingsCardShape),
                content = content
            )
        }
    }
}

@Composable
internal fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
    )
}

@Composable
internal fun SettingsRowIcon(icon: Painter, isDestructive: Boolean = false) {
    val accentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Icon(
        icon,
        contentDescription = null,
        tint = accentColor.copy(alpha = if (isDestructive) 1f else 0.8f),
        modifier = Modifier.size(20.dp)
    )
}

@Composable
internal fun SettingsValuePill(label: String, isDestructive: Boolean = false) {
    val labelColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = labelColor.copy(alpha = if (isDestructive) 0.9f else 0.55f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 180.dp)
    )
}

@Composable
internal fun SettingsFootnote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 14.dp)
    )
}

@Composable
private fun SettingsWarningNotice(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(Res.drawable.triangle_alert),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
internal fun <T> SettingsSegmentedOptions(
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) SelectedOptionBackground else Color.Transparent)
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLabel(option),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: Painter,
    title: String,
    trailingLabel: String? = null,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val titleColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    val contentAlpha = if (isEnabled) 1f else 0.38f

    SettingsRowLayout(modifier = Modifier.clickable(enabled = isEnabled) { onClick() }) {
        Box(modifier = Modifier.alpha(contentAlpha)) {
            SettingsRowIcon(icon = icon, isDestructive = isDestructive)
        }

        Spacer(modifier = Modifier.width(16.dp))

        SettingsRowTitle(
            title = title,
            subtitle = subtitle,
            titleColor = titleColor,
            titleFontWeight = if (isDestructive) FontWeight.Medium else FontWeight.Normal,
            titleAlpha = contentAlpha,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (trailingLabel != null) {
                SettingsValuePill(label = trailingLabel, isDestructive = isDestructive)
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (!isDestructive) {
                Icon(
                    painterResource(Res.drawable.chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun InterfaceScaleStepperRow(
    savedPercent: Int,
    onPercentChosen: (Int) -> Unit
) {
    SettingsRowLayout(modifier = Modifier) {
        SettingsRowIcon(icon = painterResource(Res.drawable.scale))
        Spacer(modifier = Modifier.width(16.dp))
        SettingsRowTitle(
            title = stringResource(Res.string.settings_interface_scale),
            subtitle = if (isDesktopPlatform) stringResource(Res.string.settings_interface_scale_hint) else null,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier.clip(SmallRoundedCornerShape).background(emberrControlContainerColor()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InterfaceScaleStepButton(
                icon = painterResource(Res.drawable.minus),
                description = stringResource(Res.string.settings_interface_scale_smaller),
                isEnabled = savedPercent > SMALLEST_INTERFACE_SCALE_PERCENT,
                onClick = { onPercentChosen(interfaceScalePercentMovedBySteps(savedPercent, -1)) }
            )
            IconTooltip(text = stringResource(Res.string.settings_interface_scale_reset)) {
                Text(
                    text = stringResource(Res.string.common_percent_value, savedPercent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(min = if (isDesktopPlatform) 52.dp else 44.dp)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClickLabel = stringResource(Res.string.settings_interface_scale_reset),
                            role = Role.Button
                        ) {
                            if (savedPercent != SyncConstants.DEFAULT_INTERFACE_SCALE_PERCENT) {
                                onPercentChosen(SyncConstants.DEFAULT_INTERFACE_SCALE_PERCENT)
                            }
                        }
                )
            }
            InterfaceScaleStepButton(
                icon = painterResource(Res.drawable.plus),
                description = stringResource(Res.string.settings_interface_scale_bigger),
                isEnabled = savedPercent < LARGEST_INTERFACE_SCALE_PERCENT,
                onClick = { onPercentChosen(interfaceScalePercentMovedBySteps(savedPercent, 1)) }
            )
        }
    }
}

@Composable
private fun InterfaceScaleStepButton(
    icon: Painter,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    IconTooltip(text = description) {
        Box(
            modifier = Modifier
                .size(if (isDesktopPlatform) 28.dp else 34.dp)
                .clickable(enabled = isEnabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isEnabled) 0.8f else 0.25f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsRowLayout(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .settingsRowSize(),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun SettingsRowTitle(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    titleFontWeight: FontWeight = FontWeight.Normal,
    titleAlpha: Float = 1f
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = titleFontWeight,
            color = titleColor,
            modifier = Modifier.alpha(titleAlpha)
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

internal class SettingsChoice(
    val label: String,
    val isSelected: Boolean,
    val labelFontFamily: FontFamily? = null,
    val subtitle: String? = null,
    val onSelect: () -> Unit
)

internal class SettingsMenuToggle(
    val label: String,
    val isOn: Boolean,
    val onToggle: () -> Unit
)

@Composable
internal fun SettingsChoiceRow(
    icon: Painter,
    title: String,
    value: String,
    choices: List<SettingsChoice>,
    menuToggle: SettingsMenuToggle? = null,
    subtitle: String? = null,
    searchPlaceholder: String? = null,
    desktopMenuMaxHeight: Dp = Dp.Unspecified
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    if (isDesktopPlatform) {
        SettingsRowLayout(modifier = Modifier) {
            SettingsRowIcon(icon = icon)

            Spacer(modifier = Modifier.width(16.dp))

            SettingsRowTitle(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.width(12.dp))

            EmberrChoiceValueMenu(
                value = value,
                isMenuOpen = isMenuOpen,
                onMenuOpenChange = { isOpen -> isMenuOpen = isOpen },
                menuMaxHeight = desktopMenuMaxHeight
            ) { closeMenuAnd ->
                choices.forEach { choice ->
                    EmberrDesktopMenuOption(
                        label = choice.label,
                        isSelected = choice.isSelected,
                        labelFontFamily = choice.labelFontFamily,
                        subtitle = choice.subtitle,
                        onClick = { closeMenuAnd(choice.onSelect) }
                    )
                }

                if (menuToggle != null) {
                    EmberrDesktopMenuOption(
                        label = menuToggle.label,
                        onClick = menuToggle.onToggle,
                        trailing = { EmberrSwitch(isOn = menuToggle.isOn) }
                    )
                }
            }
        }
    } else {
        SettingsActionRow(
            icon = icon,
            title = title,
            trailingLabel = value,
            subtitle = subtitle,
            onClick = { isMenuOpen = true }
        )

        val sheetOption: @Composable (SettingsChoice) -> Unit = { choice ->
            EmberrBottomSheetOption(
                label = choice.label,
                isSelected = choice.isSelected,
                labelFontFamily = choice.labelFontFamily,
                subtitle = choice.subtitle,
                onClick = {
                    choice.onSelect()
                    isMenuOpen = false
                }
            )
        }

        if (searchPlaceholder != null) {
            EmberrBottomSheet(
                expanded = isMenuOpen,
                onDismiss = { isMenuOpen = false },
                title = title,
                contentHorizontalPadding = 0.dp,
                scrollsContent = false
            ) {
                var searchText by remember { mutableStateOf("") }
                val matchingChoices = choices.filter { choice ->
                    choice.label.contains(searchText.trim(), ignoreCase = true) ||
                        choice.subtitle?.contains(searchText.trim(), ignoreCase = true) == true
                }
                EmberrSearchSheetBody(
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    placeholder = searchPlaceholder
                ) {
                    matchingChoices.forEach { choice -> sheetOption(choice) }
                }
            }
        } else {
            EmberrBottomSheet(
                expanded = isMenuOpen,
                onDismiss = { isMenuOpen = false },
                title = title,
                contentHorizontalPadding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    choices.forEach { choice -> sheetOption(choice) }

                    if (menuToggle != null) {
                        EmberrBottomSheetOption(
                            label = menuToggle.label,
                            onClick = menuToggle.onToggle,
                            trailing = { EmberrSwitch(isOn = menuToggle.isOn) }
                        )
                    }

                    EmberrButtonPrimary(
                        text = stringResource(Res.string.common_close),
                        onClick = { isMenuOpen = false },
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: Painter,
    title: String,
    isChecked: Boolean,
    subtitle: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRowLayout(modifier = Modifier.clickable { onCheckedChange(!isChecked) }) {
        SettingsRowIcon(icon = icon)

        Spacer(modifier = Modifier.width(16.dp))

        SettingsRowTitle(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.width(12.dp))

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.secondary,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

internal fun Modifier.settingsRowSize(): Modifier =
    heightIn(min = SettingsRowMinHeight).padding(horizontal = SettingsRowSidePadding, vertical = 12.dp)

@Composable
private fun fontSizeLabelFor(preferenceName: String): String = stringResource(
    FontSizeOptions.firstOrNull { it.first == preferenceName }?.second
        ?: FontSizeOptions[2].second
)

private fun topBarFadeStyleFor(styleName: String): TopBarFadeStyle =
    runCatching { TopBarFadeStyle.valueOf(styleName) }.getOrDefault(TopBarFadeStyle.BLUR)

private fun backupTimeLabel(backupTime: String, usesTwentyFourHourClock: Boolean): String {
    val hour = backupTime.substringBefore(":").toIntOrNull()
    val minute = backupTime.substringAfter(":").toIntOrNull()
    if (hour == null || minute == null) return backupTime
    return formatTimeOfDay(hour, minute, usesTwentyFourHourClock)
}
