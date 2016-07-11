package kr.ac.snu.hcil.omnitrack.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "omnitrack.db", null, 1) {

    abstract class TableScheme{
        val _ID = "_id"

        abstract val tableName : String

        val creationQueryString: String by lazy{
            "CREATE TABLE ${tableName} (${getCreationColumnContentString()})"
        }

        open fun getCreationColumnContentString() : String
        {
                return "${_ID} INTEGER PRIMARY KEY"
        }
    }

    abstract class TableWithNameScheme : TableScheme(){
        val NAME = "name"
        override fun getCreationColumnContentString() : String
        {
            return  super.getCreationColumnContentString() + ", ${NAME} TEXT"
        }
    }

    object UserScheme : TableWithNameScheme(){
        override val tableName: String
            get() = "omnitrack_users"

        val EMAIL = "email"

        val columnNames = arrayOf(_ID, NAME, EMAIL)

        override fun getCreationColumnContentString() : String
        {
            return  super.getCreationColumnContentString() + ", ${EMAIL} TEXT UNIQUE"
        }
    }

    object ProjectScheme : TableWithNameScheme(){
        override val tableName: String
            get() = "omnitrack_projects"

        val USER_ID = "user_id"


        val columnNames = arrayOf(_ID, NAME, USER_ID)

        override fun getCreationColumnContentString() : String
        {
            return  super.getCreationColumnContentString() + ", ${USER_ID} INTEGER"
        }
    }


    override fun onCreate(db: SQLiteDatabase) {
        println("Create Database Tables")
        Log.d("OMNITRACK", "Create Database Tables")

        db.execSQL(UserScheme.creationQueryString)
        db.execSQL(ProjectScheme.creationQueryString)

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun findUserById(id: Long, database: SQLiteDatabase) : UserEntity?{
        val result : Cursor = database.query(UserScheme.tableName, UserScheme.columnNames,  "${UserScheme._ID}=?", arrayOf(id.toString()), null, null, "_id ASC");
        result.moveToFirst()
        if(result.count == 0)
        {
            return null
        }
        else{
            val id = result.getLong(result.getColumnIndex(UserScheme._ID));
            val name = result.getString(result.getColumnIndex(UserScheme.NAME));
            val email = result.getString(result.getColumnIndex(UserScheme.EMAIL));
            val entity = UserEntity(id, name, email, ArrayList<ProjectEntity>() )
            return entity
        }
    }

    fun makeNewUser(name: String, email: String, database: SQLiteDatabase) : UserEntity{
        val values = ContentValues()
        values.put(UserScheme.NAME, name)
        values.put(UserScheme.EMAIL, email)
        val newRowId = database.insert(UserScheme.tableName, null, values)
        return UserEntity(newRowId, name, email, ArrayList<ProjectEntity>())
    }

    fun save(user: UserEntity)
    {

    }


}