package kr.ac.snu.hcil.omnitrack.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Handler
import android.os.PowerManager
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.OTApplication
import java.math.BigDecimal
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */

@TargetApi(23)
fun isInDozeMode(): Boolean {
    val powerManager = OTApplication.app.getSystemService(PowerManager::class.java)
    return powerManager.isDeviceIdleMode
}

fun Boolean.toInt(): Int{
    return if(this==true) 1 else 0
}

fun Int.toBoolean(): Boolean{
    return if(this==0) false else true
}

fun AppCompatActivity.startActivityOnDelay(intent: Intent, delay: Long = 50) {
    val handler = Handler()
    handler.postDelayed(object : Runnable {
        override fun run() {
            startActivity(intent)
        }
    }, delay)

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

fun ViewGroup.inflateContent(layout: Int, attach: Boolean): View {
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    return inflater.inflate(layout, this, attach)
}

fun isNumericPrimitive(value: Any): Boolean {
    return value is Int || value is Float || value is Double || value is Long
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
        val addresses = geocoder.getFromLocation(latitude, longitude, 1);
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



