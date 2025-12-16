package com.kieronquinn.app.utag

import android.app.Application
import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.kieronquinn.app.utag.components.navigation.RootNavigation
import com.kieronquinn.app.utag.components.navigation.RootNavigationImpl
import com.kieronquinn.app.utag.components.navigation.SettingsNavigation
import com.kieronquinn.app.utag.components.navigation.SettingsNavigationImpl
import com.kieronquinn.app.utag.components.navigation.SetupNavigation
import com.kieronquinn.app.utag.components.navigation.SetupNavigationImpl
import com.kieronquinn.app.utag.components.navigation.TagContainerNavigation
import com.kieronquinn.app.utag.components.navigation.TagContainerNavigationImpl
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigation
import com.kieronquinn.app.utag.components.navigation.TagMoreNavigationImpl
import com.kieronquinn.app.utag.components.navigation.TagPickerNavigation
import com.kieronquinn.app.utag.components.navigation.TagPickerNavigationImpl
import com.kieronquinn.app.utag.components.navigation.TagRootNavigation
import com.kieronquinn.app.utag.components.navigation.TagRootNavigationImpl
import com.kieronquinn.app.utag.components.navigation.UnknownTagNavigation
import com.kieronquinn.app.utag.components.navigation.UnknownTagNavigationImpl
import com.kieronquinn.app.utag.components.navigation.WidgetContainerNavigation
import com.kieronquinn.app.utag.components.navigation.WidgetContainerNavigationImpl
import com.kieronquinn.app.utag.model.database.UTagDatabase
import com.kieronquinn.app.utag.model.database.cache.CacheDatabase
import com.kieronquinn.app.utag.repositories.AnalyticsRepository
import com.kieronquinn.app.utag.repositories.AnalyticsRepositoryImpl
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.ApiRepositoryImpl
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.AuthRepositoryImpl
import com.kieronquinn.app.utag.repositories.AutomationRepository
import com.kieronquinn.app.utag.repositories.AutomationRepositoryImpl
import com.kieronquinn.app.utag.repositories.BackupRepository
import com.kieronquinn.app.utag.repositories.BackupRepositoryImpl
import com.kieronquinn.app.utag.repositories.CacheRepository
import com.kieronquinn.app.utag.repositories.CacheRepositoryImpl
import com.kieronquinn.app.utag.repositories.ChaserRepository
import com.kieronquinn.app.utag.repositories.ChaserRepositoryImpl
import com.kieronquinn.app.utag.repositories.ContentCreatorRepository
import com.kieronquinn.app.utag.repositories.ContentCreatorRepositoryImpl
import com.kieronquinn.app.utag.repositories.DeviceRepository
import com.kieronquinn.app.utag.repositories.DeviceRepositoryImpl
import com.kieronquinn.app.utag.repositories.DownloadRepository
import com.kieronquinn.app.utag.repositories.DownloadRepositoryImpl
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepositoryImpl
import com.kieronquinn.app.utag.repositories.EncryptionRepository
import com.kieronquinn.app.utag.repositories.EncryptionRepositoryImpl
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepository
import com.kieronquinn.app.utag.repositories.FindMyDeviceRepositoryImpl
import com.kieronquinn.app.utag.repositories.GeocoderRepository
import com.kieronquinn.app.utag.repositories.GeocoderRepositoryImpl
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepositoryImpl
import com.kieronquinn.app.utag.repositories.LeftBehindRepository
import com.kieronquinn.app.utag.repositories.LeftBehindRepositoryImpl
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository
import com.kieronquinn.app.utag.repositories.LocationHistoryRepositoryImpl
import com.kieronquinn.app.utag.repositories.LocationRepository
import com.kieronquinn.app.utag.repositories.LocationRepositoryImpl
import com.kieronquinn.app.utag.repositories.NonOwnerTagRepository
import com.kieronquinn.app.utag.repositories.NonOwnerTagRepositoryImpl
import com.kieronquinn.app.utag.repositories.NotificationRepository
import com.kieronquinn.app.utag.repositories.NotificationRepositoryImpl
import com.kieronquinn.app.utag.repositories.PassiveModeRepository
import com.kieronquinn.app.utag.repositories.PassiveModeRepositoryImpl
import com.kieronquinn.app.utag.repositories.QcServiceRepository
import com.kieronquinn.app.utag.repositories.QcServiceRepositoryImpl
import com.kieronquinn.app.utag.repositories.RulesRepository
import com.kieronquinn.app.utag.repositories.RulesRepositoryImpl
import com.kieronquinn.app.utag.repositories.SafeAreaRepository
import com.kieronquinn.app.utag.repositories.SafeAreaRepositoryImpl
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepositoryImpl
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepositoryImpl
import com.kieronquinn.app.utag.repositories.SmartTagServiceRepository
import com.kieronquinn.app.utag.repositories.SmartTagServiceRepositoryImpl
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepositoryImpl
import com.kieronquinn.app.utag.repositories.SmartspacerRepository
import com.kieronquinn.app.utag.repositories.SmartspacerRepositoryImpl
import com.kieronquinn.app.utag.repositories.StaticMapRepository
import com.kieronquinn.app.utag.repositories.StaticMapRepositoryImpl
import com.kieronquinn.app.utag.repositories.UTagServiceRepository
import com.kieronquinn.app.utag.repositories.UTagServiceRepositoryImpl
import com.kieronquinn.app.utag.repositories.UpdateRepository
import com.kieronquinn.app.utag.repositories.UpdateRepositoryImpl
import com.kieronquinn.app.utag.repositories.UserRepository
import com.kieronquinn.app.utag.repositories.UserRepositoryImpl
import com.kieronquinn.app.utag.repositories.UwbRepository
import com.kieronquinn.app.utag.repositories.WidgetRepository
import com.kieronquinn.app.utag.repositories.WidgetRepositoryImpl
import com.kieronquinn.app.utag.ui.screens.findmydevice.FindMyDeviceViewModel
import com.kieronquinn.app.utag.ui.screens.findmydevice.FindMyDeviceViewModelImpl
import com.kieronquinn.app.utag.ui.screens.login.AuthResponseViewModel
import com.kieronquinn.app.utag.ui.screens.login.AuthResponseViewModelImpl
import com.kieronquinn.app.utag.ui.screens.root.RootViewModel
import com.kieronquinn.app.utag.ui.screens.root.RootViewModelImpl
import com.kieronquinn.app.utag.ui.screens.safearea.list.SafeAreaListViewModel
import com.kieronquinn.app.utag.ui.screens.safearea.list.SafeAreaListViewModelImpl
import com.kieronquinn.app.utag.ui.screens.safearea.location.SafeAreaLocationViewModel
import com.kieronquinn.app.utag.ui.screens.safearea.location.SafeAreaLocationViewModelImpl
import com.kieronquinn.app.utag.ui.screens.safearea.type.SafeAreaTypeViewModel
import com.kieronquinn.app.utag.ui.screens.safearea.type.SafeAreaTypeViewModelImpl
import com.kieronquinn.app.utag.ui.screens.safearea.wifi.SafeAreaWiFiViewModel
import com.kieronquinn.app.utag.ui.screens.safearea.wifi.SafeAreaWiFiViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.advanced.SettingsAdvancedViewModel
import com.kieronquinn.app.utag.ui.screens.settings.advanced.SettingsAdvancedViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.advanced.language.SettingsLanguageViewModel
import com.kieronquinn.app.utag.ui.screens.settings.advanced.language.SettingsLanguageViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.backuprestore.BackupRestoreViewModel
import com.kieronquinn.app.utag.ui.screens.settings.backuprestore.BackupRestoreViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.backuprestore.backup.BackupViewModel
import com.kieronquinn.app.utag.ui.screens.settings.backuprestore.backup.BackupViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.backuprestore.restore.config.RestoreConfigViewModel
import com.kieronquinn.app.utag.ui.screens.settings.backuprestore.restore.config.RestoreConfigViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.backuprestore.restore.progress.RestoreProgressViewModel
import com.kieronquinn.app.utag.ui.screens.settings.backuprestore.restore.progress.RestoreProgressViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.container.SettingsContainerViewModel
import com.kieronquinn.app.utag.ui.screens.settings.container.SettingsContainerViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.contentcreator.SettingsContentCreatorViewModel
import com.kieronquinn.app.utag.ui.screens.settings.contentcreator.SettingsContentCreatorViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.encryption.SettingsEncryptionViewModel
import com.kieronquinn.app.utag.ui.screens.settings.encryption.SettingsEncryptionViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.encryption.confirm.SettingsEncryptionConfirmPINViewModel
import com.kieronquinn.app.utag.ui.screens.settings.encryption.confirm.SettingsEncryptionConfirmPINViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.encryption.pintimeout.PinTimeoutViewModel
import com.kieronquinn.app.utag.ui.screens.settings.encryption.pintimeout.PinTimeoutViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.encryption.set.SettingsEncryptionSetPINViewModel
import com.kieronquinn.app.utag.ui.screens.settings.encryption.set.SettingsEncryptionSetPINViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.location.SettingsLocationViewModel
import com.kieronquinn.app.utag.ui.screens.settings.location.SettingsLocationViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.location.chaser.SettingsChaserViewModel
import com.kieronquinn.app.utag.ui.screens.settings.location.chaser.SettingsChaserViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.location.refreshfrequency.RefreshFrequencyViewModel
import com.kieronquinn.app.utag.ui.screens.settings.location.refreshfrequency.RefreshFrequencyViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.location.staleness.LocationStalenessViewModel
import com.kieronquinn.app.utag.ui.screens.settings.location.staleness.LocationStalenessViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.location.widgetfrequency.WidgetFrequencyViewModel
import com.kieronquinn.app.utag.ui.screens.settings.location.widgetfrequency.WidgetFrequencyViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.main.SettingsMainViewModel
import com.kieronquinn.app.utag.ui.screens.settings.main.SettingsMainViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.map.SettingsMapViewModel
import com.kieronquinn.app.utag.ui.screens.settings.map.SettingsMapViewModelImpl
import com.kieronquinn.app.utag.ui.screens.settings.security.SettingsSecurityViewModel
import com.kieronquinn.app.utag.ui.screens.settings.security.SettingsSecurityViewModelImpl
import com.kieronquinn.app.utag.ui.screens.setup.account.SetupAccountViewModel
import com.kieronquinn.app.utag.ui.screens.setup.account.SetupAccountViewModelImpl
import com.kieronquinn.app.utag.ui.screens.setup.chaser.SetupChaserViewModel
import com.kieronquinn.app.utag.ui.screens.setup.chaser.SetupChaserViewModelImpl
import com.kieronquinn.app.utag.ui.screens.setup.landing.SetupLandingViewModel
import com.kieronquinn.app.utag.ui.screens.setup.landing.SetupLandingViewModelImpl
import com.kieronquinn.app.utag.ui.screens.setup.mod.SetupModViewModel
import com.kieronquinn.app.utag.ui.screens.setup.mod.SetupModViewModelImpl
import com.kieronquinn.app.utag.ui.screens.setup.permissions.SetupPermissionsViewModel
import com.kieronquinn.app.utag.ui.screens.setup.permissions.SetupPermissionsViewModelImpl
import com.kieronquinn.app.utag.ui.screens.setup.privacy.SetupPrivacyViewModel
import com.kieronquinn.app.utag.ui.screens.setup.privacy.SetupPrivacyViewModelImpl
import com.kieronquinn.app.utag.ui.screens.setup.uts.SetupUtsViewModel
import com.kieronquinn.app.utag.ui.screens.setup.uts.SetupUtsViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.TagLocationHistoryViewModel
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.TagLocationHistoryViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.export.TagLocationExportDialogViewModel
import com.kieronquinn.app.utag.ui.screens.tag.locationhistory.export.TagLocationExportDialogViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.guide.LostModeGuideViewModel
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.guide.LostModeGuideViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.LostModeSettingsViewModel
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.LostModeSettingsViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.customurl.LostModeCustomURLViewModel
import com.kieronquinn.app.utag.ui.screens.tag.lostmode.settings.customurl.LostModeCustomURLViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.map.TagMapViewModel
import com.kieronquinn.app.utag.ui.screens.tag.map.TagMapViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.TagMoreAutomationViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.TagMoreAutomationViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.apppicker.TagMoreAutomationAppPickerViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.apppicker.TagMoreAutomationAppPickerViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.permission.TagMoreAutomationPermissionViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.permission.TagMoreAutomationPermissionViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.shortcutpicker.TagMoreAutomationShortcutPickerViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.shortcutpicker.TagMoreAutomationShortcutPickerViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.tasker.TagMoreAutomationTaskerViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.tasker.TagMoreAutomationTaskerViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.type.TagMoreAutomationTypeFragmentViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.automation.type.TagMoreAutomationTypeFragmentViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.finddevice.TagMoreFindDeviceViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.finddevice.TagMoreFindDeviceViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.main.MoreMainViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.main.MoreMainViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.nearby.TagMoreNearbyViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.nearby.TagMoreNearbyViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.notifydisconnect.TagMoreNotifyDisconnectViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.notifydisconnect.TagMoreNotifyDisconnectViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.more.passivemode.TagMorePassiveModeViewModel
import com.kieronquinn.app.utag.ui.screens.tag.more.passivemode.TagMorePassiveModeViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.picker.TagDevicePickerViewModel
import com.kieronquinn.app.utag.ui.screens.tag.picker.TagDevicePickerViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.picker.favourites.TagDevicePickerFavouritesViewModel
import com.kieronquinn.app.utag.ui.screens.tag.picker.favourites.TagDevicePickerFavouritesViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogViewModel
import com.kieronquinn.app.utag.ui.screens.tag.pinentry.TagPinEntryDialogViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.ring.TagRingDialogViewModel
import com.kieronquinn.app.utag.ui.screens.tag.ring.TagRingDialogViewModelImpl
import com.kieronquinn.app.utag.ui.screens.tag.root.TagRootViewModel
import com.kieronquinn.app.utag.ui.screens.tag.root.TagRootViewModelImpl
import com.kieronquinn.app.utag.ui.screens.unknowntag.container.UnknownTagContainerViewModel
import com.kieronquinn.app.utag.ui.screens.unknowntag.container.UnknownTagContainerViewModelImpl
import com.kieronquinn.app.utag.ui.screens.unknowntag.list.UnknownTagListViewModel
import com.kieronquinn.app.utag.ui.screens.unknowntag.list.UnknownTagListViewModelImpl
import com.kieronquinn.app.utag.ui.screens.unknowntag.settings.UnknownTagSettingsViewModel
import com.kieronquinn.app.utag.ui.screens.unknowntag.settings.UnknownTagSettingsViewModelImpl
import com.kieronquinn.app.utag.ui.screens.unknowntag.tag.UnknownTagViewModel
import com.kieronquinn.app.utag.ui.screens.unknowntag.tag.UnknownTagViewModelImpl
import com.kieronquinn.app.utag.ui.screens.update.smartthings.SmartThingsUpdateViewModel
import com.kieronquinn.app.utag.ui.screens.update.smartthings.SmartThingsUpdateViewModelImpl
import com.kieronquinn.app.utag.ui.screens.update.utag.UTagUpdateViewModel
import com.kieronquinn.app.utag.ui.screens.update.utag.UTagUpdateViewModelImpl
import com.kieronquinn.app.utag.ui.screens.updates.UpdatesViewModel
import com.kieronquinn.app.utag.ui.screens.updates.UpdatesViewModelImpl
import com.kieronquinn.app.utag.ui.screens.widget.container.WidgetContainerViewModel
import com.kieronquinn.app.utag.ui.screens.widget.container.WidgetContainerViewModelImpl
import com.kieronquinn.app.utag.ui.screens.widget.history.WidgetHistoryViewModel
import com.kieronquinn.app.utag.ui.screens.widget.history.WidgetHistoryViewModelImpl
import com.kieronquinn.app.utag.ui.screens.widget.location.WidgetLocationViewModel
import com.kieronquinn.app.utag.ui.screens.widget.location.WidgetLocationViewModelImpl
import com.kieronquinn.app.utag.ui.screens.widget.picker.WidgetDevicePickerViewModel
import com.kieronquinn.app.utag.ui.screens.widget.picker.WidgetDevicePickerViewModelImpl
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper.RoomEncryptionFailedCallback
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.movement.MovementMethodPlugin
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Application: Application(), LifecycleEventObserver {

    companion object {
        const val PACKAGE_NAME_ONECONNECT = "com.samsung.android.oneconnect"
        const val CLIENT_ID_LOGIN = "yfrtglt53o" //From Samsung Account on Windows
        const val CLIENT_ID_FIND = "27zmg0v1oo" //From Samsung Find on Android
        const val CLIENT_ID_ONECONNECT = "6iado3s6jc" //From SmartThings on Android
        const val LOSTMESSAGE_URL = "https://lostmessage.smartthings.com"
        const val PP_VERSION = "1.0.0"

        fun isMainProcess() = getProcessName() == BuildConfig.APPLICATION_ID
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        startKoin {
            androidContext(base)
            modules(singles(), repositories(), viewModels())
        }
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun singles() = module {
        single<UTagDatabase> { UTagDatabase.getDatabase(get()) }
        single<CacheDatabase> { CacheDatabase.getDatabase(get()) }
        single<RootNavigation> { RootNavigationImpl() }
        single<SettingsNavigation> { SettingsNavigationImpl() }
        single<SetupNavigation> { SetupNavigationImpl() }
        single<TagRootNavigation> { TagRootNavigationImpl() }
        single<TagContainerNavigation> { TagContainerNavigationImpl() }
        single<TagMoreNavigation> { TagMoreNavigationImpl() }
        single<TagPickerNavigation> { TagPickerNavigationImpl() }
        single<WidgetContainerNavigation> { WidgetContainerNavigationImpl() }
        single<UnknownTagNavigation> { UnknownTagNavigationImpl() }
        single<Gson> { createGson() }
        single<Retrofit> { createRetrofit(get()) }
        single<Markwon> { createMarkwon() }
        single<RoomEncryptionHelper> { RoomEncryptionHelper{ getAll() } }
    }

    private fun repositories() = module {
        single<SmartThingsRepository> { SmartThingsRepositoryImpl(get(), get()) }
        single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
        single<ApiRepository> { ApiRepositoryImpl(get(), get(), get(), get(), get(), get()) }
        single<DeviceRepository> { DeviceRepositoryImpl(get(), get(), get(), get()) }
        single<LocationRepository> { LocationRepositoryImpl(get(), get()) }
        single<GeocoderRepository> {
            GeocoderRepositoryImpl(get(), get())
        } bind RoomEncryptionFailedCallback::class
        single<UserRepository> { UserRepositoryImpl(get(), get(), get()) }
        single<EncryptedSettingsRepository> { EncryptedSettingsRepositoryImpl(get()) }
        single<SmartTagServiceRepository> { SmartTagServiceRepositoryImpl(get()) }
        single<QcServiceRepository> { QcServiceRepositoryImpl(get()) }
        single<UTagServiceRepository> {
            UTagServiceRepositoryImpl(get())
        } bind RoomEncryptionFailedCallback::class
        single<SmartTagRepository> {
            SmartTagRepositoryImpl(
                geocoderRepository = get(),
                apiRepository = get(),
                context = get(),
                passiveModeRepository = get(),
                contentCreatorRepository = get(),
                database = get()
            )
        } bind RoomEncryptionFailedCallback::class
        single<SettingsRepository> { SettingsRepositoryImpl(get()) }
        single<LocationHistoryRepository> { LocationHistoryRepositoryImpl(get(), get(), get()) }
        single<EncryptionRepository> { EncryptionRepositoryImpl(get(), get()) }
        single<UpdateRepository> { UpdateRepositoryImpl(get(), get()) }
        single<DownloadRepository> { DownloadRepositoryImpl(get()) }
        single<UwbRepository> { UwbRepository.getImplementation(get()) }
        single<NotificationRepository> { NotificationRepositoryImpl(get(), get()) }
        single<FindMyDeviceRepository> { FindMyDeviceRepositoryImpl(get(), get(), get(), get()) }
        single<RulesRepository> { RulesRepositoryImpl(get(), get(), get()) }
        single<AutomationRepository> { AutomationRepositoryImpl(get(), get(), get(), get()) }
        single<SafeAreaRepository> {
            SafeAreaRepositoryImpl(get(), get(), get(), get())
        } bind RoomEncryptionFailedCallback::class
        single<StaticMapRepository> { StaticMapRepositoryImpl(get(), get(), get()) }
        single<LeftBehindRepository> { LeftBehindRepositoryImpl(get(), get(), get()) }
        single<SmartspacerRepository> { SmartspacerRepositoryImpl(get()) }
        single<WidgetRepository>(createdAtStart = isMainProcess()) {
            WidgetRepositoryImpl(get(), get(), get(), get(), get())
        } bind RoomEncryptionFailedCallback::class
        single<HistoryWidgetRepository>(createdAtStart = isMainProcess()) { HistoryWidgetRepositoryImpl(
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        ) } bind RoomEncryptionFailedCallback::class
        single<BackupRepository> { BackupRepositoryImpl(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        single<PassiveModeRepository> { PassiveModeRepositoryImpl(get(), get(), get()) }
        single<AnalyticsRepository>(createdAtStart = isMainProcess()) { AnalyticsRepositoryImpl(get(), get()) }
        single<ContentCreatorRepository> { ContentCreatorRepositoryImpl(get(), get(), get()) }
        single<NonOwnerTagRepository>(createdAtStart = isMainProcess()) { NonOwnerTagRepositoryImpl(get(), get(), get(), get(), get(), get(), get()) }
        single<ChaserRepository>(createdAtStart = isMainProcess()) { ChaserRepositoryImpl(get(), get(), get(), get(), get()) }
        single<CacheRepository> { CacheRepositoryImpl(get(), get()) }
    }

    private fun viewModels() = module {
        viewModel<SettingsContainerViewModel> { SettingsContainerViewModelImpl(get(), get(), get()) }
        viewModel<RootViewModel> { RootViewModelImpl(get(), get()) }
        viewModel<SettingsMainViewModel> { SettingsMainViewModelImpl(
            get(),
            get(),
            get(),
            get(),
            get()
        ) }
        viewModel<SetupLandingViewModel> { SetupLandingViewModelImpl(get(), get(), get()) }
        viewModel<SetupModViewModel> { SetupModViewModelImpl(get(), get(), get(), get(), get()) }
        viewModel<SetupPermissionsViewModel> { SetupPermissionsViewModelImpl(get(), get(), get(), get(), get(), get()) }
        viewModel<SetupChaserViewModel> { SetupChaserViewModelImpl(get(), get()) }
        viewModel<SetupUtsViewModel> { SetupUtsViewModelImpl(get(), get()) }
        viewModel<SetupAccountViewModel> { SetupAccountViewModelImpl(get(), get(), get()) }
        viewModel<AuthResponseViewModel> { AuthResponseViewModelImpl(get(), get()) }
        viewModel<TagRootViewModel> { TagRootViewModelImpl(get(), get()) }
        viewModel<TagMapViewModel> { deviceId ->
            TagMapViewModelImpl(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                deviceId.get()
            )
        }
        viewModel<TagDevicePickerViewModel> { params ->
            TagDevicePickerViewModelImpl(get(), get(), get(), get(), get(), get(), get(), get(), params.get(), params.get())
        }
        viewModel<TagDevicePickerFavouritesViewModel> { params ->
            TagDevicePickerFavouritesViewModelImpl(get(), get(), get(), get(), params.get())
        }
        viewModel<TagRingDialogViewModel> { deviceId ->
            TagRingDialogViewModelImpl(deviceId.get(), get())
        }
        viewModel<TagLocationHistoryViewModel> { params ->
            TagLocationHistoryViewModelImpl(get(), params.get(), params.get(), get(), get(), get(), get(), get(), get(), get(), get())
        }
        viewModel<MoreMainViewModel> { deviceId ->
            MoreMainViewModelImpl(get(), get(), get(), get(), deviceId.get(), get(), get(), get(), get(), get(), get(), get(), get())
        }
        viewModel<LostModeGuideViewModel> { params -> LostModeGuideViewModelImpl(get(), params.get(), params.get()) }
        viewModel<LostModeSettingsViewModel> { params ->
            LostModeSettingsViewModelImpl(
                get(),
                get(),
                params.get(),
                params.get(),
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        viewModel<LostModeCustomURLViewModel> { deviceId ->
            LostModeCustomURLViewModelImpl(get(), get(), get(), deviceId.get(), get(), get(), get())
        }
        viewModel<SetupPrivacyViewModel> { SetupPrivacyViewModelImpl(get(), get(), get()) }
        viewModel<TagPinEntryDialogViewModel> { TagPinEntryDialogViewModelImpl() }
        viewModel<SettingsEncryptionViewModel> { SettingsEncryptionViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
        viewModel<PinTimeoutViewModel> { PinTimeoutViewModelImpl(get()) }
        viewModel<SettingsEncryptionSetPINViewModel> { deviceId ->
            SettingsEncryptionSetPINViewModelImpl(get())
        }
        viewModel<SettingsEncryptionConfirmPINViewModel> { params ->
            SettingsEncryptionConfirmPINViewModelImpl(get(), get(), get(), get(), params.get())
        }
        viewModel<SettingsSecurityViewModel> { SettingsSecurityViewModelImpl(get(), get(), get(), get()) }
        viewModel<SettingsMapViewModel> { SettingsMapViewModelImpl(get(), get(), get()) }
        viewModel<SettingsLocationViewModel> { SettingsLocationViewModelImpl(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        ) }
        viewModel<SettingsAdvancedViewModel> { SettingsAdvancedViewModelImpl(get(), get(), get(), get()) }
        viewModel<SettingsLanguageViewModel> { SettingsLanguageViewModelImpl(get(), get()) }
        viewModel<UTagUpdateViewModel> { params -> UTagUpdateViewModelImpl(get(), get(), get(), params.get()) }
        viewModel<SmartThingsUpdateViewModel> { params -> SmartThingsUpdateViewModelImpl(get(), get(), get(), get(), params.get()) }
        viewModel<TagMoreNearbyViewModel> { params ->
            TagMoreNearbyViewModelImpl(get(), get(), get(), get(), get(), get(), get(), get(), params.get(), params.get(), params.get())
        }
        viewModel<TagMoreFindDeviceViewModel> { params ->
            TagMoreFindDeviceViewModelImpl(get(), get(), get(), params.get(), get(), params.get())
        }
        viewModel<FindMyDeviceViewModel> { config ->
            FindMyDeviceViewModelImpl(config.get(), get())
        }
        viewModel<TagMoreAutomationViewModel> { params ->
            TagMoreAutomationViewModelImpl(get(), get(), get(), get(), params.get(), params.get(), params.get())
        }
        viewModel<TagMoreAutomationPermissionViewModel> {
            TagMoreAutomationPermissionViewModelImpl(get(), get())
        }
        viewModel<TagMoreAutomationTypeFragmentViewModel> {
            TagMoreAutomationTypeFragmentViewModelImpl(get())
        }
        viewModel<TagMoreAutomationAppPickerViewModel> {
            TagMoreAutomationAppPickerViewModelImpl(get(), get())
        }
        viewModel<TagMoreAutomationShortcutPickerViewModel> {
            TagMoreAutomationShortcutPickerViewModelImpl(get(), get())
        }
        viewModel<TagMoreAutomationTaskerViewModel> {
            TagMoreAutomationTaskerViewModelImpl(get(), get())
        }
        viewModel<RefreshFrequencyViewModel> { RefreshFrequencyViewModelImpl(get()) }
        viewModel<WidgetFrequencyViewModel> { WidgetFrequencyViewModelImpl(get()) }
        viewModel<SafeAreaWiFiViewModel> { params ->
            SafeAreaWiFiViewModelImpl(get(), get(), get(), get(), params.get(), params.get(), params.get())
        }
        viewModel<SafeAreaTypeViewModel> { params ->
            SafeAreaTypeViewModelImpl(get(), get(), get(), get(), params.get(), params.get())
        }
        viewModel<SafeAreaListViewModel> { SafeAreaListViewModelImpl(get(), get()) }
        viewModel<TagMoreNotifyDisconnectViewModel> { deviceId ->
            TagMoreNotifyDisconnectViewModelImpl(get(), get(), deviceId.get(), get())
        }
        viewModel<SafeAreaLocationViewModel> { params ->
            SafeAreaLocationViewModelImpl(get(), get(), get(), get(), get(), get(), get(), params.get(), params.get(), params.get())
        }
        viewModel<LocationStalenessViewModel> { LocationStalenessViewModelImpl(get()) }
        viewModel<WidgetContainerViewModel> { type ->
            WidgetContainerViewModelImpl(get(), get(), type.get())
        }
        viewModel<WidgetLocationViewModel> { params ->
            WidgetLocationViewModelImpl(get(), get(), get(), get(), get(), params.get(), params.get())
        }
        viewModel<WidgetDevicePickerViewModel> { params ->
            WidgetDevicePickerViewModelImpl(get(), get(), get(), get(), get(), params.get(), params.get(), params.get())
        }
        viewModel<WidgetHistoryViewModel> { params ->
            WidgetHistoryViewModelImpl(get(), get(), get(), get(), get(), get(), params.get(), params.get())
        }
        viewModel<TagLocationExportDialogViewModel> { params ->
            TagLocationExportDialogViewModelImpl(get(), get(), get(), get(), params.get(), params.get())
        }
        viewModel<BackupRestoreViewModel> { BackupRestoreViewModelImpl(get()) }
        viewModel<BackupViewModel> { params -> BackupViewModelImpl(get(), get(), params.get()) }
        viewModel<RestoreConfigViewModel> { params -> RestoreConfigViewModelImpl(get(), get(), get(), params.get()) }
        viewModel<RestoreProgressViewModel> { params -> RestoreProgressViewModelImpl(get(), get(), params.get(), params.get()) }
        viewModel<TagMorePassiveModeViewModel> { params -> TagMorePassiveModeViewModelImpl(get(), params.get()) }
        viewModel<SettingsContentCreatorViewModel> { SettingsContentCreatorViewModelImpl(
            get(),
            get()
        ) }
        viewModel<UpdatesViewModel> { UpdatesViewModelImpl(get(), get(), get(), get(), get(), get(), get()) }
        viewModel<SettingsChaserViewModel> { SettingsChaserViewModelImpl(get(), get()) }
        viewModel<UnknownTagContainerViewModel> { UnknownTagContainerViewModelImpl(get(), get()) }
        viewModel<UnknownTagListViewModel> { params -> UnknownTagListViewModelImpl(params.get(), get(), get(), get(), get()) }
        viewModel<UnknownTagSettingsViewModel> { UnknownTagSettingsViewModelImpl(
            get(),
            get(),
            get()
        ) }
        viewModel<UnknownTagViewModel> { params -> UnknownTagViewModelImpl(get(), get(), get(), get(), get(), params.get(), params.get()) }
    }

    private fun createRetrofit(gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://localhost/") //Default to allowing custom URLs
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun createGson(): Gson {
        return Gson().newBuilder()
            .disableHtmlEscaping()
            .create()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when(event) {
            Lifecycle.Event.ON_STOP -> {
                val authRepository by inject<AuthRepository>()
                authRepository.onAppBackgrounded()
            }
            else -> {
                //No-op
            }
        }
    }

    private fun createMarkwon(): Markwon {
        val typeface = ResourcesCompat.getFont(this, R.font.oneui_sans_medium)
        return Markwon.builder(this)
            .usePlugins(listOf(
                MovementMethodPlugin.create(BetterLinkMovementMethod.getInstance()),
                object: AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        typeface?.let {
                            builder.headingTypeface(it)
                            builder.headingBreakHeight(0)
                        }
                    }
                }
            )).build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Glide.get(this).trimMemory(level)
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).clearMemory()
    }

    private fun isMainProcess(): Boolean {
        return getProcessName() == packageName
    }

}