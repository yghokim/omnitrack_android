package kr.ac.snu.hcil.omnitrack.core.database

import android.net.Uri
import android.webkit.URLUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.File

/**
 * Created by Young-Ho on 3/4/2017.
 */
class SynchronizedUri {

    class JsonTypeAdapter : TypeAdapter<SynchronizedUri>() {
        override fun read(reader: JsonReader): SynchronizedUri {
            reader.beginObject()
            reader.nextName()
            val localUri = reader.nextString()
            reader.nextName()
            val serverUri = reader.nextString()
            reader.endObject()

            return SynchronizedUri(Uri.parse(localUri), Uri.parse(serverUri))
        }

        override fun write(out: JsonWriter, value: SynchronizedUri) {
            out.beginObject()

            out.name("local").value(value.localUri.toString())
            out.name("server").value(value.serverUri.toString())

            out.endObject()
        }
    }

    companion object {
        val parser: Gson by lazy {
            GsonBuilder().registerTypeAdapter(SynchronizedUri::class.java, JsonTypeAdapter()).create()
        }
    }

    var localUri: Uri = Uri.EMPTY
        private set

    var serverUri: Uri = Uri.EMPTY
        private set

    val isEmpty: Boolean get() = localUri == Uri.EMPTY && serverUri == Uri.EMPTY

    val isSynchronized: Boolean get() = Uri.EMPTY != serverUri

    val isLocalUriValid: Boolean get() {
        return if (Uri.EMPTY != localUri) {
            if (URLUtil.isFileUrl(localUri.toString())) {
                File(localUri.path).exists()
            } else false
        } else false
    }

    val primaryUri: Uri get() = if (Uri.EMPTY != localUri) {
        if (URLUtil.isFileUrl(localUri.toString())) {
            if (File(localUri.path).exists()) {
                localUri
            } else serverUri
        } else localUri
    } else serverUri

    constructor() {

    }

    constructor(localUri: Uri, serverUri: Uri = Uri.EMPTY) {
        this.localUri = localUri
        this.serverUri = serverUri
    }


    fun setSynchronized(serverUri: Uri) {
        this.serverUri = serverUri
    }

    override fun equals(other: Any?): Boolean {
        if (other is SynchronizedUri) {
            return localUri.toString() == other.localUri.toString() && serverUri.toString() == other.serverUri.toString()
        } else return false
    }


}