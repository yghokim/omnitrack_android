package kr.ac.snu.hcil.omnitrack.ui.components.common.wizard

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
abstract class AWizardViewPagerAdapter() : PagerAdapter() {
    interface IWizardPageListener {
        fun onGoNextRequested(currentPosition: Int, nextPosition: Int)
        fun onGoBackRequested(position: Int)
    }

    private var listener: IWizardPageListener? = null

    abstract fun getPageAt(position: Int): AWizardPage

    fun setListener(listener: IWizardPageListener) {
        this.listener = listener
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val page = getPageAt(position)

        page.goNextRequested += {
            sender, nextPosition ->
            listener?.onGoNextRequested(position, nextPosition)
        }

        page.goBackRequested += {
            sender, args ->
            listener?.onGoBackRequested(position)
        }


        val view = page.getView(container.context)
        container.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return obj === view
    }

    override fun getPageTitle(position: Int): CharSequence {
        return OTApplication.app.getString(this.getPageAt(position).getTitleResourceId)
    }
}