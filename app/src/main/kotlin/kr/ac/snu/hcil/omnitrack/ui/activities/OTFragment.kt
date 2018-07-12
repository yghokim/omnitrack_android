package kr.ac.snu.hcil.omnitrack.ui.activities

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.support.v4.app.Fragment
import com.github.salomonbrys.kotson.set
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import org.jetbrains.anko.support.v4.act
import javax.inject.Inject

/**
 * Created by younghokim on 2016. 11. 15..
 */
open class OTFragment : Fragment(), LifecycleObserver {

    protected val startSubscriptions = CompositeDisposable()
    protected val creationSubscriptions = CompositeDisposable()
    protected val createViewSubscriptions = CompositeDisposable()

    @Inject
    protected lateinit var eventLogger: Lazy<IEventLogger>

    @Inject
    protected lateinit var configuredContext: ConfiguredContext

    private var shownAt: Long? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onInject((context.applicationContext as OTApp).currentConfiguredContext)
    }

    protected open fun onInject(configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        creationSubscriptions.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        createViewSubscriptions.clear()
    }

    override fun onStop() {
        super.onStop()
        startSubscriptions.clear()

        activity?.lifecycle?.removeObserver(this)

        if (userVisibleHint) {
            logSession()
            shownAt = null
        }
    }

    override fun onStart() {
        super.onStart()

        activity?.lifecycle?.addObserver(this)

        if (shownAt == null && userVisibleHint) {
            shownAt = System.currentTimeMillis()
            println("start ${this.javaClass.name} session - OnStart")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onActivityStarted() {
        if (shownAt == null && userVisibleHint) {
            shownAt = System.currentTimeMillis()
            println("start ${this.javaClass.name} session - AppState")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onActivityStopped() {
        if (shownAt != null) {
            logSession()
            shownAt = null
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            shownAt = System.currentTimeMillis()
            println("start ${this.javaClass.name} session - setUserVisible")
        } else {
            if (shownAt != null) {
                logSession()
                shownAt = null
            }
        }
    }

    private fun logSession() {
        if (shownAt != null) {

            val now = System.currentTimeMillis()
            val elapsed = now - shownAt!!
            println("end ${this.javaClass.name} session - ${elapsed.toFloat() / 1000} seconds")
            if (elapsed > 100) {
                eventLogger.get().logSession(this.javaClass.name, IEventLogger.SUB_SESSION_TYPE_FRAGMENT, elapsed, now, null) { content ->
                    content["parent"] = act.localClassName
                    onSessionLogContent(content)
                }
            }
        }
    }

    protected open fun onSessionLogContent(contentObject: JsonObject) {
    }
}