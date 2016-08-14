package kr.ac.snu.hcil.omnitrack.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.AutoTransition
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.activities.ItemBrowserActivity
import kr.ac.snu.hcil.omnitrack.activities.NewItemActivity
import kr.ac.snu.hcil.omnitrack.activities.TrackerDetailActivity
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.ui.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.startActivityOnDelay

/**
 * Created by Young-Ho Kim on 2016-07-18.
 */
class TrackerListFragment : Fragment() {

    private lateinit var user : OTUser

    lateinit private var listView : RecyclerView

    lateinit private var trackerListAdapter : TrackerListAdapter

    lateinit private var trackerListLayoutManager: LinearLayoutManager

    lateinit private var popupMessages : Array<String>

    companion object{
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

        user = OmniTrackApplication.app.currentUser

        //attach events
        // user.trackerAdded += onTrackerAddedHandler
        //  user.trackerRemoved += onTrackerRemovedHandler
    }

    override fun onResume() {
        super.onResume()
        trackerListAdapter.notifyDataSetChanged()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home_trackers, container, false)

        val fab = rootView.findViewById(R.id.fab) as FloatingActionButton?
        fab!!.setOnClickListener { view ->
            val newTracker = OmniTrackApplication.app.currentUser.newTrackerWithDefaultName(context, true)

            val intent = Intent(context, TrackerDetailActivity::class.java)
            intent.putExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, newTracker.objectId)
            startActivityOnDelay(intent)
            Toast.makeText(context,
                    String.format(resources.getString(R.string.sentence_new_tracker_added), newTracker.name), Toast.LENGTH_LONG).show()

        }

        listView = rootView.findViewById(R.id.ui_tracker_list_view) as RecyclerView
        trackerListLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listView.layoutManager = trackerListLayoutManager
        trackerListAdapter = TrackerListAdapter(user)

        listView.adapter = trackerListAdapter

        popupMessages = arrayOf(getString(R.string.msg_change_tracker_settings), getString(R.string.msg_remove_tracker))
        //listView.itemAnimator = SlideInRightAnimator()
        listView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, context, 1.5f))
        return rootView
    }

    override fun onPause() {
        super.onPause()

        trackerListAdapter.currentlyExpandedIndex = -1
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

    private fun handleTrackerClick(tracker: OTTracker)
    {
        val intent = Intent(context, NewItemActivity::class.java)
        intent.putExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
        startActivityOnDelay(intent)
    }

    private fun handleTrackerLongClick(tracker: OTTracker)
    {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(tracker.name)
        builder.setItems(popupMessages){
            dialog, which ->
            when(which) {
                CHANGE_TRACKER_SETTINGS -> {
                    val intent = Intent(context, TrackerDetailActivity::class.java)
                    intent.putExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
                    startActivityOnDelay(intent)

                }
                REMOVE_TRACKER -> DialogHelper.makeYesNoDialogBuilder(context, tracker.name, getString(R.string.msg_confirm_remove_tracker), {->user.trackers.remove(tracker)}).show()
            }
        }
        builder.show()
    }


    inner class TrackerListAdapter(val user: OTUser) : RecyclerView.Adapter<TrackerListAdapter.ViewHolder>(){

        var currentlyExpandedIndex = -1

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


        inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view){
            lateinit var name: TextView
            lateinit var color: View
            lateinit var expandButton: ImageButton

            lateinit var expandedView: View

            lateinit var editButton: View
            lateinit var listButton: View
            lateinit var removeButton: View

            private var collapsed = true

            init{
                name = view.findViewById(R.id.name) as TextView
                color = view.findViewById(R.id.color_bar) as View
                expandButton = view.findViewById(R.id.ui_expand_button) as ImageButton

                expandedView = view.findViewById(R.id.ui_expanded_view)

                editButton = view.findViewById(R.id.ui_button_edit)
                listButton = view.findViewById(R.id.ui_button_list)
                removeButton = view.findViewById(R.id.ui_button_remove)

                view.setOnClickListener {
                    handleTrackerClick(user.trackers[adapterPosition])
                }

                editButton.setOnClickListener {
                    val intent = Intent(context, TrackerDetailActivity::class.java)
                    intent.putExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, user.trackers[adapterPosition].objectId)
                    startActivityOnDelay(intent)
                }

                listButton.setOnClickListener {
                    val intent = Intent(context, ItemBrowserActivity::class.java)
                    intent.putExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, user.trackers[adapterPosition].objectId)
                    startActivityOnDelay(intent)
                }

                removeButton.setOnClickListener {
                    val tracker = user.trackers[adapterPosition]
                    DialogHelper.makeYesNoDialogBuilder(context, tracker.name, getString(R.string.msg_confirm_remove_tracker), { -> user.trackers.remove(tracker); notifyItemRemoved(adapterPosition) }).show()
                }


                expandButton.setOnClickListener {
                    var toClose = -1
                    if (collapsed) {
                        if (currentlyExpandedIndex != -1) {
                            toClose = currentlyExpandedIndex
                            currentlyExpandedIndex = adapterPosition
                            //notifyItemChanged(close)
                        } else {
                            currentlyExpandedIndex = adapterPosition
                        }
                    } else {
                        currentlyExpandedIndex = -1
                    }

                    if (toClose >= 0 && (trackerListLayoutManager.findFirstVisibleItemPosition() > toClose || trackerListLayoutManager.findLastVisibleItemPosition() < toClose)) {
                        //item to close is outside the screen
                        println("$toClose is out of the screen.")
                    }

                    TransitionManager.beginDelayedTransition(listView, transition)
                    notifyDataSetChanged()
                }

                collapse()
            }

            fun bindTracker(tracker: OTTracker){

                name.text = tracker.name
                color.setBackgroundColor(tracker.color)

                if (currentlyExpandedIndex == adapterPosition) {
                    expand()
                } else {
                    collapse()
                }
            }

            fun collapse() {
                expandedView.visibility = View.GONE
                expandButton.setImageResource(R.drawable.down_dark)
                val lp = itemView.layoutParams
                lp.height = resources.getDimensionPixelSize(R.dimen.tracker_list_element_collapsed_height)
                itemView.layoutParams = lp
                collapsed = true
            }

            fun expand() {
                expandedView.visibility = View.VISIBLE
                expandButton.setImageResource(R.drawable.up_dark)
                val lp = itemView.layoutParams
                lp.height = resources.getDimensionPixelSize(R.dimen.tracker_list_element_expanded_height)
                itemView.layoutParams = lp
                collapsed = false
            }
        }
    }
}