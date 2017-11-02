package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Component
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTAudioRecordAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTFileInvolvedAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTImageAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.OTSynchronizationService
import kr.ac.snu.hcil.omnitrack.core.net.OTOfficialServerApiController
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.models.*
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService
import kr.ac.snu.hcil.omnitrack.services.OTTableExportService
import kr.ac.snu.hcil.omnitrack.services.messaging.OTFirebaseInstanceIdService
import kr.ac.snu.hcil.omnitrack.ui.activities.OTActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.sound.AudioItemListView
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.AttributeEditDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AudioRecordInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ImageInputView
import kr.ac.snu.hcil.omnitrack.ui.pages.SendReportActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.ShortcutPanelWidgetConfigActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.TrackerListFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.items.NewItemCreationViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.settings.SettingsActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.FieldPresetSelectionBottomSheetFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.ManagedReminderListViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TrackerAssignPanel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerListViewModel
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetService
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService
import javax.inject.Singleton

/**
 * Created by Young-Ho on 10/31/2017.
 */
@Singleton
@Component(modules = arrayOf(ApplicationModule::class, OmniTrackModule::class))
interface ApplicationComponent {

    fun makeDaoSerializationComponentBuilder(): DaoSerializationComponent.Builder

    fun inject(application: OTApp)
    fun inject(realmViewModel: RealmViewModel)

    fun inject(triggerViewModel: ATriggerListViewModel)

    fun inject(loggingService: OTItemLoggingService)

    fun inject(itemViewModel: NewItemCreationViewModel)

    fun inject(service: OTShortcutPanelWidgetService)
    fun inject(service: OTShortcutPanelWidgetUpdateService)

    fun inject(activity: OTActivity)

    fun inject(activity: SignInActivity)

    fun inject(fragment: AttributeEditDialogFragment)

    fun inject(viewModel: TrackerDetailViewModel)

    fun inject(fragment: FieldPresetSelectionBottomSheetFragment)

    fun inject(fragment: TrackerListFragment)

    fun inject(viewModel: ManagedReminderListViewModel)

    fun inject(fragment: SettingsActivity.SettingsFragment)

    fun inject(service: OTFirebaseInstanceIdService)

    fun inject(view: TrackerAssignPanel)

    fun inject(service: OTOfficialServerApiController)

    fun inject(authManager: OTAuthManager)

    fun inject(activity: SendReportActivity)

    fun inject(attributeManager: OTAttributeManager.Companion)

    fun inject(audioItemListView: AudioItemListView)

    fun inject(audioRecordInputView: AudioRecordInputView)

    fun inject(imageInputView: ImageInputView)

    fun inject(activity: ShortcutPanelWidgetConfigActivity)

    fun inject(service: OTSynchronizationService)

    fun inject(service: OTTableExportService)

    fun inject(helper: OTFileInvolvedAttributeHelper)
    fun inject(helper: OTAudioRecordAttributeHelper)
    fun inject(helper: OTImageAttributeHelper)

    fun inject(chartModel: LoggingHeatMapModel)
    fun inject(chartModel: DailyCountChartModel)
    fun inject(chartModel: TimelineComparisonLineChartModel)
    fun inject(chartModel: DurationTimelineModel)
    fun inject(chartModel: ChoiceCategoricalBarChartModel)
}