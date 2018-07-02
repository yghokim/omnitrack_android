package kr.ac.snu.hcil.omnitrack

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.support.multidex.MultiDex
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import com.jakewharton.threetenabp.AndroidThreeTen
import com.jenzz.appstate.AppState
import com.jenzz.appstate.adapter.rxjava2.RxAppStateMonitor
import com.squareup.leakcanary.LeakCanary
import io.reactivex.Observable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.core.di.global.*
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.services.OTDeviceStatusService
import kr.ac.snu.hcil.omnitrack.utils.LocaleHelper
import org.jetbrains.anko.telephonyManager
import rx_activity_result2.RxActivityResult
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OTApp : MultiDexApplication() {

    companion object {
        lateinit var instance: OTApp
            private set

        lateinit var logger: LoggingDbHelper
            private set

        const val PREFIX_ACTION = "${BuildConfig.APPLICATION_ID}.action"
        const val PREFIX_PREF_KEY = "${BuildConfig.APPLICATION_ID}.preference"

        const val ACCOUNT_DATASET_EXPERIMENT_KEY_GENDER = "gender"
        const val ACCOUNT_DATASET_EXPERIMENT_KEY_OCCUPATION = "occupation"
        const val ACCOUNT_DATASET_EXPERIMENT_KEY_AGE_GROUP = "age_group"
        const val ACCOUNT_DATASET_EXPERIMENT_KEY_COUNTRY = "country"
        const val ACCOUNT_DATASET_EXPERIMENT_KEY_PURPOSES = "purpose"

        const val INTENT_EXTRA_OBJECT_ID_TRACKER = "trackerId"
        const val INTENT_EXTRA_OBJECT_ID_ATTRIBUTE = "attributeObjectId"
        const val INTENT_EXTRA_LOCAL_ID_ATTRIBUTE = "attributeLocalId"

        const val INTENT_EXTRA_OBJECT_ID_TRACKER_ARRAY = "trackerIds"

        const val INTENT_EXTRA_OBJECT_ID_USER = "userObjectId"
        const val INTENT_EXTRA_OBJECT_ID_TRIGGER = "triggerObjectId"
        const val INTENT_EXTRA_OBJECT_ID_ITEM = "itemDbId"

        val INTENT_EXTRA_CONFIGURATION_ID = "${BuildConfig.APPLICATION_ID}.extra.configurationId"

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


        fun getString(resId: Int): String {
            return instance.resourcesWrapped.getString(resId)
        }

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

    val appState: Observable<AppState> by lazy {
        RxAppStateMonitor.monitor(this)
    }

    val deviceId: String by lazy {
        val deviceUUID: UUID
        val cached: String = applicationComponent.defaultPreferences().getString("cached_device_id", "")
        if (cached.isNotBlank()) {
            return@lazy cached
        } else {
            val androidUUID = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidUUID.isNullOrBlank()) {
                deviceUUID = UUID.nameUUIDFromBytes(androidUUID.toByteArray(Charset.forName("utf8")))
            } else {
                try {
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

    //Dependency Injection

    private val appModule: ApplicationModule by lazy {
        ApplicationModule(this)
    }

    private val appLevelDatabaseModule: AppDatabaseModule by lazy {
        AppDatabaseModule()
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

    val applicationComponent: ApplicationComponent by lazy {
        DaggerApplicationComponent.builder()
                .applicationModule(appModule)
                .appDatabaseModule(appLevelDatabaseModule)
                .jobDispatcherModule(jobDispatcherModule)
                .serializationModule(serializationModule)
                .systemIdentifierFactoryModule(systemIdentifierFactoryModule)
                .build()
    }

    val jobDispatcherComponent: JobDispatcherComponent by lazy {
        DaggerJobDispatcherComponent.builder()
                .jobDispatcherModule(jobDispatcherModule)
                .applicationModule(appModule)
                .build()
    }

    val serializationComponent: SerializationComponent by lazy {
        DaggerSerializationComponent.builder()
                .serializationModule(serializationModule)
                .build()
    }

    val colorPalette: IntArray by lazy {
        this.resources.getStringArray(R.array.colorPaletteArray).map { Color.parseColor(it) }.toIntArray()
    }

    val currentConfiguredContext: ConfiguredContext
        get() {
            return this.applicationComponent.configurationController().currentConfiguredContext
        }

    override fun attachBaseContext(base: Context) {
        LocaleHelper.init(base)
        refreshConfiguration(base)
        super.attachBaseContext(wrappedContext)
        MultiDex.install(this)
    }

    fun refreshConfiguration(context: Context) {
        val wrapped = LocaleHelper.wrapContextWithLocale(context.applicationContext ?: context, LocaleHelper.getLanguageCode(context))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OTNotificationManager.refreshChannels(wrapped)
        }

        wrappedContext = wrapped
    }

    override fun onCreate() {
        super.onCreate()
        val startedAt = SystemClock.elapsedRealtime()

        //Fabric.with(this, Crashlytics())
        AndroidThreeTen.init(this)
        RxActivityResult.register(this)
        Realm.init(this)

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your instance in this process.
        } else {
            LeakCanary.install(this)
        }

        instance = this
        println("set application instance.")


        //initialize modules===============================================

        logger = LoggingDbHelper(this)
        logger.writeSystemLog("Application creates.", "OTApp")

        //=================================================================

        OTExternalService.init()
        for (service in OTExternalService.availableServices) {
            if (service.state == OTExternalService.ServiceState.ACTIVATED) {
                service.activateSilently().subscribe { success ->
                    if (success != true) {
                        println("failed to activite ${service.identifier} service silently.")
                    }
                }
            }
        }

        //TODO start service in job controller
        //startService(this.binaryUploadServiceController.makeResumeUploadIntent())

        applicationComponent.configurationController().currentConfiguredContext.activateOnSystem()


        appState.subscribe { appState ->
            when (appState) {
                AppState.FOREGROUND -> {
                    println("App Screen State: FOREGROUND")
                    foregroundTime.set(System.currentTimeMillis())
                }
                AppState.BACKGROUND -> {
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
            }
        }

        startService(Intent(this, OTDeviceStatusService::class.java))

        println("creation took ${SystemClock.elapsedRealtime() - startedAt}")
    }

    fun unlinkUser() {
        applicationComponent.
                configurationController().
                currentConfiguredContext.
                configuredAppComponent.
                shortcutPanelManager().disposeShortcutPanel()
        applicationComponent.defaultPreferences().edit().remove(INTENT_EXTRA_OBJECT_ID_USER).apply()
    }

    override fun onTerminate() {
        super.onTerminate()

        logger.writeSystemLog("App terminates.", "Application")

    }

/*    private fun createUsabilityTestingTrackers(user: OTUser) {

        val bookTracker = user.newTracker("독서록", true)
        bookTracker.attributes += OTAttribute.createAttribute(bookTracker, "제목", OTAttributeManager.TYPE_SHORT_TEXT)
        bookTracker.attributes += OTAttribute.createAttribute(bookTracker, "날짜", OTAttributeManager.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }
        bookTracker.attributes += OTAttribute.createAttribute(bookTracker, "감상평", OTAttributeManager.TYPE_LONG_TEXT)
        bookTracker.attributes += OTAttribute.createAttribute(bookTracker, "별점", OTAttributeManager.TYPE_RATING)

        //===================================================================================================================================
        val stepComparisonTracker = user.newTracker("걸음 수 비교", true)

        val stepNumberStyle = NumberStyle().apply {
            this.unit = "걸음"
            this.unitPosition = NumberStyle.UnitPosition.Rear
            this.fractionPart = 0
        }

        val fitbitAttribute = OTAttribute.createAttribute(stepComparisonTracker, "Fitbit", OTAttributeManager.TYPE_NUMBER) as OTNumberAttribute
        fitbitAttribute.numberStyle = stepNumberStyle

        val googleFitAttribute = OTAttribute.createAttribute(stepComparisonTracker, "Google Fit", OTAttributeManager.TYPE_NUMBER) as OTNumberAttribute
        googleFitAttribute.numberStyle = stepNumberStyle

        stepComparisonTracker.attributes.add(fitbitAttribute)
        stepComparisonTracker.attributes.add(googleFitAttribute)


        //===================================================================================================================================
        val diaryTracker = user.newTracker("일기", true)

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, "날짜", OTAttributeManager.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, "기분", OTAttributeManager.TYPE_RATING).apply {
            this.setPropertyValue(OTRatingAttribute.PROPERTY_OPTIONS, kr.ac.snu.hcil.omnitrack.utils.RatingOptions().apply {
                this.isFractional = true
                this.leftLabel = "매우 나쁨"
                this.middleLabel = "보통"
                this.rightLabel = "매우 좋음"
                this.type = kr.ac.snu.hcil.omnitrack.utils.RatingOptions.DisplayType.Likert
            })
        }

        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, "날씨", OTAttributeManager.TYPE_CHOICE).apply {
            this.setPropertyValue(OTChoiceAttribute.PROPERTY_ENTRIES, UniqueStringEntryList("맑음", "흐림", "비", "눈"))
            this.setPropertyValue(OTChoiceAttribute.PROPERTY_MULTISELECTION, false)
        }
        diaryTracker.attributes += OTAttribute.Companion.createAttribute(diaryTracker, "제목", OTAttributeManager.TYPE_SHORT_TEXT)
        diaryTracker.attributes += OTAttribute.createAttribute(diaryTracker, "내용", OTAttributeManager.TYPE_LONG_TEXT)


        //=====================================================================================================================================
        val stressTracker = user.newTracker("스트레스", true)
        stressTracker.attributes += OTAttribute.createAttribute(stressTracker, "시간", OTAttributeManager.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_MINUTE)
        }

        stressTracker.attributes += OTAttribute.createAttribute(stressTracker, "기분", OTAttributeManager.TYPE_RATING).apply {
            this.setPropertyValue(OTRatingAttribute.PROPERTY_OPTIONS, kr.ac.snu.hcil.omnitrack.utils.RatingOptions().apply {
                this.isFractional = true
                this.leftLabel = "매우 나쁨"
                this.middleLabel = "보통"
                this.rightLabel = "매우 좋음"
                this.type = kr.ac.snu.hcil.omnitrack.utils.RatingOptions.DisplayType.Likert
            })
        }

        stressTracker.attributes += OTAttribute.createAttribute(stressTracker, "이유", OTAttributeManager.TYPE_LONG_TEXT)

        //=====================================================================================================================================
        val foodTracker = user.newTracker("맛집", true)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, "식당명", OTAttributeManager.TYPE_SHORT_TEXT)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, "먹은 날", OTAttributeManager.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, "위치", OTAttributeManager.TYPE_LOCATION)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, "사진", OTAttributeManager.TYPE_IMAGE)
        foodTracker.attributes += OTAttribute.createAttribute(foodTracker, "평점", OTAttributeManager.TYPE_RATING)


        //dbHelper.save(user)

    }

    private fun createExampleTrackers(user: OTUser) {
        //====================================================================================================================================================
        val coffeeTracker = user.newTracker("Coffee", true)
        coffeeTracker.isOnShortcut = true
        coffeeTracker.attributes.add(OTAttribute.Companion.createAttribute(coffeeTracker, "Name", OTAttributeManager.TYPE_SHORT_TEXT))
        coffeeTracker.attributes.add(OTAttribute.Companion.createAttribute(coffeeTracker, "Drank At", OTAttributeManager.TYPE_TIME))

        val waterTracker = user.newTracker("Water", true)
        waterTracker.attributes.add(OTAttribute.Companion.createAttribute(waterTracker, "Drank At", OTAttributeManager.TYPE_TIME))


        //====================================================================================================================================================
        val sleepTracker = user.newTracker("Sleep", true)
        sleepTracker.isOnShortcut = true

        val sleepTimeAttribute = OTAttribute.Companion.createAttribute(sleepTracker, "Sleep Duration", OTAttributeManager.TYPE_TIMESPAN)
        sleepTimeAttribute.setPropertyValue(OTTimeSpanAttribute.PROPERTY_GRANULARITY, OTTimeAttribute.GRANULARITY_MINUTE)

        val sleepTimeConnection = OTConnection()
        sleepTimeConnection.source = FitbitRecentSleepTimeMeasureFactory.makeMeasure()
        sleepTimeConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, 0)
        sleepTimeAttribute.valueConnection = sleepTimeConnection

        sleepTracker.attributes.add(sleepTimeAttribute)
        sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(sleepTracker, "Quality", OTAttributeManager.TYPE_RATING))
        sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(sleepTracker, "Memo", OTAttributeManager.TYPE_LONG_TEXT))

        //====================================================================================================================================================
        val beerTracker = user.newTracker("Beer", true)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(beerTracker, "Photo", OTAttributeManager.TYPE_IMAGE))
        val dateAttribute = OTAttribute.createAttribute(beerTracker, "Date", OTAttributeManager.TYPE_TIME)
        dateAttribute.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        beerTracker.attributes.add(dateAttribute)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(beerTracker, "Name", OTAttributeManager.TYPE_SHORT_TEXT))

        val typeAttribute = OTAttribute.createAttribute(beerTracker, "Type", OTAttributeManager.TYPE_CHOICE)
        typeAttribute.setPropertyValue(OTChoiceAttribute.PROPERTY_ENTRIES, UniqueStringEntryList("Lager", "Stout", "Ale", "Hybrid"))
        typeAttribute.setPropertyValue(OTChoiceAttribute.PROPERTY_MULTISELECTION, false)
        beerTracker.attributes.add(typeAttribute)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(beerTracker, "Score", OTAttributeManager.TYPE_RATING))
        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(beerTracker, "Review", OTAttributeManager.TYPE_LONG_TEXT))


        //====================================================================================================================================================
        val stepComparisonTracker = user.newTracker("Step Devices", true)
        val fitbitAttribute = OTAttribute.createAttribute(stepComparisonTracker, "Fitbit", OTAttributeManager.TYPE_NUMBER) as OTNumberAttribute
        fitbitAttribute.numberStyle.fractionPart = 0
        val fitbitStepConnection = OTConnection()
        fitbitStepConnection.source = FitbitStepCountMeasureFactory.makeMeasure()
        fitbitStepConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        fitbitAttribute.valueConnection = fitbitStepConnection

        val misfitAttribute = OTAttribute.createAttribute(stepComparisonTracker, "MisFit", OTAttributeManager.TYPE_NUMBER) as OTNumberAttribute
        misfitAttribute.numberStyle.fractionPart = 0
        val misfitStepConnection = OTConnection()

        misfitStepConnection.source = MisfitStepMeasureFactory.makeMeasure()
        misfitStepConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        misfitAttribute.valueConnection = misfitStepConnection

        val googleFitAttribute = OTAttribute.createAttribute(stepComparisonTracker, "Google Fit", OTAttributeManager.TYPE_NUMBER) as OTNumberAttribute
        googleFitAttribute.numberStyle.fractionPart = 0
        val googleFitConnection = OTConnection()
        googleFitConnection.source = GoogleFitStepsFactory.makeMeasure()
        googleFitConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        googleFitAttribute.valueConnection = googleFitConnection

        stepComparisonTracker.attributes.add(fitbitAttribute)
        stepComparisonTracker.attributes.add(misfitAttribute)
        stepComparisonTracker.attributes.add(googleFitAttribute)

        //dbHelper.save(user)

        return // skip example data
    }

    private fun addStepsExampleItems(stepTracker: OTTracker) {

        //actual data:  9/10 ~ 9/18 fake data: 9/2 ~ 9/10
        val fitbitSteps = arrayOf(11200 as Any?, 14246, 11700, 13820, 12500, 10270, 15000, 9416, 3665, 6384, 22013, 13709, 10096, 13611, 16265, 8187, 6045)

        //9/2 ~ 9/18
        val misfitSteps = arrayOf(10506 as Any?, 12700, 10464, 12576, 10748, 9248, 12478, 7416, 10776, 7414, 14000, 11298, 7762, 11558, 13810, 4210, 5198)

        //9/2 ~ 9/18
        val googleSteps = arrayOf(5623 as Any?, 8086, 5806, 5913, 0, 1994, 5108, 502, 5024, 2518, 3654, 5302, 2054, 3220, 5394, 0, 436)

        val stepStart = GregorianCalendar(2016, 8, 3).timeInMillis

        val timestamps = LongArray(18 - 2 + 1) {
            index ->
            stepStart + index * DateUtils.DAY_IN_MILLIS
        }

        val stepItems = ArrayList<OTItem>()
        OTItem.createItemsWithColumnArrays(stepTracker, timestamps, OTItem.ItemLoggingSource.Unspecified, stepItems, fitbitSteps, misfitSteps, googleSteps)

    }

    private fun addExampleBeerReviews(beerTracker: OTTracker) {
        val rowValues = arrayOf(
                arrayOf("https://images.vingle.net/upload/t_ca_xl/jop8mlrsqtahbt8uuyzr.jpg", GregorianCalendar(2016, 7, 22, 20, 30).timeInMillis, "Green Flash West Coast IPA", intArrayOf(2), 3f, "Aroma pine, citrus, very hoppy. Taste pine, grapefruit, topical, dank.."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/hv9buv5fixwtjlw2qmdp.jpg", GregorianCalendar(2016, 7, 22, 17, 30).timeInMillis, "Cuvee Delphine", intArrayOf(1), 4.5f, "Dark, dark black. Tiny head.\nIntense aroma of roasted malt, bourbon, dark fruit, and licorice.\nTaste followed the nose. Chocolate, bourbon, raisins, licorice.\nA big beer. The bourbon compliments the stout beautifully."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/wkgjgf7hswzpaousvjhn.jpg", GregorianCalendar(2016, 7, 20, 21, 30).timeInMillis, "Struise Pannepot", intArrayOf(2), 3f, "Prunes, figs, toffee, burnt sugar. All the goods. Big, not too boozy, and a perfect amount of carb. Medium mouthfeel, right where it should be. This was the 2011 bottle. Stored in a 55°cellar the entire time."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/pmaf21cycyjiieisog0m.jpg", GregorianCalendar(2016, 7, 18, 20, 30).timeInMillis, "Verboden Vrucht", intArrayOf(2), 4f, "Very nice collar and a fabulous taste. It's a typical Belgian ale with no doubt. Perfect in all items. It's a beer that everybody should drink at least one time in life."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/p95ybbv38ufnncqncgix.jpg", GregorianCalendar(2016, 7, 15, 19, 30).timeInMillis, "Ypres Reserva", intArrayOf(3), 3.5f, "Dark brown color with minimal head. Intense dark fruit aroma. Taste is also following the nose with the dark fruit component and a balsamic vinegar sourness. Very complex beer."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/bjlmhbnix86ixvtgna0j.jpg", GregorianCalendar(2016, 7, 13, 21, 30).timeInMillis, "Hop Head Red", intArrayOf(2), 3.5f, "Ale of the month at the BBC. This beer poured a cloudy red color with a nice head. As it drained the head left a very nice lace"),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/hv9buv5fixwtjlw2qmdp.jpg", GregorianCalendar(2016, 7, 12, 17, 30).timeInMillis, "Orange Avenue Wit", intArrayOf(3), 2.5f, "Floral orange, coriander, and wheat malt aroma. Hazy golden yellow with moderate head and effervescence. \nLightly sweet lemon/orange peel, strong coriander, and wheat malt flavor"),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/gikqvib13avzk6yh0fo5.jpg", GregorianCalendar(2016, 7, 10, 23, 30).timeInMillis, "By Udder Means", intArrayOf(1), 3f, "Very dark brown poue with a soft, tall tan head that is slow to rise to the surface. Pine and grapefruit up front, followed by milk chocolate, lactose, cocoa, licorice and a bit of vanilla."),
                arrayOf("https://images.vingle.net/upload/t_ca_xl/wldibexysoy03hdtq2q6.jpg", GregorianCalendar(2016, 7, 8, 12, 30).timeInMillis, "Tsingtao", intArrayOf(0), 2.5f, "It's an OK beer, nothing special. It has a watery taste and the aftertaste is not the best, but I've had worse. Not sure if I will buy this one again, but give it a try if you can so you won't miss out on anything.")
        )


        dbHelper.save(
                rowValues.map {
                    OTItem(
                            beerTracker,
                            it[1] as Long,
                            OTItem.ItemLoggingSource.Unspecified,
                            OTApp.instance.deviceId,
                            *it
                    )
                }, beerTracker)


    }*/
}