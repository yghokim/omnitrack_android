package kr.ac.snu.hcil.omnitrack

import android.app.Application
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.*;
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OmniTrackApplication : Application() {
    companion object{
        lateinit var app : OmniTrackApplication
            private set

        const val INTENT_EXTRA_OBJECT_ID_TRACKER = "trackerObjectId"
        const val INTENT_EXTRA_OBJECT_ID_ATTRIBUTE = "attributeObjectId"
        const val INTENT_EXTRA_OBJECT_ID_USER = "userObjectId"
        const val INTENT_EXTRA_ITEMBUILDER = "itemBuilderId"


        const val PREFERENCE_KEY_FOREGROUND_ITEM_BUILDER_STORAGE = "item_builder_storage_foreground"

        const val PREFERENCE_KEY_BACKGROUND_ITEM_BUILDER_STORAGE = "item_builder_storage_background"
    }

    private lateinit var _currentUser : OTUser

    lateinit var dbHelper : DatabaseHelper
        private set

    val currentUser: OTUser
        get(){
            return _currentUser
        }

    override fun onCreate() {
        super.onCreate()

        app = this

        dbHelper = DatabaseHelper(this)

        _currentUser = dbHelper.findUserById(1) ?: OTUser("Young-Ho Kim", "yhkim@hcil.snu.ac.kr")
    }

    fun syncUserToDb(){
        dbHelper.save(_currentUser)
    }

    override fun onTerminate() {
        super.onTerminate()

        syncUserToDb()

        dbHelper.close()
    }
}