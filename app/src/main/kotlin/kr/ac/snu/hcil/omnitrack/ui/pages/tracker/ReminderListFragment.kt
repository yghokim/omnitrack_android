package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import io.reactivex.Single
import kotlinx.android.synthetic.main.fragment_tracker_detail_triggers.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerListFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerListViewModel
import java.util.*

/**
 * Created by younghokim on 2017. 10. 22..
 */
class ReminderListFragment : ATriggerListFragment<ATriggerListViewModel>() {

    private lateinit var parentViewModel: TrackerDetailViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ui_empty_list_message.setText(R.string.msg_reminder_empty)
    }

    override fun initializeNewViewModel(savedInstanceState: Bundle?): Single<ATriggerListViewModel> {
        return Single.defer {
            parentViewModel = ViewModelProviders.of(activity!!).get(TrackerDetailViewModel::class.java)
            return@defer parentViewModel.isInitializedObservable.filter { it }.firstOrError().map { isInitialized ->
                val trackerId = parentViewModel.trackerId
                if (trackerId != null) // editmode
                {
                    return@map ViewModelProviders.of(this@ReminderListFragment).get(ManagedReminderListViewModel::class.java)
                            .apply { if (savedInstanceState == null) this.init(trackerId) }
                } else {
                    //new mode
                    creationSubscriptions.add(parentViewModel.onChangesApplied.subscribe { trackerId ->
                        (viewModel as? OfflineReminderListViewModel)?.applyToDb(trackerId)
                    })
                    return@map ViewModelProviders.of(this@ReminderListFragment).get(OfflineReminderListViewModel::class.java)
                            .apply { if (savedInstanceState == null) this.init() }
                }
            }
        }
    }

    override fun onProcessNewDefaultTrigger(dao: OTTriggerDAO) {
        super.onProcessNewDefaultTrigger(dao)
        dao.objectId = UUID.randomUUID().toString()
    }


}