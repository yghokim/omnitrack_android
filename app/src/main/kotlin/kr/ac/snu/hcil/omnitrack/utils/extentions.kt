package kr.ac.snu.hcil.omnitrack.utils

import android.content.Intent
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity

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