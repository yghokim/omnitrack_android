package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.view.Gravity
import android.view.View
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.SignInActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.diagnostics.SystemLogActivity
import rx.internal.util.SubscriptionList

class HomeActivity : MultiButtonActionBarActivity(R.layout.activity_home), DrawerLayout.DrawerListener {

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    /**
     * The [ViewPager] that will host the section contents.
     */
    private val mViewPager: ViewPager by bindView(R.id.container)

    private lateinit var drawerLayout: DrawerLayout

    private lateinit var sidebar: SidebarWrapper

    private val startSubscriptions = SubscriptionList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rightActionBarButton?.visibility = View.VISIBLE
        rightActionBarButton?.setImageResource(R.drawable.settings_dark)
        leftActionBarButton?.visibility = View.VISIBLE
        leftActionBarButton?.setImageResource(R.drawable.menu_dark)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager.adapter = mSectionsPagerAdapter

        val tabLayout = findViewById(R.id.tabs) as TabLayout?
        tabLayout!!.setupWithViewPager(mViewPager)

        //Setup sliding menu
        drawerLayout = findViewById(R.id.ui_drawer_layout) as DrawerLayout

        drawerLayout.addDrawerListener(this)

        sidebar = SidebarWrapper(findViewById(R.id.ui_sidebar), this)

        /*
        slidingMenu = SlidingMenu(this, SlidingMenu.SLIDING_WINDOW)
        slidingMenu.mode = SlidingMenu.LEFT
        slidingMenu.setFadeDegree(0.3f)
        slidingMenu.setFadeEnabled(true)
        slidingMenu.setBehindOffsetRes(R.dimen.home_sliding_menu_right_region)
*/
    }

    override fun onResume() {
        super.onResume()
        //AWSMobileClient.defaultMobileClient().identityManager.addSignInStateChangeListener(this)
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


    override fun onDrawerClosed(drawerView: View?) {
    }

    override fun onDrawerStateChanged(newState: Int) {
    }

    override fun onDrawerSlide(drawerView: View?, slideOffset: Float) {
    }

    override fun onDrawerOpened(drawerView: View?) {
    }

    override fun onStart() {
        super.onStart()
        startSubscriptions.add(
                super.signedInUserObservable.subscribe {
                    user ->
                    println("OMNITRACK: signed in user instance received.")
                    //Ask permission if needed
                    if (Build.VERSION.SDK_INT >= 23) {
                        val permissions = user.getPermissionsRequiredForFields().filter {
                            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                        }.toTypedArray()

                        if (permissions.isNotEmpty()) {
                            requestPermissions(permissions,
                                    10)
                        }
                    }

                    sidebar.refresh(user)
                    /*
                    val provider = AWSMobileClient.defaultMobileClient().identityManager.currentIdentityProvider
                    AWSMobileClient.defaultMobileClient().identityManager.loadUserInfoAndImage(
                            provider
                    ) {
                        user.name = provider.userName
                        user.photoUrl = provider.userImageUrl
                        sidebar.refresh(user)
                    }*/
                }
        )
    }

    override fun onStop() {
        super.onStop()
        startSubscriptions.clear()
    }

    override fun onPause() {
        super.onPause()
        //AWSMobileClient.defaultMobileClient().identityManager.removeSignInStateChangeListener(this)
        (application as OTApplication).syncUserToDb()
    }

    override fun onBackPressed() {
        if (mViewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            mViewPager.setCurrentItem(0, true)
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val requester = OTExternalService.requestCodeDict.getKeyFromId(requestCode)
        requester?.onActivityActivationResult(resultCode, data)
    }

/*
    override fun onUserSignedIn() {
    }

    override fun onUserSignedOut() {
        goSignInPage()
    }*/

    private fun goSignInPage() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}
