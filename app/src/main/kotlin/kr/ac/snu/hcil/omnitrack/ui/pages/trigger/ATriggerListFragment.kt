package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.arch.lifecycle.ViewModelProviders
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import kotlinx.android.synthetic.main.fragment_tracker_detail_triggers.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.OTTriggerInformationHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.DrawableListBottomSpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerListViewModel
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId

/**
 * Created by younghokim on 2017. 10. 21..
 */
abstract class ATriggerListFragment<ViewModelType : ATriggerListViewModel>(val viewModelClass: Class<ViewModelType>) : OTFragment(), View.OnClickListener {
    companion object {
        // private const val STATE_EXPANDED_POSITION = "expandedTriggerPosition"
        //used when opending TriggerDetailActivity
        const val DETAIL_REQUEST_CODE = 12214

        const val VIEWTYPE_NORMAL = 0
        const val VIEWTYPE_GHOST = 1
    }

    protected lateinit var viewModel: ViewModelType

    private val currentTriggerViewModelList = ArrayList<ATriggerListViewModel.TriggerViewModel>()

    private val triggerListAdapter = TriggerListAdapter()

    private val triggerTypeDialog: AlertDialog by lazy {
        NewTriggerConditionTypeSelectionDialogHelper.builder(context, OTTriggerInformationHelper.getActionName(viewModel.defaultTriggerInterfaceOptions.defaultActionType) ?: 0, viewModel.defaultTriggerInterfaceOptions.supportedConditionTypes) { type ->
            println("trigger type selected - $type")
            viewModel.addNewTrigger(makeNewDefaultTrigger(type))
            //startActivityForResult(TriggerDetailActivity.makeNewTriggerIntent(context, type, triggerActionType, hideTrackerAssignmentInterface()), ATriggerListFragmentCore.DETAIL_REQUEST_CODE)
            triggerTypeDialog.dismiss()
        }.create()
    }

    abstract fun onViewModelUpdate(viewModel: ViewModelType)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(viewModelClass)
        onViewModelUpdate(viewModel)

        ui_trigger_list.emptyView = ui_empty_list_message.apply {
            setText(viewModel.emptyMessageResId)
        }

        ui_button_new_trigger.setOnClickListener(this)

        creationSubscriptions.add(
                viewModel.currentTriggerViewModelListObservable.subscribe { newList ->
                    val diffResult = DiffUtil.calculateDiff(IReadonlyObjectId.DiffUtilCallback(currentTriggerViewModelList, newList))

                    this.currentTriggerViewModelList.clear()
                    this.currentTriggerViewModelList.addAll(newList)

                    diffResult.dispatchUpdatesTo(triggerListAdapter)
                }
        )
    }

    override fun onClick(view: View) {
        if (view === ui_button_new_trigger) {
            val conditionTypes = viewModel.defaultTriggerInterfaceOptions.supportedConditionTypes
            if (conditionTypes?.size == 1) {
                //immediately create a trigger.
                //open trigger activity.
                viewModel.addNewTrigger(makeNewDefaultTrigger(conditionTypes.first()))
            } else {
                //show dialog
                triggerTypeDialog.show()
            }
        }
    }

    protected open fun onProcessNewDefaultTrigger(dao: OTTriggerDAO) {
    }

    protected open fun makeNewDefaultTrigger(conditionType: Byte): OTTriggerDAO {
        val newDao = OTTriggerDAO()
        newDao.conditionType = conditionType
        newDao.initialize()

        onProcessNewDefaultTrigger(newDao)
        return newDao
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

        ui_trigger_list.adapter = triggerListAdapter

        setFloatingButtonColor(ContextCompat.getColor(context, R.color.colorPointed))
    }

    fun setFloatingButtonColor(color: Int) {
        ui_button_new_trigger.backgroundTintList = ColorStateList.valueOf(color)
        val hsv = floatArrayOf(0f, 0f, 0f)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.8f
        ui_button_new_trigger.rippleColor = Color.HSVToColor(hsv)
    }

    inner class TriggerListAdapter : RecyclerView.Adapter<ATriggerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ATriggerViewHolder {
            return when (viewType) {
                VIEWTYPE_GHOST -> {
                    TriggerViewHolder(parent)
                }
                VIEWTYPE_NORMAL -> {
                    TriggerViewHolder(parent)
                }
                else -> TriggerViewHolder(parent)
            }
        }

        override fun onBindViewHolder(holder: ATriggerViewHolder?, position: Int) {
            holder?.bind(getTriggerViewModelAt(position))
        }

        override fun getItemCount(): Int {
            return currentTriggerViewModelList.size
        }

        private fun getTriggerViewModelAt(position: Int): ATriggerListViewModel.TriggerViewModel {
            return currentTriggerViewModelList[position]
        }

        override fun getItemViewType(position: Int): Int {
            return if (OTTriggerViewFactory.getConditionViewProvider(getTriggerViewModelAt(position).dao) == null) {
                VIEWTYPE_GHOST
            } else VIEWTYPE_NORMAL
        }


    }

    inner abstract class ATriggerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(triggerViewModel: ATriggerListViewModel.TriggerViewModel)
    }

    inner class TriggerViewHolder(parentView: ViewGroup?) : ATriggerViewHolder(LayoutInflater.from(context).inflate(R.layout.trigger_list_element, parentView, false)) {
        override fun bind(triggerViewModel: ATriggerListViewModel.TriggerViewModel) {

        }

    }

}