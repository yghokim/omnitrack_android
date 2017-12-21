package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dagger.Lazy
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.ui.components.common.wizard.AWizardPage
import org.jetbrains.anko.padding
import javax.inject.Inject

/**
 * Created by junhoe on 2017. 10. 30..
 */
class TrackerSelectionPage(override val parent : ServiceWizardView) : AWizardPage(parent) {

    companion object {
        const val TAG = "TrackerSelectionPage"
    }
    override val canGoBack: Boolean = false
    override val canGoNext: Boolean = true
    override val getTitleResourceId: Int = R.string.msg_service_wizard_title_tracker_selection

    private val trackers: List<OTTrackerDAO.SimpleTrackerInfo>

    var selectedTrackerId: String? = null

    @Inject
    protected lateinit var realm: Realm

    @Inject
    protected lateinit var dbManager: Lazy<RealmDatabaseManager>

    @Inject
    protected lateinit var authManager: OTAuthManager

    init {
        val component = (parent.context.applicationContext as OTApp).applicationComponent
        component.inject(this)
    }

    init {
        val userId = authManager.userId!!
        trackers = dbManager.get().makeTrackersOfUserQuery(userId, realm).findAll().map {
            it.getSimpleInfo()
        }
    }

    override fun onLeave() {
    }

    override fun onEnter() {
    }

    override fun makeViewInstance(context: Context): View {
        return TrackerListWizardPanel(context)
    }

    inner class TrackerListWizardPanel : RecyclerView {

        constructor(context: Context?) : super(context)
        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

        init {
            padding = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = TrackerListAdapter()
        }

    }

    inner class TrackerListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                0 -> TrackerAttachViewHolder(parent)
                1 -> TrackerListViewHolder(parent)
                else -> throw IllegalArgumentException()
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            if (position >= 1) {
                (holder as TrackerListViewHolder).run {
                    holder.bind(trackers[position - 1])
                }
            }
        }

        override fun getItemCount(): Int = trackers.size + 1

        override fun getItemViewType(position: Int): Int = if (position == 0) 0 else 1

    }

    private inner class TrackerAttachViewHolder(parent: ViewGroup?) :
            RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.layout_attached_tracker_list_add, parent, false)), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
        }
    }

    private inner class TrackerListViewHolder(parent: ViewGroup?) :
            RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.simple_colored_circle_and_text, parent, false)), View.OnClickListener {

        var trackerId: String? = null
        val circle: AppCompatImageView = itemView.findViewById(R.id.colored_circle)
        val textView: TextView = itemView.findViewById(R.id.text)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            selectedTrackerId = trackerId
            requestGoNextPage(ServiceWizardView.PAGE_FIELD_SELECTION)
        }

        fun bind(trackerInfo: OTTrackerDAO.SimpleTrackerInfo) {
            trackerId = trackerInfo.objectId
            circle.setColorFilter(trackerInfo.color)
            textView.text = trackerInfo.name
        }
    }

}