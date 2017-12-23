package kr.ac.snu.hcil.omnitrack.utils

import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfigurationController
import kr.ac.snu.hcil.omnitrack.services.WakefulService
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 12. 21..
 */
abstract class ConfigurableWakefulService(tag: String) : WakefulService(tag) {
    inner abstract class AConfiguredTask(val startId: Int, val configuredContext: ConfiguredContext) {
        abstract fun dispose()
        abstract fun onStartCommand(intent: Intent, flags: Int): Int
        open fun finishSelf() {
            stopSelf(startId)
            tasks.remove(this)
        }
    }


    @Inject
    lateinit var configController: OTConfigurationController

    private val tasks = HashSet<AConfiguredTask>()

    protected open fun onInject(app: OTApp) {
        app.applicationComponent.inject(this)
    }

    override fun onCreate() {
        super.onCreate()
        onInject(application as OTApp)
    }

    override fun onDestroy() {
        tasks.forEach { it.dispose() }
        tasks.clear()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val configId = intent.getStringExtra(OTApp.INTENT_EXTRA_CONFIGURATION_ID)
        if (configId != null) {
            val configuredContext = configController.getConfiguredContextOf(configId)
            if (configuredContext != null) {
                val task = makeConfiguredTask(startId, configuredContext)
                tasks.add(task)
                return task.onStartCommand(intent, flags)
            } else return START_NOT_STICKY
        } else return START_NOT_STICKY
    }

    abstract fun makeConfiguredTask(startId: Int, configuredContext: ConfiguredContext): AConfiguredTask
}