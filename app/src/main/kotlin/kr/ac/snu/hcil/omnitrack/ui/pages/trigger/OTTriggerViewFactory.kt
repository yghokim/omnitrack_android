package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.util.SparseArray
import android.view.View
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO

/**
 * Created by younghokim on 2017. 10. 22..
 */
object OTTriggerViewFactory {
    interface ITriggerConditionViewProvider {
        fun getTriggerDisplayView(original: View?, trigger: OTTriggerDAO, context: Context): View
        fun getTriggerConfigurationPanel(original: View?, trigger: OTTriggerDAO, context: Context): View
    }

    private val providerDict: SparseArray<ITriggerConditionViewProvider> by lazy {
        SparseArray<ITriggerConditionViewProvider>().apply {
            this.setValueAt(OTTriggerDAO.CONDITION_TYPE_TIME.toInt(), object : ITriggerConditionViewProvider {
                override fun getTriggerDisplayView(original: View?, trigger: OTTriggerDAO, context: Context): View {
                    val displayView: TimeTriggerDisplayView = if (original is TimeTriggerDisplayView) {
                        original
                    } else TimeTriggerDisplayView(context)

                    return displayView
                }

                override fun getTriggerConfigurationPanel(original: View?, trigger: OTTriggerDAO, context: Context): View {
                    val configPanel = if (original is TimeTriggerConfigurationPanel) {
                        original
                    } else kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TimeTriggerConfigurationPanel(context)
                    return configPanel
                }

            })
        }
    }

    fun getConditionViewProvider(trigger: OTTriggerDAO): ITriggerConditionViewProvider? {
        return providerDict.get(trigger.conditionType.toInt())
    }
}