package kr.ac.snu.hcil.omnitrack.ui.pages.export

import ItemSpacingDecoration
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_package_export.*
import kotlinx.android.synthetic.main.tracker_for_package_list_element.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.AttachedTrackerViewHolder
import kr.ac.snu.hcil.omnitrack.utils.events.IEventListener
import org.jetbrains.anko.backgroundColor

class PackageExportActivity : MultiButtonActionBarActivity(R.layout.activity_package_export) {

    companion object {
        const val TAG = "PackageExportActivity"
    }

    private lateinit var viewModel: PackageExportViewModel

    private val packageHandlingDialogFragment: PackageHandlingBottomSheetFragment by lazy {
        PackageHandlingBottomSheetFragment()
    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {
        creationSubscriptions.add(
                viewModel.extractPackage().subscribe { jsonString ->
                    packageHandlingDialogFragment.showJsonDialog(jsonString, supportFragmentManager, TAG)
                }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setActionBarButtonMode(Mode.OKCancel)
        rightActionBarTextButton?.visibility = View.VISIBLE
        rightActionBarButton?.visibility = View.GONE
        rightActionBarTextButton?.text = "Export"

        creationSubscriptions.add(
                super.signedInUserObservable.subscribe { user ->
                    viewModel = ViewModelProviders.of(this).get(PackageExportViewModel::class.java)
                    viewModel.userId = user
                }
        )

        tabs.setupWithViewPager(main_viewpager)
        main_viewpager.adapter = ViewPager(supportFragmentManager)
    }

    inner class ViewPager(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> resources.getString(R.string.msg_tab_trackers)
                1 -> resources.getString(R.string.msg_tab_background_loggers)
                else -> null
            }
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> TrackerSelectionFragment()
                1 -> TriggerSelectionFragment()
                else -> TrackerSelectionFragment()
            }
        }

        override fun getCount(): Int {
            return 2
        }

    }

    class TrackerSelectionFragment : EntitySelectionFragment<OTTrackerDAO.SimpleTrackerInfo, OTTriggerDAO.SimpleTriggerInfo>() {

        override fun getEntityListObservable(viewModel: PackageExportViewModel): Observable<List<OTTrackerDAO.SimpleTrackerInfo>> {
            return viewModel.trackerInfoListObservable
        }

        override fun getSelectedIdsObservable(viewModel: PackageExportViewModel): Observable<List<String>> {
            return viewModel.trackerSelectionListObservable
        }

        override fun makeEntityViewHolder(parent: ViewGroup): ACheckableTrackingEntityViewHolder<OTTrackerDAO.SimpleTrackerInfo, OTTriggerDAO.SimpleTriggerInfo> {
            return TrackerViewHolder(creationSubscriptions, viewModel, parent)
        }

        override fun setEntityChecked(entity: OTTrackerDAO.SimpleTrackerInfo, viewModel: PackageExportViewModel, checked: Boolean, id: String) {
            viewModel.setTrackerIdChecked(checked, id)
            if (checked) {
                entity.reminders?.forEach {
                    viewModel.setReminderIdChecked(true, it.objectId)
                }
            }
        }

    }

    class TriggerSelectionFragment : EntitySelectionFragment<OTTriggerDAO.SimpleTriggerInfo, OTTrackerDAO.SimpleTrackerInfo>() {
        override fun getEntityListObservable(viewModel: PackageExportViewModel): Observable<List<OTTriggerDAO.SimpleTriggerInfo>> {
            return viewModel.loggingTriggerInfoListObservable
        }

        override fun getSelectedIdsObservable(viewModel: PackageExportViewModel): Observable<List<String>> {
            return viewModel.loggingTriggerSelectionListObservable
        }

        override fun makeEntityViewHolder(parent: ViewGroup): ACheckableTrackingEntityViewHolder<OTTriggerDAO.SimpleTriggerInfo, OTTrackerDAO.SimpleTrackerInfo> {
            return TriggerViewHolder(parent, true)
        }

        override fun setEntityChecked(entity: OTTriggerDAO.SimpleTriggerInfo, viewModel: PackageExportViewModel, checked: Boolean, id: String) {
            viewModel.setLoggingTriggerIdChecked(checked, id)
        }
    }

    class TrackerViewHolder(val subscriptionManager: CompositeDisposable, val viewModel: PackageExportViewModel, parent: ViewGroup) : ACheckableTrackingEntityViewHolder<OTTrackerDAO.SimpleTrackerInfo, OTTriggerDAO.SimpleTriggerInfo>(parent, true) {

        override fun getEntityChildren(entity: OTTrackerDAO.SimpleTrackerInfo): Array<OTTriggerDAO.SimpleTriggerInfo> {
            return entity.reminders ?: emptyArray()
        }

        override fun getChildrenAdapter(entity: OTTrackerDAO.SimpleTrackerInfo): RecyclerView.Adapter<out RecyclerView.ViewHolder> {
            return RemindersAdapter(subscriptionManager, viewModel, getEntityChildren(entity))
        }

        override val childrenHeaderNameRes: Int
            get() = R.string.msg_text_reminder

        override fun onBindEntity(entity: OTTrackerDAO.SimpleTrackerInfo) {
            itemView.color_bar.backgroundColor = entity.color
            title = entity.name
        }

        class RemindersAdapter(subscriptionManager: CompositeDisposable, val viewModel: PackageExportViewModel, val reminders: Array<OTTriggerDAO.SimpleTriggerInfo>) : RecyclerView.Adapter<TriggerViewHolder>(), IEventListener<Boolean> {

            init {
                subscriptionManager.add(
                        viewModel.reminderSelectionListObservable.subscribe { selectedReminders ->
                            notifyDataSetChanged()
                        }
                )
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TriggerViewHolder {
                return TriggerViewHolder(parent, false)
            }

            override fun getItemCount(): Int {
                return reminders.size
            }

            override fun onBindViewHolder(holder: TriggerViewHolder, position: Int) {
                holder.entity = reminders[position]

                holder.selectionChangedHandler = this
                holder.isSelected = viewModel.selectedReminderIds.contains(reminders[position].objectId)
            }

            override fun onEvent(sender: Any, args: Boolean) {
                viewModel.setReminderIdChecked(args, reminders[(sender as TriggerViewHolder).adapterPosition].objectId)
            }


        }
    }

    class TriggerViewHolder(parent: ViewGroup, useTrackerList: Boolean) : ACheckableTrackingEntityViewHolder<OTTriggerDAO.SimpleTriggerInfo, OTTrackerDAO.SimpleTrackerInfo>(parent, useTrackerList) {
        override fun getEntityChildren(entity: OTTriggerDAO.SimpleTriggerInfo): Array<OTTrackerDAO.SimpleTrackerInfo> {
            return entity.trackers ?: emptyArray()
        }

        override fun getChildrenAdapter(entity: OTTriggerDAO.SimpleTriggerInfo): RecyclerView.Adapter<out RecyclerView.ViewHolder> {
            if (useChildrenList) {
                return AttachedTrackerAdapter(getEntityChildren(entity))
            } else throw Exception("This method should not be called.")
        }

        override val childrenHeaderNameRes: Int
            get() = R.string.msg_text_tracker

        override fun onBindEntity(entity: OTTriggerDAO.SimpleTriggerInfo) {
            itemView.ui_name_checkbox.text = entity.condition?.makeInformationText()
        }

        init {

            itemView.ui_children_list.addItemDecoration(ItemSpacingDecoration.fromDIP(8, 10, parent.context))

            itemView.ui_children_list.layoutManager = FlexboxLayoutManager(parent.context).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
            }
            itemView.color_bar.visibility = View.GONE
        }

        class AttachedTrackerAdapter(val trackers: Array<OTTrackerDAO.SimpleTrackerInfo>) : RecyclerView.Adapter<AttachedTrackerViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachedTrackerViewHolder {
                return AttachedTrackerViewHolder(parent)
            }

            override fun getItemCount(): Int {
                return trackers.size
            }

            override fun onBindViewHolder(holder: AttachedTrackerViewHolder, position: Int) {
                holder.setColor(trackers[position].color)
                holder.setName(trackers[position].name)
            }
        }
    }
}