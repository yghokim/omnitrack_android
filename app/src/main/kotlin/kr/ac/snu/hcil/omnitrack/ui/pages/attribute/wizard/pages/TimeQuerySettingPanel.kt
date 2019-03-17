package kr.ac.snu.hcil.omnitrack.ui.pages.attribute.wizard.pages

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import kr.ac.snu.hcil.android.common.events.IEventListener
import kr.ac.snu.hcil.android.common.view.inflateContent
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.views.properties.ComboBoxPropertyView
import kr.ac.snu.hcil.omnitrack.views.properties.NumericUpDownPropertyView
import kr.ac.snu.hcil.omnitrack.views.properties.SelectionPropertyView

/**
 * Created by younghokim on 16. 8. 31..
 */
class TimeQuerySettingPanel : LinearLayout, IEventListener<Int> {

    private val pivotTimeComboBox: ComboBoxPropertyView
    private val scopeView: SelectionPropertyView
    private val offsetView: NumericUpDownPropertyView

    private var suspendEvent: Boolean = false

    private val pivotEntryAdapter = ArrayAdapter<PivotEntry>(context, R.layout.simple_text_element, R.id.textView)

    var timeQuery: OTTimeRangeQuery = OTTimeRangeQuery()
        set(value) {
            if (field != value) {
                field = value
                suspendEvent = true



                suspendEvent = false
            }
        }


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        inflateContent(R.layout.connection_time_query_panel, true)

        pivotTimeComboBox = findViewById(R.id.ui_pivot_selection)
        scopeView = findViewById(R.id.ui_bin_size)
        offsetView = findViewById(R.id.ui_bin_offset)

        scopeView.setEntries(
                arrayOf(
                        resources.getString(R.string.msg_connection_wizard_time_query_scope_hour),
                        resources.getString(R.string.msg_connection_wizard_time_query_scope_day),
                        resources.getString(R.string.msg_connection_wizard_time_query_scope_week)
                )
        )

        pivotTimeComboBox.adapter = pivotEntryAdapter

        pivotTimeComboBox.valueChanged += this
        scopeView.valueChanged += this
        offsetView.valueChanged += this
    }

    fun init() {
        buildPivotEntries()
    }

    private fun buildPivotEntries() {
        pivotEntryAdapter.clear()
        pivotEntryAdapter.add(PivotEntry(R.string.msg_connection_wizard_time_query_pivot_present, OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP))

        /*
        val siblingAttributes = this.attribute?.tracker?.attributes
        if (siblingAttributes != null) {

            val fieldNameFormat = resources.getString(R.string.msg_connection_wizard_time_query_pivot_field_format)

            for (attribute in siblingAttributes) {
                if (attribute !== this.attribute) {
                    when (attribute.typeId) {
                        OTAttributeManager.TYPE_TIME -> {
                            pivotEntryAdapter.add(
                                    AttributePivotEntry(String.format(fieldNameFormat, attribute.name), OTTimeRangeQuery.TYPE_PIVOT_TIMEPOINT, attribute)
                            )
                        }
                        OTAttributeManager.TYPE_TIMESPAN -> {
                            pivotEntryAdapter.add(
                                    AttributePivotEntry(String.format(fieldNameFormat, attribute.name), OTTimeRangeQuery.TYPE_LINK_TIMESPAN, attribute)
                            )
                        }
                    }
                }
            }
        }*/
    }

    override fun onEvent(sender: Any, args: Int) {
        if (!suspendEvent) {
            if (sender === pivotTimeComboBox) {
                if (args == OTTimeRangeQuery.TYPE_LINK_TIMESPAN) {
                    TransitionManager.beginDelayedTransition(this)
                    scopeView.visibility = View.INVISIBLE
                    offsetView.visibility = View.INVISIBLE
                } else {

                    TransitionManager.beginDelayedTransition(this)
                    scopeView.visibility = View.VISIBLE
                    offsetView.visibility = View.VISIBLE
                }

                pivotEntryAdapter.getItem(pivotTimeComboBox.value).applyToQuery(timeQuery)
            } else if (sender === scopeView) {
                println("binSize value changed: $args")
                timeQuery.binSize = scopeView.value
            } else if (sender === offsetView) {
                println("binOffset value changed: $args")
                timeQuery.binOffset = offsetView.value
            }
        }
    }

    fun refreshQueryFromViewValues() {
        onEvent(pivotTimeComboBox, pivotTimeComboBox.value)
        onEvent(scopeView, scopeView.value)
        onEvent(offsetView, offsetView.value)
    }


    open inner class PivotEntry(val name: String, val mode: Int) {
        constructor(nameResId: Int, mode: Int) : this(resources.getString(nameResId), mode)

        open fun applyToQuery(timeQuery: OTTimeRangeQuery) {
            timeQuery.mode = mode
        }

        override fun toString(): String {
            return name
        }
    }

    inner class AttributePivotEntry(name: String, mode: Int, val attributeId: String) : PivotEntry(name, mode) {
        override fun applyToQuery(timeQuery: OTTimeRangeQuery) {
            super.applyToQuery(timeQuery)
            timeQuery.linkedAttributeId = attributeId
        }
    }
}