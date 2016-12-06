package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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
import android.widget.Toast
import it.sephiroth.android.library.tooltip.Tooltip
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseHelper
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.components.common.FallbackRecyclerView
import kr.ac.snu.hcil.omnitrack.ui.components.common.TooltipHelper
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.DrawableListBottomSpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.decorations.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemBrowserActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemEditingActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.tracker.TrackerDetailActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.visualization.ChartViewActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.InterfaceHelper
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.startActivityOnDelay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-18.
 */
class TrackerListFragment : OTFragment() {

    private lateinit var user: OTUser

    lateinit private var listView: FallbackRecyclerView

    lateinit private var emptyMessageView: TextView

    lateinit private var trackerListAdapter: TrackerListAdapter

    lateinit private var trackerListLayoutManager: LinearLayoutManager

    private lateinit var lastLoggedTimeFormat: SimpleDateFormat

    private lateinit var dateSizeSpan: AbsoluteSizeSpan
    private lateinit var dateStyleSpan: StyleSpan
    private lateinit var dateColorSpan: ForegroundColorSpan

    private lateinit var timeStyleSpan: StyleSpan

    private val collapsedHeight = OTApplication.app.resources.getDimensionPixelSize(R.dimen.tracker_list_element_collapsed_height)
    private val expandedHeight = OTApplication.app.resources.getDimensionPixelSize(R.dimen.tracker_list_element_expanded_height)


    private val itemEventReceiver: BroadcastReceiver by lazy {
        ItemEventReceiver()
    }

    private val itemEventIntentFilter: IntentFilter by lazy {
        val filter = IntentFilter()
        filter.addAction(OTApplication.BROADCAST_ACTION_ITEM_ADDED)
        filter.addAction(OTApplication.BROADCAST_ACTION_ITEM_EDITED)
        filter.addAction(OTApplication.BROADCAST_ACTION_ITEM_REMOVED)

        filter
    }

    companion object {
        const val CHANGE_TRACKER_SETTINGS = 0
        const val REMOVE_TRACKER = 1

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = OTApplication.app.currentUser

        lastLoggedTimeFormat = SimpleDateFormat(context.resources.getString(R.string.msg_tracker_list_time_format))
        dateStyleSpan = StyleSpan(Typeface.NORMAL)
        timeStyleSpan = StyleSpan(Typeface.BOLD)
        dateSizeSpan = AbsoluteSizeSpan(context.resources.getDimensionPixelSize(R.dimen.tracker_list_element_information_text_headerSize))
        dateColorSpan = ForegroundColorSpan(ContextCompat.getColor(context, R.color.textColorLight))
        //attach events
        // user.trackerAdded += onTrackerAddedHandler
        //  user.trackerRemoved += onTrackerRemovedHandler
    }

    override fun onResume() {
        super.onResume()
        trackerListAdapter.notifyDataSetChanged()
        context.registerReceiver(itemEventReceiver, itemEventIntentFilter)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home_trackers, container, false)

        val fab = rootView.findViewById(R.id.fab) as FloatingActionButton?
        fab!!.setOnClickListener { view ->
            val newTracker = OTApplication.app.currentUser.newTrackerWithDefaultName(context, true)

            startActivityOnDelay(TrackerDetailActivity.makeIntent(newTracker.objectId, context))
            Toast.makeText(context,
                    String.format(resources.getString(R.string.sentence_new_tracker_added), newTracker.name), Toast.LENGTH_LONG).show()

        }

        listView = rootView.findViewById(R.id.ui_tracker_list_view) as FallbackRecyclerView
        emptyMessageView = rootView.findViewById(R.id.ui_empty_list_message) as TextView
        listView.emptyView = emptyMessageView
        trackerListLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listView.layoutManager = trackerListLayoutManager
        trackerListAdapter = TrackerListAdapter(user)

        listView.adapter = trackerListAdapter

        listView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, context, resources.getFraction(R.fraction.tracker_list_separator_height_ratio, 1, 1)))
        listView.addItemDecoration(DrawableListBottomSpaceItemDecoration(R.drawable.expanded_view_inner_shadow_top, resources.getDimensionPixelSize(R.dimen.tracker_list_bottom_space)))

        return rootView
    }

    override fun onPause() {
        super.onPause()

        trackerListAdapter.currentlyExpandedIndex = -1

        context.unregisterReceiver(itemEventReceiver)
    }

    /*

    private val onTrackerAddedHandler = {
        sender: Any, args: ReadOnlyPair<OTTracker, Int> ->
        println("tracker added - ${args.second}")
        trackerListAdapter.notifyItemInserted(args.second)
        listView.scrollToPosition(args.second)
    }

    private val onTrackerRemovedHandler = {
        sender: Any, args: ReadOnlyPair<OTTracker, Int> ->
        println("tracker removed - ${args.second}")
        trackerListAdapter.notifyItemRemoved(args.second)
    }*/

    private fun handleTrackerClick(tracker: OTTracker) {
        startActivityOnDelay(ItemEditingActivity.makeIntent(tracker.objectId, context))
    }

    private fun handleTrackerLongClick(tracker: OTTracker) {
        /*
        val builder = AlertDialog.Builder(context)
        builder.setTitle(tracker.name)
        builder.setItems(popupMessages){
            dialog, which ->
            when(which) {
                CHANGE_TRACKER_SETTINGS -> {
                    val intent = Intent(context, TrackerDetailActivity::class.java)
                    intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
                    startActivityOnDelay(intent)

                }
                REMOVE_TRACKER -> DialogHelper.makeYesNoDialogBuilder(context, tracker.name, getString(R.string.msg_confirm_remove_tracker), {->user.trackers.remove(tracker)}).show()
            }
        }
        builder.show()*/
    }


    inner class TrackerListAdapter(val user: OTUser) : RecyclerView.Adapter<TrackerListAdapter.ViewHolder>() {

        var currentlyExpandedIndex = -1
        private var lastExpandedViewHolder: ViewHolder? = null

        init {
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.tracker_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindTracker(user.trackers[position])
        }

        override fun getItemCount(): Int {
            return user.trackers.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong();
        }


        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

            val name: TextView
            val color: View
            val expandButton: ImageButton

            val lastLoggingTimeView: TextView
            val todayLoggingCountView: TextView

            val expandedView: View

            val editButton: View
            val listButton: View
            val removeButton: View
            val chartViewButton: View

            val errorIndicator: AppCompatImageButton

            private val validationErrorMessages = ArrayList<CharSequence>()

            var collapsed = true

            val expandedViewHeight: Int

            private var lastLoggingTimeRetrievalTask: DatabaseHelper.LastItemTimeRetrievalTask? = null
            private var todayLoggingCountTask: DatabaseHelper.LoggingCountOfDayRetrievalTask? = null

            init {
                name = view.findViewById(R.id.name) as TextView
                color = view.findViewById(R.id.color_bar) as View
                expandButton = view.findViewById(R.id.ui_expand_button) as ImageButton

                lastLoggingTimeView = view.findViewById(R.id.ui_last_logging_time) as TextView
                todayLoggingCountView = view.findViewById(R.id.ui_today_logging_count) as TextView

                expandedView = view.findViewById(R.id.ui_expanded_view)

                expandedView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                expandedViewHeight = expandedView.measuredHeight

                editButton = view.findViewById(R.id.ui_button_edit)
                listButton = view.findViewById(R.id.ui_button_list)
                removeButton = view.findViewById(R.id.ui_button_remove)
                chartViewButton = view.findViewById(R.id.ui_button_charts)

                errorIndicator = view.findViewById(R.id.ui_invalid_icon) as AppCompatImageButton

                view.setOnClickListener(this)
                editButton.setOnClickListener(this)
                listButton.setOnClickListener(this)
                removeButton.setOnClickListener(this)
                chartViewButton.setOnClickListener(this)

                expandButton.setOnClickListener(this)

                errorIndicator.setOnClickListener(this)

                collapse(false)
            }

            override fun onClick(view: View) {
                if (view === itemView) {
                    handleTrackerClick(user.trackers[adapterPosition])
                } else if (view === editButton) {
                    startActivityOnDelay(TrackerDetailActivity.makeIntent(user.trackers[adapterPosition].objectId, this@TrackerListFragment.context))
                } else if (view === listButton) {
                    startActivityOnDelay(ItemBrowserActivity.makeIntent(user.trackers[adapterPosition], this@TrackerListFragment.context))
                } else if (view === removeButton) {
                    val tracker = user.trackers[adapterPosition]
                    DialogHelper.makeYesNoDialogBuilder(context, tracker.name, getString(R.string.msg_confirm_remove_tracker), { -> user.trackers.remove(tracker); notifyItemRemoved(adapterPosition); listView.invalidateItemDecorations(); }).show()
                } else if (view === chartViewButton) {
                    val tracker = user.trackers[adapterPosition]
                    startActivityOnDelay(ChartViewActivity.makeIntent(tracker.objectId, this@TrackerListFragment.context))


                } else if (view === expandButton) {
                    var toClose = -1
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
                        val tooltipView = Tooltip.make(context, TooltipHelper.makeTooltipBuilder(adapterPosition, errorIndicator).build())

                        tooltipView.setText(
                                validationErrorMessages.joinToString("\n")
                        )
                        tooltipView.show()
                    }
                }
            }


            fun bindTracker(tracker: OTTracker) {

                name.text = tracker.name
                color.setBackgroundColor(tracker.color)

                validationErrorMessages.clear()
                errorIndicator.visibility = if (tracker.isValid(validationErrorMessages)) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }

                lastLoggingTimeRetrievalTask?.cancel(true)
                todayLoggingCountTask?.cancel(true)

                lastLoggingTimeRetrievalTask = OTApplication.app.dbHelper.getLastLoggingTimeAsync(tracker) {

                    timestamp ->
                    if (timestamp != null) {
                        InterfaceHelper.setTextAppearance(lastLoggingTimeView, R.style.trackerListInformationTextViewStyle)
                        val dateText = TimeHelper.getDateText(timestamp, context).toUpperCase()
                        val timeText = lastLoggedTimeFormat.format(Date(timestamp)).toUpperCase()
                        val builder = SpannableString(dateText + "\n" + timeText).apply {
                            setSpan(dateSizeSpan, 0, dateText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            setSpan(dateColorSpan, 0, dateText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            setSpan(timeStyleSpan, dateText.length + 1, dateText.length + 1 + timeText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }


                        todayLoggingCountView.visibility = View.VISIBLE

                        lastLoggingTimeView.text = builder
                    } else {
                        lastLoggingTimeView.text = context.resources.getString(R.string.msg_never_logged).toUpperCase()
                        InterfaceHelper.setTextAppearance(lastLoggingTimeView, R.style.trackerListInformationTextViewStyle_HeaderAppearance)
                        todayLoggingCountView.visibility = View.INVISIBLE
                    }
                    lastLoggingTimeRetrievalTask = null
                }


                todayLoggingCountTask = OTApplication.app.dbHelper.getLoggingCountOfDayAsync(tracker, System.currentTimeMillis()) {

                    count ->
                    val header = context.resources.getString(R.string.msg_todays_log).toUpperCase()
                    val builder = SpannableString(header + "\n" + count).apply {
                        setSpan(dateSizeSpan, 0, header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setSpan(dateColorSpan, 0, header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setSpan(timeStyleSpan, header.length + 1, header.length + 1 + count.toString().length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    todayLoggingCountView.text = builder
                }

                if (currentlyExpandedIndex == adapterPosition) {
                    expand(false)
                } else {
                    collapse(false)
                }
            }

            fun collapse(animate: Boolean) {
                if (animate) {
                    val animator = ValueAnimator.ofFloat(1f, 0f).apply {
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
                                expandButton.setImageResource(R.drawable.down_dark)
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

                    animator.start()
                    collapsed = true

                } else {
                    expandedView.visibility = View.GONE
                    expandButton.setImageResource(R.drawable.down_dark)
                    val lp = itemView.layoutParams.apply { height = collapsedHeight }
                    itemView.layoutParams = lp
                    collapsed = true
                }
            }

            fun expand(animate: Boolean) {
                if (animate) {
                    val animator = ValueAnimator.ofFloat(0f, 1f).apply {
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

                    animator.start()
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

    inner class ItemEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val trackerPosition = user.trackers.indexOf(user[intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)]!!)
            trackerListAdapter.notifyItemChanged(trackerPosition)
        }

    }
}