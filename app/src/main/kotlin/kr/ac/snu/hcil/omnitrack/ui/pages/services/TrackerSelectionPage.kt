package kr.ac.snu.hcil.omnitrack.ui.pages.services

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.android.common.view.wizard.AWizardPage
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.views.color.ColorHelper
import org.jetbrains.anko.padding
import java.util.*
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

    private val trackers = ArrayList<OTTrackerDAO>()
    private var trackerListView: RecyclerView? = null

    lateinit var selectedTrackerDAO: OTTrackerDAO

    @field:[Inject Backend]
    lateinit var realmProvider: Factory<Realm>

    @Inject
    protected lateinit var dbManager: Lazy<BackendDbManager>

    @Inject
    protected lateinit var authManager: OTAuthManager

    init {
        (parent.context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun onLeave() {
    }

    override fun onEnter() {
        val userId = authManager.userId!!
        val realm = realmProvider.get()
        trackers.clear()
        trackers.addAll(dbManager.get().makeTrackersOfUserVisibleQuery(userId, realm).findAll().filter { !it.isEditingLocked() })
        realm.close()
        trackerListView?.adapter?.notifyDataSetChanged()
    }

    private fun refreshTrackerList() {
        onEnter()
    }

    override fun makeViewInstance(context: Context): View {
        trackerListView = TrackerListView(context)
        return trackerListView!!
    }

    inner class TrackerListView : RecyclerView {

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

        init {
            padding = context.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
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

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position >= 1 || BuildConfig.DISABLE_TRACKER_CREATION == true) {
                (holder as TrackerListViewHolder).run {
                    holder.bind(trackers[position - indexShift])
                }
            }
        }

        override fun getItemCount(): Int = trackers.size + indexShift

        private val indexShift: Int
            get() {
                return if (BuildConfig.DISABLE_TRACKER_CREATION == true) 0 else 1
            }

        override fun getItemViewType(position: Int): Int = if (position == 0) {
            if (BuildConfig.DISABLE_TRACKER_CREATION != true) 0 else 1
        } else 1

    }

    private inner class TrackerAttachViewHolder(parent: ViewGroup?) :
            RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.layout_attached_tracker_list_add, parent, false)), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            val trackerDefaultName = parent.context.getString(R.string.msg_new_tracker_prefix)
            newTrackerNameDialog.input(null, trackerDefaultName, false) {
                _, text ->
                addNewTracker(text.toString())
                refreshTrackerList()
            }.show()
        }
    }

    private inner class TrackerListViewHolder(parent: ViewGroup?) :
            RecyclerView.ViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.simple_colored_circle_and_text, parent, false)), View.OnClickListener {

        var trackerDao: OTTrackerDAO? = null
        val circle: AppCompatImageView = itemView.findViewById(R.id.colored_circle)
        val textView: TextView = itemView.findViewById(R.id.text)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            selectedTrackerDAO = trackerDao!!
            requestGoNextPage(ServiceWizardView.PAGE_ATTRIBUTE_SELECTION)
        }

        fun bind(trackerDao: OTTrackerDAO) {
            this.trackerDao = trackerDao
            circle.setColorFilter(trackerDao.color)
            textView.text = trackerDao.name
        }
    }

    private val newTrackerNameDialog: MaterialDialog.Builder by lazy {
        MaterialDialog.Builder(parent.context!!)
                .title(R.string.msg_new_tracker_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .setSyncWithKeyboard(true)
                .inputRangeRes(1, 20, R.color.colorRed)
                .cancelable(true)
                .negativeText(R.string.msg_cancel)
    }

    private fun addNewTracker(name: String) {
        val realm = realmProvider.get()
        realm.executeTransaction {
            val trackerDao = realm.createObject(OTTrackerDAO::class.java, UUID.randomUUID().toString())
            trackerDao.userId = authManager.userId
            trackerDao.name = name
            trackerDao.isBookmarked = false
            trackerDao.color = ColorHelper.getTrackerColorPalette(parent.context)[0]
        }
    }

}