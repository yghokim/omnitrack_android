package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import kotlinx.android.synthetic.main.layout_attached_tracker_list_element_removable.view.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.TrackerPickerDialogBuilder
import kr.ac.snu.hcil.omnitrack.utils.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.utils.dipRound
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import java.util.*

/**
 * Created by Young-Ho on 9/2/2016.
 */
class TrackerAssignPanel : RecyclerView {

    val trackerListChanged = BehaviorSubject.create<List<OTTrackerDAO.SimpleTrackerInfo>>()

    val trackers = ArrayList<OTTrackerDAO.SimpleTrackerInfo>()

    private var realm: Realm? = null

    private val elementAdapter = AssignElementAdapter()

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet)

    private val subscriptions = CompositeDisposable()

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onChanged(position: Int, count: Int, payload: Any?) {
            notifyTrackerListChanged()
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {

        }

        override fun onInserted(position: Int, count: Int) {
        }

        override fun onRemoved(position: Int, count: Int) {
        }

    }

    init {
        addItemDecoration(SpacingItemDecoration(dipRound(8), dipRound(10)))
        layoutManager = ChipsLayoutManager.newBuilder(context)
                .setChildGravity(Gravity.CENTER_VERTICAL)
                .setOrientation(ChipsLayoutManager.HORIZONTAL)
                .build()
        this.adapter = elementAdapter
    }

    fun init(trackers: List<OTTrackerDAO.SimpleTrackerInfo>) {
        val diffResults = DiffUtil.calculateDiff(IReadonlyObjectId.DiffUtilCallback(this.trackers, trackers))
        this.trackers.clear()
        this.trackers.addAll(trackers)
        diffResults.dispatchUpdatesTo(elementAdapter)
        //diffResults.dispatchUpdatesTo(listUpdateCallback)
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        realm = OTApp.instance.databaseManager.getRealmInstance()
    }

    private fun notifyTrackerListChanged() {
        trackerListChanged.onNext(trackers.toList())
    }

    private fun onAttachButtonClicked() {
        realm?.let {
            subscriptions.add(
                    OTApp.instance.databaseManager.makeTrackersOfUserQuery(OTAuthManager.userId!!, it).findAllAsync()
                            .asFlowable().filter { it.isLoaded && it.isValid }.firstOrError().subscribe { snapshot ->
                        val dialog = TrackerPickerDialogBuilder(snapshot.map { it.getSimpleInfo() }).createDialog(getActivity()!!, trackers.mapNotNull { it.objectId }.toTypedArray()) { trackerId ->
                            snapshot.find { it.objectId == trackerId }?.getSimpleInfo()?.let {
                                trackers.add(it)
                                elementAdapter.notifyItemInserted(trackers.size - 1)
                                notifyTrackerListChanged()
                            }
                        }
                        dialog.show()
                    }
            )
        }
    }

    /*
    override fun onClick(view: View) {
        val fm = this.getActivity()?.supportFragmentManager
        if (fm != null) {
            /*
            subscriptions.add(

                    OTApplication.instance.currentUserObservable.subscribe {
                        user ->
                        val dialog = TrackerPickerDialogBuilder(user.trackers.unObservedList).createDialog(getActivity()!!, trackerIds.toTypedArray(), {
                            tracker ->
                            if (tracker != null) {
                                trackerIds += tracker.objectId
                                refresh()
                            }
                        })
                        dialog.show()
                    }
            )*/
        }
    }*/

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        subscriptions.clear()
        realm?.close()
    }

    private inner class AssignElementAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
            return when (viewType) {
                0 -> RemovableAttachedTrackerViewHolder(parent)
                1 -> AttachButtonViewHolder(parent)
                else -> throw IllegalArgumentException()
            }
        }

        override fun getItemViewType(position: Int): Int {
            if (position < trackers.size) {
                return 0
            } else return 1
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            if (position < trackers.size) {
                (holder as RemovableAttachedTrackerViewHolder).run {
                    val info = trackers[position]
                    this.setColor(info.color)
                    this.setName(info.name)
                }
            }
        }

        override fun getItemCount(): Int {
            return trackers.size + 1
        }

    }

    private inner class AttachButtonViewHolder(parent: ViewGroup?) : RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.layout_attached_tracker_list_add, parent, false)) {
        init {
            itemView.setOnClickListener {
                onAttachButtonClicked()
            }
        }
    }

    private inner class RemovableAttachedTrackerViewHolder(viewParent: ViewGroup?) : AttachedTrackerViewHolder(viewParent, R.layout.layout_attached_tracker_list_element_removable), View.OnClickListener {

        init {
            itemView.ui_button_remove.setOnClickListener(this)
        }

        override fun onClick(clickedView: View) {
            if (clickedView === itemView.ui_button_remove) {
                trackers.removeAt(adapterPosition)
                elementAdapter.notifyItemRemoved(adapterPosition)
                notifyTrackerListChanged()
            }
        }

    }
}