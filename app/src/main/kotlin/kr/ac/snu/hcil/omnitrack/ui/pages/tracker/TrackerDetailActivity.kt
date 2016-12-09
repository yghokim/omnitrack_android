package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.View
import at.markushi.ui.RevealColorView
import butterknife.bindView
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.activities.OTTrackerAttachedActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity

class TrackerDetailActivity : OTTrackerAttachedActivity(R.layout.activity_tracker_detail) {

    companion object {
        const val IS_EDIT_MODE = "isEditMode"

        const val INTENT_KEY_FOCUS_ATTRIBUTE_ID = "focusAttributeId"

        fun makeIntent(trackerId: String, context: Context): Intent {
            val intent = Intent(context, TrackerDetailActivity::class.java)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            return intent
        }

        fun makeIntent(trackerId: String, focusAttribute: OTAttribute<out Any>, context: Context): Intent {
            val intent = makeIntent(trackerId, context)
            intent.putExtra(INTENT_KEY_FOCUS_ATTRIBUTE_ID, focusAttribute.objectId)
            return intent
        }
    }

    private var isEditMode = true

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    private val appBarRevealView: RevealColorView by bindView(R.id.ui_appbar_reveal)

    /**
     * The [ViewPager] that will host the section contents.
     */
    private val mViewPager: ViewPager by bindView(R.id.container)

    override fun onSessionLogContent(contentObject: JsonObject) {
        super.onSessionLogContent(contentObject)
        contentObject.addProperty("isEditMode", isEditMode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager.adapter = mSectionsPagerAdapter

        val tabLayout = findViewById(R.id.tabs) as TabLayout?
        tabLayout!!.setupWithViewPager(mViewPager)

        setActionBarButtonMode(Mode.Back)
        title = resources.getString(R.string.title_activity_tracker_edit)

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)) {
                this.intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, savedInstanceState.getString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER))
            }

            this.intent.putExtra(IS_EDIT_MODE, savedInstanceState.getBoolean(IS_EDIT_MODE, true))
        }

        isEditMode = intent.getBooleanExtra(IS_EDIT_MODE, true)
    }

    override fun onTrackerLoaded(tracker: OTTracker) {
        super.onTrackerLoaded(tracker)
        transitionToColor(tracker.color, false)
    }

    private fun tossToHome() {

        val homeActivityIntent = Intent(this, HomeActivity::class.java)
        homeActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(homeActivityIntent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker?.objectId)
        outState.putBoolean(IS_EDIT_MODE, true)
    }

    override fun onPause(){
        super.onPause()
        /*
        for(child in childFragments)
        {
            child.value.onClose()
        }*/

        OTApplication.app.syncUserToDb()
    }

    override fun onStart(){
        super.onStart()

        if (intent.hasExtra(INTENT_KEY_FOCUS_ATTRIBUTE_ID)) {
            //val attributeId = intent.getStringExtra(INTENT_KEY_FOCUS_ATTRIBUTE_ID)
            mViewPager.setCurrentItem(0, true)

        }
    }

    override fun onToolbarLeftButtonClicked() {

        finish()
    }

    override fun onToolbarRightButtonClicked() {
            //add
        /*
            if(namePropertyView.validate()) {
                //modify
                tracker.name = namePropertyView.value
                tracker.color = colorPropertyView.value

                if (!isEditMode) OTApplication.app.currentUser.trackers.add(tracker)
                finish()
            }*/
    }

    fun transitionToColor(color: Int, animate: Boolean = true) {
        val blendedColor = getDimmedHeaderColor(color)

        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = blendedColor
        }

        if (animate) {
            appBarRevealView.reveal(0, appBarRevealView.measuredHeight / 2, blendedColor, 0, 400, object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {

                }

                override fun onAnimationEnd(p0: Animator?) {
                }

                override fun onAnimationCancel(p0: Animator?) {
                }

                override fun onAnimationStart(p0: Animator?) {
                }

            }
            )
        } else {
            appBarRevealView.setBackgroundColor(blendedColor)
        }
    }


    abstract class ChildFragment : OTFragment() {
        protected var isEditMode = false
            private set
        protected lateinit var tracker: OTTracker
            private set

        override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val args = arguments
            val trackerObjectId = args.getString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)

            isEditMode = args.getBoolean(IS_EDIT_MODE, true)
            if (trackerObjectId != null) {
                tracker = OTApplication.app.currentUser[trackerObjectId]!!
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
            bundle.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker?.objectId)
            bundle.putBoolean(IS_EDIT_MODE, isEditMode)

            val fragment =
                    (when (position) {
                        0 -> TrackerDetailStructureTabFragment()
                        1 -> TrackerDetailReminderTabFragment()
                        else -> TrackerDetailReminderTabFragment()
                    })

            fragment.arguments = bundle

            return fragment
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return resources.getString(R.string.msg_tab_tracker_detail_structure)
                1 -> return resources.getString(R.string.msg_tab_tracker_detail_reminders)
            }
            return null
        }
    }
}
