package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.arch.lifecycle.ViewModelProviders
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kotlinx.android.synthetic.main.fragment_tracker_detail_triggers.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.OTTriggerInformationHelper
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.DrawableListBottomSpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerListViewModel

/**
 * Created by younghokim on 2017. 10. 21..
 */
abstract class ATriggerListFragment<ViewModelType : ATriggerListViewModel>(val viewModelClass: Class<ViewModelType>) : OTFragment(), View.OnClickListener {
    companion object {
        // private const val STATE_EXPANDED_POSITION = "expandedTriggerPosition"
        //used when opending TriggerDetailActivity
        const val DETAIL_REQUEST_CODE = 12214

        const val PARAM_MODE = "userIdMode"
        const val PARAM_MODE_STANDALONE = "standalone"
        const val PARAM_MODE_FOLLOW_PARENT_ACTIVITY = "followParentActivity"

        const val PARAM_VIEWMODEL_CLASS = "viewModelClass"
        const val PARAM_PARENT_VIEWMODEL_CLASS = "parentViewModelClass"
    }

    private lateinit var viewModel: ViewModelType

    private val triggerTypeDialog: AlertDialog by lazy {
        NewTriggerConditionTypeSelectionDialogHelper.builder(context, OTTriggerInformationHelper.getActionName(viewModel.defaultTriggerInterfaceOptions.defaultActionType) ?: 0, viewModel.defaultTriggerInterfaceOptions.supportedConditionTypes) { type ->
            println("trigger type selected - $type")
            //startActivityForResult(TriggerDetailActivity.makeNewTriggerIntent(context, type, triggerActionType, hideTrackerAssignmentInterface()), ATriggerListFragmentCore.DETAIL_REQUEST_CODE)
            triggerTypeDialog.dismiss()
        }.create()
    }

    abstract fun onViewModelUpdate(viewModel: ViewModelType)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(viewModelClass)
        onViewModelUpdate(viewModel)
/*
        val vmClass = (arguments?.getString(PARAM_VIEWMODEL_CLASS)?.let{Class.forName(it)} as? Class<ATriggerListViewModel>) ?: ATriggerListViewModel::class.java

        viewModel = ViewModelProviders.of(this).get(vmClass)

        when(arguments?.getString(PARAM_MODE))
        {
            PARAM_MODE_STANDALONE->{
                val userId= arguments?.getString(OTApp.INTENT_EXTRA_OBJECT_ID_USER) ?: OTAuthManager.userId
                viewModel.userId = userId
            }
            PARAM_MODE_FOLLOW_PARENT_ACTIVITY->
            {
                val parentViewModel = ViewModelProviders.of(activity).get(Class.forName(arguments.getString(PARAM_PARENT_VIEWMODEL_CLASS)) as Class<UserAttachedViewModel>)
                if(parentViewModel!=null) {
                    creationSubscriptions.add(
                            parentViewModel.userIdObservable.subscribe {
                                (userId)->
                                viewModel.userId = userId
                            }
                    )
                }
            }
        }*/


        ui_trigger_list.emptyView = ui_empty_list_message.apply {
            setText(viewModel.emptyMessageResId)
        }

        ui_button_new_trigger.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        if (view === ui_button_new_trigger) {
            val actionTypes = viewModel.defaultTriggerInterfaceOptions.supportedConditionTypes
            if (actionTypes?.size == 1) {
                //immediately create a trigger.
                //open trigger activity.
            } else {
                //show dialog
                triggerTypeDialog.show()
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_tracker_detail_triggers, container, false)
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui_trigger_list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        ui_trigger_list.addItemDecoration(HorizontalImageDividerItemDecoration(context = context))
        ui_trigger_list.addItemDecoration(DrawableListBottomSpaceItemDecoration(R.drawable.expanded_view_inner_shadow_top, context.resources.getDimensionPixelSize(R.dimen.tracker_list_bottom_space), false))

        ui_trigger_list.itemAnimator = SlideInLeftAnimator(DecelerateInterpolator(2.0f))


        setFloatingButtonColor(ContextCompat.getColor(context, R.color.colorPointed))
    }

    fun setFloatingButtonColor(color: Int) {
        ui_button_new_trigger.backgroundTintList = ColorStateList.valueOf(color)
        val hsv = floatArrayOf(0f, 0f, 0f)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.8f
        ui_button_new_trigger.rippleColor = Color.HSVToColor(hsv)
    }

}