package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.util.SparseArray
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.android.common.time.Time
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.IConditionConfigurationView
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.data.DataDrivenConditionViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.data.DataDrivenTriggerConfigurationPanel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.data.DataDrivenTriggerDisplayView
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.time.SamplingTimeConditionDisplayView
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.time.TimeConditionViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.time.TimeTriggerConfigurationPanel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.time.TimeTriggerDisplayView
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerConditionViewModel

/**
 * Created by younghokim on 2017. 10. 22..
 */
object OTTriggerViewFactory {
    interface ITriggerConditionViewProvider {
        fun getTriggerConditionViewModel(trigger: OTTriggerDAO, context: Context): ATriggerConditionViewModel
        fun getTriggerDisplayView(original: View?, trigger: OTTriggerDAO, uiContext: Context): View
        fun getTriggerConfigurationPanel(original: View?, uiContext: Context): IConditionConfigurationView
        fun connectViewModelToDisplayView(viewModel: ATriggerConditionViewModel, displayView: View, outSubscription: CompositeDisposable)
    }

    private val providerDict: SparseArray<ITriggerConditionViewProvider> by lazy {
        SparseArray<ITriggerConditionViewProvider>().apply {
            this.append(OTTriggerDAO.CONDITION_TYPE_TIME.toInt(), object : ITriggerConditionViewProvider {
                override fun getTriggerConditionViewModel(trigger: OTTriggerDAO, context: Context): ATriggerConditionViewModel {
                    return TimeConditionViewModel(trigger, context)
                }

                override fun getTriggerDisplayView(original: View?, trigger: OTTriggerDAO, uiContext: Context): View {
                    val condition = trigger.condition as OTTimeTriggerCondition

                    when (condition.timeConditionType) {
                        OTTimeTriggerCondition.TIME_CONDITION_ALARM, OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> {
                            val displayView: TimeTriggerDisplayView = if (original is TimeTriggerDisplayView) {
                                original
                            } else TimeTriggerDisplayView(uiContext)

                            when (condition.timeConditionType) {
                                OTTimeTriggerCondition.TIME_CONDITION_ALARM -> {
                                    val time = Time(condition.alarmTimeHour.toInt(), condition.alarmTimeMinute.toInt(), 0)
                                    displayView.setAlarmInformation(time.hour, time.minute, time.amPm)
                                }
                                OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> {
                                    displayView.setIntervalInformation(condition.intervalSeconds.toInt())
                                }
                            }
                            return displayView
                        }
                        OTTimeTriggerCondition.TIME_CONDITION_SAMPLING -> {
                            val displayView: SamplingTimeConditionDisplayView = if (original is SamplingTimeConditionDisplayView) {
                                original
                            } else SamplingTimeConditionDisplayView(uiContext)

                            displayView.samplingCount = condition.samplingCount
                            if (condition.samplingRangeUsed) {
                                displayView.setSamplingRange(condition.samplingHourStart, condition.samplingHourEnd)
                            } else displayView.setSamplingFullDay()

                            return displayView
                        }
                        else -> throw kotlin.IllegalArgumentException()
                    }
                }

                override fun getTriggerConfigurationPanel(original: View?, uiContext: Context): IConditionConfigurationView {
                    return if (original is TimeTriggerConfigurationPanel) {
                        original
                    } else TimeTriggerConfigurationPanel(uiContext)
                }

                override fun connectViewModelToDisplayView(viewModel: ATriggerConditionViewModel, displayView: View, outSubscription: CompositeDisposable) {
                    if (viewModel is TimeConditionViewModel) {
                        if (displayView is TimeTriggerDisplayView) {
                            outSubscription.add(
                                    viewModel.nextAlarmTime.observeOn(AndroidSchedulers.mainThread()).subscribe { nextAlarmTime ->
                                        displayView.nextTriggerTime = nextAlarmTime.datum
                                    }
                            )
                        } else if (displayView is SamplingTimeConditionDisplayView) {
                            outSubscription.add(
                                    viewModel.nextAlarmTime.observeOn(AndroidSchedulers.mainThread()).subscribe { nextAlarmTime ->
                                        displayView.nextAlertTime = nextAlarmTime.datum
                                    }
                            )
                        }
                    }
                }

            })

            this.append(OTTriggerDAO.CONDITION_TYPE_DATA.toInt(), object : ITriggerConditionViewProvider {
                override fun getTriggerConditionViewModel(trigger: OTTriggerDAO, context: Context): ATriggerConditionViewModel {
                    return DataDrivenConditionViewModel(trigger, context)
                }

                override fun getTriggerDisplayView(original: View?, trigger: OTTriggerDAO, uiContext: Context): View {
                    val displayView = if (original is DataDrivenTriggerDisplayView) original else DataDrivenTriggerDisplayView(uiContext)
                    val condition = trigger.condition
                    if (condition != null && condition is OTDataDrivenTriggerCondition) {
                        displayView.setMeasure(condition.measure)
                        displayView.setComparison(condition.comparison)
                        displayView.setThreshold(condition.threshold)
                    } else {
                        displayView.setNullCondition()
                    }

                    return displayView
                }

                override fun getTriggerConfigurationPanel(original: View?, uiContext: Context): IConditionConfigurationView {
                    return if (original is DataDrivenTriggerConfigurationPanel) original else DataDrivenTriggerConfigurationPanel(uiContext)
                }

                override fun connectViewModelToDisplayView(viewModel: ATriggerConditionViewModel, displayView: View, outSubscription: CompositeDisposable) {
                    if (viewModel is DataDrivenConditionViewModel && displayView is DataDrivenTriggerDisplayView) {
                        outSubscription.add(
                                viewModel.latestMeasuredInfo.observeOn(AndroidSchedulers.mainThread()).subscribe { (info) ->
                                    if (info != null) {
                                        displayView.setLatestMeasureInfo(info.first, info.second)
                                    }
                                }
                        )

                        outSubscription.add(
                                viewModel.getServiceStateObservable().subscribe { serviceState ->
                                    displayView.setServiceState(serviceState)
                                }
                        )
                    }
                }

            })
        }
    }

    fun getConditionViewProvider(conditionType: Byte): ITriggerConditionViewProvider? {
        return providerDict.get(conditionType.toInt())
    }
}