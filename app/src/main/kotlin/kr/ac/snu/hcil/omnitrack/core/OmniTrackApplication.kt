package kr.ac.snu.hcil.omnitrack.core

import android.app.Application
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kr.ac.snu.hcil.omnitrack.core.database.*;

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

        app = this

        dbHelper = DatabaseHelper(this)

        val user: UserEntity = dbHelper.findUserById(1) ?: dbHelper.makeNewUser("Young-Ho Kim", "yhkim@hcil.snu.ac.kr")

        println(user)

        _currentUser = OTUser(user)
    }

    override fun onTerminate() {
        super.onTerminate()

        dbHelper.close()
    }
}