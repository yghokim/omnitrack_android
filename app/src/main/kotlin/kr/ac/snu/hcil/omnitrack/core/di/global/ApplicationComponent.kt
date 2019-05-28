package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.work.PeriodicWorkRequest
import com.google.gson.Gson
import com.udojava.evalex.Expression
import dagger.Component
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.analytics.OTUsageLoggingManager
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAudioRecordAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTFileInvolvedAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTImageAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.properties.OTPropertyManager
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions.RealmLazyFunction
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.DaoSerializationManager
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.externals.misfit.MisfitApi
import kr.ac.snu.hcil.omnitrack.core.net.OTOfficialServerApiController
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSynchronizationCommands
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataDrivenTriggerManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTReminderCommands
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.measures.OTItemMetadataMeasureFactoryLogicImpl
import kr.ac.snu.hcil.omnitrack.core.visualization.models.*
import kr.ac.snu.hcil.omnitrack.core.workers.OTBinaryUploadWorker
import kr.ac.snu.hcil.omnitrack.core.workers.OTInformationUploadWorker
import kr.ac.snu.hcil.omnitrack.core.workers.OTResearchSynchronizationWorker
import kr.ac.snu.hcil.omnitrack.core.workers.OTUsageLogUploadWorker
import kr.ac.snu.hcil.omnitrack.receivers.DataDrivenTriggerCheckReceiver
import kr.ac.snu.hcil.omnitrack.receivers.PackageReceiver
import kr.ac.snu.hcil.omnitrack.receivers.RebootReceiver
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import kr.ac.snu.hcil.omnitrack.services.OTTableExportService
import kr.ac.snu.hcil.omnitrack.services.messaging.OTFirebaseMessagingService
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioItemListView
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.AttributeEditDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactoryManager
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AudioRecordInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ChoiceInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ImageInputView
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.components.scales.QuantizedTimeScale
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.drawers.MultiLineChartDrawer
import kr.ac.snu.hcil.omnitrack.ui.pages.SendReportActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.AttributeDetailActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.AttributeDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages.SourceSelectionPage
import kr.ac.snu.hcil.omnitrack.ui.pages.auth.SignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.auth.SignUpActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.auth.SignUpCredentialSlideFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.auth.SignUpViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.SettingsActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.ShortcutPanelWidgetConfigActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.export.PackageExportViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.export.UploadTemporaryPackageDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.home.*
import kr.ac.snu.hcil.omnitrack.ui.pages.items.*
import kr.ac.snu.hcil.omnitrack.ui.pages.research.ResearchViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.services.*
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.FieldPresetSelectionBottomSheetFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailStructureTabFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TrackerAssignPanel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TriggerDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.data.DataDrivenConditionViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.data.DataDrivenTriggerConfigurationPanel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.time.TimeConditionViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.AManagedTriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.OfflineTriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerViewModel
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.utils.time.LocalTimeFormats
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetProvider
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetService
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService
import java.util.*
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 15..
 */
@Singleton
@Component(modules = [
    ApplicationModule::class,
    FirebaseModule::class,
    UsageLoggingModule::class,
    ScheduledJobModule::class,
    UIHelperModule::class,
    SerializationModule::class,
    ExternalServiceModule::class,
    SystemIdentifierFactoryModule::class,
    MeasureModule::class,
    AuthModule::class,
    DaoSerializationModule::class,
    SynchronizationModule::class,
    TriggerSystemModule::class,
    InformationHelpersModule::class,
    ScriptingModule::class,
    NetworkModule::class,
    ResearchModule::class
])
interface ApplicationComponent {

    @Default
    fun defaultPreferences(): SharedPreferences

    fun application(): OTAndroidApp

    fun applicationContext(): Provider<Context>

    fun wrappedResources(): Provider<Resources>


    fun getPreferredTimeZone(): TimeZone

    fun getLocalTimeFormats(): LocalTimeFormats

    @ForGeneric
    fun genericGson(): Gson

    @FirebaseInstanceIdToken
    fun getFirebaseInstanceIdToken(): Single<String>

    fun getServiceManager(): Lazy<OTExternalServiceManager>

    fun getTriggerSystemManager(): Lazy<OTTriggerSystemManager>

    fun getAttributeViewFactoryManager(): AttributeViewFactoryManager


    @ServerFullSync
    fun getFullSyncPeriodicRequestProvider(): Provider<PeriodicWorkRequest>


    fun manager(): DaoSerializationManager
    fun dataDrivenConditionTypeAdapter(): OTDataDrivenTriggerCondition.ConditionTypeAdapter

    fun inject(wrapper: OTItemBuilderWrapperBase)

    fun inject(service: OTFirebaseMessagingService)

    fun inject(service: OTShortcutPanelWidgetService)
    fun inject(service: OTShortcutPanelWidgetUpdateService)
    fun inject(provider: OTShortcutPanelWidgetProvider)

    fun inject(drawer: MultiLineChartDrawer)

    fun inject(panel: DataDrivenTriggerConfigurationPanel)
    fun inject(page: SourceSelectionPage)

    fun inject(scale: QuantizedTimeScale)

    fun inject(receiver: PackageReceiver)

    fun inject(service: DataDrivenTriggerCheckReceiver.DataDrivenConditionHandlingService)

    //-====

    fun inject(alarmService: TimeTriggerAlarmReceiver.TimeTriggerWakefulHandlingService)
    fun inject(viewModel: TimeConditionViewModel)
    fun inject(service: OTReminderService)

    fun inject(viewModel: ResearchViewModel)
    fun inject(service: OTResearchSynchronizationWorker)

    fun shortcutPanelManager(): OTShortcutPanelManager

    fun getSupportedScriptFunctions(): Array<Expression.LazyFunction>

    fun getBackendDbManager(): BackendDbManager

    fun getConnectionTypeAdapter(): OTConnection.ConnectionTypeAdapter

    @Backend
    fun backendRealmFactory(): Factory<Realm>

    fun getEventLogger(): IEventLogger

    fun getAuthManager(): OTAuthManager

    fun getSyncManager(): OTSyncManager

    fun getAttributeManager(): OTAttributeManager
    fun getPropertyManager(): OTPropertyManager

    fun inject(application: OTApp)
    fun inject(realmViewModel: RealmViewModel)

    fun inject(triggerViewModel: ATriggerListViewModel)
    fun inject(offlineTriggerListViewModel: OfflineTriggerListViewModel)

    fun inject(loggingManager: OTUsageLoggingManager)

    fun inject(factory: OTShortcutPanelWidgetService.PanelWidgetElementFactory)

    fun inject(activity: OTActivity)

    fun inject(activity: HomeActivity)

    fun inject(activity: SignInActivity)

    fun inject(fragment: OTFragment)

    fun inject(fragment: AttributeEditDialogFragment)

    fun inject(fragment: SettingsActivity.SettingsFragment)

    fun inject(fragment: FieldPresetSelectionBottomSheetFragment)

    fun inject(fragment: TrackerListFragment)

    fun inject(view: TrackerAssignPanel)

    fun inject(service: OTOfficialServerApiController)

    fun inject(authManager: OTAuthManager)

    fun inject(activity: SendReportActivity)

    fun inject(fragment: ItemBrowserActivity.SettingsDialogFragment)

    fun inject(audioRecordInputView: AudioRecordInputView)
    fun inject(view: ImageInputView)
    fun inject(view: AudioItemListView)
    fun inject(view: ChoiceInputView)

    fun inject(activity: ShortcutPanelWidgetConfigActivity)

    fun inject(service: OTTableExportService)

    fun inject(helper: OTFileInvolvedAttributeHelper)
    fun inject(helper: OTAudioRecordAttributeHelper)
    fun inject(helper: OTImageAttributeHelper)

    fun inject(chartModel: LoggingHeatMapModel)
    fun inject(chartModel: DailyCountChartModel)
    fun inject(chartModel: TimelineComparisonLineChartModel)
    fun inject(chartModel: ChoiceCategoricalBarChartModel)

    fun inject(receiver: RebootReceiver)


    fun inject(viewModel: HomeScreenViewModel)
    fun inject(sidebar: SidebarWrapper)
    fun inject(viewModel: OrderedTrackerListViewModel)
    fun inject(viewModel: TrackerDetailViewModel)
    fun inject(viewModel: TrackerListViewModel)
    fun inject(viewModel: ItemListViewModel)
    fun inject(viewModel: AManagedTriggerListViewModel)
    fun inject(viewModel: ItemEditionViewModelBase)
    fun inject(service: OTItemLoggingService)
    fun inject(viewModel: TriggerDetailViewModel)
    fun inject(viewModel: TriggerViewModel)
    fun inject(viewModel: NewItemCreationViewModel)

    fun inject(wizardView: ServiceWizardView)
    fun inject(page: TrackerSelectionPage)
    fun inject(page: AttributeSelectionPage)

    fun inject(viewModel: TimeSeriesPlotModel)
    fun inject(viewModel: DurationHeatMapModel)
    fun inject(viewModel: AttributeDetailViewModel)

    fun inject(viewModel: PackageExportViewModel)

    fun inject(activity: ItemBrowserActivity)
    fun inject(activity: NewItemActivity)
    fun inject(activity: AttributeDetailActivity)

    fun inject(fragment: TrackerDetailStructureTabFragment)

    fun inject(expression: RealmLazyFunction)

    fun inject(worker: OTBinaryUploadWorker)
    fun inject(worker: OTUsageLogUploadWorker)
    fun inject(worker: OTInformationUploadWorker)
    fun inject(commands: OTSynchronizationCommands)

    fun inject(task: OTReminderCommands)

    fun inject(viewModel: UploadTemporaryPackageDialogFragment.ViewModel)

    fun inject(fragment: ServiceListFragment)

    fun inject(activity: ExternalServiceActivationActivity)

    fun inject(dataDrivenTriggerManager: OTDataDrivenTriggerManager)

    fun inject(worker: OTDataDrivenTriggerManager.InactiveMeasureEntryClearanceWorker)
    fun inject(viewModel: DataDrivenConditionViewModel)

    fun inject(impl: OTItemMetadataMeasureFactoryLogicImpl)

    fun inject(misfitApi: MisfitApi)

    fun inject(activity: ApiKeySettingsActivity)

    fun inject(viewModel: SignUpViewModel)

    fun inject(fragment: SignUpCredentialSlideFragment)

    fun inject(activity: SignUpActivity)

    fun inject(viewModel: LoggingTriggerListViewModel)
}