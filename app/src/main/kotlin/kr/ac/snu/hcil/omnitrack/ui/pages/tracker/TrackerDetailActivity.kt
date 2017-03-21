package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import at.markushi.ui.RevealColorView
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

class TrackerDetailActivity : MultiButtonActionBarActivity(R.layout.activity_tracker_detail) {

    companion object {
        const val IS_EDIT_MODE = "isEditMode"

        const val NEW_TRACKER_NAME = "newTrackerName"

        const val INTENT_KEY_FOCUS_ATTRIBUTE_ID = "focusAttributeId"

        fun makeIntent(trackerId: String, context: Context, new_mode: Boolean = false): Intent {
            val intent = Intent(context, TrackerDetailActivity::class.java)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            intent.putExtra(IS_EDIT_MODE, !new_mode)
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

    private var user: OTUser? = null
    private var tracker: OTTracker? = null

    val trackerSubject = BehaviorSubject.create<OTTracker>()

    val trackerColorOnUI = BehaviorSubject.create<Int>()

    override fun onSessionLogContent(contentObject: Bundle) {
        super.onSessionLogContent(contentObject)
        contentObject.putBoolean("isEditMode", isEditMode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)


        mViewPager.pageMargin = resources.getDimensionPixelSize(R.dimen.viewpager_page_margin)
        mViewPager.setPageMarginDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.darkerBackground)))
        // Set up the ViewPager with the sections adapter.
        mViewPager.adapter = mSectionsPagerAdapter

        val tabLayout = findViewById(R.id.tabs) as TabLayout?
        tabLayout!!.setupWithViewPager(mViewPager)

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)) {
                this.intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, savedInstanceState.getString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER))
            }

            this.intent.putExtra(IS_EDIT_MODE, savedInstanceState.getBoolean(IS_EDIT_MODE, true))
        }

        isEditMode = intent.getBooleanExtra(IS_EDIT_MODE, true)

        if (isEditMode) {
            setActionBarButtonMode(Mode.Back)
            title = resources.getString(R.string.title_activity_tracker_edit)
        } else {
            setActionBarButtonMode(Mode.SaveCancel)
            title = resources.getString(R.string.title_activity_tracker_new)
        }

    }

    private fun onTrackerLoaded(tracker: OTTracker) {
        transitionToColor(tracker.color, false)
    }

    private fun tossToHome() {

        val homeActivityIntent = Intent(this, HomeActivity::class.java)
        homeActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(homeActivityIntent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_EDIT_MODE, isEditMode)
        if (isEditMode) {
            outState.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker?.objectId)
        }
    }

    override fun onPause(){
        super.onPause()

        OTApplication.app.syncUserToDb()
    }

    override fun onStart(){
        super.onStart()
        val trackerObjectId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        signedInUserObservable.subscribe {
            user ->
            this.user = user
            val tracker = user[trackerObjectId]
            if (tracker != null) {
                this.tracker = tracker
                trackerSubject.onNext(tracker)
                onTrackerLoaded(tracker)
            } else {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        if (intent.hasExtra(INTENT_KEY_FOCUS_ATTRIBUTE_ID)) {
            //val attributeId = intent.getStringExtra(INTENT_KEY_FOCUS_ATTRIBUTE_ID)
            mViewPager.setCurrentItem(0, true)

        }
    }

    override fun onToolbarLeftButtonClicked() {
        if (!isEditMode) {
            if (tracker != null) {
                user?.trackers?.remove(tracker!!)
            }
        }

        setResult(Activity.RESULT_CANCELED)

        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onToolbarLeftButtonClicked()
    }

    override fun onToolbarRightButtonClicked() {
            //add
        if (!isEditMode) {

            setResult(RESULT_OK, Intent().putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker?.objectId))
            finish()
            /*
            signedInUserObservable.subscribe(){
                user->
                user.trackers.add(tracker!!)
                finish()
            }*/
        }
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

        protected val startSubscriptions = CompositeSubscription()

        override fun onStart() {
            super.onStart()
            val args = arguments

            isEditMode = args.getBoolean(IS_EDIT_MODE, true)
            val activity = activity
            if (activity is TrackerDetailActivity) {
                startSubscriptions.add(
                        activity.trackerSubject.subscribe {
                            tracker ->
                            onTrackerLoaded(tracker)
                        })
            }
        }

        protected abstract fun onTrackerLoaded(tracker: OTTracker);

        override fun onStop() {
            super.onStop()
            startSubscriptions.clear()
        }
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {

            val bundle = Bundle()
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
