package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition

class ConditionConfigurationPanelImpl<T : ATriggerCondition>(val clazz: Class<T>) {

    var currentCondition: T? = null

    var suspendConditionChangeEvent = false

    private val conditionChangedSubject = PublishSubject.create<T>()
    val onConditionChanged: Observable<ATriggerCondition>
        get() = conditionChangedSubject.cast(ATriggerCondition::class.java)

    fun notifyConditionChanged() {
        if (!suspendConditionChangeEvent)
            currentCondition?.let { this.conditionChangedSubject.onNext(it) }
    }


    inline fun applyConditionAndGetChanged(condition: ATriggerCondition, changeHandler: (T) -> Unit): Boolean {
        if (clazz.isAssignableFrom(condition.javaClass) && currentCondition != condition) {
            @Suppress("UNCHECKED_CAST")
            currentCondition = condition.clone() as T

            suspendConditionChangeEvent = true

            changeHandler.invoke(currentCondition!!)

            suspendConditionChangeEvent = false

            return true
        } else return false
    }
}