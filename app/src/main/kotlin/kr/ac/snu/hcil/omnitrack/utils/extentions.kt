package kr.ac.snu.hcil.omnitrack.utils

import android.content.ContextWrapper
import android.content.Intent
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View

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