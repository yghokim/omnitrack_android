package kr.ac.snu.hcil.omnitrack.ui.pages

import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.res.ResourcesCompat
import io.reactivex.disposables.Disposable
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO

/**
 * Created by Young-Ho Kim on 2016-11-04.
 */
class ConnectionIndicatorStubProxy(val parent: View, stubId: Int) : View.OnAttachStateChangeListener {

    private var connectionAvailabilitySubscription: Disposable? = null

    private val connectionIndicatorStub: ViewStub
    private var connectionIndicator: View? = null
    private var connectionIndicatorView: View? = null
    private var connectionIndicatorLinkIconView: AppCompatImageView? = null
    private var connectionIndicatorSourceNameView: TextView? = null
    private var connectionIndicatorErrorMark: View? = null

    init {
        connectionIndicatorStub = parent.findViewById(stubId)
        parent.addOnAttachStateChangeListener(this)
    }

    fun setContainerVisibility(visibility: Int) {
        connectionIndicator?.visibility = visibility
    }

    fun setVisibility(visibility: Int) {
        connectionIndicatorView?.visibility = visibility
    }

    fun onBind(attribute: OTAttributeDAO, connection: OTConnection?) {
        println("Bind connection")
        if (connection != null) {
            val connectionSource = connection.source
            if (connectionIndicator == null) {
                println("inflate new connection stub")
                connectionIndicator = connectionIndicatorStub.inflate()
                connectionIndicatorView = connectionIndicator?.findViewById(R.id.ui_indicator_view)
                connectionIndicatorLinkIconView = connectionIndicator?.findViewById(R.id.ui_connection_link_icon)
                connectionIndicatorSourceNameView = connectionIndicator?.findViewById(R.id.ui_connection_source_name)
                connectionIndicatorErrorMark = connectionIndicator?.findViewById(R.id.ui_invalid_icon)
            } else {
                println("set visible to current connection indicator")
                setVisibility(View.VISIBLE)
            }

            /*
            connectionInvalidMessages?.clear()
            if (connection.isAvailableToRequestValue(attribute, connectionInvalidMessages)) {
                connectionIndicatorSourceNameView?.setTextColor(ResourcesCompat.getColor(parent.resources, R.color.colorPointed, null))
                connectionIndicatorErrorMark?.visibility = View.GONE
                connectionIndicatorLinkIconView?.setImageResource(R.drawable.link)
                if (connectionIndicatorErrorMark != null) {
                    TooltipCompat.setTooltipText(connectionIndicatorErrorMark!!, null)
                }
            } else {
                turnOnInvalidMode()
            }*/
            if (connectionAvailabilitySubscription?.isDisposed == false) {
                connectionAvailabilitySubscription?.dispose()
            }

            connectionAvailabilitySubscription = connection.makeAvailabilityCheckObservable(attribute).subscribe { (valid, invalidMessages) ->
                if (valid) {
                    connectionIndicatorSourceNameView?.setTextColor(ResourcesCompat.getColor(parent.resources, R.color.colorPointed, null))
                    connectionIndicatorErrorMark?.visibility = View.GONE
                    connectionIndicatorLinkIconView?.setImageResource(R.drawable.link)
                    if (connectionIndicatorErrorMark != null) {
                        TooltipCompat.setTooltipText(connectionIndicatorErrorMark!!, null)
                    }
                } else {
                    connectionIndicatorSourceNameView?.setTextColor(ResourcesCompat.getColor(parent.resources, R.color.colorRed_Light, null))
                    connectionIndicatorErrorMark?.visibility = View.VISIBLE
                    connectionIndicatorLinkIconView?.setImageResource(R.drawable.unlink_dark)

                    if (connectionIndicatorErrorMark != null)
                        TooltipCompat.setTooltipText(connectionIndicatorErrorMark!!, invalidMessages?.joinToString("\n"))
                }
            }

            if (connectionSource != null) {
                connectionIndicatorSourceNameView?.text = connectionSource.getFormattedName()
            } else {
                // Unsupported source
                connectionIndicatorSourceNameView?.setText(R.string.msg_unsupported_connection)
            }
        } else {
            println("hide the connection indicator view")
            setVisibility(View.GONE)
        }

    }

    override fun onViewDetachedFromWindow(v: View?) {
        if (connectionAvailabilitySubscription?.isDisposed == false) {
            connectionAvailabilitySubscription?.dispose()
        }
    }

    override fun onViewAttachedToWindow(v: View?) {
    }
}