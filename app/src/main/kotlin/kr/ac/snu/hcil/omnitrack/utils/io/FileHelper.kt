package kr.ac.snu.hcil.omnitrack.utils.io

import android.content.Intent
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

    fun makeSaveLocationPickIntent(filename: String): Intent {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("*/*")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "application/zip"))
        intent.putExtra(Intent.EXTRA_TITLE, filename)

        return intent
    }
}