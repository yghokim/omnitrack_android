package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import io.reactivex.Observable
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition

/**
 * Created by younghokim on 2017. 10. 25..
 */
interface IConditionConfigurationView {

    fun applyCondition(condition: ATriggerCondition)

    val onConditionChanged: Observable<ATriggerCondition>
}