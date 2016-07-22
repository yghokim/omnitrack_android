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

        deleteDatabase("omnitrack.db")

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