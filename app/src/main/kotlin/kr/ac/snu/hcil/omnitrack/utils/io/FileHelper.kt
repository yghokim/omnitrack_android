package kr.ac.snu.hcil.omnitrack.utils.io

import com.koushikdutta.async.util.FileUtility
import rx.Single
import java.io.File

/**
 * Created by Young-Ho Kim on 2017-03-07.
 */
object FileHelper {
    fun removeAllFilesIn(dir: File): Single<Boolean> {
        return Single.just(FileUtility.deleteDirectory(dir))
    }
}