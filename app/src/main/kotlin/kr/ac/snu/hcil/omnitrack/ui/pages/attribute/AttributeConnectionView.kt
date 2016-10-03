package kr.ac.snu.hcil.omnitrack.ui.pages.attribute

import android.content.Context
import android.text.Html
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by younghokim on 16. 8. 11..
 */
class AttributeConnectionView : LinearLayout, View.OnClickListener {

    val onRemoveButtonClicked = Event<Any>()

    var connection: OTConnection? = null
        set(value) {
            if (field != value) {
                field = value
                refresh()
            }
        }

    private lateinit var sourceView: TextView
    private lateinit var queryViewGroup: View
    private lateinit var queryView: TextView


    private lateinit var removeButton: Button

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        this.orientation = VERTICAL

        inflateContent(R.layout.component_attribute_connection, true)

        sourceView = findViewById(R.id.ui_source) as TextView
        queryView = findViewById(R.id.ui_query) as TextView

        queryViewGroup = findViewById(R.id.ui_group_time_query)

        removeButton = findViewById(R.id.ui_button_remove) as Button
        removeButton.setOnClickListener(this)

    }


    override fun onClick(p0: View?) {
        onRemoveButtonClicked.invoke(this, true)
    }

    fun refresh() {

        val source = connection?.source
        if (source == null) {
            sourceView.text = "No source"
        } else {
            sourceView.text = source.factory.getFormattedName()
        }

        if (connection?.isRangedQueryAvailable ?: false) {
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

                queryView.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    Html.fromHtml(builder.toString(), Html.FROM_HTML_MODE_LEGACY)
                } else {
                    Html.fromHtml(builder.toString())
                }

            }
        } else {
            queryViewGroup.visibility = View.GONE
        }
    }

}