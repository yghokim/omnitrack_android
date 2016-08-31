package kr.ac.snu.hcil.omnitrack.ui.components.common.wizard

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import me.relex.circleindicator.CircleIndicator

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
abstract class WizardView : FrameLayout, AWizardViewPagerAdapter.IWizardPageListener, ViewPager.OnPageChangeListener {

    interface IWizardListener {
        fun onComplete(wizard: WizardView)
        fun onCanceled(wizard: WizardView)
    }

    private val indicator: CircleIndicator
    private val titleView: TextView
    private val viewPager: ViewPager

    private lateinit var adapter: AWizardViewPagerAdapter


    private var listener: IWizardListener? = null

    fun setWizardListener(listener: IWizardListener) {
        this.listener = listener
    }


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.layout_wizard_view_parent, this, true)

        indicator = findViewById(R.id.ui_wizard_indicator) as CircleIndicator

        titleView = findViewById(R.id.ui_wizard_title) as TextView

        viewPager = findViewById(R.id.ui_wizard_pager) as ViewPager
        viewPager.addOnPageChangeListener(this)
        indicator.setViewPager(viewPager)

    }

    fun setAdapter(value: AWizardViewPagerAdapter) {
        this.adapter = value
        this.adapter.registerDataSetObserver(indicator.dataSetObserver)
        viewPager.adapter = value

        value.setListener(this)
        onPageSelected(0)
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        println("Wizard page $position selected")
        titleView.text = adapter.getPageTitle(position)
        onEnterPage(adapter.getPageAt(position), position)
    }

    override fun onGoNextRequested(currentPosition: Int, nextPosition: Int) {
        adapter.getPageAt(currentPosition).onLeave()
        onLeavePage(adapter.getPageAt(currentPosition), currentPosition)

        if (!adapter.canComplete() && nextPosition != -1) {

            adapter.getPageAt(nextPosition).onEnter()
            onEnterPage(adapter.getPageAt(nextPosition), nextPosition)

            viewPager.setCurrentItem(nextPosition, true)
        } else {
            if (adapter.canComplete())
                listener?.onComplete(this)
            else listener?.onCanceled(this)
        }
    }

    abstract fun onEnterPage(page: AWizardPage, position: Int)


    abstract fun onLeavePage(page: AWizardPage, position: Int)

    override fun onGoBackRequested(position: Int) {
        ;
    }

}