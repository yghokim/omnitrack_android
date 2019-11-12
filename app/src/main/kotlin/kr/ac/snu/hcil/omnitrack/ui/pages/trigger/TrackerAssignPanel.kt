package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import dagger.internal.Factory
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import kotlinx.android.synthetic.main.layout_attached_tracker_list_element_removable.view.*
import kr.ac.snu.hcil.android.common.view.IReadonlyObjectId
import kr.ac.snu.hcil.android.common.view.InterfaceHelper
import kr.ac.snu.hcil.android.common.view.container.decoration.ItemSpacingDecoration
import kr.ac.snu.hcil.android.common.view.getActivity
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.di.Backend
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.TrackerPickerDialogBuilder
import java.util.*
import javax.inject.Inject

/**
 * Created by Young-Ho on 9/2/2016.
 */
class TrackerAssignPanel : RecyclerView {

    val trackerListChanged = BehaviorSubject.create<List<OTTrackerDAO.SimpleTrackerInfo>>()

    val trackers = ArrayList<OTTrackerDAO.SimpleTrackerInfo>()

    private var realm: Realm? = null

    @field:[Inject Backend]
    lateinit var realmProvider: Factory<Realm>

    @Inject
    lateinit var dbManager: BackendDbManager

    @Inject
    lateinit var authManager: OTAuthManager

    private val elementAdapter = AssignElementAdapter()

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    override fun onFinishInflate() {
        super.onFinishInflate()
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    private val subscriptions = CompositeDisposable()

    init {
        addItemDecoration(ItemSpacingDecoration.fromDIP(8, 10, context))
        layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
        }

        this.adapter = elementAdapter
    }

    fun init(trackers: List<OTTrackerDAO.SimpleTrackerInfo>) {
        val diffResults = DiffUtil.calculateDiff(IReadonlyObjectId.DiffUtilCallback(this.trackers, trackers))
        this.trackers.clear()
        this.trackers.addAll(trackers)
        diffResults.dispatchUpdatesTo(elementAdapter)
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        realm = realmProvider.get()
    }

    private fun notifyTrackerListChanged() {
        trackerListChanged.onNext(trackers.toList())
    }


    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) InterfaceHelper.ALPHA_ORIGINAL else InterfaceHelper.ALPHA_INACTIVE
    }


    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (isEnabled) {
            super.onInterceptTouchEvent(ev)
        } else {
            true
        }
    }

    private fun onAttachButtonClicked() {
        realm?.let {
            subscriptions.add(
                    dbManager.makeTrackersOfUserVisibleQuery(authManager.userId!!, it).findAllAsync()
                            .asFlowable().filter { it.isLoaded && it.isValid }.firstOrError().subscribe { snapshot ->
                                val dialog = TrackerPickerDialogBuilder(snapshot.map { it.getSimpleInfo() }).createDialog(getActivity()!!, trackers.mapNotNull { it._id }.toTypedArray()) { trackerId ->
                                    snapshot.find { it._id == trackerId }?.getSimpleInfo()?.let {
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        subscriptions.clear()
        realm?.close()
    }

    private inner class AssignElementAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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