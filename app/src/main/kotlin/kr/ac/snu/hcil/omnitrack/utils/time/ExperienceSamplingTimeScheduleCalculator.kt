package kr.ac.snu.hcil.omnitrack.utils.time

import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import kr.ac.snu.hcil.android.common.containers.WritablePair
import kr.ac.snu.hcil.android.common.time.getDayOfWeek
import kr.ac.snu.hcil.android.common.time.getHourOfDay
import kr.ac.snu.hcil.android.common.time.getYear
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.utils.toDatetimeString
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
) : TimeScheduleCalculator() {

    private data class CalculationResult(val alertTime: Long, val pingIndex: Int, val rangeStart: Long, val rangeDateString: String) {
        fun toMetadata(): JsonObject {
            return jsonObject(
                    "pingIndex" to pingIndex,
                    "pivotDate" to rangeDateString
            )
        }
    }

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
                println(it.toDatetimeString())
            }

            return randomPoints.toLongArray()
        }

    }


    override fun calculateInfiniteNextTime(last: Long?, now: Long): WritablePair<Long, JsonObject?>? {
        return calculateInfiniteNextTimeInfo(last, now)?.let {
            WritablePair(it.alertTime, it.toMetadata())
        }
    }

    private fun calculateInfiniteNextTimeInfo(last: Long?, now: Long): CalculationResult? {
        val cacheCal = GregorianCalendar.getInstance()
        var finalRangeStart = rangeStart

        var currentRandomPoints = generateRandomPoints(rangeStart, randomSeedBase, numAlerts, rangeLength, minIntervalMillis)
        if (currentRandomPoints.find { it > now && (last == null || it > last) } == null) {
            finalRangeStart += TimeHelper.daysInMilli
        }

        cacheCal.timeInMillis = finalRangeStart
        var result: CalculationResult? = null

        //we can get into a situation where we are at near end of the day, and the last random point is earlier than the time.

        do {
            while (!isAvailableDayOfWeek(cacheCal.getDayOfWeek())) {
                cacheCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            finalRangeStart = cacheCal.timeInMillis

            if (rangeStart < finalRangeStart) {
                currentRandomPoints = generateRandomPoints(finalRangeStart, randomSeedBase, numAlerts, rangeLength, minIntervalMillis)
            }


            for (i in currentRandomPoints.indices) {
                if (currentRandomPoints[i] > now && (last == null || currentRandomPoints[i] > last)) {
                    result = CalculationResult(currentRandomPoints[i], i, finalRangeStart, TimeHelper.FORMAT_YYYY_MM_DD.format(Date(finalRangeStart)))
                    break
                } else continue
            }

            cacheCal.add(Calendar.DAY_OF_YEAR, 1)
        } while (result == null)

        OTApp.logger.writeSystemLog("EMA trigger calculation: Last: ${last?.toDatetimeString()
                ?: "null"}, now: ${now.toDatetimeString()} " +
                "EMA trigger timestamp list: \n" + currentRandomPoints.joinToString("\n") { it.toDatetimeString() } + "\npicked timestamp: " + result.pingIndex, TAG)

        return result
    }

    override fun calculateNext(last: Long?, now: Long): WritablePair<Long, JsonObject?>? {
        val result = calculateInfiniteNextTimeInfo(last, now)
        if (result == null) return null
        else {
            if (result.rangeStart > endAt) {
                return null
            } else return WritablePair(result.alertTime, result.toMetadata())
        }
    }
}
