package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.calculation.AConditioner
import kr.ac.snu.hcil.omnitrack.core.calculation.SingleNumericComparison
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataTrigger
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ComboBoxPropertyView
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditionerviews.SingleNumericConditionerSettingView

/**
 * Created by younghokim on 16. 9. 5..
 */
class EventTriggerConfigurationPanel : FrameLayout, ITriggerConfigurationCoordinator {


    var selectedMeasureFactory: OTMeasureFactory?
        get() {
            return availableMeasures[measureSelectionView.value]
        }
        set(value) {
            val pos = if (value != null) availableMeasures.indexOf(value) else -1
            if (pos != -1) {
                measureSelectionView.value = pos
            }
        }

    var conditioner: AConditioner?
        get() {
            return conditionerView.conditioner
        }
        set(value) {
            if (value is SingleNumericComparison) {
                conditionerView.conditioner = value
            }
        }


    val availableMeasures: List<OTMeasureFactory>

    private val measureSelectionView: ComboBoxPropertyView

    private val conditionerView: SingleNumericConditionerSettingView

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        //TODO change to merge
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(R.layout.trigger_event_trigger_config_panel, this, false))


        availableMeasures = OTExternalService.getFilteredMeasureFactories {
            it.isDemandingUserInput == false
        }

        measureSelectionView = findViewById(R.id.ui_event_trigger_measure_selection) as ComboBoxPropertyView
        measureSelectionView.adapter = MeasureSpinnerAdapter()

        conditionerView = findViewById(R.id.ui_condition_setting) as SingleNumericConditionerSettingView

    }

    inner class MeasureSpinnerAdapter() : ArrayAdapter<OTMeasureFactory>(context, R.layout.simple_list_element_category_name, availableMeasures) {

        init {

        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view = getView(position, convertView, parent)
            view.setBackgroundResource(R.drawable.bottom_separator_thin)

            return view
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view = convertView ?:
                    LayoutInflater.from(parent.context).inflate(R.layout.simple_list_element_category_name, parent, false)

            if (view.tag !is MeasureViewHolder) {
                view.tag = MeasureViewHolder(view)
            }

            val holder = view.tag as MeasureViewHolder
            holder.bind(getItem(position))
            return view
        }
    }

    inner class MeasureViewHolder(val view: View) {

        private val titleView: TextView
        private val categoryView: TextView

        init {
            categoryView = view.findViewById(R.id.category) as TextView
            titleView = view.findViewById(R.id.title) as TextView
        }

        fun bind(factory: OTMeasureFactory) {
            categoryView.setText(factory.service.nameResourceId)
            titleView.setText(factory.nameResourceId)
        }
    }

    override fun applyConfigurationToTrigger(trigger: OTTrigger) {
        if (trigger is OTDataTrigger) {
            trigger.conditioner = conditioner
            trigger.measure = selectedMeasureFactory?.makeMeasure()
        }
    }

    override fun importTriggerConfiguration(trigger: OTTrigger) {
        if (trigger is OTDataTrigger) {
            conditioner = trigger.conditioner
            selectedMeasureFactory = trigger.measure?.factory
        }
    }

    override fun writeConfigurationToIntent(out: Intent) {

    }

    override fun validateConfigurations(errorMessagesOut: MutableList<String>): Boolean {
        return true
    }

    override fun writeConfiguratinoToBundle(out: Bundle) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun readConfigurationFromBundle(bundle: Bundle) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}