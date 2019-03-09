package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.reactivex.Completable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import java.lang.reflect.Type
import java.util.*


fun Long.toDatetimeString(): String {
    return TimeHelper.FORMAT_ISO_8601.format(Date(this))
}

fun Long.toDateString(): String {
    return TimeHelper.FORMAT_YYYY_MM_DD.format(Date(this))
}


private val hashMapType: Type by lazy { object : TypeToken<HashMap<String, Any>>() {}.type }

fun HashMap<String, Any?>.toJson(gson: Gson): String {
    return gson.toJson(this, hashMapType)
}


fun LatLng.getAddress(context: Context): Address? {

    val geocoder = Geocoder(context)
    try {
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        return addresses?.firstOrNull()
    } catch(e: Exception) {
        e.printStackTrace()

        return null
    }
}

fun <T> BehaviorSubject<T>.onNextIfDifferAndNotNull(i: T?) {
    if (i != null) {
        if (this.hasValue()) {
            if (this.value != i) {
                this.onNext(i)
            }
        } else this.onNext(i)
    }
}

fun Realm.executeTransactionIfNotIn(transaction: (Realm) -> Unit) {
    if (this.isInTransaction) {
        transaction.invoke(this)
    } else {
        this.executeTransaction(transaction)
    }
}

fun Realm.executeTransactionAsObservable(transaction: (Realm) -> Unit): Completable {
    return Completable.create { disposable ->
        val task =
                this.executeTransactionAsync(transaction, {
                    if (!disposable.isDisposed) {
                        disposable.onComplete()
                    }
                }, { err ->
                    if (!disposable.isDisposed) {
                        disposable.onError(err)
                    }
                })

        disposable.setDisposable(Disposables.fromAction { if (!task.isCancelled) task.cancel() })
    }
}

fun JsonObject.getElementCompat(key: String): JsonElement? {
    val value = this[key]
    if (value?.isJsonNull != false) {
        return null
    } else return value
}

fun JsonObject.getStringCompat(key: String): String? {
    val element = getElementCompat(key)
    return try {
        element?.asString
    } catch (ex: UnsupportedOperationException) {
        element?.toString()
    }
}

fun JsonObject.getBooleanCompat(key: String): Boolean? {
    return getElementCompat(key)?.asBoolean
}

fun JsonObject.getIntCompat(key: String): Int? {
    return getElementCompat(key)?.asInt
}

fun JsonObject.getLongCompat(key: String): Long? {
    return getElementCompat(key)?.asLong
}

fun argbIntToCssString(color: Int): String {
    return "rgba(${Color.red(color)}, ${Color.green(color)}, ${Color.blue(color)}, ${Color.alpha(color).toFloat() / 255})"
}