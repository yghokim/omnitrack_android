package kr.ac.snu.hcil.omnitrack

import android.app.Application
import android.graphics.Color
import android.os.AsyncTask
import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.core.*
import kr.ac.snu.hcil.omnitrack.core.attributes.*
import kr.ac.snu.hcil.omnitrack.core.database.CacheHelper
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseHelper
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitRecentSleepTimeMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.externals.fitbit.FitbitStepCountMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.externals.google.fit.GoogleFitStepsFactory
import kr.ac.snu.hcil.omnitrack.core.externals.misfit.MisfitStepMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerManager
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OTApplication : Application() {

    companion object {
        lateinit var app: OTApplication
            private set

        const val INTENT_EXTRA_OBJECT_ID_TRACKER = "trackerObjectId"
        const val INTENT_EXTRA_OBJECT_ID_ATTRIBUTE = "attributeObjectId"
        const val INTENT_EXTRA_OBJECT_ID_USER = "userObjectId"
        const val INTENT_EXTRA_OBJECT_ID_TRIGGER = "triggerObjectId"
        const val INTENT_EXTRA_DB_ID_ITEM = "itemDbId"


        const val INTENT_EXTRA_ITEMBUILDER = "itemBuilderId"

        const val BROADCAST_ACTION_TIME_TRIGGER_ALARM = "kr.ac.snu.hcil.omnitrack.action.ALARM"
        const val BROADCAST_ACTION_EVENT_TRIGGER_CHECK_ALARM = "kr.ac.snu.hcil.omnitrack.action.EVENT_TRIGGER_ALARM"


        /*
        const val BROADCAST_ACTION_SHORTCUT_PUSH_NOW = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_PUSH_NOW"
        const val BROADCAST_ACTION_SHORTCUT_OPEN_TRACKER = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_OPEN_TRACKER"
        const val BROADCAST_ACTION_SHORTCUT_INCLUDE_TRACKER = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_INCLUDE_TRACKER"
        const val BROADCAST_ACTION_SHORTCUT_EXCLUDE_TRACKER = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_EXCLUDE_TRACKER"
        const val BROADCAST_ACTION_SHORTCUT_TRACKER_INFO_CHANGED = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_TRACKER_INFO_CHANGED"
*/
        const val BROADCAST_ACTION_SHORTCUT_REFRESH = "kr.ac.snu.hcil.omnitrack.action.SHORTCUT_TRACKER_REFRESH"

        const val BROADCAST_ACTION_ITEM_ADDED = "kr.ac.snu.hcil.omnitrack.action.ITEM_ADDED"
        const val BROADCAST_ACTION_ITEM_REMOVED = "kr.ac.snu.hcil.omnitrack.action.ITEM_REMOVED"
        const val BROADCAST_ACTION_ITEM_EDITED = "kr.ac.snu.hcil.omnitrack.action.ITEM_EDITED"

        const val BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED = "kr.ac.snu.hcil.omnitrack.action.BACKGROUND_LOGGING_STARTED"
        const val BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED = "kr.ac.snu.hcil.omnitrack.action.BACKGROUND_LOGGING_SUCCEEDED"

        const val PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE = "item_builder_storage_foreground"

        const val PREFERENCE_KEY_BACKGROUND_ITEM_BUILDER_STORAGE = "item_builder_storage_background"
    }

    private lateinit var _currentUser: OTUser

    lateinit var dbHelper: DatabaseHelper
        private set

    val cacheHelper: CacheHelper by lazy {
        CacheHelper(this)
    }

    val currentUser: OTUser
        get() {
            return _currentUser
        }

    val colorPalette: IntArray by lazy {
        this.resources.getStringArray(R.array.colorPaletteArray).map { Color.parseColor(it) }.toIntArray()
    }

    val googleApiKey: String by lazy {
        this.resources.getString(R.string.google_maps_key)
    }

    lateinit var triggerManager: OTTriggerManager
        private set


    lateinit var supportedAttributePresets: Array<AttributePresetInfo>
        private set

    override fun onCreate() {
        super.onCreate()

        app = this

        dbHelper = DatabaseHelper(this)

        var initialRun = false
        val user = dbHelper.findUserById(1)
        if (user == null) {
            val defaultUser = OTUser("Young-Ho Kim", "yhkim@hcil.snu.ac.kr")
            _currentUser = defaultUser

            initialRun = true

        } else {
            _currentUser = user
        }


        triggerManager = OTTriggerManager(_currentUser, if (_currentUser.dbId != null) {
            dbHelper.findTriggersOfUser(_currentUser.dbId!!)
        } else {
            null
        })


        for (service in OTExternalService.availableServices) {
            if (service.state == OTExternalService.ServiceState.ACTIVATED) {
                service.prepareServiceAsync({
                    result ->

                })
            }
        }

        for (tracker in currentUser.getTrackersOnShortcut()) {
            OTShortcutManager += tracker
        }


        supportedAttributePresets = arrayOf(
                SimpleAttributePresetInfo(OTAttribute.TYPE_SHORT_TEXT, R.drawable.field_icon_shorttext, this.getString(R.string.type_shorttext_name), this.getString(R.string.type_shorttext_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_LONG_TEXT, R.drawable.field_icon_longtext, this.getString(R.string.type_longtext_name), this.getString(R.string.type_longtext_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_NUMBER, R.drawable.field_icon_number, this.getString(R.string.type_number_name), this.getString(R.string.type_number_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_RATING, R.drawable.field_icon_rating, this.getString(R.string.type_rating_name), this.getString(R.string.type_rating_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_TIME, R.drawable.field_icon_time, this.getString(R.string.type_timepoint_name), this.getString(R.string.type_timepoint_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_TIMESPAN, R.drawable.field_icon_timer, this.getString(R.string.type_timespan_name), this.getString(R.string.type_timespan_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_LOCATION, R.drawable.field_icon_location, this.getString(R.string.type_location_name), this.getString(R.string.type_location_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_IMAGE, R.drawable.field_icon_image, this.getString(R.string.type_image_name), this.getString(R.string.type_image_desc)),
                SimpleAttributePresetInfo(OTAttribute.TYPE_AUDIO, R.drawable.field_icon_audio, this.getString(R.string.type_audio_record_name), this.getString(R.string.type_audio_record_desc)),

                AttributePresetInfo(R.drawable.field_icon_singlechoice, this.getString(R.string.type_single_choice_name), this.getString(R.string.type_single_choice_desc),
                        { user, columnName ->
                            val attr = OTAttribute.createAttribute(user, columnName, OTAttribute.TYPE_CHOICE) as OTChoiceAttribute
                            attr.allowedMultiSelection = false
                            attr
                        }),

                AttributePresetInfo(R.drawable.field_icon_multiplechoice, this.getString(R.string.type_multiple_choices_name), this.getString(R.string.type_multiple_choices_desc),
                        { user, columnName ->
                            val attr = OTAttribute.createAttribute(user, columnName, OTAttribute.TYPE_CHOICE) as OTChoiceAttribute
                            attr.allowedMultiSelection = true
                            attr
                        })

        )

        if (initialRun) {
            AsyncTask.execute {
                createExampleTrackers()
            }
        }
    }

    fun syncUserToDb() {
        dbHelper.save(_currentUser)
        for (triggerEntry in triggerManager.withIndex()) {
            dbHelper.save(triggerEntry.value, _currentUser, triggerEntry.index)
        }
        dbHelper.deleteObjects(DatabaseHelper.TriggerScheme, *triggerManager.fetchRemovedTriggerIds())

        OTTimeTriggerAlarmManager.storeTableToPreferences()
    }

    override fun onTerminate() {
        super.onTerminate()

        syncUserToDb()

        dbHelper.close()
    }

    private fun createExampleTrackers() {
        //====================================================================================================================================================
        val coffeeTracker = currentUser.newTracker("Coffee", true)
        coffeeTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Name", OTAttribute.TYPE_SHORT_TEXT))
        coffeeTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Drank At", OTAttribute.TYPE_TIME))

        val waterTracker = currentUser.newTracker("Water", true)
        waterTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Drank At", OTAttribute.TYPE_TIME))


        //====================================================================================================================================================
        val sleepTracker = currentUser.newTracker("Sleep", true)

        val sleepTimeAttribute = OTAttribute.Companion.createAttribute(currentUser, "Sleep Duration", OTAttribute.TYPE_TIMESPAN)
        sleepTimeAttribute.setPropertyValue(OTTimeSpanAttribute.PROPERTY_GRANULARITY, OTTimeAttribute.GRANULARITY_MINUTE)

        val sleepTimeConnection = OTConnection()
        sleepTimeConnection.source = FitbitRecentSleepTimeMeasureFactory.makeMeasure()
        sleepTimeConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, 0)
        sleepTimeAttribute.valueConnection = sleepTimeConnection

        sleepTracker.attributes.add(sleepTimeAttribute)
        sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Quality", OTAttribute.TYPE_RATING))
        sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Memo", OTAttribute.TYPE_LONG_TEXT))

        //====================================================================================================================================================
        val beerTracker = currentUser.newTracker("Beer", true)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Photo", OTAttribute.TYPE_IMAGE))
        val dateAttribute = OTAttribute.createAttribute(currentUser, "Date", OTAttribute.TYPE_TIME)
        dateAttribute.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        beerTracker.attributes.add(dateAttribute)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Name", OTAttribute.TYPE_SHORT_TEXT))

        val typeAttribute = OTAttribute.createAttribute(currentUser, "Type", OTAttribute.TYPE_CHOICE)
        typeAttribute.setPropertyValue(OTChoiceAttribute.PROPERTY_ENTRIES, UniqueStringEntryList("Lager", "Stout", "Ale", "Hybrid"))
        typeAttribute.setPropertyValue(OTChoiceAttribute.PROPERTY_MULTISELECTION, false)
        beerTracker.attributes.add(typeAttribute)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Score", OTAttribute.TYPE_RATING))
        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Review", OTAttribute.TYPE_LONG_TEXT))


        //====================================================================================================================================================
        val stepComparisonTracker = currentUser.newTracker("Step Devices", true)
        val fitbitAttribute = OTAttribute.createAttribute(currentUser, "Fitbit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        fitbitAttribute.numberStyle.fractionPart = 0
        val fitbitStepConnection = OTConnection()
        fitbitStepConnection.source = FitbitStepCountMeasureFactory.makeMeasure()
        fitbitStepConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        fitbitAttribute.valueConnection = fitbitStepConnection

        val misfitAttribute = OTAttribute.createAttribute(currentUser, "MisFit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        misfitAttribute.numberStyle.fractionPart = 0
        val misfitStepConnection = OTConnection()

        misfitStepConnection.source = MisfitStepMeasureFactory.makeMeasure()
        misfitStepConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        misfitAttribute.valueConnection = misfitStepConnection

        val googleFitAttribute = OTAttribute.createAttribute(currentUser, "Google Fit", OTAttribute.TYPE_NUMBER) as OTNumberAttribute
        googleFitAttribute.numberStyle.fractionPart = 0
        val googleFitConnection = OTConnection()
        googleFitConnection.source = GoogleFitStepsFactory.makeMeasure()
        googleFitConnection.rangedQuery = OTTimeRangeQuery(OTTimeRangeQuery.TYPE_PIVOT_TIMESTAMP, OTTimeRangeQuery.BIN_SIZE_DAY, -1)
        googleFitAttribute.valueConnection = googleFitConnection

        stepComparisonTracker.attributes.add(fitbitAttribute)
        stepComparisonTracker.attributes.add(misfitAttribute)
        stepComparisonTracker.attributes.add(googleFitAttribute)

        dbHelper.save(_currentUser)

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
        OTItem.createItemsWithColumnArrays(stepTracker, timestamps, stepItems, fitbitSteps, misfitSteps, googleSteps)

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
                            *it
                    )
                }, beerTracker)


    }
}