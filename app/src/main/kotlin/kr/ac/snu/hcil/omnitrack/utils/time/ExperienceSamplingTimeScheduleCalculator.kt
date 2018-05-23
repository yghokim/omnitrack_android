package kr.ac.snu.hcil.omnitrack.utils.time

import kr.ac.snu.hcil.omnitrack.utils.getDayOfWeek
import kr.ac.snu.hcil.omnitrack.utils.getYear
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class ExperienceSamplingTimeScheduleCalculator : TimeScheduleCalculator<ExperienceSamplingTimeScheduleCalculator>() {

    companion object {

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

    /**
     * a random seed globally assigned to calculation.
     */
    var randomSeedBase: String = ""
    var numAlerts: Short = 0
    var rangeStart: Long = 0
    var rangeLength = TimeHelper.daysInMilli
    var minIntervalMillis: Long = 10 * TimeHelper.minutesInMilli

    override fun calculateInfiniteNextTime(last: Long?, now: Long): Long? {
        return calculateInfiniteNextTimeInfo(last, now)?.first
    }

    fun calculateInfiniteNextTimeInfo(last: Long?, now: Long): Triple<Long, Int, Long>? {
        val cacheCal = GregorianCalendar.getInstance()
        var finalRangeStart = rangeStart

        var currentRandomPoints = generateRandomPoints(rangeStart, randomSeedBase, numAlerts, rangeLength, minIntervalMillis)
        if (currentRandomPoints.find { it > now } == null) {
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

        for (i in currentRandomPoints.indices) {
            if (currentRandomPoints[i] > now) {
                return Triple(currentRandomPoints[i], i, finalRangeStart)
            }
        }
        return null
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
