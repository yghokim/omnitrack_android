package kr.ac.snu.hcil.omnitrack.utils.serialization

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Created by Young-Ho Kim on 2016-07-26.
 */

data class SerializedKeyEntry<out T>(val key: T, val value: String)

data class SerializedIntegerKeyEntry(val key: Int, val value: String)

data class SerializedStringKeyEntry(val key: String, val value: String)


val integerKeyEntryParser: Gson by lazy { GsonBuilder().registerTypeAdapter(SerializedIntegerKeyEntry::class.java, SerializedIntegerKeyEntryTypeAdapter()).create() }

class SerializedIntegerKeyEntryTypeAdapter : TypeAdapter<SerializedIntegerKeyEntry>() {
    override fun read(input: JsonReader): SerializedIntegerKeyEntry {

        input.beginObject()

        input.nextName()
        val key: Int = input.nextInt()
        input.nextName()
        val value: String = input.nextString()


        input.endObject()

        return SerializedIntegerKeyEntry(key, value)
    }

    override fun write(out: JsonWriter, value: SerializedIntegerKeyEntry) {
        out.beginObject()
        out.name("k").value(value.key)
        out.name("v").value(value.value)
        out.endObject()
    }
}

val stringKeyEntryParser: Gson by lazy { GsonBuilder().registerTypeAdapter(SerializedStringKeyEntry::class.java, SerializedStringKeyEntryTypeAdapter()).create() }

class SerializedStringKeyEntryTypeAdapter : TypeAdapter<SerializedStringKeyEntry>() {
    override fun read(input: JsonReader): SerializedStringKeyEntry {

        input.beginObject()

        input.nextName()
        val key: String = input.nextString()
        input.nextName()
        val value: String = input.nextString()


        input.endObject()

        return SerializedStringKeyEntry(key, value)
    }

    override fun write(out: JsonWriter, value: SerializedStringKeyEntry) {
        out.beginObject()
        out.name("k").value(value.key)
        out.name("v").value(value.value)
        out.endObject()
    }
}