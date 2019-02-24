package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.util.*

/**
 * Created by Young-Ho Kim on 2017-04-24.
 */
data class AudioRecordMetadata(var durationMillis: Int, var fileSizeBytes: Long, var recordedAt: String) {
    companion object {

        fun parseDefaultFormat(dateString: String): Long {
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    val parsed = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSX").parse(dateString)
                    val date = ZonedDateTime.of(
                            parsed.get(ChronoField.YEAR),
                            parsed.get(ChronoField.MONTH_OF_YEAR),
                            parsed.get(ChronoField.DAY_OF_MONTH),
                            parsed.get(ChronoField.HOUR_OF_DAY),
                            parsed.get(ChronoField.MINUTE_OF_HOUR),
                            parsed.get(ChronoField.SECOND_OF_MINUTE),
                            0,
                            ZoneId.from(parsed))
                    return date.toEpochSecond() * 1000
                } catch (ex: DateTimeParseException) {
                    throw ParseException(ex.parsedString, ex.errorIndex)
                }
            } else {
                val parsed = SimpleDateFormat("yyyyMMdd'T'HHmmss.SSSX", Locale.ROOT).parse(dateString)
                return parsed.time
            }
        }

        fun parseFormatWithoutTimeZone(dateString: String, format: String): Long {
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    val parsed = DateTimeFormatter.ofPattern(format).parse(dateString)
                    val date = ZonedDateTime.of(
                            parsed.get(ChronoField.YEAR),
                            parsed.get(ChronoField.MONTH_OF_YEAR),
                            parsed.get(ChronoField.DAY_OF_MONTH), 0, 0, 0, 0, ZoneOffset.UTC)

                    return date.toEpochSecond() * 1000
                } catch (ex: DateTimeParseException) {
                    throw ParseException(ex.parsedString, ex.errorIndex)
                }
            } else {
                val parsed = SimpleDateFormat(format, Locale.ROOT).parse(dateString)
                return parsed.time
            }
        }

        fun readMetadata(filePath: String, context: Context): AudioRecordMetadata? {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val dateString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)

                val localTimeFormats = (context.applicationContext as OTAndroidApp).applicationComponent.getLocalTimeFormats()

                val parsedDateString = try {
                    localTimeFormats.FORMAT_DATETIME.format(Date(parseDefaultFormat(dateString)))
                } catch (ex: ParseException) {
                    localTimeFormats.FORMAT_DAY.format(Date(parseFormatWithoutTimeZone(dateString, "yyyy MM dd")))
                } catch (ex: ParseException) {
                    localTimeFormats.FORMAT_DAY.format(Date(parseFormatWithoutTimeZone(dateString, "dd MMMM yyyy")))
                }

                val fileSize = File(filePath).length()
                retriever.release()
                return AudioRecordMetadata(duration.toInt(), fileSize, parsedDateString)
            } catch (ex: Exception) {
                println("Metadata extraction failed:")
                ex.printStackTrace()
                return null
            }
        }
    }
}