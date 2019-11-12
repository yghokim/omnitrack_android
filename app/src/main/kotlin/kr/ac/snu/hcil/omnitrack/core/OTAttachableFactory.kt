package kr.ac.snu.hcil.omnitrack.core

import android.content.Context
import android.content.pm.PackageManager
import android.text.Html
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Lazy
import kr.ac.snu.hcil.android.common.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.OTAndroidApp

abstract class OTAttachableFactory<AttachableType : OTAttachableFactory.OTAttachable>(val context: Context, val factoryTypeName: String) : INameDescriptionResourceProvider {

    abstract val typeCode: String

    open val requiredPermissions: Array<String> = arrayOf()

    open val isPermissionGranted: Boolean
        get() {
            for (permission in requiredPermissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            return true
        }

    open val unGrantedPermissions: List<String>
        get(){
            return requiredPermissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
        }


    fun getFormattedName(): CharSequence {
        val html = onMakeFormattedName()
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    abstract fun getCategoryName(): String

    protected open fun onMakeFormattedName(): String {
        return "<b>${context.resources.getString(nameResourceId)}</b>"
    }

    abstract fun makeAttachable(arguments: JsonObject? = null): AttachableType

    fun makeAttachable(serializedArguments: String?): AttachableType {
        if (serializedArguments == null || serializedArguments == "null") {
            return makeAttachable(null as JsonObject?)
        } else {
            val args = (context.applicationContext as OTAndroidApp).applicationComponent.genericGson().fromJson(serializedArguments, JsonObject::class.java)
            return makeAttachable(args)
        }
    }


    abstract class OTAttachable(private val factory: OTAttachableFactory<out OTAttachable>, val arguments: JsonObject?) {

        open val factoryCode: String get() = this.factory.typeCode

        open fun getFormattedName(): CharSequence {
            return factory.getFormattedName()
        }

        fun <T : OTAttachableFactory<out OTAttachable>> getFactory(): T {
            @Suppress("UNCHECKED_CAST")
            return factory as T
        }
    }

    class OTAttachableTypeAdapter<AttachableType : OTAttachable>(val gson: Lazy<Gson>, val attachableCreationFunc: (factoryCode: String, arguments: JsonObject?) -> AttachableType?) : TypeAdapter<AttachableType>() {


        override fun read(reader: JsonReader): AttachableType? {
            if (reader.peek() == JsonToken.NULL) {
                reader.skipValue()
                return null
            } else {
                var factoryCode: String? = null
                var arguments: JsonObject? = null

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "code" -> {
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull()
                            } else {
                                factoryCode = reader.nextString()
                            }
                        }
                        "args" -> {
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull()
                            } else {
                                arguments = gson.get().fromJson(reader, JsonObject::class.java)
                            }
                        }
                    }
                }
                reader.endObject()

                return factoryCode?.let {
                    attachableCreationFunc(it, arguments)
                }
            }
        }

        override fun write(out: JsonWriter, value: AttachableType?) {
            if(value!=null) {
                out.beginObject()
                        .name("code")
                        .value(value.factoryCode)
                        .name("args")
                        .jsonValue(value.arguments.toString())
                        .endObject()
            }else out.nullValue()
        }
    }
}