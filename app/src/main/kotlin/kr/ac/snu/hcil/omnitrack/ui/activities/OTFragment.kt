package kr.ac.snu.hcil.omnitrack.ui.activities

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
open class OTFragment : Fragment() {

    private var shownAt: Long? = null

    protected val startSubscriptions = CompositeDisposable()
    protected val creationSubscriptions = CompositeDisposable()
    protected val createViewSubscriptions = CompositeDisposable()

    @Inject
    protected lateinit var eventLogger: Lazy<IEventLogger>

    @Inject
    protected lateinit var configuredContext: ConfiguredContext

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

    override fun onPause() {
        super.onPause()

        if (userVisibleHint) {
            logSession(true)
        }
    }

    override fun onStop() {
        super.onStop()
        startSubscriptions.clear()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (isVisibleToUser) {
            shownAt = System.currentTimeMillis()
        } else {
            if (shownAt != null) {
                logSession(activityPausing = false)
            }
        }
    }

    private fun logSession(activityPausing: Boolean) {
        if (shownAt != null) {


            val elapsed = System.currentTimeMillis() - shownAt!!

            val now = System.currentTimeMillis()
            eventLogger.get().logSession(this.javaClass.name, IEventLogger.SUB_SESSION_TYPE_FRAGMENT, elapsed, now, null) { content ->
                content["caused_by_activity_pause"] = activityPausing
                content["parent"] = act.localClassName
                onSessionLogContent(content)
            }
        }
    }

    protected open fun onSessionLogContent(contentObject: JsonObject) {
    }
}