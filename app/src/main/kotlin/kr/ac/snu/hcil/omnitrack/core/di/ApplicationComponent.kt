package kr.ac.snu.hcil.omnitrack.core.di

import android.content.SharedPreferences
import com.udojava.evalex.Expression
import dagger.Component
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAudioRecordAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTFileInvolvedAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTImageAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions.RealmLazyFunction
import kr.ac.snu.hcil.omnitrack.core.net.OTOfficialServerApiController
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.visualization.models.*
import kr.ac.snu.hcil.omnitrack.receivers.RebootReceiver
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService
import kr.ac.snu.hcil.omnitrack.services.OTSynchronizationService
import kr.ac.snu.hcil.omnitrack.services.OTTableExportService
import kr.ac.snu.hcil.omnitrack.services.messaging.OTFirebaseInstanceIdService
import kr.ac.snu.hcil.omnitrack.services.messaging.OTFirebaseMessagingService
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.AttributeEditDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AudioRecordInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.SendReportActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.ShortcutPanelWidgetConfigActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.*
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditionViewModelBase
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.items.NewItemCreationViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.settings.SettingsActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.FieldPresetSelectionBottomSheetFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TrackerAssignPanel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TriggerDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.AManagedTriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerViewModel
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetService
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService
import javax.inject.Singleton

/**
 * Created by Young-Ho on 10/31/2017.
 */
@Singleton
@Component(modules = arrayOf(
        ApplicationModule::class,
        AuthModule::class,
        BackendDatabaseModule::class,
        NetworkModule::class,
        InformationHelpersModule::class,
        DaoSerializationModule::class,
        ScheduledJobModule::class,
        SynchronizationModule::class,
        TriggerSystemModule::class,
        ScriptingModule::class
))
interface ApplicationComponent {

    fun defaultPreferences(): SharedPreferences

    fun shortcutPanelManager(): Lazy<OTShortcutPanelManager>

    fun getSupportedScriptFunctions(): Array<Expression.LazyFunction>

    fun inject(application: OTApp)
    fun inject(realmViewModel: RealmViewModel)

    fun inject(triggerViewModel: ATriggerListViewModel)

    fun inject(service: OTShortcutPanelWidgetService)
    fun inject(service: OTShortcutPanelWidgetUpdateService)

    fun inject(activity: OTActivity)

    fun inject(activity: SignInActivity)

    fun inject(fragment: AttributeEditDialogFragment)

    fun inject(fragment: SettingsActivity.SettingsFragment)

    fun inject(fragment: FieldPresetSelectionBottomSheetFragment)

    fun inject(fragment: TrackerListFragment)

    fun inject(service: OTFirebaseInstanceIdService)

    fun inject(view: TrackerAssignPanel)

    fun inject(service: OTOfficialServerApiController)

    fun inject(authManager: OTAuthManager)

    fun inject(activity: SendReportActivity)

    fun inject(attributeManager: OTAttributeManager.Companion)

    fun inject(audioRecordInputView: AudioRecordInputView)

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
    fun inject(viewModel: TimeSeriesPlotModel)
    fun inject(viewModel: DurationHeatMapModel)

    fun inject(expression: RealmLazyFunction)

    fun inject(service: OTFirebaseMessagingService)
}