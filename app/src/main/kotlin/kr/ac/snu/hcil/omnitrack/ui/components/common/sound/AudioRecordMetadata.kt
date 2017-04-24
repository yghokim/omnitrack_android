package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.net.Uri
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import java.io.File

/**
 * Created by Young-Ho Kim on 2017-04-24.
 */
data class AudioRecordMetadata(var durationMillis:Int, var fileSizeBytes: Long) {
    companion object{
        fun readMetadata(filePath: String): AudioRecordMetadata?
        {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val fileSize = File(filePath).length()
                return AudioRecordMetadata(duration.toInt(), fileSize)
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