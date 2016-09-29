package kr.ac.snu.hcil.omnitrack.ui.components.common.sound

import kr.ac.snu.hcil.omnitrack.utils.Ticker

/**
 * Created by Young-Ho Kim on 2016-09-29.
 */
abstract class AAudioModule(val progressTerm: Int = 200) {

    private val ticker: Ticker

    var startedAt: Long = 0
        private set


    init {
        ticker = Ticker(progressTerm)

        ticker.tick += {
            sender, time ->
            onTick(time)
        }
    }

    abstract fun getCurrentProgressRatio(now: Long): Float
    abstract fun getCurrentProgressDuration(now: Long): Int

    protected abstract fun onTick(time: Long)

    protected abstract fun onStart()

    protected abstract fun onStop(cancel: Boolean = false)

    abstract fun isRunning(): Boolean

    fun startAsync() {
        startedAt = System.currentTimeMillis()
        onStart()

        ticker.start()
    }

    fun stop() {
        onStop(false)

        ticker.stop()
    }

    fun cancel() {
        onStop(true)
        ticker.stop()
    }


}