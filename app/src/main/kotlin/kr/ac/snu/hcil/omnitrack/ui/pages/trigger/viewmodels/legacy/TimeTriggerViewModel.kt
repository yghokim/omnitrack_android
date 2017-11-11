package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.legacy

import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTrigger
import kr.ac.snu.hcil.omnitrack.utils.time.Time

/**
 * Created by younghokim on 2017. 6. 5..
 */
class TimeTriggerViewModel(trigger: OTTimeTrigger) : TriggerViewModel<OTTimeTrigger>(trigger) {

    val configType: BehaviorSubject<Int> = BehaviorSubject.create()
    val nextAlarmTime: BehaviorSubject<Long?> = BehaviorSubject.create()

    val isRepeated: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val isRangeSpecified: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val rangeVariable: BehaviorSubject<Int> = BehaviorSubject.create()
    val configVariable: BehaviorSubject<Int> = BehaviorSubject.create()

    val configuredAlarmTime: BehaviorSubject<Time> = BehaviorSubject.create()
    val configuredIntervalSeconds: BehaviorSubject<Int> = BehaviorSubject.create()

    override fun register() {
        super.register()

        nextAlarmTime.onNext(OTApp.instance.triggerAlarmManager.getNearestAlarmTime(trigger, System.currentTimeMillis())!!)

        subscriptions.add(
                trigger.switchTurned.subscribe {
                    isOn ->
                    if (isOn == false) {
                        nextAlarmTime.onNext(/*null*/ 0)
                    }
                }
        )

        subscriptions.add(
                trigger.onAlarmReserved.subscribe {
                    alarmTime ->
                    nextAlarmTime.onNext(alarmTime!!)
                }
        )

        configType.onNext(trigger.configType)

        refreshConfigSummary()
        refreshTimeConfiguration(trigger.configVariables)

        subscriptions.add(
                trigger.propertyChanged.subscribe { changedProperty ->
                    when (changedProperty.first) {
                        "configType" -> {
                            configType.onNext(changedProperty.second as Int)
                        }

                        "configVariables" -> {
                            val newValue = changedProperty.second as Int
                            configVariable.onNext(newValue)
                            refreshConfigSummary()
                            if (newValue != null) {
                                refreshTimeConfiguration(newValue)
                            }
                        }

                        "rangeVariables" -> {
                            rangeVariable.onNext(changedProperty.second as Int)
                            refreshConfigSummary()
                        }
                    }
                }
        )
    }

    private fun refreshTimeConfiguration(configVariables: Int) {
        if (configType.value == OTTimeTrigger.CONFIG_TYPE_ALARM) {
            val alarmTimeConfig = OTTimeTrigger.AlarmConfig.getAlarmTimeConfig(configVariables)
            if (configuredAlarmTime.value != alarmTimeConfig) {
                configuredAlarmTime.onNext(alarmTimeConfig)
            }
        } else if (configType.value == OTTimeTrigger.CONFIG_TYPE_INTERVAL) {
            val intervalConfig = OTTimeTrigger.IntervalConfig.getIntervalSeconds(configVariables)
            if (configuredIntervalSeconds.value != intervalConfig) {
                configuredIntervalSeconds.onNext(intervalConfig)
            }
        }
    }

    private fun refreshConfigSummary(): CharSequence {
        val summary = if (trigger.isRangeSpecified && trigger.isRepeated) {
            //display only days of weeks
            if (OTTimeTrigger.Range.isAllDayUsed(trigger.rangeVariables)) {
                OTApp.instance.resourcesWrapped.getString(R.string.msg_everyday)
            } else {

                val names = OTApp.instance.resourcesWrapped.getStringArray(R.array.days_of_week_short)

                val stringBuilder = StringBuilder()

                for (day in 1..7) {
                    if (OTTimeTrigger.Range.isDayOfWeekUsed(trigger.rangeVariables, day)) {
                        stringBuilder.append(names[day - 1].toUpperCase(), "  ")
                    }
                }

                stringBuilder.trim()
            }
        } else OTApp.instance.resourcesWrapped.getString(R.string.msg_once)

        triggerConfigSummary.onNext(summary)

        return summary
    }
}