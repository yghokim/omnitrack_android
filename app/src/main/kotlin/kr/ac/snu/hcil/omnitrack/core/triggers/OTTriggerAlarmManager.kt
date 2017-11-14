package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.OTTriggerAlarmInstance
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.OTTriggerSchedule
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.utils.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import kr.ac.snu.hcil.omnitrack.utils.time.DesignatedTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.IntervalTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import java.util.*
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

    override fun activateOnSystem() {
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
            else -> throw Exception("Unsupported time condition trigger.")
        }
    }


    override fun registerTriggerAlarm(pivot: Long?, trigger: OTTriggerDAO): Boolean {
        val condition = retrieveTimeCondition(trigger)
        val nextAlarmTime = calculateNextAlarmTime(pivot, condition)
        if (nextAlarmTime != null) {
            OTApp.logger.writeSystemLog("Next alarm time is ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(nextAlarmTime))}", TAG)
            reserveAlarm(trigger, nextAlarmTime, !condition.isRepeated)
            return true
        } else {
            realmProvider.get().use { realm ->
                realm.executeTransactionIfNotIn {
                    trigger.isOn = false
                }
            }
            return false
        }
    }

    override fun onAlarmFired(systemAlarmId: Int): Completable {
        return Completable.defer {
            OTApp.logger.writeSystemLog("System alarm is fired. alarmId: ${systemAlarmId}", TAG)
            val realm = realmProvider.get()
            return@defer handleFiredAlarmAndGetTriggerInfo(systemAlarmId, realm).flatMapCompletable { (list) ->
                if (list?.isNotEmpty() == true) {
                    Completable.merge(
                            list.map { schedule ->
                                OTApp.logger.writeSystemLog("handling trigger schedule (id: ${schedule.id}). oneshot: ${schedule.oneShot}, intrinsicAlarmTime: ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(schedule.intrinsicAlarmTime))}", TAG)
                                val trigger = schedule.trigger
                                if (trigger != null) {
                                    val triggerTime = schedule.intrinsicAlarmTime

                                    if (!schedule.oneShot) {
                                        //reserve next alarm.
                                        OTApp.logger.writeSystemLog("This schedule is not oneshot. reserve next alarm.", TAG)
                                        if (!registerTriggerAlarm(triggerTime, trigger)) {
                                            //time trigger chain was broken.
                                            realm.executeTransaction {
                                                schedule.final = true
                                            }
                                        }
                                    } else {
                                        OTApp.logger.writeSystemLog("This schedule is oneshot. turn off the trigger.", TAG)
                                        realm.executeTransactionIfNotIn {
                                            trigger.isOn = false
                                        }
                                    }

                                    when (trigger.conditionType) {
                                        OTTriggerDAO.CONDITION_TYPE_TIME -> {
                                            //simple alarm. fire

                                            OTApp.logger.writeSystemLog("Fire time trigger. schedule id: ${schedule.id}", TAG)
                                            if (trigger.validateScriptIfExist(context)) {
                                                trigger.performFire(triggerTime, context)
                                            } else Completable.complete()
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
                }.doOnTerminate {
                    OTApp.logger.writeSystemLog("System alarm fire was succesfully handled. alarmId: ${systemAlarmId}", TAG)
                    realm.close()
                }
            }
        }
    }

    fun reserveAlarm(trigger: OTTriggerDAO, alarmTime: Long, oneShot: Boolean): OTTriggerAlarmInstance.AlarmInfo {

        var result: OTTriggerAlarmInstance.AlarmInfo? = null
        realmProvider.get().use { realm ->
            realm.executeTransactionIfNotIn {
                val schedule = realm.createObject(OTTriggerSchedule::class.java, scheduleIdGenerator.getNewUniqueLong()).apply {
                    this.trigger = trigger
                    this.intrinsicAlarmTime = alarmTime
                    this.oneShot = oneShot
                    this.final = oneShot
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
                    val newAlarmId = System.currentTimeMillis().toInt()
                    val alarmInstance = realm.createObject(OTTriggerAlarmInstance::class.java, alarmDbIdGenerator.getNewUniqueLong())
                    alarmInstance.alarmId = newAlarmId

                    val alarmTimeToReserve = TimeHelper.roundToSeconds(alarmTime)


                    alarmInstance.reservedAlarmTime = alarmTimeToReserve
                    schedule.parentAlarm = alarmInstance
                    alarmInstance.userId = trigger.userId

                    registerSystemAlarm(alarmTimeToReserve, trigger.userId!!, newAlarmId)
                    OTApp.logger.writeSystemLog("The actual alarm was reserved at: ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(alarmTimeToReserve))}", TAG)

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

    override fun cancelTrigger(trigger: OTTriggerDAO) {
        val realm = realmProvider.get()

        val pendingTriggerSchedules = realm.where(OTTriggerSchedule::class.java)
                .equalTo("trigger.${RealmDatabaseManager.FIELD_OBJECT_ID}", trigger.objectId)
                .equalTo(OTTriggerSchedule.FIELD_FIRED, false)
                .equalTo(OTTriggerSchedule.FIELD_SKIPPED, false)
                .findAll()

        println("canceling trigger - found ${pendingTriggerSchedules.size} pending schedules.")

        if (pendingTriggerSchedules.isNotEmpty()) {
            realm.executeTransactionIfNotIn { realm ->
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

    override fun getNearestAlarmTime(triggerId: String, now: Long): Single<Nullable<Long>> {
        return Single.defer {
            realmProvider.get().use { realm ->
                val nearestTime = realm.where(OTTriggerSchedule::class.java)
                        .equalTo("trigger.${RealmDatabaseManager.FIELD_OBJECT_ID}", triggerId)
                        .equalTo(OTTriggerSchedule.FIELD_FIRED, false)
                        .equalTo(OTTriggerSchedule.FIELD_SKIPPED, false)
                        .greaterThan(OTTriggerSchedule.FIELD_INTRINSIC_ALARM_TIME, now)
                        .min(OTTriggerSchedule.FIELD_INTRINSIC_ALARM_TIME)

                return@defer Single.just(Nullable(nearestTime?.toLong()))
            }
        }.subscribeOn(Schedulers.newThread())
    }

    override fun makeNextAlarmTimeObservable(triggerId: String): Flowable<Nullable<Long>> {
        return Flowable.defer {
            val realm = realmProvider.get()
            return@defer realm.where(OTTriggerSchedule::class.java)
                    .equalTo("trigger.${RealmDatabaseManager.FIELD_OBJECT_ID}", triggerId)
                    .equalTo(OTTriggerSchedule.FIELD_FIRED, false)
                    .equalTo(OTTriggerSchedule.FIELD_SKIPPED, false)
                    .findAllSorted(OTTriggerSchedule.FIELD_INTRINSIC_ALARM_TIME, Sort.ASCENDING)
                    .asFlowable()
                    .filter { it.isLoaded && it.isValid }
                    .map { list ->
                        if (list.isEmpty()) {
                            return@map Nullable<Long>(null)
                        } else {
                            return@map Nullable(list.first()!!.intrinsicAlarmTime)
                        }
                    }.doAfterTerminate {
                realm.close()
            }
        }
    }


    private fun handleFiredAlarmAndGetTriggerInfo(alarmId: Int, realm: Realm): Single<Nullable<List<OTTriggerSchedule>>> {
        return Single.defer {
            println("find alarm instance with id: ${alarmId}")
            val alarmInstance = realm.where(OTTriggerAlarmInstance::class.java)
                    .equalTo(OTTriggerAlarmInstance.FIELD_ALARM_ID, alarmId)
                    .equalTo(OTTriggerAlarmInstance.FIELD_FIRED, false)
                    .equalTo(OTTriggerAlarmInstance.FIELD_SKIPPED, false)
                    .findFirst()

            if (alarmInstance != null) {
                realm.executeTransactionIfNotIn {
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
    }


    override fun continueTriggerInChainIfPossible(trigger: OTTriggerDAO): Boolean {

        val realm = realmProvider.get()
        val latestPastSchedule = realm.where(OTTriggerSchedule::class.java)
                .equalTo("trigger.${RealmDatabaseManager.FIELD_OBJECT_ID}", trigger.objectId)
                .findAllSorted(OTTriggerSchedule.FIELD_INTRINSIC_ALARM_TIME, Sort.DESCENDING)
                .find { it.fired || it.skipped }

        if (latestPastSchedule == null) {
            //start new Alarm
            //TODO naive. check condition the same.
            cancelTrigger(trigger)
            registerTriggerAlarm(null, trigger)
        } else {
            if (!latestPastSchedule.final) {
                //TODO naive. check condition the same.
                cancelTrigger(trigger)
                registerTriggerAlarm(latestPastSchedule.intrinsicAlarmTime, trigger)
            } else {
                //start new chain.
                cancelTrigger(trigger)
                registerTriggerAlarm(null, trigger)
            }
        }
        return false
    }


    fun calculateNextAlarmTime(pivot: Long?, condition: OTTimeTriggerCondition): Long? {
        val now = System.currentTimeMillis()

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