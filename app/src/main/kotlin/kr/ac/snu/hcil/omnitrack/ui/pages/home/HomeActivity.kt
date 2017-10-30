package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.tutorial.TutorialManager
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.diagnostics.SystemLogActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.services.ServiceListFragment
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService

class HomeActivity : MultiButtonActionBarActivity(R.layout.activity_home), DrawerLayout.DrawerListener {

    companion object {
        const val TAB_INDEX_TRACKERS = 0
        const val TAB_INDEX_TRIGGERS = 1
        const val TAB_INDEX_SERVICES = 2

    }

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    /**
     * The [ViewPager] that will host the section contents.
     */
    private val mViewPager: ViewPager by bindView(R.id.container)

    private val drawerLayout: DrawerLayout by bindView(R.id.ui_drawer_layout)

    private lateinit var sidebar: SidebarWrapper

    private val tabLayout: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rightActionBarButton?.visibility = View.VISIBLE
        rightActionBarButton?.setImageResource(R.drawable.settings_dark)
        leftActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.setImageResource(R.drawable.menu_dark)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        mViewPager.pageMargin = resources.getDimensionPixelSize(R.dimen.viewpager_page_margin)
        mViewPager.setPageMarginDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.darkerBackground)))
        // Set up the ViewPager with the sections adapter.
        mViewPager.adapter = mSectionsPagerAdapter

        tabLayout.setupWithViewPager(mViewPager)

        //Setup sliding menu
        drawerLayout.addDrawerListener(this)

        sidebar = SidebarWrapper(findViewById(R.id.ui_sidebar), this)
        sidebar.onCreate()

        creationSubscriptions.add(
                super.signedInUserObservable.subscribe {
                    user ->
                    val viewModel = ViewModelProviders.of(this).get(UserAttachedViewModel::class.java)
                    viewModel.userId = user
                    println("OMNITRACK: signed in user instance received.")
                    //Ask permission if needed
                    if (TutorialManager.hasShownTutorials(TutorialManager.FLAG_TRACKER_LIST_ADD_TRACKER)) {
                        val tabs = tabLayout.getChildAt(0) as ViewGroup
                        TutorialManager.checkAndShowSequence("home_main_tabs", true, this, false,
                                listOf(
                                        TutorialManager.TapTargetInfo(R.string.msg_tutorial_home_tab_trackers_primary,
                                                R.string.msg_tutorial_home_tab_trackers_secondary,
                                                ContextCompat.getColor(this, R.color.colorPointed),
                                                tabs.getChildAt(TAB_INDEX_TRACKERS), 50
                                        ),
                                        TutorialManager.TapTargetInfo(R.string.msg_tutorial_home_tab_triggers_primary,
                                                R.string.msg_tutorial_home_tab_triggers_secondary,
                                                ContextCompat.getColor(this, R.color.colorPointed),
                                                tabs.getChildAt(TAB_INDEX_TRIGGERS), 50
                                        ),
                                        TutorialManager.TapTargetInfo(R.string.msg_tutorial_home_tab_services_primary,
                                                R.string.msg_tutorial_home_tab_services_secondary,
                                                ContextCompat.getColor(this, R.color.colorPointed),
                                                tabs.getChildAt(TAB_INDEX_SERVICES), 50
                                        )
                                ))
                    }

                    /* TODO permission check in trackerlist fragment
                    val rxPermissions = RxPermissions(this)

                    val permissions = viewModel.getPermissionsRequiredForFields().filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

                    if (permissions.isNotEmpty()) {
                        DialogHelper.makeYesNoDialogBuilder(this, resources.getString(R.string.msg_permission_required),
                                String.format(resources.getString(R.string.msg_permission_request_of_tracker)),
                                cancelable = false,
                                onYes = {
                                    rxPermissions.request(*permissions.toTypedArray()).subscribe {
                                        granted ->
                                        if (granted)
                                            println("permissions granted.")
                                        else println("permissions not granted.")
                                    }
                                },
                                onCancel = null,
                                yesLabel = R.string.msg_allow_permission,
                                noLabel = R.string.msg_cancel
                        ).show()
                    }*/
                }
        )
    }

    override fun onToolbarLeftButtonClicked() {
        //       slidingMenu.toggle(true)
        if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            drawerLayout.closeDrawer(Gravity.LEFT)
        } else {
            drawerLayout.openDrawer(Gravity.LEFT)
        }
    }

    override fun onToolbarRightButtonClicked() {
        val intent = Intent(this, SystemLogActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        sidebar.onDestroy()
    }


    override fun onDrawerClosed(drawerView: View) {
    }

    override fun onDrawerStateChanged(newState: Int) {
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
    }

    override fun onDrawerOpened(drawerView: View) {
    }


    override fun onBackPressed() {
        if (mViewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            mViewPager.setCurrentItem(0, true)
        }
    }

    override fun onStop() {
        super.onStop()
        OTApp.instance.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(this))

    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            when (position) {
                0 -> return TrackerListFragment()
                1 -> return LoggingTriggerListFragment()
                2 -> return ServiceListFragment()
                else -> throw Exception("wrong tab index")
            }
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return resources.getString(R.string.msg_tab_trackers)
                1 -> return resources.getString(R.string.msg_tab_background_loggers)
                2 -> return resources.getString(R.string.msg_tab_services)
            }
            return null
        }
    }

    private fun goSignInPage() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}
