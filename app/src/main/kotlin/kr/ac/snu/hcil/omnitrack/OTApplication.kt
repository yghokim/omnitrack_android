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
import kr.ac.snu.hcil.omnitrack.core.externals.misfit.MisfitStepMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTriggerAlarmManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerManager
import kr.ac.snu.hcil.omnitrack.utils.TimeHelper
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList
import java.math.BigDecimal
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

        for (tracker in currentUser.getTrackersOnShortcut())
        {
            OTShortcutManager+= tracker
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
        val dateAttribute = OTAttribute.createAttribute(currentUser, "Date", OTAttribute.TYPE_TIME)
        dateAttribute.setPropertyValue(OTTimeAttribute.GRANULARITY, OTTimeAttribute.GRANULARITY_DAY)
        beerTracker.attributes.add(dateAttribute)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Name", OTAttribute.TYPE_SHORT_TEXT))

        val typeAttribute = OTAttribute.createAttribute(currentUser, "Type", OTAttribute.TYPE_CHOICE)
        typeAttribute.setPropertyValue(OTChoiceAttribute.PROPERTY_ENTRIES, UniqueStringEntryList("Lager", "Dark Lager", "Pale Ale", "IPA", "Stout", "German Bock"))
        typeAttribute.setPropertyValue(OTChoiceAttribute.PROPERTY_MULTISELECTION, false)
        beerTracker.attributes.add(typeAttribute)

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Flavor", OTAttribute.TYPE_RATING))
        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Review", OTAttribute.TYPE_LONG_TEXT))

        beerTracker.attributes.add(OTAttribute.Companion.createAttribute(currentUser, "Photo", OTAttribute.TYPE_IMAGE))


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


        stepComparisonTracker.attributes.add(fitbitAttribute)
        stepComparisonTracker.attributes.add(misfitAttribute)

        dbHelper.save(_currentUser)

        java.lang.Runnable {

            //batch input
            val end = System.currentTimeMillis()
            val start = end - 200 * DateUtils.DAY_IN_MILLIS

            val stepItems = ArrayList<OTItem>()
            val sleepItems = ArrayList<OTItem>()
            val coffeeItems = ArrayList<OTItem>()

            TimeHelper.loopForDays(start, end)
            {
                time, from, to, dayOfYear ->
                stepItems.add(
                        OTItem(
                                stepComparisonTracker,
                                time,
                                BigDecimal(Math.random() * 5000 + 5000),
                                BigDecimal(Math.random() * 3000 + 4000)
                        )
                )

                sleepItems.add(
                        OTItem(
                                sleepTracker,
                                time,
                                TimeSpan.fromDuration(from + (Math.random() * 3 * DateUtils.HOUR_IN_MILLIS - 1.5 * DateUtils.HOUR_IN_MILLIS).toLong(), 5 * DateUtils.HOUR_IN_MILLIS + (Math.random() * DateUtils.HOUR_IN_MILLIS * 2.5).toLong()),
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
            dbHelper.save(stepItems, stepComparisonTracker)
            dbHelper.save(sleepItems, sleepTracker)
            dbHelper.save(coffeeItems, coffeeTracker)


        }.run()
    }
}