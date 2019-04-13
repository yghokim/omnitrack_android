package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import butterknife.bindView
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.layout_home_sidebar.*
import kr.ac.snu.hcil.android.common.net.NetworkNotConnectedException
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.components.tutorial.TutorialManager
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.diagnostics.SystemLogActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.services.ServiceListFragment
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService
import javax.inject.Inject

class HomeActivity : MultiButtonActionBarActivity(R.layout.activity_home), DrawerLayout.DrawerListener {

    companion object {

        const val INTENT_EXTRA_INITIAL_LOGIN = "${BuildConfig.APPLICATION_ID}.extra.initial_login"
    }

    /**
     * The [ViewPager] that will host the section contents.
     */
    private val mViewPager: ViewPager by bindView(R.id.main_viewpager)

    private val drawerLayout: DrawerLayout by bindView(R.id.ui_drawer_layout)

    private lateinit var sidebar: SidebarWrapper

    private val tabLayout: TabLayout by bindView(R.id.tabs)

    private lateinit var viewModel: HomeScreenViewModel

    private val homeTabInfos: Array<HomeTabInfo>

    @Inject
    protected lateinit var tutorialManager: TutorialManager

    override fun onInject(app: OTAndroidApp) {
        super.onInject(app)
        app.applicationComponent.inject(this)
    }

    init {
        val homeTabs = HomeTabInfo.values().toMutableList()
        if (BuildConfig.HIDE_SERVICES_TAB) {
            homeTabs.remove(HomeTabInfo.TAB_SERVICES)
        }
        if (BuildConfig.HIDE_TRIGGERS_TAB) {
            homeTabs.remove(HomeTabInfo.TAB_TRIGGERS)
        }
        homeTabInfos = homeTabs.toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rightActionBarButton?.visibility = View.VISIBLE
        rightActionBarButton?.setImageResource(R.drawable.icon_reorder_dark)
        leftActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.setImageResource(R.drawable.menu_dark)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        if (BuildConfig.HIDE_TRIGGERS_TAB && BuildConfig.HIDE_SERVICES_TAB) {
            //only a tracker tab exists. hide tab bar.
            tabLayout.visibility = View.GONE
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                if (homeTabInfos[tab.position] != HomeTabInfo.TAB_TRACKERS) {
                    if (BuildConfig.DEBUG) {
                        rightActionBarButton?.setImageResource(R.drawable.settings_dark)
                    } else {
                        rightActionBarButton?.visibility = View.INVISIBLE
                    }
                } else
                    rightActionBarButton?.setImageResource(R.drawable.icon_reorder_dark)
                    rightActionBarButton?.visibility = View.VISIBLE
            }

        })

        mViewPager.pageMargin = resources.getDimensionPixelSize(R.dimen.viewpager_page_margin)
        mViewPager.setPageMarginDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.darkerBackground)))
        // Set up the ViewPager with the sections adapter.
        mViewPager.adapter = SectionsPagerAdapter(supportFragmentManager)

        tabLayout.setupWithViewPager(mViewPager)

        //Setup sliding menu
        drawerLayout.addDrawerListener(this)

        sidebar = SidebarWrapper(ui_sidebar, this)
        sidebar.onCreate()

        creationSubscriptions.add(
                super.signedInUserObservable.subscribe {
                    user ->
                    viewModel = ViewModelProviders.of(this).get(HomeScreenViewModel::class.java)
                    viewModel.userId = user
                    //Ask permission if needed
                    if (BuildConfig.SHOW_TUTORIALS && tutorialManager.hasShownTutorials(TutorialManager.FLAG_TRACKER_LIST_ADD_TRACKER)) {
                        val tabs = tabLayout.getChildAt(0) as ViewGroup
                        tutorialManager.checkAndShowSequence("home_main_tabs", true, this, false,
                                homeTabInfos.mapIndexed { index, info ->
                                    when (info) {
                                        HomeTabInfo.TAB_TRACKERS -> TutorialManager.TapTargetInfo(R.string.msg_tutorial_home_tab_trackers_primary,
                                                R.string.msg_tutorial_home_tab_trackers_secondary,
                                                ContextCompat.getColor(this, R.color.colorPointed),
                                                tabs.getChildAt(index), 50)
                                        HomeTabInfo.TAB_TRIGGERS -> TutorialManager.TapTargetInfo(R.string.msg_tutorial_home_tab_triggers_primary,
                                                R.string.msg_tutorial_home_tab_triggers_secondary,
                                                ContextCompat.getColor(this, R.color.colorPointed),
                                                tabs.getChildAt(index), 50)
                                        HomeTabInfo.TAB_SERVICES -> TutorialManager.TapTargetInfo(R.string.msg_tutorial_home_tab_services_primary,
                                                R.string.msg_tutorial_home_tab_services_secondary,
                                                ContextCompat.getColor(this, R.color.colorPointed),
                                                tabs.getChildAt(index), 50)
                                    }
                                })
                    }

                    if(intent.getBooleanExtra(INTENT_EXTRA_INITIAL_LOGIN, false))
                    {
                        creationSubscriptions.add(
                                serverConnectionChecker.get().subscribe({
                                    viewModel.startPullSync()
                                    if (BuildConfig.DEFAULT_EXPERIMENT_ID.isNullOrBlank()) {
                                        viewModel.syncResearch()
                                    }
                                }, { ex ->
                                    if (ex is NetworkNotConnectedException) {
                                        Toast.makeText(this, "Server does not response.", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        )
                    }
                }
        )
    }

    override fun onToolbarLeftButtonClicked() {
        //       slidingMenu.toggle(true)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onToolbarRightButtonClicked() {
        if (homeTabInfos[tabLayout.selectedTabPosition] == HomeTabInfo.TAB_TRACKERS) {
            val intent = Intent(this, TrackerReorderActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.anim_slide_up, R.anim.anim_noop)
        } else if (BuildConfig.DEBUG) {
            startActivity(Intent(this, SystemLogActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sidebar.onDestroy()
    }


    override fun onDrawerClosed(drawerView: View) {
        sidebar.onHideSidebar()
    }

    override fun onDrawerStateChanged(newState: Int) {
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
    }

    override fun onDrawerOpened(drawerView: View) {
        sidebar.onShowSidebar()
    }


    override fun onBackPressed() {
        if (mViewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            mViewPager.setCurrentItem(0, true)
        }
    }

    override fun onStart() {
        super.onStart()

        if (drawerLayout.isDrawerOpen(ui_sidebar)) {
            sidebar.onShowSidebar()
        }
    }

    override fun onStop() {
        super.onStop()
        startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(this))
    }


    enum class HomeTabInfo(@StringRes val labelRes: Int, val fragmentFunc: () -> Fragment) {
        TAB_TRACKERS(R.string.msg_tab_trackers, { TrackerListFragment() }),
        TAB_TRIGGERS(R.string.msg_tab_background_loggers, { LoggingTriggerListFragment() }),
        TAB_SERVICES(R.string.msg_tab_services, { ServiceListFragment() }),

    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return homeTabInfos[position].fragmentFunc()
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return homeTabInfos.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return resources.getString(homeTabInfos[position].labelRes)
        }
    }

    private fun goSignInPage() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}
