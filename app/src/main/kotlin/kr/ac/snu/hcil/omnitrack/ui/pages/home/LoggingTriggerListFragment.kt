package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.arch.lifecycle.ViewModelProviders
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.ATriggerListFragment
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel

/**
 * Created by younghokim on 2017. 10. 21..
 */
class LoggingTriggerListFragment : ATriggerListFragment<LoggingTriggerListViewModel>(LoggingTriggerListViewModel::class.java) {

    override fun onViewModelUpdate(viewModel: LoggingTriggerListViewModel) {
        val parentViewModel = ViewModelProviders.of(activity).get(UserAttachedViewModel::class.java)
        creationSubscriptions.add(
                parentViewModel.userIdObservable.subscribe { (userId) ->
                    if (userId != null)
                        viewModel.init(userId)
            }
        )
    }

}