package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.android.gms.maps.model.LatLng
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */

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

fun LatLng.getAddress(context: Context): Address? {

    val geocoder = Geocoder(context)
    try {
        // 세번째 인수는 최대결과값인데 하나만 리턴받도록 설정했다
        val addresses = geocoder.getFromLocation(latitude, longitude, 1);
        // 설정한 데이터로 주소가 리턴된 데이터가 있으면
        return addresses?.firstOrNull()
    } catch(e: Exception) {
        e.printStackTrace()

        return null
    }
}