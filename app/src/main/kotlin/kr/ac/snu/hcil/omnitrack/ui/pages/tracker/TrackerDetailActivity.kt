package kr.ac.snu.hcil.omnitrack.ui.pages.tracker

import android.animation.Animator
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import at.markushi.ui.RevealColorView
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import kr.ac.snu.hcil.omnitrack.utils.applyTint

class TrackerDetailActivity : MultiButtonActionBarActivity(R.layout.activity_tracker_detail) {

    companion object {

        const val INTENT_KEY_FOCUS_ATTRIBUTE_ID = "focusAttributeId"
        const val INTENT_KEY_NEW_TRACKER_PRESET_NAME = "newTrackerName"

        const val TAB_INDEX_STRUCTURE = 0
        const val TAB_INDEX_REMINDERS = 1

        fun makeIntent(trackerId: String?, context: Context): Intent {
            val intent = Intent(context, TrackerDetailActivity::class.java)
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            return intent
        }

        fun makeNewTrackerIntent(presetName: String?, context: Context): Intent {
            return Intent(context, TrackerDetailActivity::class.java)
                    .apply {
                        this.putExtra(INTENT_KEY_NEW_TRACKER_PRESET_NAME, presetName)
                    }
        }

        fun makeIntent(trackerId: String, focusAttribute: OTAttribute<out Any>, context: Context): Intent {
            val intent = makeIntent(trackerId, context)
            intent.putExtra(INTENT_KEY_FOCUS_ATTRIBUTE_ID, focusAttribute.objectId)
            return intent
        }
    }

    private var isEditMode = true

    private lateinit var mSectionsPagerAdapter: SectionsPagerAdapter

    private val appBarRevealView: RevealColorView by bindView(R.id.ui_appbar_reveal)

    /**
     * The [ViewPager] that will host the section contents.
     */
    private val mViewPager: ViewPager by bindView(R.id.container)

    private val tabLayout: TabLayout by bindView(R.id.tabs)

    private lateinit var viewModel: TrackerDetailViewModel

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

        tabLayout.setupWithViewPager(mViewPager)
        val numPages = mSectionsPagerAdapter.count
        val inflater = LayoutInflater.from(this)
        for (i in 0..numPages - 1) {
            val tabView = inflater.inflate(R.layout.layout_tracker_detail_tab_view, tabLayout, false)
            tabLayout.getTabAt(i)?.customView = tabView

            val viewHolder = TabViewHolder(tabView)
            viewHolder.iconView.setImageResource(mSectionsPagerAdapter.getIconId(i))
            viewHolder.textView.text = mSectionsPagerAdapter.getPageTitle(i)
            tabView.tag = viewHolder
            if (tabLayout.getTabAt(i)?.isSelected == false) {
                tabView.alpha = 0.5f
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.customView?.alpha = 0.5f
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.customView?.alpha = 1f
            }

        })

        this.viewModel = ViewModelProviders.of(this).get(TrackerDetailViewModel::class.java)

        isEditMode = intent.hasExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)

        setActionBarButtonMode(Mode.SaveCancel)
        if (isEditMode) {
            val trackerId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)
            this.viewModel.init(trackerId)
            //setActionBarButtonMode(Mode.Back)
            title = resources.getString(R.string.title_activity_tracker_edit)
        } else {
            this.viewModel.init(null)
            if (intent.hasExtra(INTENT_KEY_NEW_TRACKER_PRESET_NAME)) {
                this.viewModel.name = intent.getStringExtra(INTENT_KEY_NEW_TRACKER_PRESET_NAME)
            }

            title = resources.getString(R.string.title_activity_tracker_new)
        }

        val trackerObjectId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)


        /*
        creationSubscriptions.add(
                signedInUserObservable.doOnNext { user -> this.user = user }
                        .flatMap {
                            user ->
                            user.getTrackerObservable(trackerObjectId)
                        }.first().toSingle().timeout(2, TimeUnit.SECONDS).subscribe({
                    tracker ->
                    this.tracker = tracker
                    trackerSubject.onNext(tracker)
                    onTrackerLoaded(tracker)
                }, {
                    ex ->
                    ex.printStackTrace()
                    println("Warning: tracker does not exists. wait.")
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                })
        )

        if (intent.hasExtra(INTENT_KEY_FOCUS_ATTRIBUTE_ID)) {
            //val attributeId = intent.getStringExtra(INTENT_KEY_FOCUS_ATTRIBUTE_ID)
            mViewPager.setCurrentItem(0, true)

        }*/

    }
/*
    private fun onTrackerLoaded(tracker: OTTracker) {
        transitionToColor(tracker.color, false)
        refreshReminderCount()

        creationSubscriptions.add(
                tracker.reminderAdded.subscribe {
                    refreshReminderCount()
                }
        )

        creationSubscriptions.add(
                tracker.reminderRemoved.subscribe {
                    refreshReminderCount()
                }
        )
    }*/

    private fun refreshReminderCount() {
        //TODO reminder count
        /*
        tracker?.let {
            tracker ->
            val reminders = tracker.owner?.triggerManager?.getAttachedTriggers(tracker, OTTrigger.ACTION_NOTIFICATION)
            (tabLayout.getTabAt(TAB_INDEX_REMINDERS)?.customView?.tag as? TabViewHolder)?.setValue(mSectionsPagerAdapter.getPageTitle(TAB_INDEX_REMINDERS) ?: "Reminders", reminders?.size ?: 0)
        }*/
    }

    private fun tossToHome() {

        val homeActivityIntent = Intent(this, HomeActivity::class.java)
        homeActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(homeActivityIntent)
    }

    override fun onToolbarLeftButtonClicked() {
        if (viewModel.isDirty) {
            DialogHelper.makeYesNoDialogBuilder(this, "OmniTrack", resources.getString(R.string.msg_confirm_tracker_apply_change), R.string.msg_apply, onYes = {
                onToolbarRightButtonClicked()
            }, onNo = {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            ).cancelable(true)
                    .neutralText(R.string.msg_cancel)
                    .show()
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onBackPressed() {
        onToolbarLeftButtonClicked()
    }

    override fun onToolbarRightButtonClicked() {
        //add
        if (viewModel.isDirty) {
            val newTrackerId = viewModel.applyChanges()
            setResult(RESULT_OK, Intent().putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, newTrackerId))
        }
        finish()
        /*
            if(namePropertyView.validate()) {
                //modify
                tracker.name = namePropertyView.value
                tracker.color = colorPropertyView.value

                if (!isEditMode) OTApp.instance.currentUser.trackers.add(tracker)
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

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {

            return (when (position) {
                0 -> TrackerDetailStructureTabFragment()
                1 -> ReminderListFragment()
                else -> ReminderListFragment()
            })
        }

        override fun getCount(): Int {
            return 2
        }

        fun getIconId(position: Int): Int {
            return when (position) {
                TAB_INDEX_STRUCTURE -> R.drawable.icon_structure
                TAB_INDEX_REMINDERS -> R.drawable.alarm_dark
                else -> R.drawable.icon_structure
            }
        }

        fun getIcon(position: Int): Drawable {
            return applyTint(ContextCompat.getDrawable(this@TrackerDetailActivity, when (position) {
                TAB_INDEX_STRUCTURE -> R.drawable.icon_structure
                TAB_INDEX_REMINDERS -> R.drawable.alarm_dark
                else -> R.drawable.icon_structure
            })!!, Color.WHITE)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                TAB_INDEX_STRUCTURE -> return resources.getString(R.string.msg_tab_tracker_detail_structure)
                TAB_INDEX_REMINDERS -> return resources.getString(R.string.msg_tab_tracker_detail_reminders)
            }
            return null
        }
    }

    class TabViewHolder(val view: View) {

        val iconView: ImageView = view.findViewById(R.id.icon)
        val textView: TextView = view.findViewById(R.id.text)

        fun setValue(text: CharSequence, count: Int? = null) {
            textView.text = if (count != null) {
                "$text ($count)"
            } else text
        }
    }
}
