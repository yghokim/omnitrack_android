package kr.ac.snu.hcil.omnitrack.utils

import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.support.v4.app.Fragment
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import java.math.BigDecimal
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */

const val ANDROID_ASSET_PATH = "file:///android_asset"

@TargetApi(23)
fun isInDozeMode(): Boolean {
    val powerManager = OTApp.instance.getSystemService(PowerManager::class.java)
    return powerManager.isDeviceIdleMode
}

fun dipRound(value: Float): Int {
    return (dipSize(value) + 0.5f).toInt()
}

fun dipRound(value: Int): Int {
    return (dipSize(value) + 0.5f).toInt()
}


fun dipSize(value: Float): Float {
    return value * OTApp.instance.resourcesWrapped.displayMetrics.density
}

fun dipSize(value: Int): Float {
    return value * OTApp.instance.resourcesWrapped.displayMetrics.density
}

fun Boolean.toInt(): Int {
    return if (this == true) 1 else 0
}

fun Int.toBoolean(): Boolean {
    return this != 0
}

fun AppCompatActivity.startActivityOnDelay(intent: Intent, delay: Long = 50) {
    val handler = Handler()
    handler.postDelayed(object : Runnable {
        override fun run() {
            startActivity(intent)
        }
    }, delay)

}

fun List<*>.isSame(other: List<*>): Boolean {
    if (this.size == other.size) {
        for (i in 0..this.size - 1) {
            if (this[i] != other[i]) {
                return false
            }
        }
        return true
    } else return false
}

fun Fragment.startActivityOnDelay(intent: Intent, delay: Long = 50) {
    val handler = Handler()
    handler.postDelayed(object : Runnable {
        override fun run() {
            startActivity(intent)
        }
    }, delay)
}

fun View.contains(x: Float, y: Float): Boolean {
    if (x < left || x > right || y < top || y > bottom) {
        return false
    }
    return true
}

fun View.getActivity(): AppCompatActivity? {

    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is AppCompatActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

fun View.setPaddingLeft(padding: Int) {
    setPadding(padding, paddingTop, paddingRight, paddingBottom)
}

fun View.setPaddingRight(padding: Int) {
    setPadding(paddingLeft, paddingTop, padding, paddingBottom)
}

fun View.setPaddingTop(padding: Int) {
    setPadding(paddingLeft, padding, paddingRight, paddingBottom)
}

fun View.setPaddingBottom(padding: Int) {
    setPadding(paddingLeft, paddingTop, paddingRight, padding)
}

fun ViewGroup.inflateMergeLayout(layout: Int) {
    LayoutInflater.from(context).inflate(layout, this, true)
}

fun ViewGroup.inflateContent(layout: Int, attach: Boolean): View {
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    return inflater.inflate(layout, this, attach)
}

fun isNumericPrimitive(value: Any?): Boolean {
    return value is Number || value is BigDecimal
}

fun convertNumericToDouble(value: Any): Double {
    if (value is Int) {
        return value.toDouble()
    } else if (value is Long) {
        return value.toDouble()
    } else if (value is Float) {
        return value.toDouble()
    } else if (value is Double) {
        return value.toDouble()
    } else return value.toString().toDouble()
}


fun toBigDecimal(value: Any): BigDecimal {
    if (value is Int) {
        return BigDecimal(value)
    } else if (value is Long) {
        return BigDecimal(value)
    } else if (value is Double) {
        return BigDecimal(value)
    } else if (value is Float) {
        return BigDecimal(value.toDouble())
    } else if (value is BigDecimal) {
        return value
    } else throw Exception("value is not number primitive.")
}

fun List<*>.move(fromPosition: Int, toPosition: Int): Boolean {
    if (fromPosition != toPosition) {
        if (fromPosition < toPosition) {
            for (i in fromPosition..toPosition - 1) {
                Collections.swap(this, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(this, i, i - 1)
            }
        }

        return true
    }
    return false
}


fun Calendar.getYear(): Int {
    return get(Calendar.YEAR)
}

fun Calendar.getZeroBasedMonth(): Int {
    return get(Calendar.MONTH)
}

fun Calendar.getDayOfMonth(): Int {
    return get(Calendar.DAY_OF_MONTH)
}

fun Calendar.getDayOfWeek(): Int {
    return get(Calendar.DAY_OF_WEEK)
}

fun Calendar.getHour(): Int {
    return get(Calendar.HOUR)
}

fun Calendar.getHourOfDay(): Int {
    return get(Calendar.HOUR_OF_DAY)
}

fun Calendar.getMinute(): Int {
    return get(Calendar.MINUTE)
}

fun Calendar.getSecond(): Int {
    return get(Calendar.SECOND)
}

fun Calendar.getAmPm(): Int {
    return get(Calendar.AM_PM)
}

fun Calendar.setHourOfDay(hour: Int, cutUnder: Boolean = false) {
    set(Calendar.HOUR_OF_DAY, hour)
    if (cutUnder) {
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
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

fun TimePicker.getHourOfDayCompat(): Int {
    if (android.os.Build.VERSION.SDK_INT >= 23) {
        return this.hour
    } else {
        return this.currentHour
    }
}

fun TimePicker.getMinuteCompat(): Int {
    if (android.os.Build.VERSION.SDK_INT >= 23) {
        return this.minute
    } else {
        return this.currentMinute
    }
}

fun TimePicker.setHourOfDayCompat(hourOfDay: Int) {
    if (android.os.Build.VERSION.SDK_INT >= 23) {
        this.hour = hourOfDay
    } else {
        this.currentHour = hourOfDay
    }
}

fun TimePicker.setMinuteCompat(minute: Int) {
    if (android.os.Build.VERSION.SDK_INT >= 23) {
        this.minute = minute
    } else {
        this.currentMinute = minute
    }
}

fun applyTint(drawable: Drawable, color: Int): Drawable {
    val wrapped = DrawableCompat.wrap(drawable).mutate()
    DrawableCompat.setTint(wrapped, color)
    return wrapped
}

val PowerManager.isInteractiveCompat: Boolean get() {

    if (Build.VERSION.SDK_INT <= 19) {
        return this.isScreenOn
    } else {
        return this.isInteractive
    }
}

val KeyguardManager.isDeviceLockedCompat: Boolean get() {
    if (Build.VERSION.SDK_INT < 22) {
        return this.isKeyguardLocked
    } else {
        return this.isDeviceLocked
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