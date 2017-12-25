package kr.ac.snu.hcil.omnitrack.utils

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfigurationController
import java.util.*
import javax.inject.Inject

/**
 * tag is the configuration id.
 * Created by younghokim on 2017. 12. 20..
 */
abstract class ConfigurableJobService : JobService() {
    interface IConfiguredTask {
        fun dispose()
        fun onStartJob(job: JobParameters): Boolean
        fun onStopJob(job: JobParameters): Boolean
    }


    @Inject
    lateinit var configController: OTConfigurationController

    private val tasks = Hashtable<String, IConfiguredTask>()

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
    }


    override fun onDestroy() {
        super.onDestroy()
        tasks.forEach { it.value?.dispose() }
        tasks.clear()
    }

    override fun onStopJob(job: JobParameters): Boolean {
        val task = tasks[extractConfigIdOfJob(job)]
        if (task != null) {
            return task.onStopJob(job)
        } else {
            return false
        }
    }

    override fun onStartJob(job: JobParameters): Boolean {
        println("try start upload usage logs...")
        val configId = extractConfigIdOfJob(job)
        val context = configController.getConfiguredContextOf(configId)
        if (context != null) {
            val task = tasks[configId] ?: makeNewTask(context).apply {
                tasks[configId] = this
                beforeStartConfiguration(configId)
            }
            return task.onStartJob(job)
        } else return false
    }

    protected fun beforeStartConfiguration(configId: String) {

    }

    protected fun afterFinishConfiguration(configId: String, stop: Boolean) {

    }

    protected open fun extractConfigIdOfJob(job: JobParameters): String {
        return job.tag
    }

    abstract fun makeNewTask(configuredContext: ConfiguredContext): IConfiguredTask

}