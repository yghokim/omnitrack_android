package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.media.MediaMetadataRetriever
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import org.threeten.bp.temporal.ChronoField
import java.io.File
import java.util.*

/**
 * Created by Young-Ho Kim on 2017-04-24.
 */
data class AudioRecordMetadata(var durationMillis: Int, var fileSizeBytes: Long, var recordedAt: String) {
    companion object{

        val formatterISO8601: DateTimeFormatter by lazy {
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSX")
        }

        val formatterDate: DateTimeFormatter by lazy {
            DateTimeFormatter.ofPattern("yyyy MM dd")
        }

        val formatterDate2: DateTimeFormatter by lazy {
            DateTimeFormatter.ofPattern("dd MMMM yyyy")
        }

        fun readMetadata(filePath: String): AudioRecordMetadata?
        {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val dateString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                val parsedDateString = try {
                    val parsed = formatterISO8601.parse(dateString)
                    val date = ZonedDateTime.of(
                            parsed.get(ChronoField.YEAR),
                            parsed.get(ChronoField.MONTH_OF_YEAR),
                            parsed.get(ChronoField.DAY_OF_MONTH),
                            parsed.get(ChronoField.HOUR_OF_DAY),
                            parsed.get(ChronoField.MINUTE_OF_HOUR),
                            parsed.get(ChronoField.SECOND_OF_MINUTE),
                            0,
                            ZoneId.from(parsed))
                    val timestamp = date.toEpochSecond() * 1000
                    TimeHelper.FORMAT_DATETIME.format(Date(timestamp))
                } catch(ex: DateTimeParseException) {
                    ex.printStackTrace()
                    try {
                        val parsed = formatterDate.parse(dateString)
                        val date = ZonedDateTime.of(
                                parsed.get(ChronoField.YEAR),
                                parsed.get(ChronoField.MONTH_OF_YEAR),
                                parsed.get(ChronoField.DAY_OF_MONTH), 0, 0, 0, 0, ZoneOffset.UTC)

                        val timestamp = date.toEpochSecond() * 1000
                        TimeHelper.FORMAT_DAY.format(Date(timestamp))
                    } catch(ex: DateTimeParseException) {
                        val parsed = formatterDate2.parse(dateString)

                        val date = ZonedDateTime.of(
                                parsed.get(ChronoField.YEAR),
                                parsed.get(ChronoField.MONTH_OF_YEAR),
                                parsed.get(ChronoField.DAY_OF_MONTH), 0, 0, 0, 0, ZoneOffset.UTC)

                        val timestamp = date.toEpochSecond() * 1000
                        TimeHelper.FORMAT_DAY.format(Date(timestamp))
                    }
                }

                val fileSize = File(filePath).length()
                retriever.release()
                return AudioRecordMetadata(duration.toInt(), fileSize, parsedDateString)
            }
            catch(ex: Exception)
            {
                println("Metadata extraction failed:")
                ex.printStackTrace()
                return null
            }
        }
    }
}