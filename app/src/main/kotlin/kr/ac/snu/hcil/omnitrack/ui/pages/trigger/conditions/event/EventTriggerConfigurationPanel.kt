package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.event

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import io.reactivex.Observable
import kotlinx.android.synthetic.main.trigger_event_trigger_config_panel.view.*
import kr.ac.snu.hcil.android.common.view.wizard.WizardView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTEventTriggerCondition
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.ConditionConfigurationPanelImpl
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.IConditionConfigurationView


class EventTriggerConfigurationPanel : LinearLayout, IConditionConfigurationView {


    private val impl = ConditionConfigurationPanelImpl(OTEventTriggerCondition::class.java)


    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init{
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.trigger_event_trigger_config_panel, this, true)

        this.ui_event_button.setOnClickListener {
            val wizardView = EventTriggerWizardView(context)
            val wizardDialog = AlertDialog.Builder(context).setView(wizardView).create()

            wizardView.setWizardListener(object : WizardView.IWizardListener {
                override fun onComplete(wizard: WizardView) {
                    wizardDialog.dismiss()
                }

                override fun onCanceled(wizard: WizardView) {
                    wizardDialog.dismiss()
                }

            })

            wizardDialog.show()
        }
    }


    override fun applyCondition(condition: ATriggerCondition) {

    }

    override val onConditionChanged: Observable<ATriggerCondition> = impl.onConditionChanged

}