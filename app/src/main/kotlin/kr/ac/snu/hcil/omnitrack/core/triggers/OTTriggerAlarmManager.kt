package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTTriggerAlarmInstance
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTTriggerSchedule
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.services.OTDeviceStatusService
import kr.ac.snu.hcil.omnitrack.utils.*
import kr.ac.snu.hcil.omnitrack.utils.time.DesignatedTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.ExperienceSamplingTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.IntervalTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import org.jetbrains.anko.alarmManager
import org.jetbrains.anko.powerManager
import org.jetbrains.anko.runOnUiThread
import java.util.*
import javax.inject.Singleton

/**
 * Created by younghokim on 16. 8. 29..
 */
@Singleton
class OTTriggerAlarmManager(val context: Context, val realmProvider: Factory<Realm>) : ITriggerAlarmController {
    companion object {

        const val TAG = "TimeTriggerAlarmManager"

        const val PREFERENCE_NAME = "TimeTriggerReservationTable"

        const val INTENT_EXTRA_TRIGGER_TIME = "triggerTime"
        const val INTENT_EXTRA_ALARM_ID = "alarmId"
    }

    private val scheduleIdGenerator = ConcurrentUniqueLongGenerator()
    private val alarmDbIdGenerator = ConcurrentUniqueLongGenerator()

    init {
    }

    //==========================================================


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

                            if (!it.stickyAlarm) {
                                it.skipped = true
                                if (it.oneShot) {
                                    //one-shot trigger was failed
                                    OTApp.logger.writeSystemLog("Trigger was missed at ${it.intrinsicAlarmTime.toDatetimeString()}. It was one-shot so just ignored.", TAG)
                                } else {
                                    //repeated trigger was failed.
                                    it.trigger?.let { trigger ->
                                        registerTriggerAlarm(it.intrinsicAlarmTime, trigger)
                                        OTApp.logger.writeSystemLog("Trigger was missed at ${it.intrinsicAlarmTime.toDatetimeString()}. reserve next alarm - ${trigger.objectId}", TAG)
                                    }
                                }
                            } else {
                                //sticky alarm
                                if (it.trigger?.validateScriptIfExist(context) == true) {
                                    val metadata = buildMetadata(it, now)
                                    metadata.addProperty("stickyDelayed", true)
                                    it.trigger?.getPerformFireCompletable(it.intrinsicAlarmTime, metadata, context)?.blockingAwait()
                                    OTApp.logger.writeSystemLog("Trigger was missed at ${it.intrinsicAlarmTime.toDatetimeString()}. But its sticky so perform immediately.", TAG)
                                }
                            }
                        }
                    } else {
                        registerSystemAlarm(alarm.reservedAlarmTime, alarm.userId
                                ?: "", alarm.alarmId)
                    }
                }


            }, {
                OTApp.logger.writeSystemLog("TriggerAlarmManager was successfully initialized the alarms", TAG)
            }, { err ->
                err.printStackTrace()
                OTApp.logger.writeSystemLog("TriggerAlarmManager find transaction was failed due to an error : ${err.message}", TAG)
            })
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
        val nextAlarmTimeInfo = calculateNextAlarmTime(pivot, condition, trigger.objectId!!)
        return if (nextAlarmTimeInfo != null) {
            OTApp.logger.writeSystemLog("Next alarm time is ${nextAlarmTimeInfo.first.toDatetimeString()}", TAG)
            reserveAlarm(trigger, nextAlarmTimeInfo, !condition.isRepeated)
            true
        } else {
            realmProvider.get().use { realm ->
                realm.executeTransactionIfNotIn {
                    trigger.isOn = false
                }
            }
            false
        }
    }

    override fun onAlarmFired(systemAlarmId: Int): Completable {
        return Completable.defer {
            OTApp.logger.writeSystemLog("System alarm is fired. alarmId: $systemAlarmId", TAG)
            val realm = realmProvider.get()
            return@defer handleFiredAlarmAndGetTriggerInfo(systemAlarmId, realm).flatMapCompletable { (list) ->
                if (list?.isNotEmpty() == true) {
                    Completable.merge(
                            list.map { schedule ->
                                OTApp.logger.writeSystemLog("handling trigger schedule (id: ${schedule.id}). oneshot: ${schedule.oneShot}, intrinsicAlarmTime: ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(schedule.intrinsicAlarmTime))}", TAG)
                                val trigger = schedule.trigger
                                if (trigger != null) {
                                    val triggerTime = schedule.intrinsicAlarmTime
                                    val actualTriggeredTime = System.currentTimeMillis()

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
                                                trigger.getPerformFireCompletable(triggerTime, buildMetadata(schedule, actualTriggeredTime), context)
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
                }.subscribeOn(AndroidSchedulers.mainThread()).doOnComplete {
                    OTApp.logger.writeSystemLog("System alarm fire was succesfully handled. alarmId: $systemAlarmId", TAG)
                }.doOnTerminate {
                    context.runOnUiThread { realm.close() }
                }
            }
        }
    }

    private fun buildMetadata(schedule: OTTriggerSchedule, actualTriggeredTime: Long): JsonObject {
        val metadata: JsonObject = schedule.serializedMetadata?.let { (context.applicationContext as OTAndroidApp).applicationComponent.genericGson().fromJson(it, JsonObject::class.java) }
                ?: JsonObject()
        metadata.addProperty("reservedAt", schedule.intrinsicAlarmTime)
        metadata.addProperty("actuallyFiredAt", actualTriggeredTime)
        return metadata
    }

    private fun reserveAlarm(trigger: OTTriggerDAO, alarmTimeInfo: Pair<Long, JsonObject?>, oneShot: Boolean): OTTriggerAlarmInstance.AlarmInfo {

        var result: OTTriggerAlarmInstance.AlarmInfo? = null
        realmProvider.get().use { realm ->

            realm.executeTransactionIfNotIn {
                val schedule = (realm.where(OTTriggerSchedule::class.java)
                        .equalTo(OTTriggerSchedule.FIELD_INTRINSIC_ALARM_TIME, alarmTimeInfo.first)
                        .equalTo("trigger.objectId", trigger.objectId).findFirst()
                        ?: realm.createObject(OTTriggerSchedule::class.java, scheduleIdGenerator.getNewUniqueLong())).apply {
                    this.trigger = trigger
                    this.stickyAlarm = trigger.condition?.isSticky ?: false
                    this.intrinsicAlarmTime = alarmTimeInfo.first
                    this.oneShot = oneShot
                    this.final = oneShot
                    this.serializedMetadata = alarmTimeInfo.second?.toString()
                }

                if (schedule.parentAlarm != null) {
                    //schedule alarm is already existing.
                    schedule.parentAlarm?.let { parentAlarm ->
                        registerSystemAlarm(parentAlarm.reservedAlarmTime, trigger.userId!!, parentAlarm.alarmId)
                    }
                } else {

                    val appendableAlarm = findAppendableAlarmInstance(alarmTimeInfo.first, realm)
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

                        val alarmTimeToReserve = TimeHelper.roundToSeconds(alarmTimeInfo.first)


                        alarmInstance.reservedAlarmTime = alarmTimeToReserve
                        schedule.parentAlarm = alarmInstance
                        alarmInstance.userId = trigger.userId

                        alarmInstance.isReservedWhenDeviceActive = OTDeviceStatusService.isBatteryCharging(context) || context.powerManager.isInteractiveCompat

                        registerSystemAlarm(alarmTimeToReserve, trigger.userId!!, newAlarmId)
                        OTApp.logger.writeSystemLog("The actual alarm was reserved at: ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(alarmTimeToReserve))}. \nReservedWhenDeviceActive: ${alarmInstance.isReservedWhenDeviceActive}", TAG)

                        result = alarmInstance.getInfo()
                    }
                }
            }
        }
        return result!!
    }

    private fun registerSystemAlarm(alarmTimeToReserve: Long, userId: String, alarmId: Int) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            context.alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeToReserve, makeIntent(context, userId, alarmTimeToReserve, alarmId))
        } else {
            context.alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTimeToReserve, makeIntent(context, userId, alarmTimeToReserve, alarmId))
        }
    }

    override fun cancelTrigger(trigger: OTTriggerDAO) {
        val realm = realmProvider.get()

        val pendingTriggerSchedules = realm.where(OTTriggerSchedule::class.java)
                .equalTo("trigger.${BackendDbManager.FIELD_OBJECT_ID}", trigger.objectId)
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
                            context.alarmManager.cancel(pendingIntent)
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
                        .equalTo("trigger.${BackendDbManager.FIELD_OBJECT_ID}", triggerId)
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
                    .equalTo("trigger.${BackendDbManager.FIELD_OBJECT_ID}", triggerId)
                    .equalTo(OTTriggerSchedule.FIELD_FIRED, false)
                    .equalTo(OTTriggerSchedule.FIELD_SKIPPED, false)
                    .sort(OTTriggerSchedule.FIELD_INTRINSIC_ALARM_TIME, Sort.ASCENDING)
                    .findAll()
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
            println("find alarm instance with id: $alarmId")
            val alarmInstance = realm.where(OTTriggerAlarmInstance::class.java)
                    .equalTo(OTTriggerAlarmInstance.FIELD_ALARM_ID, alarmId)
                    .equalTo(OTTriggerAlarmInstance.FIELD_FIRED, false)
                    .equalTo(OTTriggerAlarmInstance.FIELD_SKIPPED, false)
                    .findFirst()

            if (alarmInstance != null) {

                val triggerSchedules = alarmInstance.triggerSchedules?.toList()
                realm.executeTransactionIfNotIn {
                    triggerSchedules?.forEach { schedule ->
                        schedule.fired = true
                        schedule.skipped = false
                    }

                    alarmInstance.deleteFromRealm()
                }
                println("handled trigger schedules: ${triggerSchedules?.size}")
                return@defer Single.just(Nullable<List<OTTriggerSchedule>>(triggerSchedules))
            } else return@defer Single.just(Nullable<List<OTTriggerSchedule>>(null))
        }
    }


    override fun continueTriggerInChainIfPossible(trigger: OTTriggerDAO): Boolean {

        val realm = realmProvider.get()
        val latestPastSchedule = realm.where(OTTriggerSchedule::class.java)
                .equalTo("trigger.${BackendDbManager.FIELD_OBJECT_ID}", trigger.objectId)
                .sort(OTTriggerSchedule.FIELD_INTRINSIC_ALARM_TIME, Sort.DESCENDING)
                .findAll()
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


    fun calculateNextAlarmTime(pivot: Long?, condition: OTTimeTriggerCondition, triggerId: String): Pair<Long, JsonObject?>? {
        val now = System.currentTimeMillis()

        val limitExclusive = if (condition.isRepeated && condition.endAt != null) {
            condition.endAt!!
        } else Long.MAX_VALUE

        val result = when (condition.timeConditionType) {
            OTTimeTriggerCondition.TIME_CONDITION_ALARM -> {

                val calculator = DesignatedTimeScheduleCalculator().setAlarmTime(condition.alarmTimeHour.toInt(), condition.alarmTimeMinute.toInt(), 0)
                if (condition.isRepeated) {
                    calculator.setAvailableDaysOfWeekFlag(condition.dayOfWeekFlags.toInt())
                            .setEndAt(limitExclusive)
                }

                calculator.calculateNext(pivot, now)
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

                calculator.calculateNext(pivot, now)
            }
            OTTimeTriggerCondition.TIME_CONDITION_SAMPLING -> {
                val builder = ExperienceSamplingTimeScheduleCalculator.Builder()
                        .setMinIntervalMillis(condition.samplingMinIntervalSeconds * TimeHelper.secondsInMilli)
                        .setNumAlerts(condition.samplingCount)
                        .setRandomSeedBase(triggerId)

                if (condition.isRepeated) {
                    builder.setAvailableDaysOfWeekFlag(condition.dayOfWeekFlags.toInt())
                    builder.setEndAt(limitExclusive)
                }

                if (condition.isWholeDayUsed) {
                    builder.setRangeToday(now)
                } else {
                    builder.setRangeWithStartEndHour(condition.samplingHourStart, condition.samplingHourEnd, now)
                }

                builder.build().calculateNext(pivot, now)
            }
            else -> null
        }

        if (result != null) {
            if (result.second == null) {
                result.second = jsonObject("conditionType" to OTTimeTriggerCondition.getConditionCodename(condition.timeConditionType))
            } else {
                result.second?.addProperty("conditionType", OTTimeTriggerCondition.getConditionCodename(condition.timeConditionType))
            }
            return Pair(result.first, result.second)
        } else return null
    }


    //this is a tweak method for Samsung devices.
    override fun rearrangeSystemAlarms() {
        if (OTDeviceStatusService.isBatteryCharging(context) || context.powerManager.isInteractiveCompat) {
            val realm = realmProvider.get()
            val alarms = realm.where(OTTriggerAlarmInstance::class.java)
                    .equalTo(OTTriggerAlarmInstance.FIELD_FIRED, false)
                    .equalTo(OTTriggerAlarmInstance.FIELD_RESERVED_WHEN_DEVICE_ACTIVE, false)
                    .greaterThan("reservedAlarmTime", System.currentTimeMillis())
                    .findAll()

            OTApp.logger.writeSystemLog("rearrange ${alarms.size} trigger alarms.", TAG)

            realm.executeTransaction {
                alarms.forEach { alarm ->
                    registerSystemAlarm(alarm.reservedAlarmTime, alarm.userId!!, alarm.alarmId)
                    alarm.isReservedWhenDeviceActive = true
                }
            }


            realm.close()
        }
    }

}