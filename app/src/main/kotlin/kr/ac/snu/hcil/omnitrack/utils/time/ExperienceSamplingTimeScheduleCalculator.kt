package kr.ac.snu.hcil.omnitrack.utils.time

import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.utils.getDayOfWeek
import kr.ac.snu.hcil.omnitrack.utils.getHourOfDay
import kr.ac.snu.hcil.omnitrack.utils.getYear
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class ExperienceSamplingTimeScheduleCalculator(
        /**
         * a random seed globally assigned to calculation.
         */
        var randomSeedBase: String = "",
        var numAlerts: Short = 0,
        var rangeStart: Long = System.currentTimeMillis(),
        var rangeLength: Long = TimeHelper.daysInMilli,
        var minIntervalMillis: Long = 10 * TimeHelper.minutesInMilli
) : TimeScheduleCalculator<ExperienceSamplingTimeScheduleCalculator>() {
    class Builder {

        private var randomSeedBase: String = ""
        private var numAlerts: Short = 0
        private var rangeStart: Long = System.currentTimeMillis()
        private var rangeLength: Long = TimeHelper.daysInMilli
        private var minIntervalMillis: Long = 10 * TimeHelper.minutesInMilli

        private var availableDaysOfWeek: Int = 0b1111111
        private var endAt: Long = Long.MAX_VALUE

        fun setRandomSeedBase(base: String): Builder {
            this.randomSeedBase = base
            return this
        }

        fun setNumAlerts(numAlerts: Short): Builder {
            this.numAlerts = numAlerts
            return this
        }

        fun setRangeStart(rangeStart: Long): Builder {
            this.rangeStart = rangeStart
            return this
        }

        fun setRangeLength(rangeLength: Long): Builder {
            this.rangeLength = rangeLength
            return this
        }

        fun setMinIntervalMillis(minIntervalMillis: Long): Builder {
            this.minIntervalMillis = minIntervalMillis
            return this
        }

        fun setRangeToday(now: Long = System.currentTimeMillis()): Builder {
            rangeStart = TimeHelper.getTodayRange(now).first
            rangeLength = TimeHelper.daysInMilli
            return this
        }

        fun setAvailableDaysOfWeekFlag(flag: Int): Builder {
            this.availableDaysOfWeek = flag
            return this
        }

        fun setEndAt(endAt: Long): Builder {
            this.endAt = endAt
            return this
        }

        fun setRangeWithStartEndHour(samplingHourStart: Byte, samplingHourEnd: Byte, now: Long = System.currentTimeMillis()): Builder {
            val nowCal = GregorianCalendar.getInstance().apply { timeInMillis = now }
            val hour = nowCal.getHourOfDay()
            val durationLengthInHour = if (samplingHourEnd < samplingHourStart) {
                24 - samplingHourStart + samplingHourEnd
            } else samplingHourEnd - samplingHourStart

            rangeLength = durationLengthInHour * TimeHelper.hoursInMilli

            val startCal = Calendar.getInstance().apply { timeInMillis = now }
            startCal.set(Calendar.HOUR_OF_DAY, samplingHourStart.toInt())
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)
            val rangeStartToday = startCal.timeInMillis
            val rangeEndToday = startCal.timeInMillis + durationLengthInHour * TimeHelper.hoursInMilli
            rangeStart = if (hour < rangeEndToday) {
                if (rangeStartToday <= hour) {
                    rangeStartToday
                } else rangeStartToday - TimeHelper.daysInMilli
            } else {
                rangeStartToday + TimeHelper.daysInMilli
            }
            return this
        }

        fun build(): ExperienceSamplingTimeScheduleCalculator {
            return ExperienceSamplingTimeScheduleCalculator(randomSeedBase, numAlerts, rangeStart, rangeLength, minIntervalMillis)
                    .apply {
                        setAvailableDaysOfWeekFlag(availableDaysOfWeek)
                        setEndAt(endAt)
                    }
        }
    }

    companion object {

        const val TAG = "ExperienceSamplingTimeScheduleCalculator"

        /**
        This random calculation code was inspired by AWARE framework.
        https://github.com/denzilferreira/aware-client/blob/dde8eaef284bb1502184dd9ce42c97ebd1feabad/aware-core/src/main/java/com/aware/utils/Scheduler.java
         */
        fun generateRandomPoints(
                rangeStart: Long,
                randomSeedBase: String,
                numAlerts: Short,
                rangeLength: Long,
                minIntervalMillis: Long): LongArray {
            val startCalendar = GregorianCalendar.getInstance().apply { timeInMillis = rangeStart }
            val randomSeed = String.format("%s-%d-%d", randomSeedBase, startCalendar.getYear(), startCalendar.get(Calendar.DAY_OF_YEAR))
            val randomSeedInt: Int = try {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(randomSeed.toByteArray(Charsets.UTF_8))
                val digest = md.digest().map { it.toInt() }
                ((((digest[0] shl 8 + digest[1] shl 8 + digest[2] shl 8 + digest[3] shl 8) + digest[4] shl 8) + digest[5] shl 8) + digest[6] shl 8) + digest[7]
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                123213
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                123213
            }

            val random = Random(randomSeedInt.toLong())
            val randomRange = rangeLength - (numAlerts - 1) * minIntervalMillis

            val randomPoints = LongArray(numAlerts.toInt(), {
                rangeStart + (random.nextDouble() * randomRange).toLong()
            }).sorted().mapIndexed { i, p ->
                p + i * minIntervalMillis
            }

            randomPoints.forEach {
                println(TimeHelper.FORMAT_DATETIME.format(Date(it)))
            }

            return randomPoints.toLongArray()
        }

    }



    override fun calculateInfiniteNextTime(last: Long?, now: Long): Long? {
        return calculateInfiniteNextTimeInfo(last, now)?.first
    }

    fun calculateInfiniteNextTimeInfo(last: Long?, now: Long): Triple<Long, Int, Long>? {
        val cacheCal = GregorianCalendar.getInstance()
        var finalRangeStart = rangeStart

        var currentRandomPoints = generateRandomPoints(rangeStart, randomSeedBase, numAlerts, rangeLength, minIntervalMillis)
        if (currentRandomPoints.find { it > now && (last == null || it > last) } == null) {
            finalRangeStart += TimeHelper.daysInMilli
        }

        cacheCal.timeInMillis = finalRangeStart

        while (!isAvailableDayOfWeek(cacheCal.getDayOfWeek())) {
            finalRangeStart += TimeHelper.daysInMilli
            cacheCal.timeInMillis = finalRangeStart
        }

        if (rangeStart < finalRangeStart) {
            currentRandomPoints = generateRandomPoints(finalRangeStart, randomSeedBase, numAlerts, rangeLength, minIntervalMillis)
        }

        var result: Triple<Long, Int, Long>? = null

        for (i in currentRandomPoints.indices) {
            if (currentRandomPoints[i] > now && (last == null || currentRandomPoints[i] > last)) {
                result = Triple(currentRandomPoints[i], i, finalRangeStart)
                break
            } else continue
        }

        OTApp.logger.writeSystemLog("EMA trigger calculation: Last: ${last?.let { TimeHelper.FORMAT_DATETIME.format(Date(it)) }
                ?: "null"}, now: ${TimeHelper.FORMAT_DATETIME.format(Date(now))} " +
                "EMA trigger timestamp list: \n" + currentRandomPoints.map { TimeHelper.FORMAT_DATETIME.format(Date(it)) }.joinToString("\n") + "\npicked timestamp: " + (result?.second
                ?: "Null"), TAG)

        if (result == null) {

        }

        return result
    }

    override fun calculateNext(last: Long?, now: Long): Long? {
        val result = calculateInfiniteNextTimeInfo(last, now)
        if (result == null) return null
        else {
            if (result.third > endAt) {
                return null
            } else return result.first
        }
    }
}
