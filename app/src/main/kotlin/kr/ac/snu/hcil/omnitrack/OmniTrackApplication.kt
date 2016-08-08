package kr.ac.snu.hcil.omnitrack

import android.app.Application
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTTriggerManager
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseHelper

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OmniTrackApplication : Application() {

    companion object {
        lateinit var app: OmniTrackApplication
            private set

        const val INTENT_EXTRA_OBJECT_ID_TRACKER = "trackerObjectId"
        const val INTENT_EXTRA_OBJECT_ID_ATTRIBUTE = "attributeObjectId"
        const val INTENT_EXTRA_OBJECT_ID_USER = "userObjectId"
        const val INTENT_EXTRA_OBJECT_ID_TRIGGER = "triggerObjectId"

        const val INTENT_EXTRA_ITEMBUILDER = "itemBuilderId"

        const val BROADCAST_ACTION_ALARM = "kr.ac.snu.hcil.omnitrack.action.ALARM"


        const val PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE = "item_builder_storage_foreground"

        const val PREFERENCE_KEY_BACKGROUND_ITEM_BUILDER_STORAGE = "item_builder_storage_background"
    }

    private lateinit var _currentUser: OTUser

    lateinit var dbHelper: DatabaseHelper
        private set

    val currentUser: OTUser
        get() {
            return _currentUser
        }


    lateinit var triggerManager: OTTriggerManager
        private set


    lateinit var supportedAttributeTypes: Array<OTAttribute.AttributeTypeInfo>
        private set

    override fun onCreate() {
        super.onCreate()

        app = this

        dbHelper = DatabaseHelper(this)

        val user = dbHelper.findUserById(1)
        if (user == null) {
            val defaultUser = OTUser("Young-Ho Kim", "yhkim@hcil.snu.ac.kr")
            val coffeeTracker = OTTracker("Coffee")
            coffeeTracker.attributes.add(OTAttribute.Companion.createAttribute(defaultUser, "Name", OTAttribute.TYPE_SHORT_TEXT))
            coffeeTracker.attributes.add(OTAttribute.Companion.createAttribute(defaultUser, "Drank At", OTAttribute.TYPE_TIME))

            val waterTracker = OTTracker("Water")
            waterTracker.attributes.add(OTAttribute.Companion.createAttribute(defaultUser, "Drank At", OTAttribute.TYPE_TIME))

            val sleepTracker = OTTracker("Manual Sleep")
            sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(defaultUser, "When to Bed", OTAttribute.TYPE_TIME))
            sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(defaultUser, "Slept for", OTAttribute.TYPE_TIMESPAN))
            sleepTracker.attributes.add(OTAttribute.Companion.createAttribute(defaultUser, "Memo", OTAttribute.TYPE_LONG_TEXT))

            defaultUser.trackers.add(sleepTracker)
            defaultUser.trackers.add(waterTracker)
            defaultUser.trackers.add(coffeeTracker)

            _currentUser = defaultUser
        } else {
            _currentUser = user
        }


        triggerManager = OTTriggerManager(_currentUser, if (_currentUser.dbId != null) {
            dbHelper.findTriggersOfUser(_currentUser.dbId!!)
        } else {
            null
        })


        supportedAttributeTypes = arrayOf(
                OTAttribute.AttributeTypeInfo(OTAttribute.TYPE_SHORT_TEXT, R.drawable.field_icon_shorttext, this.getString(R.string.type_shorttext_name), this.getString(R.string.type_shorttext_desc)),
                OTAttribute.AttributeTypeInfo(OTAttribute.TYPE_LONG_TEXT, R.drawable.field_icon_longtext, this.getString(R.string.type_longtext_name), this.getString(R.string.type_longtext_desc)),
                OTAttribute.AttributeTypeInfo(OTAttribute.TYPE_NUMBER, R.drawable.field_icon_number, this.getString(R.string.type_number_name), this.getString(R.string.type_number_desc)),
                OTAttribute.AttributeTypeInfo(OTAttribute.TYPE_TIME, R.drawable.field_icon_time, this.getString(R.string.type_timepoint_name), this.getString(R.string.type_timepoint_desc)),
                OTAttribute.AttributeTypeInfo(OTAttribute.TYPE_LOCATION, R.drawable.field_icon_location, this.getString(R.string.type_location_name), this.getString(R.string.type_location_desc)),
                OTAttribute.AttributeTypeInfo(OTAttribute.TYPE_TIMESPAN, R.drawable.field_icon_timer, this.getString(R.string.type_timespan_name), this.getString(R.string.type_timespan_desc)))
    }

    fun syncUserToDb(){
        dbHelper.save(_currentUser)
        for (triggerEntry in triggerManager.withIndex()) {
            dbHelper.save(triggerEntry.value, _currentUser, triggerEntry.index)
        }
        dbHelper.deleteObjects(DatabaseHelper.TriggerScheme, *triggerManager.fetchRemovedTriggerIds())
    }

    override fun onTerminate() {
        super.onTerminate()

        syncUserToDb()

        dbHelper.close()
    }
}