package kr.ac.snu.hcil.omnitrack.utils

import android.content.ContextWrapper
import android.content.Intent
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
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