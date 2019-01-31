package kr.ac.snu.hcil.omnitrack.ui.components.common.wizard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import kr.ac.snu.hcil.omnitrack.R
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
abstract class WizardView : FrameLayout, AWizardViewPagerAdapter.IWizardPageListener, ViewPager.OnPageChangeListener, View.OnClickListener {

    interface IWizardListener {
        fun onComplete(wizard: WizardView)
        fun onCanceled(wizard: WizardView)
    }

    private val titleView: TextView
    private val viewPager: ViewPager

    private val cancelButton: View
    private val prevButton: View
    private val okButton: View

    private val pagePositionHistory: Stack<Int> = Stack()

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

        titleView = findViewById(R.id.ui_wizard_title)

        viewPager = findViewById(R.id.ui_wizard_pager)
        viewPager.addOnPageChangeListener(this)

        cancelButton = findViewById(R.id.ui_button_cancel)
        prevButton = findViewById(R.id.ui_button_previous)
        okButton = findViewById(R.id.ui_button_ok)

        cancelButton.setOnClickListener(this)
        prevButton.setOnClickListener(this)
        okButton.setOnClickListener(this)

    }

    fun setAdapter(value: AWizardViewPagerAdapter) {
        this.adapter = value
        viewPager.adapter = value
        value.setListener(this)
        onPageSelected(0)
    }

    override fun onClick(view: View) {
        if (view === cancelButton) {
            pagePositionHistory.clear()
            listener?.onCanceled(this)
        } else if (view === prevButton) {
            val currPagePosition = pagePositionHistory.pop()
            val prevPagePosition = pagePositionHistory.pop()
            val currPage = adapter.getPageAt(currPagePosition)
            currPage.onLeave()
            onLeavePage(currPage, currPagePosition)
            adapter.getPageAt(prevPagePosition)
            viewPager.setCurrentItem(prevPagePosition, true)
        } else if (view === okButton) {
            complete()
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        titleView.text = adapter.getPageTitle(position)
        onEnterPage(adapter.getPageAt(position), position)
        adapter.getPageAt(position).onEnter()
    }

    override fun onGoNextRequested(currentPosition: Int, nextPosition: Int) {
        adapter.getPageAt(currentPosition).onLeave()
        onLeavePage(adapter.getPageAt(currentPosition), currentPosition)

        if (nextPosition != -1) {
            viewPager.setCurrentItem(nextPosition, true)
        } else {
            listener?.onComplete(this)
        }
    }

    fun complete() {
        val currentPage = adapter.getPageAt(viewPager.currentItem)
        currentPage.onLeave()
        onLeavePage(currentPage, viewPager.currentItem)
        pagePositionHistory.clear()

        listener?.onComplete(this)
    }


    open fun onEnterPage(page: AWizardPage, position: Int) {
        pagePositionHistory.push(position)
        if (page.isCompleteButtonAvailable) {
            okButton.visibility = View.VISIBLE
        } else {
            okButton.visibility = View.INVISIBLE
        }

        if (page.canGoBack) {
            cancelButton.visibility = GONE
            prevButton.visibility = VISIBLE
        } else {
            cancelButton.visibility = VISIBLE
            prevButton.visibility = GONE
        }
    }


    abstract fun onLeavePage(page: AWizardPage, position: Int)

    override fun onGoBackRequested(position: Int) {
    }

}