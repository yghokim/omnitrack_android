package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.data

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.TextView
import io.reactivex.Observable
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.externals.OTUnSupportedDummyService
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.ConditionConfigurationPanelImpl
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.IConditionConfigurationView
import kr.ac.snu.hcil.omnitrack.views.properties.ComboBoxPropertyView
import javax.inject.Inject

/**
 * Created by younghokim on 16. 9. 5..
 */
class DataDrivenTriggerConfigurationPanel : FrameLayout, IConditionConfigurationView {

    abstract class AServiceFactoryData {
        abstract val typeCode: String
        abstract fun makeMeasure(): OTMeasureFactory.OTMeasure
        abstract fun getCategoryName(context: Context): CharSequence
        abstract fun getName(context: Context): CharSequence

    }

    class AvailableServiceFactoryData(val serviceMeasureFactory: OTServiceMeasureFactory) : AServiceFactoryData() {
        override fun getCategoryName(context: Context): CharSequence {
            return serviceMeasureFactory.getCategoryName()
        }

        override fun getName(context: Context): CharSequence {
            return context.getString(serviceMeasureFactory.nameResourceId)
        }

        override val typeCode: String = serviceMeasureFactory.typeCode

        override fun makeMeasure(): OTMeasureFactory.OTMeasure {
            return serviceMeasureFactory.makeAttachable()
        }
    }

    class UnSupportedServiceFactoryData(val dummyMeasure: OTUnSupportedDummyService.DummyMeasure) : AServiceFactoryData() {
        override fun getName(context: Context): CharSequence {
            return "Unsupported Service"
        }

        override fun getCategoryName(context: Context): CharSequence {
            return dummyMeasure.originalFactoryCode
        }

        override val typeCode: String = dummyMeasure.originalFactoryCode

        override fun makeMeasure(): OTMeasureFactory.OTMeasure {
            return dummyMeasure
        }

    }

    @Inject
    lateinit var externalServiceManager: OTExternalServiceManager

    val selectedMeasureFactory: AServiceFactoryData?
        get() {
            return measureSelectionView.adapter?.getItem(measureSelectionView.value) as AServiceFactoryData
        }

    val availableMeasures: List<AServiceFactoryData>

    private val measureSelectionView: ComboBoxPropertyView

    private val comparisonSettingView: DataDrivenComparisonSettingView

    private val impl = ConditionConfigurationPanelImpl(OTDataDrivenTriggerCondition::class.java)

    override val onConditionChanged: Observable<ATriggerCondition>
        get() = impl.onConditionChanged

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)

        //TODO change to merge
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(R.layout.trigger_data_driven_trigger_config_panel, this, false))


        availableMeasures = externalServiceManager.getFilteredMeasureFactories {
            !it.isDemandingUserInput && TypeStringSerializationHelper.isNumeric(it.dataTypeName)
        }.map { AvailableServiceFactoryData(it) }

        measureSelectionView = findViewById(R.id.ui_event_trigger_measure_selection)
        measureSelectionView.adapter = MeasureSpinnerAdapter(availableMeasures)

        measureSelectionView.valueChanged += { sender, selectedFactoryIndex ->
            val factory = selectedMeasureFactory
            impl.currentCondition?.measure = factory?.makeMeasure()
            impl.notifyConditionChanged()
        }

        comparisonSettingView = findViewById(R.id.ui_condition_setting)

        comparisonSettingView.onComparisonChanged += { sender, newComparison ->
            impl.currentCondition?.comparison = newComparison
            impl.notifyConditionChanged()
        }

        comparisonSettingView.onThresholdChanged += { sender, newThreshold ->
            impl.currentCondition?.threshold = newThreshold
            impl.notifyConditionChanged()
        }
    }

    override fun applyCondition(condition: ATriggerCondition) {
        var shouldNotifyChanges = false
        impl.applyConditionAndGetChanged(condition) { newCondition ->
            val newConditionMeasure = newCondition.measure
            if (newConditionMeasure == null) {
                impl.currentCondition?.measure = selectedMeasureFactory?.makeMeasure()
                (condition as OTDataDrivenTriggerCondition).measure = impl.currentCondition?.measure
                shouldNotifyChanges = true
            } else {
                if (newConditionMeasure is OTUnSupportedDummyService.DummyMeasure) {
                    //if unsupported measure, add a temporary choice entry
                    measureSelectionView.adapter = MeasureSpinnerAdapter(availableMeasures + arrayOf(UnSupportedServiceFactoryData(newConditionMeasure)))
                    measureSelectionView.value = availableMeasures.size
                } else {
                    measureSelectionView.adapter = MeasureSpinnerAdapter(availableMeasures)
                    val measureFactory = newConditionMeasure.getFactory<OTServiceMeasureFactory>()
                    measureSelectionView.value = availableMeasures.indexOfFirst { it.typeCode == measureFactory.typeCode }
                }
            }

            comparisonSettingView.comparison = newCondition.comparison
            comparisonSettingView.threshold = newCondition.threshold
        }

        if (shouldNotifyChanges) {
            impl.notifyConditionChanged()
        }
    }

    inner class MeasureSpinnerAdapter(list: List<AServiceFactoryData>) : ArrayAdapter<AServiceFactoryData>(context, R.layout.simple_list_element_category_name, list) {

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
            holder.bind(getItem(position)!!)
            return view
        }
    }

    inner class MeasureViewHolder(val view: View) {

        private val titleView: TextView = view.findViewById(R.id.category)
        private val categoryView: TextView = view.findViewById(R.id.title)

        fun bind(factory: AServiceFactoryData) {

            categoryView.text = factory.getCategoryName(context)
            titleView.text = factory.getName(context)

        }
    }
}