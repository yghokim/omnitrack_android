package kr.ac.snu.hcil.omnitrack.activities

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.activities.fragments.AttributeDetailBasicFragment
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import java.util.*

class AttributeDetailActivity : MultiButtonActionBarActivity(R.layout.activity_attribute_detail) {

    interface ChildFragment {
        var parent: AttributeDetailActivity?
        fun refresh()
    }

    private val childFragments = Hashtable<Int, ChildFragment>()

    var attribute: OTAttribute<out Any>? = null

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private var mViewPager: ViewPager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarButtonMode(Mode.Back)

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager, this)

        mViewPager = findViewById(R.id.container) as ViewPager?
        mViewPager!!.adapter = mSectionsPagerAdapter

        val tabLayout = findViewById(R.id.tabs) as TabLayout?
        tabLayout!!.setupWithViewPager(mViewPager)
    }

    override fun onStart() {
        super.onStart()
        if (intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE) != null) {
            attribute = OmniTrackApplication.app.currentUser.findAttributeByObjectId(intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_ATTRIBUTE))
            for (child in childFragments) {
                child.value.refresh()
            }
        }
    }

    override fun onLeftButtonClicked() {
        finish()
    }

    override fun onRightButtonClicked() {
    }

    class PlaceholderFragment : Fragment(), ChildFragment {
        override var parent: AttributeDetailActivity? = null

        override fun refresh() {
        }

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater!!.inflate(R.layout.fragment_attribute_detail_connection, container, false)
            val textView = rootView.findViewById(R.id.section_label) as TextView
            textView.text = getString(R.string.section_format, arguments.getInt(ARG_SECTION_NUMBER))
            return rootView
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }


    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager, val parent: AttributeDetailActivity) : FragmentPagerAdapter(fm) {

        override fun instantiateItem(container: ViewGroup?, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as ChildFragment
            fragment.parent = parent
            childFragments[position] = fragment
            return fragment
        }

        override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
            childFragments.remove(position)
            super.destroyItem(container, position, `object`)
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> AttributeDetailBasicFragment()
                1 -> PlaceholderFragment.newInstance(position + 1)
                else -> PlaceholderFragment.newInstance(position + 1)
            }
        }


        override fun getCount(): Int {
            // Show 3 total pages.
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return resources.getString(R.string.msg_tab_attribute_basic)
                1 -> return resources.getString(R.string.msg_tab_attribute_connection)
            }
            return null
        }
    }
}
