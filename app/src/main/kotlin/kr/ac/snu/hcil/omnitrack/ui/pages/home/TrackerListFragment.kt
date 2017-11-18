package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity.RESULT_OK
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.transition.AutoTransition
import android.transition.Fade
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import butterknife.bindView
import com.afollestad.materialdialogs.MaterialDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.tracker_list_element.view.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.common.TooltipHelper
import kr.ac.snu.hcil.omnitrack.ui.components.common.container.FallbackRecyclerView
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.TopBottomHorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.tutorial.TutorialManager
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemBrowserActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemDetailActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.visualization.ChartViewActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.startActivityOnDelay
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.verticalMargin
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2016-07-18.
 */
class TrackerListFragment : OTFragment() {

    @Inject
    lateinit var authManager: OTAuthManager

    lateinit private var listView: FallbackRecyclerView

    lateinit private var emptyMessageView: TextView

    private lateinit var trackerListAdapter: TrackerListAdapter

    lateinit private var trackerListLayoutManager: LinearLayoutManager

    private lateinit var addTrackerFloatingButton: FloatingActionButton

    private lateinit var lastLoggedTimeFormat: SimpleDateFormat

    private lateinit var statHeaderSizeSpan: AbsoluteSizeSpan
    private lateinit var dateStyleSpan: StyleSpan
    private lateinit var statHeaderColorSpan: ForegroundColorSpan

    private lateinit var statContentStyleSpan: StyleSpan

    private val emptyTrackerDialog: MaterialDialog.Builder by lazy {
        MaterialDialog.Builder(context!!)
                .cancelable(true)
                .positiveColorRes(R.color.colorPointed)
                .negativeColorRes(R.color.colorRed_Light)
                .title("OmniTrack")
                .content(R.string.msg_confirm_empty_tracker_log)
                .positiveText(R.string.msg_confirm_log)
                .negativeText(R.string.msg_cancel)
                .neutralText(R.string.msg_go_to_add_field)
    }

    private val newTrackerNameDialog: MaterialDialog.Builder by lazy {
        MaterialDialog.Builder(this.context!!)
                .title(R.string.msg_new_tracker_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .setSyncWithKeyboard(true)
                .inputRangeRes(1, 20, R.color.colorRed)
                .cancelable(true)
                .negativeText(R.string.msg_cancel)
    }

    private val collapsedHeight = OTApp.instance.resourcesWrapped.getDimensionPixelSize(R.dimen.tracker_list_element_collapsed_height)
    private val expandedHeight = OTApp.instance.resourcesWrapped.getDimensionPixelSize(R.dimen.tracker_list_element_expanded_height)

    private val currentTrackerViewModelList = ArrayList<TrackerListViewModel.TrackerInformationViewModel>()

    private lateinit var viewModel: TrackerListViewModel

    companion object {
        const val CHANGE_TRACKER_SETTINGS = 0
        const val REMOVE_TRACKER = 1

        const val REQUEST_CODE_NEW_TRACKER = 1504

        const val STATE_EXPANDED_TRACKER_INDEX = "expandedTrackerIndex"

        //val transition = AutoTransition()
        val transition = TransitionSet()

        //val transition = ChangeBounds()

        init {
            //transition.addTransition(ChangeBounds())
            transition.addTransition(Fade())
            // transition.ordering = TransitionSet.ORDERING_TOGETHER

            transition.ordering = AutoTransition.ORDERING_TOGETHER
            transition.duration = 300
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_EXPANDED_TRACKER_INDEX, trackerListAdapter.currentlyExpandedIndex)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastLoggedTimeFormat = SimpleDateFormat(act.resources.getString(R.string.msg_tracker_list_time_format))
        dateStyleSpan = StyleSpan(Typeface.NORMAL)
        statContentStyleSpan = StyleSpan(Typeface.BOLD)
        statHeaderSizeSpan = AbsoluteSizeSpan(act.resources.getDimensionPixelSize(R.dimen.tracker_list_element_information_text_headerSize))
        statHeaderColorSpan = ForegroundColorSpan(ContextCompat.getColor(act, R.color.textColorLight))
        //attach events
        // user.trackerAdded += onTrackerAddedHandler
        //  user.trackerRemoved += onTrackerRemovedHandler


        trackerListAdapter = TrackerListAdapter()
        trackerListAdapter.currentlyExpandedIndex = savedInstanceState?.getInt(STATE_EXPANDED_TRACKER_INDEX, -1) ?: -1
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (act.application as OTApp).applicationComponent.inject(this)

        viewModel = ViewModelProviders.of(this).get(TrackerListViewModel::class.java)
        viewModel.userId = authManager.userId

        createViewSubscriptions.add(
                viewModel.trackerViewModels.subscribe { trackerViewModelList ->

                    val rxPermissions = RxPermissions(act)

                    val permissions = viewModel.getPermissionsRequiredForFields().filter { ContextCompat.checkSelfPermission(act, it) != PackageManager.PERMISSION_GRANTED }

                    if (permissions.isNotEmpty()) {
                        DialogHelper.makeYesNoDialogBuilder(act, resources.getString(R.string.msg_permission_required),
                                String.format(resources.getString(R.string.msg_permission_request_of_tracker)),
                                cancelable = false,
                                onYes = {
                                    rxPermissions.request(*permissions.toTypedArray()).subscribe { granted ->
                                        if (granted)
                                            println("permissions granted.")
                                        else println("permissions not granted.")
                                    }
                                },
                                onCancel = null,
                                yesLabel = R.string.msg_allow_permission,
                                noLabel = R.string.msg_cancel
                        ).show()
                    }

                    val diffResult = DiffUtil.calculateDiff(
                            IReadonlyObjectId.DiffUtilCallback(currentTrackerViewModelList, trackerViewModelList)
                    )

                    currentTrackerViewModelList.clear()
                    currentTrackerViewModelList.addAll(trackerViewModelList)
                    diffResult.dispatchUpdatesTo(trackerListAdapter)
                }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home_trackers, container, false)

        addTrackerFloatingButton = rootView.findViewById(R.id.fab)
        addTrackerFloatingButton.setOnClickListener { view ->
            newTrackerNameDialog.input(null, viewModel.generateNewTrackerName(), false) {
                    dialog, text ->
                startActivityForResult(TrackerDetailActivity.makeNewTrackerIntent(text.toString(), act), REQUEST_CODE_NEW_TRACKER)

                }.show()
                //Toast.makeText(context,String.format(resources.getString(R.string.sentence_new_tracker_added), newTracker.name), Toast.LENGTH_LONG).show()
        }

        listView = rootView.findViewById(R.id.ui_tracker_list_view)
        emptyMessageView = rootView.findViewById(R.id.ui_empty_list_message)
        listView.emptyView = emptyMessageView
        trackerListLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listView.layoutManager = trackerListLayoutManager

        val shadowDecoration = TopBottomHorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern_upper, R.drawable.horizontal_separator_pattern_under, act, resources.getFraction(R.fraction.tracker_list_separator_height_ratio, 1, 1))
        listView.addItemDecoration(shadowDecoration)
        (listView.layoutParams as CoordinatorLayout.LayoutParams).verticalMargin = -shadowDecoration.upperDividerHeight
        listView.adapter = trackerListAdapter

        return rootView
    }

    override fun onStart() {
        super.onStart()
        TutorialManager.checkAndShowTargetPrompt(TutorialManager.FLAG_TRACKER_LIST_ADD_TRACKER, true, this.act, addTrackerFloatingButton,
                R.string.msg_tutorial_add_tracker_primary,
                R.string.msg_tutorial_add_tracker_secondary,
                ContextCompat.getColor(act, R.color.colorPointed))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        for (viewHolder in trackerListAdapter.viewHolders) {
            println("viewHolder viewmodel subscriptions clear: ${viewHolder.subscriptions.size() > 0}, ${viewHolder.subscriptions.isDisposed}")
            viewHolder.subscriptions.clear()
        }
        trackerListAdapter.viewHolders.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_NEW_TRACKER) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    if (data.hasExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)) {
                        val newTrackerId = data.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)

                        /* TODO logging
                        EventLoggingManager.logTrackerChangeEvent(EventLoggingManager.EVENT_NAME_CHANGE_TRACKER_ADD, newTracker)
                        */
                    }
                }
            }
        }
    }

    private fun handleTrackerClick(tracker: OTTrackerDAO) {
        if (tracker.makeAttributesQuery(false, false).findAll().count() == 0) {
            emptyTrackerDialog
                    .onPositive { materialDialog, dialogAction ->
                        activity?.startService(OTItemLoggingService.makeLoggingIntent(act, ItemLoggingSource.Manual, true, tracker.objectId!!))
                        //OTBackgroundLoggingService.log(context, tracker, OTItem.ItemLoggingSource.Manual, notify = false).subscribe()
                    }
                    .onNeutral { materialDialog, dialogAction ->
                        startActivity(TrackerDetailActivity.makeIntent(tracker.objectId, act))
                    }
                    .show()
        } else {
            startActivity(ItemDetailActivity.makeNewItemPageIntent(tracker.objectId!!, act))
        }
    }

    inner class TrackerListAdapter : RecyclerView.Adapter<TrackerListAdapter.ViewHolder>() {

        val viewHolders = ArrayList<ViewHolder>()

        var currentlyExpandedIndex = -1
        private var lastExpandedViewHolder: ViewHolder? = null

        init {
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            try {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.tracker_list_element, parent, false)
                return ViewHolder(view).apply {
                    viewHolders.add(this)
                }
            } catch(ex: Exception) {
                ex.printStackTrace()
                throw Exception("Inflation failed")
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            println("Bind tracker viewmodel: ${currentTrackerViewModelList[position]}")
            holder.bindViewModel(currentTrackerViewModelList[position])
        }

        override fun getItemCount(): Int {
            return currentTrackerViewModelList.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

            val name: TextView by bindView(R.id.name)
            val color: View by bindView(R.id.color_bar)
            val lockedIndicator: View by bindView(R.id.ui_locked_indicator)
            val expandButton: ImageButton by bindView(R.id.ui_expand_button)

            val lastLoggingTimeView: TextView by bindView(R.id.ui_last_logging_time)
            val todayLoggingCountView: TextView by bindView(R.id.ui_today_logging_count)
            val totalItemCountView: TextView by bindView(R.id.ui_total_item_count)

            val alarmIcon: View by bindView(R.id.alarm_icon)
            val alarmText: TextView by bindView(R.id.alarm_text)

            val expandedView: View by bindView(R.id.ui_expanded_view)

            val editButton: View by bindView(R.id.ui_button_edit)
            val listButton: View by bindView(R.id.ui_button_list)
            val removeButton: View by bindView(R.id.ui_button_remove)
            val chartViewButton: View by bindView(R.id.ui_button_charts)

            val errorIndicator: AppCompatImageButton by bindView(R.id.ui_invalid_icon)

            private val validationErrorMessages = ArrayList<CharSequence>()

            private var trackerId: String? = null

            var collapsed = true

            val expandedViewHeight: Int

            var subscriptions = CompositeDisposable()

            init {

                expandedView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                expandedViewHeight = expandedView.measuredHeight

                view.setOnClickListener(this)
                editButton.setOnClickListener(this)
                listButton.setOnClickListener(this)
                removeButton.setOnClickListener(this)
                chartViewButton.setOnClickListener(this)

                expandButton.setOnClickListener(this)

                errorIndicator.setOnClickListener(this)

                collapse(false)
            }

            private val collapseAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 250
                interpolator = DecelerateInterpolator()
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {
                    }

                    override fun onAnimationEnd(p0: Animator?) {
                        expandButton.isEnabled = true
                        collapse(false)
                    }

                    override fun onAnimationCancel(p0: Animator?) {
                        collapse(false)
                    }

                    override fun onAnimationStart(p0: Animator?) {
                        expandButton.isEnabled = false
                        expandButton.setImageResource(R.drawable.more_horiz_scarse)
                    }

                })
                addUpdateListener {
                    val progress = (animatedValue as Float)
                    expandedView.alpha = progress
                    val lp = itemView.layoutParams.apply { height = (collapsedHeight + (expandedHeight - collapsedHeight) * progress).toInt() }
                    itemView.layoutParams = lp
                    itemView.requestLayout()

                    expandedView.layoutParams.height = (0.5f + (expandedViewHeight) * progress).toInt()
                    expandedView.requestLayout()
                }
            }

            private val expandAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 250
                interpolator = DecelerateInterpolator()
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {
                    }

                    override fun onAnimationEnd(p0: Animator?) {
                        expandButton.isEnabled = true
                        expand(false)
                    }

                    override fun onAnimationCancel(p0: Animator?) {
                        expand(false)
                    }

                    override fun onAnimationStart(p0: Animator?) {
                        expandButton.isEnabled = false
                        expandButton.setImageResource(R.drawable.up_dark)
                        expandedView.visibility = View.VISIBLE
                        expandedView.layoutParams.height = 0
                        expandedView.requestLayout()
                    }

                })
                addUpdateListener {
                    val progress = (animatedValue as Float)
                    expandedView.alpha = progress
                    val lp = itemView.layoutParams.apply { height = (collapsedHeight + (expandedHeight - collapsedHeight) * progress).toInt() }
                    itemView.layoutParams = lp
                    itemView.requestLayout()

                    expandedView.layoutParams.height = (0.5f + (expandedViewHeight) * progress).toInt()
                    expandedView.requestLayout()
                }
            }

            override fun onClick(view: View) {
                val trackerViewModel = currentTrackerViewModelList[adapterPosition]
                if (view === itemView) {
                    handleTrackerClick(trackerViewModel.trackerDao)
                } else if (view === editButton) {
                    startActivityOnDelay(TrackerDetailActivity.makeIntent(trackerId, this@TrackerListFragment.act))
                } else if (view === listButton) {
                    startActivityOnDelay(ItemBrowserActivity.makeIntent(trackerId!!, this@TrackerListFragment.act))
                } else if (view === removeButton) {
                    DialogHelper.makeNegativePhrasedYesNoDialogBuilder(this@TrackerListFragment.act, trackerViewModel.trackerName.value, getString(R.string.msg_confirm_remove_tracker), R.string.msg_remove,
                            onYes = { dialog ->
                        viewModel.removeTracker(trackerViewModel)
                        listView.invalidateItemDecorations()
                        //TODO logging tracker removal
                        //EventLoggingManager.logTrackerChangeEvent(EventLoggingManager.EVENT_NAME_CHANGE_TRACKER_REMOVE, tracker)
                    }).show()
                } else if (view === chartViewButton) {
                    startActivityOnDelay(ChartViewActivity.makeIntent(trackerId!!, this@TrackerListFragment.act))


                } else if (view === expandButton) {
                    if (collapsed) {

                        lastExpandedViewHolder?.collapse(true)

                        currentlyExpandedIndex = adapterPosition
                        lastExpandedViewHolder = this
                        expand(true)

                    } else {
                        currentlyExpandedIndex = -1
                        lastExpandedViewHolder = null
                        collapse(true)
                    }

                } else if (view === errorIndicator) {
                    if (validationErrorMessages.size > 0) {
                        TooltipHelper.makeTooltipBuilder(adapterPosition, errorIndicator)
                                .text(
                                        validationErrorMessages.joinToString("\n")
                                ).show()
                    }
                }
            }

            private fun setLastLoggingTime(timestamp: Long?) {
                println("Last logging time: ${timestamp}")
                if (timestamp != null) {
                    InterfaceHelper.setTextAppearance(lastLoggingTimeView, R.style.trackerListInformationTextViewStyle)
                    val dateText = TimeHelper.getDateText(timestamp, act).toUpperCase()
                    val timeText = lastLoggedTimeFormat.format(Date(timestamp)).toUpperCase()
                    putStatistics(lastLoggingTimeView, dateText, timeText)
                    todayLoggingCountView.visibility = View.VISIBLE
                    totalItemCountView.visibility = View.VISIBLE

                } else {
                    lastLoggingTimeView.text = act.resources.getString(R.string.msg_never_logged).toUpperCase()
                    InterfaceHelper.setTextAppearance(lastLoggingTimeView, R.style.trackerListInformationTextViewStyle_HeaderAppearance)
                    todayLoggingCountView.visibility = View.INVISIBLE
                    totalItemCountView.visibility = View.INVISIBLE
                }
            }

            private fun setTodayLoggingCount(count: Long) {
                val header = act.resources.getString(R.string.msg_todays_log).toUpperCase()
                putStatistics(todayLoggingCountView, header, count.toString())
            }

            private fun setTotalItemCount(count: Long) {
                val header = OTApp.instance.resourcesWrapped.getString(R.string.msg_tracker_list_stat_total).toUpperCase()
                putStatistics(totalItemCountView, header, count.toString())
            }

            fun bindViewModel(viewModel: TrackerListViewModel.TrackerInformationViewModel) {

                trackerId = viewModel.trackerDao.objectId

                subscriptions.clear()
                subscriptions.add(
                        viewModel.trackerName.subscribe {
                            nameText ->
                            name.text = nameText
                        }
                )

                subscriptions.add(
                        viewModel.trackerColor.subscribe {
                            colorInt ->
                            color.setBackgroundColor(colorInt)
                        }
                )

                subscriptions.add(
                        viewModel.isBookmarked.subscribe { isBookmarked ->
                            if (isBookmarked) {
                                itemView.ui_bookmark_indicator.visibility = View.VISIBLE
                            } else {
                                itemView.ui_bookmark_indicator.visibility = View.GONE
                            }
                        }
                )

                subscriptions.add(
                        viewModel.trackerEditable.subscribe {
                            editable ->
                            if (editable) {
                                lockedIndicator.visibility = View.GONE
                                editButton.visibility = View.VISIBLE
                                removeButton.visibility = View.VISIBLE
                            } else {
                                lockedIndicator.visibility = View.VISIBLE
                                editButton.visibility = View.GONE
                                removeButton.visibility = View.GONE
                            }
                        }
                )

                subscriptions.add(
                        viewModel.activeNotificationCount.subscribe {
                            count ->
                            if (count > 0) {
                                alarmIcon.visibility = View.VISIBLE
                                alarmText.visibility = View.VISIBLE
                                alarmText.text = count.toString()
                            } else {
                                alarmIcon.visibility = View.INVISIBLE
                                alarmText.visibility = View.INVISIBLE
                            }

                        }
                )

                subscriptions.add(viewModel.lastLoggingTimeObservable.observeOn(AndroidSchedulers.mainThread()).subscribe { (time) -> setLastLoggingTime(time) })
                subscriptions.add(viewModel.todayCount.observeOn(AndroidSchedulers.mainThread()).subscribe { count -> setTodayLoggingCount(count) })
                subscriptions.add(viewModel.totalItemCount.observeOn(AndroidSchedulers.mainThread()).subscribe { count -> setTotalItemCount(count) })

                validationErrorMessages.clear()

                subscriptions.add(
                        viewModel.validationResult.subscribe { (isValid, invalidateMessages) ->
                            this.validationErrorMessages.clear()
                            invalidateMessages?.let {
                                this.validationErrorMessages.addAll(it)
                            }

                            errorIndicator.visibility = if (isValid) {
                                View.INVISIBLE
                            } else {
                                View.VISIBLE
                            }
                        }
                )

                if (currentlyExpandedIndex == adapterPosition) {
                    lastExpandedViewHolder = this
                    expand(false)
                } else {
                    collapse(false)
                }
            }


            private fun putStatistics(view: TextView, header: CharSequence, content: CharSequence) {
                val builder = SpannableString("$header\n$content").apply {
                    setSpan(statHeaderSizeSpan, 0, header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(statHeaderColorSpan, 0, header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(statContentStyleSpan, header.length + 1, header.length + 1 + content.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                view.text = builder
            }

            fun collapse(animate: Boolean) {
                if (animate) {
                    collapseAnimator.start()
                    collapsed = true

                } else {
                    expandedView.visibility = View.GONE
                    expandButton.setImageResource(R.drawable.more_horiz_scarse)
                    val lp = itemView.layoutParams.apply { height = collapsedHeight }
                    itemView.layoutParams = lp
                    collapsed = true
                }
            }

            fun expand(animate: Boolean) {
                if (animate) {
                    expandAnimator.start()
                    collapsed = false
                } else {
                    expandedView.visibility = View.VISIBLE
                    expandButton.setImageResource(R.drawable.up_dark)
                    val lp = itemView.layoutParams.apply { height = expandedHeight }
                    itemView.layoutParams = lp
                    collapsed = false
                }
            }
        }
    }
}