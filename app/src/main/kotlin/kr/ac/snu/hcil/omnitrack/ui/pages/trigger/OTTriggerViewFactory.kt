package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.util.SparseArray
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.ATriggerConditionViewModel
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TimeConditionViewModel

/**
 * Created by younghokim on 2017. 10. 22..
 */
object OTTriggerViewFactory {
    interface ITriggerConditionViewProvider {
        fun getTriggerConditionViewModel(trigger: OTTriggerDAO, configuredContext: ConfiguredContext): ATriggerConditionViewModel
        fun getTriggerDisplayView(original: View?, trigger: OTTriggerDAO, uiContext: Context, configuredContext: ConfiguredContext): View
        fun getTriggerConfigurationPanel(original: View?, uiContext: Context, configuredContext: ConfiguredContext): IConditionConfigurationView
        fun connectViewModelToDisplayView(viewModel: ATriggerConditionViewModel, displayView: View, outSubscription: CompositeDisposable)
    }

    private val providerDict: SparseArray<ITriggerConditionViewProvider> by lazy {
        SparseArray<ITriggerConditionViewProvider>().apply {
            this.append(OTTriggerDAO.CONDITION_TYPE_TIME.toInt(), object : ITriggerConditionViewProvider {
                override fun getTriggerConditionViewModel(trigger: OTTriggerDAO, configuredContext: ConfiguredContext): ATriggerConditionViewModel {
                    return TimeConditionViewModel(trigger, configuredContext)
                }

                override fun getTriggerDisplayView(original: View?, trigger: OTTriggerDAO, uiContext: Context, configuredContext: ConfiguredContext): View {
                    val condition = trigger.condition as OTTimeTriggerCondition

                    when (condition.timeConditionType) {
                        OTTimeTriggerCondition.TIME_CONDITION_ALARM, OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> {
                            val displayView: TimeTriggerDisplayView = if (original is TimeTriggerDisplayView) {
                                original
                            } else TimeTriggerDisplayView(uiContext)

                            when (condition.timeConditionType) {
                                OTTimeTriggerCondition.TIME_CONDITION_ALARM -> {
                                    val time = kr.ac.snu.hcil.omnitrack.utils.time.Time(condition.alarmTimeHour.toInt(), condition.alarmTimeMinute.toInt(), 0)
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
                            displayView.setSamplingRange(condition.samplingHourStart, condition.samplingHourEnd)

                            return displayView
                        }
                        else -> throw kotlin.IllegalArgumentException()
                    }
                }

                override fun getTriggerConfigurationPanel(original: View?, uiContext: Context, configuredContext: ConfiguredContext): IConditionConfigurationView {
                    val configPanel = if (original is TimeTriggerConfigurationPanel) {
                        original
                    } else kr.ac.snu.hcil.omnitrack.ui.pages.trigger.TimeTriggerConfigurationPanel(uiContext)
                    return configPanel
                }

                override fun connectViewModelToDisplayView(viewModel: ATriggerConditionViewModel, displayView: View, outSubscription: CompositeDisposable) {
                    if (viewModel is TimeConditionViewModel && displayView is TimeTriggerDisplayView) {
                        outSubscription.add(
                                viewModel.nextAlarmTime.observeOn(AndroidSchedulers.mainThread()).subscribe { nextAlarmTime ->
                                    displayView.nextTriggerTime = nextAlarmTime.datum
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