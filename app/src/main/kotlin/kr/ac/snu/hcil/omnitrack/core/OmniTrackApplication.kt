package kr.ac.snu.hcil.omnitrack.core

import android.app.Application
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kr.ac.snu.hcil.omnitrack.core.database.*;

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
// The annotated class is not used, but the package name is used to place the OrmaDatabase class.
public class DatabaseConfiguration { }

class OmniTrackApplication : Application() {

    private lateinit var _currentUser : OTUser

    val currentUser: OTUser
        get(){
            return _currentUser
        }

    override fun onCreate() {
        super.onCreate()

        val dbHelper = DatabaseHelper(this)

        val db : SQLiteDatabase = dbHelper.writableDatabase

        val user: UserEntity = dbHelper.findUserById(1, db) ?: dbHelper.makeNewUser("Young-Ho Kim", "yhkim@hcil.snu.ac.kr", db)

        println(user)

        _currentUser = OTUser(user)

    }

    override fun onTerminate() {
        super.onTerminate()
    }
}