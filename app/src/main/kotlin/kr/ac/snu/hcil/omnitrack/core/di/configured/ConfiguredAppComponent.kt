package kr.ac.snu.hcil.omnitrack.core.di.configured

import android.content.SharedPreferences
import com.udojava.evalex.Expression
import dagger.Subcomponent
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.analytics.OTUsageLoggingManager
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAudioRecordAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTFileInvolvedAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTImageAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions.RealmLazyFunction
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.net.OTOfficialServerApiController
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTReminderCommands
import kr.ac.snu.hcil.omnitrack.core.visualization.models.*
import kr.ac.snu.hcil.omnitrack.receivers.RebootReceiver
import kr.ac.snu.hcil.omnitrack.services.*
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioItemListView
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.AttributeEditDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AudioRecordInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ChoiceInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ImageInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.SendReportActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.AttributeDetailActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.attribute.AttributeDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.SettingsActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.ShortcutPanelWidgetConfigActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.export.PackageExportViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.export.UploadTemporaryPackageDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.home.*
import kr.ac.snu.hcil.omnitrack.ui.pages.items.*
import kr.ac.snu.hcil.omnitrack.ui.pages.services.AttributeSelectionPage
import kr.ac.snu.hcil.omnitrack.ui.pages.services.ServiceWizardView
import kr.ac.snu.hcil.omnitrack.ui.pages.services.TrackerSelectionPage
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.FieldPresetSelectionBottomSheetFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailStructureTabFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TrackerAssignPanel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TriggerDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.AManagedTriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerViewModel
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetService
import java.util.*

/**
 * Created by Young-Ho on 10/31/2017.
 */
@Configured
@Subcomponent(modules = [
    ConfiguredModule::class,
    AuthModule::class,
    BackendDatabaseModule::class,
    NetworkModule::class,
    InformationHelpersModule::class,
    DaoSerializationModule::class,
    ScheduledJobModule::class,
    SynchronizationModule::class,
    TriggerSystemModule::class,
    ScriptingModule::class,
    UsageLoggingModule::class,
    ResearchModule::class
])
interface ConfiguredAppComponent {

    @Subcomponent.Builder
    interface Builder {

        fun plus(module: ConfiguredModule): Builder
        fun plus(module: FirebaseModule): Builder
        fun plus(module: AuthModule): Builder
        fun plus(module: BackendDatabaseModule): Builder
        fun plus(module: NetworkModule): Builder
        fun plus(module: InformationHelpersModule): Builder
        fun plus(module: DaoSerializationModule): Builder
        fun plus(module: ScheduledJobModule): Builder
        fun plus(module: SynchronizationModule): Builder
        fun plus(module: TriggerSystemModule): Builder
        fun plus(module: ScriptingModule): Builder
        fun plus(module: UsageLoggingModule): Builder
        fun plus(module: ResearchModule): Builder

        fun build(): ConfiguredAppComponent
    }

    fun shortcutPanelManager(): OTShortcutPanelManager

    fun getSupportedScriptFunctions(): Array<Expression.LazyFunction>

    fun getBackendDbManager(): BackendDbManager

    fun getPreferredTimeZone(): TimeZone

    @Backend
    fun backendRealmFactory(): Factory<Realm>

    fun getEventLogger(): IEventLogger

    fun getAuthManager(): OTAuthManager

    fun getSyncManager(): OTSyncManager

    fun getAttributeManager(): OTAttributeManager

    @ConfiguredObject
    fun getConfiguredPreferences(): SharedPreferences

    fun inject(application: OTApp)
    fun inject(realmViewModel: RealmViewModel)

    fun inject(triggerViewModel: ATriggerListViewModel)

    fun inject(loggingManager: OTUsageLoggingManager)

    fun inject(factory: OTShortcutPanelWidgetService.PanelWidgetElementFactory)

    fun inject(activity: OTActivity)

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
    fun inject(chartModel: DurationTimelineModel)
    fun inject(chartModel: ChoiceCategoricalBarChartModel)

    fun inject(receiver: RebootReceiver)


    fun inject(viewModel: HomeScreenViewModel)
    fun inject(service: OTSynchronizationService)
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
    fun inject(activity: ItemDetailActivity)
    fun inject(activity: AttributeDetailActivity)

    fun inject(fragment: TrackerDetailStructureTabFragment)

    fun inject(expression: RealmLazyFunction)

    fun inject(task: OTUsageLogUploadService.ConfiguredTask)
    fun inject(task: OTInformationUploadService.ConfiguredTask)
    fun inject(task: OTSynchronizationService.ConfiguredTask)
    fun inject(task: OTItemLoggingService.ConfiguredTask)
    fun inject(task: OTTableExportService.ConfiguredTask)

    fun inject(service: OTBinaryUploadService)

    fun inject(task: OTBinaryUploadService.ConfiguredTask)
    fun inject(task: OTReminderCommands)

    fun inject(viewModel: UploadTemporaryPackageDialogFragment.ViewModel)
}