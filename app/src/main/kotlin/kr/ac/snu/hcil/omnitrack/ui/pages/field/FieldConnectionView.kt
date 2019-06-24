package kr.ac.snu.hcil.omnitrack.ui.pages.field

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import butterknife.bindView
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.android.common.TextHelper
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.events.Event
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery

/**
 * Created by Young-Ho Kim on 16. 8. 11
 */
class FieldConnectionView : LinearLayout, View.OnClickListener {

    val onRemoveButtonClicked = Event<Any>()

    val onConnectionChanged = PublishSubject.create<Nullable<OTConnection>>()

    var connection: OTConnection? = null
        set(value) {
            if (field != value) {
                field = value
                onConnectionChanged.onNext(Nullable(value))
                refresh()
            }
        }

    private val sourceView: TextView by bindView(R.id.ui_source)
    private val queryViewGroup: View by bindView(R.id.ui_group_time_query)
    private val queryView: TextView by bindView(R.id.ui_query)
    private val removeButton: Button by bindView(R.id.ui_button_remove)

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        this.orientation = VERTICAL

        inflateContent(R.layout.component_attribute_connection, true)

        removeButton.setOnClickListener(this)
    }


    override fun onClick(p0: View?) {
        onRemoveButtonClicked.invoke(this, true)
    }

    fun refresh() {

        val source = connection?.source
        if (source == null) {
            sourceView.setText(R.string.msg_unsupported_connection)
        } else {
            sourceView.text = source.getFormattedName()
        }

        if (connection?.isRangedQueryAvailable == true) {
            queryViewGroup.visibility = View.VISIBLE
            val query = connection?.rangedQuery
            if (query != null) {
                val builder = StringBuilder()

                val matchedPreset = OTTimeRangeQuery.Preset.values().find { it.equals(query) }

                if (matchedPreset != null) {
                    builder.append("<b>${context.resources.getString(matchedPreset.nameResId)}</b>")
                } else {
                    if (!query.isBinAndOffsetAvailable) {
                        builder.append(query.getModeName(context))
                    } else {
                        builder.append("<b>${context.resources.getString(R.string.msg_connection_wizard_time_query_pivot_time)}</b> : ${query.getModeName(context)}<br>")
                        builder.append("<b>${context.resources.getString(R.string.msg_connection_wizard_time_query_scope)}</b> : ${query.getScopeName(context)}<br>")
                        builder.append("<b>${context.resources.getString(R.string.msg_connection_wizard_time_query_offset)}</b> : ${query.binOffset}")
                    }
                }

                queryView.text = TextHelper.fromHtml(builder.toString())

            }
        } else {
            queryViewGroup.visibility = View.GONE
        }
    }

}