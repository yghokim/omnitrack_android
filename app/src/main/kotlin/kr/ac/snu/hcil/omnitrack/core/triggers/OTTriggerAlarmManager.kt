package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.OTTriggerAlarmInstance
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.OTTriggerSchedule
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.utils.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.time.DesignatedTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.IntervalTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import javax.inject.Provider

/**
 * Created by younghokim on 16. 8. 29..
 */
class OTTriggerAlarmManager(val context: Context, val realmProvider: Provider<Realm>) : ITriggerAlarmController {


    companion object {

        const val TAG = "TimeTriggerAlarmManager"

        const val PREFERENCE_NAME = "TimeTriggerReservationTable"

        const val INTENT_EXTRA_TRIGGER_TIME = "triggerTime"
        const val INTENT_EXTRA_ALARM_ID = "alarmId"

        private fun makeIntent(context: Context, userId: String, triggerTime: Long, alarmId: Int): PendingIntent {
            val intent = Intent(context, TimeTriggerAlarmReceiver::class.java)
            intent.action = OTApp.BROADCAST_ACTION_TIME_TRIGGER_ALARM
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId)
            intent.putExtra(INTENT_EXTRA_ALARM_ID, alarmId)
            intent.putExtra(INTENT_EXTRA_TRIGGER_TIME, triggerTime)
            return PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        }

        private fun findAppendableAlarmInstance(alarmTime: Long, realm: Realm): OTTriggerAlarmInstance? {
            return realm.where(OTTriggerAlarmInstance::class.java).equalTo("reservedAlarmTime", TimeHelper.roundToSeconds(alarmTime)).findFirst()
        }
    }

    private val scheduleIdGenerator = ConcurrentUniqueLongGenerator()
    private val alarmDbIdGenerator = ConcurrentUniqueLongGenerator()

    private val alarmManager by lazy { OTApp.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    init {
    }


    //==========================================================

    fun activateOnSystem() {
        val realm = realmProvider.get()
        realm.use {
            realm.executeTransactionAsync({ realm ->

                val unManagedAlarms = realm.where(OTTriggerAlarmInstance::class.java)
                        .equalTo(OTTriggerAlarmInstance.FIELD_FIRED, false)
                        .equalTo(OTTriggerAlarmInstance.FIELD_SKIPPED, false).findAll()

                val now = System.currentTimeMillis() + 1000

                unManagedAlarms.forEach { alarm ->
                    if (alarm.reservedAlarmTime < now) {
                        //failed alarm.
                        alarm.skipped = true
                        alarm.triggerSchedules?.forEach {
                            it.skipped = true

                            if (it.oneShot) {
                                //one-shot trigger was failed
                            } else {
                                //repeated trigger was failed.
                            }
                        }
                    } else {
                        registerSystemAlarm(alarm.reservedAlarmTime, alarm.userId ?: "", alarm.alarmId)
                    }
                }
            }, {}, {})
        }
    }

    private fun retrieveTimeCondition(trigger: OTTriggerDAO): OTTimeTriggerCondition {
        return when (trigger.conditionType) {
            OTTriggerDAO.CONDITION_TYPE_TIME -> trigger.condition as OTTimeTriggerCondition
            OTTriggerDAO.CONDITION_TYPE_ITEM -> TODO("Implement item condition's time condition extraction")
            else -> throw Exception("Unsupported time condition trigger.")
        }
    }

    override fun onAlarmFired(systemAlarmId: Int): Completable {
        return handleFiredAlarmAndGetTriggerInfo(systemAlarmId).flatMapCompletable { (list) ->
            if (list?.isNotEmpty() == true) {
                Completable.merge(
                        list.map { schedule ->
                            val trigger = schedule.trigger
                            if (trigger != null) {
                                val triggerTime = schedule.intrinsicAlarmTime

                                if (!schedule.oneShot) {
                                    //reserve next alarm.
                                    val nextAlarmTime = getNextAlarmTime(triggerTime, retrieveTimeCondition(trigger))
                                    if (nextAlarmTime != null) {
                                        reserveAlarm(trigger, nextAlarmTime, false)
                                    } else {
                                        realmProvider.get().use { realm ->
                                            realm.executeTransaction {
                                                trigger.isOn = false
                                            }
                                        }
                                    }
                                } else {
                                    realmProvider.get().use { realm ->
                                        realm.executeTransaction {
                                            trigger.isOn = false
                                        }
                                    }
                                }

                                when (trigger.conditionType) {
                                    OTTriggerDAO.CONDITION_TYPE_TIME -> {
                                        //simple alarm. fire
                                        trigger.performFire(triggerTime, context)
                                    }
                                    OTTriggerDAO.CONDITION_TYPE_ITEM -> {
                                        //check items
                                        //TODO("Implement ItemCondition check and fire")
                                        Completable.complete()
                                    }
                                    else -> {
                                        println("Condition type ${trigger.conditionType} is not related to alarm.")
                                        Completable.complete()
                                    }
                                }
                            } else Completable.complete()
                        }
                )
            } else {
                Completable.complete()
            }
        }
    }

    fun reserveAlarm(trigger: OTTriggerDAO, alarmTime: Long, oneShot: Boolean): OTTriggerAlarmInstance.AlarmInfo {

        var result: OTTriggerAlarmInstance.AlarmInfo? = null
        realmProvider.get().use { realm ->
            realm.executeTransaction {
                val schedule = realm.createObject(OTTriggerSchedule::class.java, scheduleIdGenerator.getNewUniqueLong()).apply {
                    this.trigger = trigger
                    this.intrinsicAlarmTime = alarmTime
                    this.oneShot = oneShot
                }

                val appendableAlarm = findAppendableAlarmInstance(alarmTime, realm)
                if (appendableAlarm != null) {
                    schedule.parentAlarm = appendableAlarm
                    result = appendableAlarm.getInfo()
                } else {
                    //make new alarm
                    /*
                * s: false, f: false
                *
                * */
                    val newAlarmId = (realm.where(OTTriggerAlarmInstance::class.java)
                            .equalTo(OTTriggerAlarmInstance.FIELD_SKIPPED, false)
                            .equalTo(OTTriggerAlarmInstance.FIELD_FIRED, false)
                            .max("alarmId") ?: 0).toInt() + 1
                    val alarmInstance = realm.createObject(OTTriggerAlarmInstance::class.java, alarmDbIdGenerator.getNewUniqueLong())
                    alarmInstance.alarmId = newAlarmId

                    val alarmTimeToReserve = TimeHelper.roundToSeconds(alarmTime)

                    alarmInstance.reservedAlarmTime = alarmTimeToReserve
                    schedule.parentAlarm = alarmInstance
                    alarmInstance.userId = trigger.userId

                    registerSystemAlarm(alarmTimeToReserve, trigger.userId!!, newAlarmId)

                    result = alarmInstance.getInfo()
                }
            }
        }
        return result!!
    }

    private fun registerSystemAlarm(alarmTimeToReserve: Long, userId: String, alarmId: Int) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeToReserve, makeIntent(OTApp.instance, userId, alarmTimeToReserve, alarmId))
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTimeToReserve, makeIntent(OTApp.instance, userId, alarmTimeToReserve, alarmId))
        }
    }

    fun cancelTrigger(trigger: OTTriggerDAO) {
        val realm = realmProvider.get()

        val pendingTriggerSchedules = realm.where(OTTriggerSchedule::class.java)
                .equalTo(OTTriggerSchedule.FIELD_TRIGGER_ID, trigger.objectId)
                .equalTo(OTTriggerSchedule.FIELD_FIRED, false)
                .equalTo(OTTriggerSchedule.FIELD_SKIPPED, false)
                .findAll()

        println("canceling trigger - found ${pendingTriggerSchedules.size} pending schedules.")

        if (pendingTriggerSchedules.isNotEmpty()) {
            realm.executeTransaction { realm ->
                pendingTriggerSchedules.forEach { schedule ->
                    val parent = schedule.parentAlarm
                    schedule.parentAlarm = null
                    if (parent != null) {
                        if (parent.triggerSchedules?.isEmpty() != false) {
                            println("remove system alarm")
                            val pendingIntent = makeIntent(context, trigger.userId!!, parent.reservedAlarmTime, parent.alarmId)
                            alarmManager.cancel(pendingIntent)
                            pendingIntent.cancel()
                            parent.deleteFromRealm()
                        }
                    }
                }

                pendingTriggerSchedules.deleteAllFromRealm()
            }
        }

        realm.close()
    }

    fun getNearestAlarmTime(triggerId: String, now: Long): Long? {
        realmProvider.get().use { realm ->
            val nearestTime = realm.where(OTTriggerSchedule::class.java)
                    .equalTo(OTTriggerSchedule.FIELD_TRIGGER_ID, triggerId)
                    .equalTo(OTTriggerSchedule.FIELD_FIRED, false)
                    .equalTo(OTTriggerSchedule.FIELD_SKIPPED, false)
                    .greaterThan(OTTriggerSchedule.FIELD_INTRINSIC_ALARM_TIME, now)
                    .min(OTTriggerSchedule.FIELD_INTRINSIC_ALARM_TIME)

            return nearestTime?.toLong()
        }
    }

    private fun handleFiredAlarmAndGetTriggerInfo(alarmId: Int): Single<Nullable<List<OTTriggerSchedule>>> {
        return Single.defer {
            realmProvider.get().use { realm ->
                println("find alarm instance with id: ${alarmId}")
                val alarmInstance = realm.where(OTTriggerAlarmInstance::class.java)
                        .equalTo(OTTriggerAlarmInstance.FIELD_ALARM_ID, alarmId)
                        .equalTo(OTTriggerAlarmInstance.FIELD_FIRED, false)
                        .equalTo(OTTriggerAlarmInstance.FIELD_SKIPPED, false)
                        .findFirst()

                if (alarmInstance != null) {
                    realm.executeTransaction {
                        alarmInstance.triggerSchedules?.forEach { schedule ->
                            schedule.fired = true
                            schedule.skipped = false
                        }

                        alarmInstance.fired = true
                        alarmInstance.skipped = false

                    }
                    println("handled trigger schedules: ${alarmInstance.triggerSchedules?.size}")
                    return@defer Single.just(Nullable<List<OTTriggerSchedule>>(alarmInstance.triggerSchedules))
                } else return@defer Single.just(Nullable<List<OTTriggerSchedule>>(null))
            }
        }.observeOn(Schedulers.newThread())
    }

    fun isAlarmRegistered(pivot: Long?, trigger: OTTriggerDAO): Boolean {
        realmProvider.get().use { realm ->
            val triggerSchedules = realm.where(OTTriggerSchedule::class.java)
                    .equalTo(OTTriggerSchedule.FIELD_TRIGGER_ID, trigger.objectId!!)
                    .findAll()


            if (triggerSchedules.isEmpty()) {
                return false
            } else {
                val nextAlarmTime = getNextAlarmTime(pivot, trigger.condition as OTTimeTriggerCondition)
                return triggerSchedules.where().equalTo(OTTriggerSchedule.FIELD_INTRINSIC_ALARM_TIME, nextAlarmTime)
                        .findAll().isNotEmpty()
            }
        }
    }


    fun getNextAlarmTime(pivot: Long?, condition: OTTimeTriggerCondition): Long? {
        val now = pivot ?: System.currentTimeMillis()

        val limitExclusive = if (condition.isRepeated && condition.endAt != null) {
            condition.endAt!!
        } else Long.MAX_VALUE

        val nextTimeCalculator = when (condition.timeConditionType) {
            OTTimeTriggerCondition.TIME_CONDITION_ALARM -> {

                val calculator = DesignatedTimeScheduleCalculator().setAlarmTime(condition.alarmTimeHour.toInt(), condition.alarmTimeMinute.toInt(), 0)
                if (condition.isRepeated) {
                    calculator.setAvailableDaysOfWeekFlag(condition.dayOfWeekFlags.toInt())
                            .setEndAt(limitExclusive)
                }

                calculator
            }
            OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> {
                val intervalMillis = condition.intervalSeconds * 1000

                val calculator = IntervalTimeScheduleCalculator().setInterval(intervalMillis.toLong())

                if (condition.isRepeated) {
                    calculator
                            .setAvailableDaysOfWeekFlag(condition.dayOfWeekFlags.toInt())
                            .setEndAt(limitExclusive)

                    if (condition.intervalIsHourRangeUsed) {
                        calculator.setHourBoundingRange(condition.intervalHourRangeStart.toInt(), condition.intervalHourRangeEnd.toInt())
                    }
                } else {
                    calculator.setNoLimit()
                }

                calculator
            }
            else -> throw Exception("Unsupported Time Config Type")
        }

        return nextTimeCalculator.calculateNext(pivot, now)
    }

}