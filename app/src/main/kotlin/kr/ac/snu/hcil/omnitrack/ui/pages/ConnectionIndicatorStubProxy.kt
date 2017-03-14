package kr.ac.snu.hcil.omnitrack.ui.pages

import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.AppCompatImageView
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewStub
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.ui.components.common.TooltipHelper
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-11-04.
 */
class ConnectionIndicatorStubProxy(val parent: View, stubId: Int) : OnClickListener, View.OnAttachStateChangeListener {

    private val connectionIndicatorStub: ViewStub
    private var connectionIndicator: View? = null
    private var connectionIndicatorLinkIconView: AppCompatImageView? = null
    private var connectionIndicatorSourceNameView: TextView? = null
    private var connectionIndicatorErrorMark: View? = null
    private var connectionInvalidMessages: ArrayList<CharSequence>? = null

    init {
        connectionIndicatorStub = parent.findViewById(stubId) as ViewStub
        parent.addOnAttachStateChangeListener(this)
    }

    fun onBind(attribute: OTAttribute<out Any>) {
        val connectionSource = attribute.valueConnection?.source
        if (connectionSource != null) {
            if (connectionIndicator == null) {
                connectionIndicator = connectionIndicatorStub.inflate()
                connectionIndicatorLinkIconView = connectionIndicator?.findViewById(R.id.ui_connection_link_icon) as AppCompatImageView
                connectionIndicatorSourceNameView = connectionIndicator?.findViewById(R.id.ui_connection_source_name) as TextView
                connectionIndicatorErrorMark = connectionIndicator?.findViewById(R.id.ui_invalid_icon)
                connectionIndicatorErrorMark?.setOnClickListener(this)
            } else {
                connectionIndicator?.visibility = View.VISIBLE
            }

            connectionIndicatorSourceNameView?.text = connectionSource.factory.getFormattedName()
            if (connectionInvalidMessages == null) {
                connectionInvalidMessages = ArrayList<CharSequence>()
            }
            connectionInvalidMessages?.clear()
            if (attribute.isConnectionValid(connectionInvalidMessages)) {
                connectionIndicatorSourceNameView?.setTextColor(ResourcesCompat.getColor(parent.resources, R.color.colorPointed, null))
                connectionIndicatorErrorMark?.visibility = View.GONE
                connectionIndicatorLinkIconView?.setImageResource(R.drawable.link)
            } else {
                connectionIndicatorSourceNameView?.setTextColor(ResourcesCompat.getColor(parent.resources, R.color.colorRed_Light, null))
                connectionIndicatorErrorMark?.visibility = View.VISIBLE
                connectionIndicatorLinkIconView?.setImageResource(R.drawable.unlink_dark)
            }
        } else {
            connectionIndicator?.visibility = View.GONE
        }

    }


    override fun onClick(view: View?) {
        if (view === connectionIndicatorErrorMark && connectionIndicatorErrorMark != null) {
            if (connectionInvalidMessages?.size ?: 0 > 0) {
                TooltipHelper.makeTooltipBuilder(0, connectionIndicatorErrorMark!!)
                        .text(connectionInvalidMessages?.joinToString("\n") ?: "")
                        .show()
            }
        }
    }

    override fun onViewDetachedFromWindow(v: View?) {
        //tooltipView?.remove()
    }

    override fun onViewAttachedToWindow(v: View?) {

    }


}