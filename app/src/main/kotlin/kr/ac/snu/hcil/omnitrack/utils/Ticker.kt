package kr.ac.snu.hcil.omnitrack.utils

import android.os.Handler
import android.os.SystemClock
import kr.ac.snu.hcil.omnitrack.utils.events.Event

/**
 * Created by Young-Ho Kim on 2016-08-30.
 */
//TODO remove this class
class Ticker(var unit: Int = 1000) {

    val handler: Handler
    val ticker: Runnable

    var tickerStopped = true
        private set

    val tick = Event<Long>()

    init {
        handler = Handler()
        ticker = object : Runnable {
            override fun run() {
                if (tickerStopped) return
                onTick()
            }
        }
    }

    private fun onTick() {
        tick.invoke(this@Ticker, System.currentTimeMillis())
        val uptime = SystemClock.uptimeMillis()
        val next: Long = uptime + (unit - uptime % unit)
        handler.postAtTime(ticker, next)
    }

    fun start() {
        if (tickerStopped) {
            tickerStopped = false
            ticker.run()
        }
    }

    fun stop() {
        tickerStopped = true
        handler.removeCallbacksAndMessages(null)
    }


}