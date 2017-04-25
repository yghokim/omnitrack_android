package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.media.MediaMetadataRetriever
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoField
import java.io.File

/**
 * Created by Young-Ho Kim on 2017-04-24.
 */
data class AudioRecordMetadata(var durationMillis: Int, var fileSizeBytes: Long, var recordedAt: Long) {
    companion object{

        val formatter: DateTimeFormatter by lazy {
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSX")
        }

        fun readMetadata(filePath: String): AudioRecordMetadata?
        {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val dateString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                val parsed = formatter.parse(dateString)

                val date = ZonedDateTime.of(
                        parsed.get(ChronoField.YEAR),
                        parsed.get(ChronoField.MONTH_OF_YEAR),
                        parsed.get(ChronoField.DAY_OF_MONTH),
                        parsed.get(ChronoField.HOUR_OF_DAY),
                        parsed.get(ChronoField.MINUTE_OF_HOUR),
                        parsed.get(ChronoField.SECOND_OF_MINUTE),
                        0,
                        ZoneId.from(parsed))

                val fileSize = File(filePath).length()
                retriever.release()
                return AudioRecordMetadata(duration.toInt(), fileSize, date.toEpochSecond() * 1000)
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