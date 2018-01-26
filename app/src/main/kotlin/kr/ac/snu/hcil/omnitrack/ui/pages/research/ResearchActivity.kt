package kr.ac.snu.hcil.omnitrack.ui.pages.research

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import kotlinx.android.synthetic.main.activity_research.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity

/**
 * Created by younghokim on 2018. 1. 3..
 */
class ResearchActivity : MultiButtonActionBarActivity(R.layout.activity_research) {

    companion object {
        const val TAB_INDEX_EXPERIMENTS = 0
        const val TAB_INDEX_INVITATIONS = 1
    }

    private lateinit var viewModel: ResearchViewModel

    private lateinit var pagerAdapter: SectionPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHeaderColor(ContextCompat.getColor(this, R.color.colorPointed), false)
        setActionBarButtonMode(Mode.Back)
        pagerAdapter = SectionPagerAdapter(supportFragmentManager)

        main_viewpager.adapter = pagerAdapter
        tabs.setupWithViewPager(main_viewpager)

        this.viewModel = ViewModelProviders.of(this).get(ResearchViewModel::class.java)
        creationSubscriptions.add(
                signedInUserObservable.subscribe { userId ->
                    this.viewModel.initialize(userId)
                }
        )

        creationSubscriptions.add(
                Observable.combineLatest<Boolean, Boolean, Boolean>(
                        this.viewModel.experimentLoadingStatus,
                        this.viewModel.invitationLoadingStatus,
                        BiFunction { loading1: Boolean, loading2: Boolean -> loading1 || loading2 })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            if (it == true)
                                this.ui_loading_indicator.show()
                            else this.ui_loading_indicator.dismiss()
                        }
        )
    }

    override fun onResume() {
        super.onResume()
        this.resumeSubscriptions.add(
                signedInUserObservable.subscribe {
                    this.viewModel.startWatchingNetworkForRefresh()
                }
        )
    }

    override fun onPause() {
        super.onPause()
        this.viewModel.stopWatchingNetworkForRefresh()
    }

    override fun onToolbarLeftButtonClicked() {
        finish()
    }

    override fun onToolbarRightButtonClicked() {

    }

    inner class SectionPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                TAB_INDEX_EXPERIMENTS -> ExperimentListFragment()
                TAB_INDEX_INVITATIONS -> InvitationListFragment()
                else -> ExperimentListFragment()
            }
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                TAB_INDEX_EXPERIMENTS -> resources.getString(R.string.msg_tab_experiments)
                TAB_INDEX_INVITATIONS -> resources.getString(R.string.msg_tab_invitations)
                else -> throw IllegalArgumentException("Unsupported index")
            }
        }

    }
}