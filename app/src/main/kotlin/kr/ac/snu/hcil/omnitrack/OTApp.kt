package kr.ac.snu.hcil.omnitrack

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.support.v7.app.AppCompatDelegate
import com.squareup.leakcanary.LeakCanary
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.core.di.global.*
import kr.ac.snu.hcil.omnitrack.core.system.OTExternalSettingsPrompter
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.services.OTDeviceStatusService
import kr.ac.snu.hcil.omnitrack.utils.LocaleHelper
import org.jetbrains.anko.telephonyManager
import rx_activity_result2.RxActivityResult
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@SuppressLint("HardwareIds")
/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OTApp : Application(), LifecycleObserver, OTAndroidApp {

    companion object {

        lateinit var logger: LoggingDbHelper
            private set

        const val PREFIX_ACTION = "${BuildConfig.APPLICATION_ID}.action"
        const val PREFIX_PREF_KEY = "${BuildConfig.APPLICATION_ID}.preference"

        const val INTENT_EXTRA_OBJECT_ID_TRACKER = "trackerId"
        const val INTENT_EXTRA_OBJECT_ID_ATTRIBUTE = "attributeObjectId"
        const val INTENT_EXTRA_LOCAL_ID_ATTRIBUTE = "attributeLocalId"

        const val INTENT_EXTRA_OBJECT_ID_TRACKER_ARRAY = "trackerIds"

        const val INTENT_EXTRA_OBJECT_ID_USER = "userObjectId"
        const val INTENT_EXTRA_OBJECT_ID_TRIGGER = "triggerObjectId"
        const val INTENT_EXTRA_OBJECT_ID_ITEM = "itemDbId"

        const val INTENT_EXTRA_METADATA = "metadata"

        const val INTENT_EXTRA_TRIGGER_TIME = "triggerTime"

        const val INTENT_EXTRA_NOTIFICATION_ID = "notificationId"
        const val INTENT_EXTRA_NOTIFICATON_TAG = "notificationTag"

        const val INTENT_EXTRA_NOTIFICATION_ID_SEED = "notificationIdSeed"

        const val INTENT_EXTRA_IGNORE_SIGN_IN_CHECK = "ignoreSignInCheck"

        const val INTENT_EXTRA_FROM = "activityOpenedFrom"

        const val INTENT_EXTRA_ITEMBUILDER = "itemBuilderId"

        const val BROADCAST_ACTION_NEW_VERSION_DETECTED = "$PREFIX_ACTION.NEW_VERSION_DETECTED"
        const val INTENT_EXTRA_LATEST_VERSION_NAME = "latest_version"

        const val BROADCAST_ACTION_USER_SIGNED_IN = "$PREFIX_ACTION.USER_SIGNED_IN"
        const val BROADCAST_ACTION_USER_SIGNED_OUT = "$PREFIX_ACTION.USER_SIGNED_OUT"

        const val BROADCAST_ACTION_TRIGGER_FIRED = "$PREFIX_ACTION.TRIGGER_FIRED"

        const val BROADCAST_ACTION_TIME_TRIGGER_ALARM = "$PREFIX_ACTION.ALARM"
        const val BROADCAST_ACTION_REMINDER_AUTO_EXPIRY_ALARM = "$PREFIX_ACTION.ALARM_REMINDER_EXPIRY"

        const val BROADCAST_ACTION_EVENT_TRIGGER_CHECK_ALARM = "$PREFIX_ACTION.EVENT_TRIGGER_ALARM"

        const val BROADCAST_ACTION_SHORTCUT_REFRESH = "$PREFIX_ACTION.SHORTCUT_TRACKER_REFRESH"

        const val BROADCAST_ACTION_ITEM_ADDED = "$PREFIX_ACTION.ITEM_ADDED"
        const val BROADCAST_ACTION_ITEM_REMOVED = "$PREFIX_ACTION.ITEM_REMOVED"
        const val BROADCAST_ACTION_ITEM_EDITED = "$PREFIX_ACTION.ITEM_EDITED"

        const val BROADCAST_ACTION_COMMAND_REMOVE_ITEM = "$PREFIX_ACTION.COMMAND_REMOVE_ITEM"

        const val BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED = "$PREFIX_ACTION.BACKGROUND_LOGGING_STARTED"
        const val BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED = "$PREFIX_ACTION.BACKGROUND_LOGGING_SUCCEEDED"

        const val BROADCAST_ACTION_TRACKER_ON_BOOKMARK = "$PREFIX_ACTION.TRACKER_ON_BOOKMARK"

        const val PREFERENCE_KEY_FIREBASE_INSTANCE_ID = "$PREFIX_PREF_KEY.firebase_instance_id"

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    val resourcesWrapped: Resources get() {
        return wrappedContext?.resources ?: resources
    }

    val contextCompat: Context get() {
        return wrappedContext ?: this
    }

    var isDeviceStateServiceStartReserved = false

    override val deviceId: String by lazy {
        val deviceUUID: UUID
        val cached: String? = applicationComponent.defaultPreferences().getString("cached_device_id", null)
        if (!cached.isNullOrBlank()) {
            return@lazy cached!!
        } else {
            val androidUUID = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidUUID.isNullOrBlank()) {
                deviceUUID = UUID.nameUUIDFromBytes(androidUUID.toByteArray(Charset.forName("utf8")))
            } else {
                try {
                    @Suppress("DEPRECATION")
                    val phoneUUID = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        this.telephonyManager.imei
                    } else this.telephonyManager.deviceId

                    if (!phoneUUID.isNullOrBlank()) {
                        deviceUUID = UUID.nameUUIDFromBytes(phoneUUID.toByteArray(Charset.forName("utf8")))
                    } else {
                        deviceUUID = UUID.randomUUID()
                    }
                } catch (ex: SecurityException) {
                    throw ex
                }
            }
            val finalId = "$deviceUUID;${BuildConfig.APPLICATION_ID}"

            applicationComponent.defaultPreferences().edit().putString("cached_device_id", finalId).apply()
            finalId
        }
    }

    private var wrappedContext: Context? = null

    private val foregroundTime = AtomicLong(0)

    val isAppForeground: Boolean get() = foregroundTime.get() == Long.MIN_VALUE

    //Dependency Injection

    private val appModule: ApplicationModule by lazy {
        ApplicationModule(this)
    }

    private val designModule: DesignModule by lazy {
        DesignModule()
    }

    private val externalServiceModule: ExternalServiceModule by lazy {
        ExternalServiceModule()
    }

    private val serializationModule: SerializationModule by lazy {
        SerializationModule()
    }

    private val systemIdentifierFactoryModule: SystemIdentifierFactoryModule by lazy {
        SystemIdentifierFactoryModule()
    }

    private val jobDispatcherModule: JobDispatcherModule by lazy {
        JobDispatcherModule()
    }

    override val applicationComponent: ApplicationComponent by lazy {
        DaggerApplicationComponent.builder()
                .applicationModule(appModule)
                .designModule(designModule)
                .externalServiceModule(externalServiceModule)
                .jobDispatcherModule(jobDispatcherModule)
                .serializationModule(serializationModule)
                .systemIdentifierFactoryModule(systemIdentifierFactoryModule)
                .build()
    }

    override val jobDispatcherComponent: JobDispatcherComponent by lazy {
        DaggerJobDispatcherComponent.builder()
                .jobDispatcherModule(jobDispatcherModule)
                .applicationModule(appModule)
                .build()
    }

    override val serializationComponent: SerializationComponent by lazy {
        DaggerSerializationComponent.builder()
                .serializationModule(serializationModule)
                .build()
    }

    override val currentConfiguredContext: ConfiguredContext
        get() {
            return this.applicationComponent.configurationController().currentConfiguredContext
        }

    override fun attachBaseContext(base: Context) {
        LocaleHelper.init(base)
        refreshConfiguration(base)
        super.attachBaseContext(wrappedContext)
    }

    override fun refreshConfiguration(context: Context) {
        val wrapped = LocaleHelper.wrapContextWithLocale(context.applicationContext ?: context, LocaleHelper.getLanguageCode(context))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OTNotificationManager.refreshChannels(wrapped)
        }

        wrappedContext = wrapped
    }

    override fun onCreate() {
        super.onCreate()

        val startedAt = SystemClock.elapsedRealtime()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your instance in this process.
            return
        } else {
            LeakCanary.install(this)
        }

        //Use a new async API to improve performance in RxAndroid 2.1.0
        RxAndroidPlugins.initMainThreadScheduler {
            AndroidSchedulers.from(Looper.getMainLooper(), true)
        }

        //Fabric.with(this, Crashlytics
        RxActivityResult.register(this)
        Realm.init(this)

        val systemDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                applicationComponent.configurationController()
                        .currentConfiguredContext.configuredAppComponent.getEventLogger()
                        .logExceptionEvent("Uncaught", throwable, thread)
                println("logged the uncaught exception.")
            } finally {
                systemDefaultUncaughtExceptionHandler.uncaughtException(thread, throwable)
            }
        }

        //initialize modules===============================================

        logger = LoggingDbHelper(this)
        logger.writeSystemLog("Application creates.", "OTApp")

        //=================================================================

        //TODO start service in job controller
        //startService(this.binaryUploadServiceController.makeResumeUploadIntent())

        applicationComponent.configurationController().currentConfiguredContext.activateOnSystem()

        if (OTExternalSettingsPrompter.isBatteryOptimizationWhiteListed(this) || ProcessLifecycleOwner.get().lifecycle.currentState >= Lifecycle.State.STARTED) {
            startService(Intent(this, OTDeviceStatusService::class.java))
        } else {
            isDeviceStateServiceStartReserved = true
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)


        println("creation took ${SystemClock.elapsedRealtime() - startedAt}")
    }


    //Lifecycle==========================================
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppDidEnterForeground() {
        println("App Screen State: FOREGROUND")
        foregroundTime.set(System.currentTimeMillis())

        if (isDeviceStateServiceStartReserved) {
            startService(Intent(this, OTDeviceStatusService::class.java))
            isDeviceStateServiceStartReserved = false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppDidEnterBackground() {
        println("App Screen State: BACKGROUND")
        if (foregroundTime.get() != Long.MIN_VALUE) {
            val finishedAt = System.currentTimeMillis()
            val duration = finishedAt - foregroundTime.getAndSet(Long.MIN_VALUE)
            if (duration > 100) {
                println("App Screen Session duration : ${duration.toFloat() / 1000} seconds")
                applicationComponent.configurationController().currentConfiguredContext.configuredAppComponent.getEventLogger()
                        .logSession("engagement_foreground", "application", duration, finishedAt, null)
            }
        }
    }
    //====================================================

    override fun onTerminate() {
        super.onTerminate()

        logger.writeSystemLog("App terminates.", "Application")

    }
}