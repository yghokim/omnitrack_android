package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.arch.lifecycle.ViewModelProviders
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerListFragment

/**
 * Created by younghokim on 2017. 10. 22..
 */
class ReminderListFragment : ATriggerListFragment<ReminderListViewModel>(ReminderListViewModel::class.java) {
    private lateinit var parentViewModel: TrackerDetailViewModel
    override fun onViewModelUpdate(viewModel: ReminderListViewModel) {
        parentViewModel = ViewModelProviders.of(activity).get(TrackerDetailViewModel::class.java)
        creationSubscriptions.add(
                parentViewModel.trackerIdObservable.subscribe { (trackerId) ->
                    if (trackerId != null)
                        viewModel.init(trackerId)
                }
        )
    }

}