package kr.ac.snu.hcil.omnitrack

import android.graphics.Color
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.support.multidex.MultiDexApplication
import android.text.format.DateUtils
import com.squareup.leakcanary.LeakCanary
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.*
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.CacheHelper
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseHelper
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitRecentSleepTimeMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitStepCountMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.externals.google.fit.GoogleFitStepsFactory
import kr.ac.snu.hcil.omnitrack.core.externals.misfit.MisfitStepMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.services.OTBackgroundLoggingService
import kr.ac.snu.hcil.omnitrack.utils.NumberStyle
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OTApplication : MultiDexApplication() {

    companion object {
        lateinit var app: OTApplication
            private set

        lateinit var logger: LoggingDbHelper
            private set

        const val INTENT_EXTRA_OBJECT_ID_TRACKER = "trackerObjectId"
        const val INTENT_EXTRA_OBJECT_ID_ATTRIBUTE = "attributeObjectId"
        const val INTENT_EXTRA_OBJECT_ID_USER = "userObjectId"
        const val INTENT_EXTRA_OBJECT_ID_TRIGGER = "triggerObjectId"
        const val INTENT_EXTRA_DB_ID_ITEM = "itemDbId"

        const val INTENT_EXTRA_FROM = "activityOpenedFrom"

        const val INTENT_EXTRA_ITEMBUILDER = "itemBuilderId"

        const val BROADCAST_ACTION_TIME_TRIGGER_ALARM = "kr.ac.snu.hcil.omnitrack.action.ALARM"
        const val BROADCAST_ACTION_EVENT_TRIGGER_CHECK_ALARM = "kr.ac.snu.hcil.omnitrack.action.EVENT_TRIGGER_ALARM"

        const val BROADCAST_ACTION_SHORTCUT_REFRESH = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_TRACKER_REFRESH"

        const val BROADCAST_ACTION_ITEM_ADDED = "kr.ac.snu.hcil.omnitrack.action.ITEM_ADDED"
        const val BROADCAST_ACTION_ITEM_REMOVED = "kr.ac.snu.hcil.omnitrack.action.ITEM_REMOVED"
        const val BROADCAST_ACTION_ITEM_EDITED = "kr.ac.snu.hcil.omnitrack.action.ITEM_EDITED"

        const val BROADCAST_ACTION_COMMAND_REMOVE_ITEM = "kr.ac.snu.hcil.omnitrack.action.COMMAND_REMOVE_ITEM"

        const val BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED = "kr.ac.snu.hcil.omnitrack.action.BACKGROUND_LOGGING_STARTED"
        const val BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED = "kr.ac.snu.hcil.omnitrack.action.BACKGROUND_LOGGING_SUCCEEDED"

        const val PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE = "item_builder_storage_foreground"

        const val PREFERENCE_KEY_BACKGROUND_ITEM_BUILDER_STORAGE = "item_builder_storage_background"

        fun getString(resId: Int): String {
            return app.resources.getString(resId)
        }
    }

    private var _currentUser: OTUser? = null

    lateinit var dbHelper: DatabaseHelper
        private set

    val cacheHelper: CacheHelper by lazy {
        CacheHelper(this)
    }

    private var initialRun = false

    var userLoaded = false
        private set

    val currentUser: OTUser
        get() {
            return if (_currentUser != null)
                _currentUser!!
            else {
                val user = dbHelper.findUserById(1)
                if (user == null) {
                    initialRun = true
                    val defaultUser = OTUser("Young-Ho Kim", "yhkim@hcil.snu.ac.kr")
                    _currentUser = defaultUser
                } else {
                    _currentUser = user
                }
                userLoaded = true
                _currentUser!!
            }
        }

    //val currentUserObservable = PublishSubject.create<OTUser>()


    val currentUserObservable: Observable<OTUser>
        get() {
            return Observable.defer {
                Observable.just(currentUser)
            }.subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .cache()
        }

    val colorPalette: IntArray by lazy {
        this.resources.getStringArray(R.array.colorPaletteArray).map { Color.parseColor(it) }.toIntArray()
    }

    val googleApiKey: String by lazy {
        this.resources.getString(R.string.google_maps_key)
    }

    lateinit var timeTriggerAlarmManager: OTTimeTriggerAlarmManager
        private set

    private lateinit var userLoadingLooper: Looper

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        val startedAt = SystemClock.elapsedRealtime()

        app = this

        logger = LoggingDbHelper(this)
        logger.writeSystemLog("Application creates.", "OTApplication")

        dbHelper = DatabaseHelper(this)

        /*
        var initialRun = false
        val user = dbHelper.findUserById(1)
        if (user == null) {
            val defaultUser = OTUser("Young-Ho Kim", "yhkim@hcil.snu.ac.kr")
            _currentUser = defaultUser

            initialRun = true

        } else {
            _currentUser = user
        }*/

        timeTriggerAlarmManager = OTTimeTriggerAlarmManager()

        for (service in OTExternalService.availableServices) {
            if (service.state == OTExternalService.ServiceState.ACTIVATED) {
                service.prepareServiceAsync({
                    result ->

                })
            }
        }


        val thread = HandlerThread("User Loading Thread", HandlerThread.NORM_PRIORITY)
        thread.start()
        userLoadingLooper = thread.looper

        currentUserObservable
                .subscribeOn(Schedulers.newThread())
                .subscribe {
                    user ->
                    for (tracker in user.getTrackersOnShortcut()) {
                        OTShortcutPanelManager += tracker
                    }

                    //handle failed background logging
                    for (pair in OTBackgroundLoggingService.getFlags()) {
                        val tracker = user[pair.first]
                        if (tracker != null) {
                            logger.writeSystemLog("${tracker.name} background logging was failed. started at ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(pair.second))}", "OmniTrack")
                        }
                    }

                    if (initialRun) {
                        //createExampleTrackers(user)
                        createUsabilityTestingTrackers(user)
                    }
                }

        println("creation took ${SystemClock.elapsedRealtime() - startedAt}")
    }

    fun syncUserToDb() {
        if (_currentUser != null)
            dbHelper.save(_currentUser!!)
    }

    override fun onTerminate() {
        super.onTerminate()

        logger.writeSystemLog("App terminates.", "Application")

        syncUserToDb()

        dbHelper.close()
    }

    private fun createUsabilityTestingTrackers(user: OTUser) {

        val bookTracker = user.newTracker("독서록", true)
        bookTracker.attributes += OTAttribute.createAttribute(user, "제목", OTAttribute.TYPE_SHORT_TEXT)
        bookTracker.attributes += OTAttribute.createAttribute(user, "날짜", OTAttribute.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }
        bookTracker.attributes += OTAttribute.createAttribute(user, "감상평", OTAttribute.TYPE_LONG_TEXT)
        bookTracker.attributes += OTAttribute.createAttribute(user, "별점", OTAttribute.TYPE_RATING)

        //===================================================================================================================================
        val stepComparisonTracker = user.newTracker("걸음 수 비교", true)

        val stepNumberStyle = NumberStyle().apply {
            this.unit = "걸음"
            this.unitPosition = NumberStyle.UnitPosition.Rear
            this.fractionPart = 0
        }

        val fitbitAttribute = OTAttribute.createAttribute(user, "Fitbit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        fitbitAttribute.numberStyle = stepNumberStyle

        val googleFitAttribute = OTAttribute.createAttribute(user, "Google Fit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        googleFitAttribute.numberStyle = stepNumberStyle

        stepComparisonTracker.attributes.add(fitbitAttribute)
        stepComparisonTracker.attributes.add(googleFitAttribute)


        //===================================================================================================================================
        val diaryTracker = user.newTracker("일기", true)

        diaryTracker.attributes += OTAttribute.createAttribute(user, "날짜", OTAttribute.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }

        diaryTracker.attributes += OTAttribute.createAttribute(user, "기분", OTAttribute.TYPE_RATING).apply {
            this.setPropertyValue(OTRatingAttribute.PROPERTY_OPTIONS, kr.ac.snu.hcil.omnitrack.utils.RatingOptions().apply {
                this.allowIntermediate = true
                this.leftLabel = "매우 나쁨"
                this.middleLabel = "보통"
                this.rightLabel = "매우 좋음"
                this.type = kr.ac.snu.hcil.omnitrack.utils.RatingOptions.DisplayType.Likert
            })
        }

        diaryTracker.attributes += OTAttribute.createAttribute(user, "날씨", OTAttribute.TYPE_CHOICE).apply {
            this.setPropertyValue(OTChoiceAttribute.PROPERTY_ENTRIES, UniqueStringEntryList("맑음", "흐림", "비", "눈"))
            this.setPropertyValue(OTChoiceAttribute.PROPERTY_MULTISELECTION, false)
        }
        diaryTracker.attributes += OTAttribute.Companion.createAttribute(user, "제목", OTAttribute.TYPE_SHORT_TEXT)
        diaryTracker.attributes += OTAttribute.createAttribute(user, "내용", OTAttribute.TYPE_LONG_TEXT)


        //=====================================================================================================================================
        val stressTracker = user.newTracker("스트레스", true)
        stressTracker.attributes += OTAttribute.createAttribute(user, "시간", OTAttribute.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_MINUTE)
        }

        stressTracker.attributes += OTAttribute.createAttribute(user, "기분", OTAttribute.TYPE_RATING).apply {
            this.setPropertyValue(OTRatingAttribute.PROPERTY_OPTIONS, kr.ac.snu.hcil.omnitrack.utils.RatingOptions().apply {
                this.allowIntermediate = true
                this.leftLabel = "매우 나쁨"
                this.middleLabel = "보통"
                this.rightLabel = "매우 좋음"
                this.type = kr.ac.snu.hcil.omnitrack.utils.RatingOptions.DisplayType.Likert
            })
        }

        stressTracker.attributes += OTAttribute.createAttribute(user, "이유", OTAttribute.TYPE_LONG_TEXT)

        //=====================================================================================================================================
        val foodTracker = user.newTracker("맛집", true)
        foodTracker.attributes += OTAttribute.createAttribute(user, "식당명", OTAttribute.TYPE_SHORT_TEXT)
        foodTracker.attributes += OTAttribute.createAttribute(user, "먹은 날", OTAttribute.TYPE_TIME).apply {
            this.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        }
        foodTracker.attributes += OTAttribute.createAttribute(user, "위치", OTAttribute.TYPE_LOCATION)
        foodTracker.attributes += OTAttribute.createAttribute(user, "사진", OTAttribute.TYPE_IMAGE)
        foodTracker.attributes += OTAttribute.createAttribute(user, "평점", OTAttribute.TYPE_RATING)


        dbHelper.save(user)
    }

    private fun createExampleTrackers(user: OTUser) {
        //====================================================================================================================================================
        val coffeeTracker = user.newTracker("Coffee", true)
        coffeeTracker.isOnShortcut = true
        coffeeTracker.attributes.add(OTAttribute.Companion.createAttribute(user, "Name", OTAttribute.TYPE_SHORT_TEXT))
        coffeeTracker.attributes.add(OTAttribute.Companion.createAttribute(user, "Drank At", OTAttribute.TYPE_TIME))

        val waterTracker = user.newTracker("Water", true)
        waterTracker.attributes.add(OTAttribute.Companion.createAttribute(user, "Drank At", OTAttribute.TYPE_TIME))


        //====================================================================================================================================================
        val sleepTracker = user.newTracker("Sleep", true)
        sleepTracker.isOnShortcut = true

        val sleepTimeAttribute = OTAttribute.Companion.createAttribute(user, "Sleep Duration", OTAttribute.TYPE_TIMESPAN)
        sleepTimeAttribute.setPropertyValue(OTTimeSpanAttribute.PROPERTY_GRANULARITY, OTTimeAttribute.GRANULARITY_MINUTE)

        val sleepTimeConnection = OTConnection()
        sleepTimeConnection.source = FitbitRecentSleepTimeMeasureFactory.makeMeasure()
        sleepTimeConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, 0)
        sleepTimeAttribute.valueConnection = sleepTimeConnection

        sleepTracker.attributes.add(sleepTimeAttribute)
        sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(user, "Quality", OTAttribute.TYPE_RATING))
        sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(user, "Memo", OTAttribute.TYPE_LONG_TEXT))

        //====================================================================================================================================================
        val beerTracker = user.newTracker("Beer", true)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(user, "Photo", OTAttribute.TYPE_IMAGE))
        val dateAttribute = OTAttribute.createAttribute(user, "Date", OTAttribute.TYPE_TIME)
        dateAttribute.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        beerTracker.attributes.add(dateAttribute)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(user, "Name", OTAttribute.TYPE_SHORT_TEXT))

        val typeAttribute = OTAttribute.createAttribute(user, "Type", OTAttribute.TYPE_CHOICE)
        typeAttribute.setPropertyValue(OTChoiceAttribute.PROPERTY_ENTRIES, UniqueStringEntryList("Lager", "Stout", "Ale", "Hybrid"))
        typeAttribute.setPropertyValue(OTChoiceAttribute.PROPERTY_MULTISELECTION, false)
        beerTracker.attributes.add(typeAttribute)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(user, "Score", OTAttribute.TYPE_RATING))
        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(user, "Review", OTAttribute.TYPE_LONG_TEXT))


        //====================================================================================================================================================
        val stepComparisonTracker = user.newTracker("Step Devices", true)
        val fitbitAttribute = OTAttribute.createAttribute(user, "Fitbit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        fitbitAttribute.numberStyle.fractionPart = 0
        val fitbitStepConnection = OTConnection()
        fitbitStepConnection.source = FitbitStepCountMeasureFactory.makeMeasure()
        fitbitStepConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        fitbitAttribute.valueConnection = fitbitStepConnection

        val misfitAttribute = OTAttribute.createAttribute(user, "MisFit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        misfitAttribute.numberStyle.fractionPart = 0
        val misfitStepConnection = OTConnection()

        misfitStepConnection.source = MisfitStepMeasureFactory.makeMeasure()
        misfitStepConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        misfitAttribute.valueConnection = misfitStepConnection

        val googleFitAttribute = OTAttribute.createAttribute(user, "Google Fit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        googleFitAttribute.numberStyle.fractionPart = 0
        val googleFitConnection = OTConnection()
        googleFitConnection.source = GoogleFitStepsFactory.makeMeasure()
        googleFitConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        googleFitAttribute.valueConnection = googleFitConnection

        stepComparisonTracker.attributes.add(fitbitAttribute)
        stepComparisonTracker.attributes.add(misfitAttribute)
        stepComparisonTracker.attributes.add(googleFitAttribute)

        dbHelper.save(user)

        return // skip example data

        java.lang.Runnable {

            //batch input
            val end = System.currentTimeMillis()
            val start = end - 200 * DateUtils.DAY_IN_MILLIS


            val sleepItems = ArrayList<OTItem>()
            val coffeeItems = ArrayList<OTItem>()


            TimeHelper.loopForDays(start, end)
            {
                time, from, to, dayOfYear ->

                val wakeUp = from + 8 * DateUtils.HOUR_IN_MILLIS + (Math.random() * DateUtils.HOUR_IN_MILLIS * 1.5).toLong()
                sleepItems.add(
                        OTItem(
                                sleepTracker,
                                wakeUp + (Math.random() * DateUtils.HOUR_IN_MILLIS).toLong(),
                                OTItem.LoggingSource.Unspecified,
                                TimeSpan.fromPoints(from + (Math.random() * 3 * DateUtils.HOUR_IN_MILLIS - 1.5 * DateUtils.HOUR_IN_MILLIS).toLong(), wakeUp),
                                (Math.random() * 5).toFloat(),
                                ""
                        )
                )

                val numCoffeeADay = ((Math.random() * 4) + .5f).toInt()
                for (i in 0..numCoffeeADay - 1) {
                    val coffeeTime = from + ((to - from) * Math.random()).toLong()
                    coffeeItems.add(
                            OTItem(
                                    coffeeTracker,
                                    coffeeTime,
                                    OTItem.LoggingSource.Unspecified,
                                    "Americano",
                                    coffeeTime
                            )
                    )
                }
            }

            addStepsExampleItems(stepComparisonTracker)

            addExampleBeerReviews(beerTracker)

            dbHelper.save(sleepItems, sleepTracker)
            dbHelper.save(coffeeItems, coffeeTracker)


        }.run()
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
        OTItem.createItemsWithColumnArrays(stepTracker, timestamps, OTItem.LoggingSource.Unspecified, stepItems, fitbitSteps, misfitSteps, googleSteps)

        dbHelper.save(stepItems, stepTracker)
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
                            OTItem.LoggingSource.Unspecified,
                            *it
                    )
                }, beerTracker)


    }
}