package kr.ac.snu.hcil.omnitrack.utils.io

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by Young-Ho Kim on 2017-03-07.
 */
object FileHelper {

    fun deleteDirectory(path: File): Boolean {
        if (path.exists()) {
            val files = path.listFiles()
            if (files != null) {
                for (i in files.indices) {
                    if (files[i].isDirectory) {
                        deleteDirectory(files[i])
                    } else {
                        files[i].delete()
                    }
                }
            }
        }
        return path.delete()
    }

    fun exists(path: String): Boolean {
        try {
            val file = File(Uri.parse(path).path)
            return file.exists()
        } catch (ex: Exception) {
            return false
        }
    }

    fun makeSaveLocationPickIntent(filename: String): Intent {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "application/zip", "text/comma-separated-values", "text/plain"))
        intent.putExtra(Intent.EXTRA_TITLE, filename)

        return intent
    }

    fun dumpStreamToOther(inputStream: InputStream, outputStream: OutputStream) {
        val buf = ByteArray(1024)
        var len = inputStream.read(buf)
        while (len > 0) {
            outputStream.write(buf, 0, len)
            len = inputStream.read(buf)
        }
    }

    fun getMimeTypeOf(uri: Uri, context: Context): String? {
        if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            return context.contentResolver.getType(uri)
        } else {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
        }
    }
}