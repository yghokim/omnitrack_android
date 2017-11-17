package kr.ac.snu.hcil.omnitrack.core.datatypes

import android.content.Context
import android.net.Uri
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper

/**
 * Created by younghokim on 2017. 11. 17..
 */
class OTServerFile {
    class OTServerFileTypeAdapter : TypeAdapter<OTServerFile>() {
        override fun write(out: JsonWriter, value: OTServerFile) {
            out.beginObject()
            out.name("path").value(value.serverPath)
            out.name("size").value(value.fileSize)
            out.name("mime").value(value.mimeType)
            out.name("origName").value(value.originalFileName)
            out.endObject()
        }

        override fun read(reader: JsonReader): OTServerFile {
            val result = OTServerFile()
            readImpl(reader, result)
            return result
        }

        fun readImpl(reader: JsonReader, out: OTServerFile) {
            reader.beginObject()
            while (reader.hasNext()) {
                val nextName = reader.nextName()
                if (reader.peek() != JsonToken.NULL) {
                    when (nextName) {
                        "path" -> out.serverPath = reader.nextString()
                        "size" -> out.fileSize = reader.nextLong()
                        "mime" -> out.mimeType = reader.nextString()
                        "origName" -> out.originalFileName = reader.nextString()
                        else -> reader.skipValue()
                    }
                } else reader.skipValue()
            }
            reader.endObject()
        }
    }

    companion object {
        val typeAdapter: OTServerFileTypeAdapter by lazy {
            OTServerFileTypeAdapter()
        }

        fun fromLocalFile(serverPath: String, localUri: Uri, context: Context): OTServerFile {
            val result = OTServerFile()
            result.serverPath = serverPath
            result.originalFileName = localUri.lastPathSegment
            result.fileSize = FileHelper.getFileSizeOf(localUri, context)
            result.mimeType = FileHelper.getMimeTypeOf(localUri, context) ?: "*/*"
            return result
        }
    }

    fun getSerializedString(): String {
        return typeAdapter.toJson(this)
    }

    var serverPath: String = ""
    var fileSize: Long = 0
    var mimeType: String = "*/*"
    var originalFileName: String = ""

    override fun equals(other: Any?): Boolean {
        return if (other is OTServerFile) {
            serverPath.equals(other.serverPath)
                    && fileSize == other.fileSize
                    && mimeType == other.mimeType
                    && originalFileName == other.originalFileName
        } else false
    }

    override fun toString(): String {
        return typeAdapter.toJson(this)
    }

    override fun hashCode(): Int {
        var result = serverPath.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + originalFileName.hashCode()
        return result
    }
}