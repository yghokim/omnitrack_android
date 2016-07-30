package kr.ac.snu.hcil.omnitrack.activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.activities.fragments.ServiceListFragment
import kr.ac.snu.hcil.omnitrack.activities.fragments.TrackerDetailStructureTabFragment
import kr.ac.snu.hcil.omnitrack.activities.fragments.TrackerDetailTriggerTabFragment
import kr.ac.snu.hcil.omnitrack.activities.fragments.TrackerListFragment
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTNumberAttribute
import kr.ac.snu.hcil.omnitrack.ui.DragItemTouchHelperCallback
import kr.ac.snu.hcil.omnitrack.ui.SpaceItemDecoration
import kr.ac.snu.hcil.omnitrack.ui.components.AttributeFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.AttributeTypeListDialogFragment
import kr.ac.snu.hcil.omnitrack.ui.components.ExpandableFrameLayout
import kr.ac.snu.hcil.omnitrack.ui.components.TriggerPanel
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ColorPalettePropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ShortTextPropertyView
import kr.ac.snu.hcil.omnitrack.utils.startActivityOnDelay
import java.util.*

class TrackerDetailActivity : MultiButtonActionBarActivity(R.layout.activity_tracker_detail) {

    /*
        interface IChild{
            fun init(tracker: OTTracker, editMode: Boolean)
            fun onClose()
        }

        private val childFragments = Hashtable<Int, IChild>()
    */
    private lateinit var tracker: OTTracker
    private var isEditMode = true

    private var mSectionsPagerAdapter: TrackerDetailActivity.SectionsPagerAdapter? = null

    /**
     * The [ViewPager] that will host the section contents.
     */
    private var mViewPager: ViewPager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container) as ViewPager?
        mViewPager!!.adapter = mSectionsPagerAdapter

        val tabLayout = findViewById(R.id.tabs) as TabLayout?
        tabLayout!!.setupWithViewPager(mViewPager)


        setActionBarButtonMode(Mode.Back)
        title = resources.getString(R.string.title_activity_tracker_edit)
    }


    override fun onPause(){
        super.onPause()
        /*
        for(child in childFragments)
        {
            child.value.onClose()
        }*/

        OmniTrackApplication.app.syncUserToDb()
    }

    override fun onStart(){
        super.onStart()

        println("tracker detail activity started")
        if (intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER) != null) {
            //edit
            //instant update
            tracker = OmniTrackApplication.app.currentUser.trackers.filter { it.objectId == intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER) }.first()
            isEditMode = true

        } else {
            tracker = OTTracker("Tracker ${System.currentTimeMillis()}")
            OmniTrackApplication.app.currentUser.trackers.add(tracker)
            isEditMode = false
        }
/*
        for(child in childFragments)
        {
            child.value.init(tracker, isEditMode)
        }*/
    }

    override fun onLeftButtonClicked() {

        finish()
    }

    override fun onRightButtonClicked() {
            //add
        /*
            if(namePropertyView.validate()) {
                //modify
                tracker.name = namePropertyView.value
                tracker.color = colorPropertyView.value

                if (!isEditMode) OmniTrackApplication.app.currentUser.trackers.add(tracker)
                finish()
            }*/
    }


    abstract class ChildFragment : Fragment() {
        protected var isEditMode = false
            private set
        protected lateinit var tracker: OTTracker
            private set

        override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val args = arguments
            val trackerObjectId = args.getString(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)

            isEditMode = args.getBoolean(TrackerDetailStructureTabFragment.IS_EDIT_MODE, true)
            if (trackerObjectId != null) {
                tracker = OmniTrackApplication.app.currentUser[trackerObjectId]!!
            }
        }
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        /*
        override fun instantiateItem(container: ViewGroup?, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as IChild
            childFragments[position] = fragment
            fragment.init(tracker, isEditMode)
            return fragment
        }

        override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
            childFragments.remove(position)
            super.destroyItem(container, position, `object`)
        }*/

        override fun getItem(position: Int): Fragment {

            val bundle = Bundle()
            bundle.putString(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            bundle.putBoolean(TrackerDetailStructureTabFragment.IS_EDIT_MODE, isEditMode)

            val fragment =
                    (when (position) {
                        0 -> TrackerDetailStructureTabFragment()
                        1 -> TrackerDetailTriggerTabFragment()
                        else -> TrackerDetailStructureTabFragment()
                    }) as Fragment

            fragment.arguments = bundle

            return fragment
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return resources.getString(R.string.msg_tab_tracker_detail_structure)
                1 -> return resources.getString(R.string.msg_tab_tracker_detail_triggers)
            }
            return null
        }
    }
}
