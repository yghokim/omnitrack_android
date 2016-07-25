package kr.ac.snu.hcil.omnitrack.activities.fragments

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.activities.HomeActivity
import kr.ac.snu.hcil.omnitrack.activities.NewItemActivity
import kr.ac.snu.hcil.omnitrack.activities.TrackerDetailActivity
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.ui.HorizontalImageDividerItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper

/**
 * Created by Young-Ho Kim on 2016-07-18.
 */
class TrackerListFragment : Fragment() {

    private lateinit var user : OTUser

    lateinit private var listView : RecyclerView

    lateinit private var trackerListAdapter : TrackerListAdapter

    lateinit private var popupMessages : Array<String>
    companion object{
        const val CHANGE_TRACKER_SETTINGS = 0
        const val REMOVE_TRACKER = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = OmniTrackApplication.app.currentUser

        //attach events
        user.trackerAdded += onTrackerAddedHandler
        user.trackerRemoved += onTrackerRemovedHandler
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
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            //(application as OmniTrackApplication).syncUserToDb()
            //user.trackers.add(OTTracker("Hihi"))
            val intent = Intent(context, TrackerDetailActivity::class.java)
            startActivity(intent)
        }

        listView = rootView.findViewById(R.id.ui_tracker_list_view) as RecyclerView
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        listView.layoutManager = layoutManager
        trackerListAdapter = TrackerListAdapter(user)

        listView.adapter = trackerListAdapter

        popupMessages = arrayOf(getString(R.string.msg_change_tracker_settings), getString(R.string.msg_remove_tracker))
        listView.itemAnimator = SlideInRightAnimator()
        listView.addItemDecoration(HorizontalImageDividerItemDecoration(R.drawable.horizontal_separator_pattern, context, 1.5f))
        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        //dettach events
        user.trackerAdded -= onTrackerAddedHandler
        user.trackerRemoved -= onTrackerRemovedHandler
    }

    private val onTrackerAddedHandler = {
        sender: Any, args: Pair<OTTracker, Int>->
        println("tracker added - ${args.second}")
        trackerListAdapter.notifyItemInserted(args.second)
        listView.scrollToPosition(args.second)
    }

    private val onTrackerRemovedHandler = {
        sender: Any, args: Pair<OTTracker, Int>->
        println("tracker removed - ${args.second}")
        trackerListAdapter.notifyItemRemoved(args.second)
    }

    private fun handleTrackerClick(tracker: OTTracker)
    {
        val intent = Intent(context, NewItemActivity::class.java)
        intent.putExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
        startActivity(intent)
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
                    startActivity(intent)

                }
                REMOVE_TRACKER -> DialogHelper.makeYesNoDialogBuilder(context, tracker.name, getString(R.string.msg_confirm_remove_tracker), {->user.trackers.remove(tracker)}).show()
            }
        }
        builder.show()
    }


    inner class TrackerListAdapter(val user: OTUser) : RecyclerView.Adapter<TrackerListAdapter.ViewHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.tracker_list_element, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindTracker(user.trackers[position])
        }

        override fun getItemCount(): Int {
            return user.trackers.size
        }

        override fun getItemId(position: Int): Long {
            return position as Long;
        }


        inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view){
            lateinit var name: TextView

            init{
                name = view.findViewById(R.id.name) as TextView

                view.setOnClickListener {
                    handleTrackerClick(user.trackers[adapterPosition])
                }

                view.setOnLongClickListener {
                    view->
                    handleTrackerLongClick(user.trackers[adapterPosition])
                    true
                }
            }

            fun bindTracker(tracker: OTTracker){
                name.text = tracker.name
            }
        }
    }
}