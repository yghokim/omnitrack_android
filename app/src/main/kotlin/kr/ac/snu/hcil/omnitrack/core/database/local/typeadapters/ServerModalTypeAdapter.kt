package kr.ac.snu.hcil.omnitrack.core.database.local.typeadapters

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Created by younghokim on 2017. 11. 6..
 */
abstract class ServerModalTypeAdapter<T>(val isServerMode: Boolean) : TypeAdapter<T>() {
    override final fun read(reader: JsonReader): T {
        return read(reader, isServerMode)
    }

    override final fun write(out: JsonWriter, value: T) {
        write(out, isServerMode)
    }

    abstract fun read(reader: JsonReader, isServerMode: Boolean): T
    abstract fun write(writer: JsonWriter, isServerMode: Boolean)
}