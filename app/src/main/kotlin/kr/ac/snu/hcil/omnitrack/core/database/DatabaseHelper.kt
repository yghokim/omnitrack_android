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

        abstract val columnNames: Array<String>

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
        val OBJECT_ID = "object_id"
        override fun getCreationColumnContentString() : String
        {
            return  super.getCreationColumnContentString() + ", ${NAME} TEXT, ${OBJECT_ID} TEXT UNIQUE"
        }
    }

    object UserScheme : TableWithNameScheme(){
        override val tableName: String
            get() = "omnitrack_users"

        val EMAIL = "email"

        override val columnNames = arrayOf(_ID, NAME, OBJECT_ID, EMAIL)

        override fun getCreationColumnContentString() : String
        {
            return  super.getCreationColumnContentString() + ", ${EMAIL} TEXT UNIQUE"
        }
    }

    object ProjectScheme : TableWithNameScheme(){
        override val tableName: String
            get() = "omnitrack_projects"

        val USER_ID = "user_id"
        val POSITION = "position"


        override val columnNames = arrayOf(_ID, NAME, OBJECT_ID, USER_ID, POSITION)

        override fun getCreationColumnContentString() : String
        {
            return  super.getCreationColumnContentString() + ", ${USER_ID} INTEGER ${POSITION} INTEGER"
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

    fun findUserById(id: Long) : UserEntity?{
        val result = queryById(id, UserScheme)
        if(result.count == 0)
        {
            result.close()
            return null
        }
        else{
            val id = result.getLong(result.getColumnIndex(UserScheme._ID));
            val name = result.getString(result.getColumnIndex(UserScheme.NAME));
            val email = result.getString(result.getColumnIndex(UserScheme.EMAIL));
            val entity = UserEntity(id, name, email, findProjectsOfUser(id) ?: ArrayList<ProjectEntity>() )
            result.close()
            return entity
        }
    }

    fun makeNewUser(name: String, email: String) : UserEntity{
        val values = ContentValues()
        values.put(UserScheme.NAME, name)
        values.put(UserScheme.EMAIL, email)
        val newRowId = writableDatabase.insert(UserScheme.tableName, null, values)
        return UserEntity(newRowId, name, email, ArrayList<ProjectEntity>())
    }

    fun findProjectsOfUser(userId : Long) : List<ProjectEntity>? {

        val query : Cursor = readableDatabase.query(ProjectScheme.tableName, ProjectScheme.columnNames,  "${ProjectScheme.USER_ID}=?", arrayOf(userId.toString()), null, null, "${ProjectScheme.POSITION} ASC")
        query.moveToFirst()
        if(query.count == 0)
        {
            query.close()
            return null;
        }
        else{
            val result = ArrayList<ProjectEntity>()
            while(query.moveToNext())
            {
                result.add(extractProjectEntity(query))
            }

            query.close()
            return result
        }
    }

    fun extractProjectEntity(cursor: Cursor) : ProjectEntity
    {
        val id = cursor.getLong(cursor.getColumnIndex(ProjectScheme._ID))
        val name = cursor.getString(cursor.getColumnIndex(ProjectScheme.NAME))
        val objectId = cursor.getString(cursor.getColumnIndex(ProjectScheme.OBJECT_ID))
        val position = cursor.getInt(cursor.getColumnIndex(ProjectScheme.POSITION))
        val userId = cursor.getLong(cursor.getColumnIndex(ProjectScheme.USER_ID))

        return ProjectEntity(id, objectId, name, userId, position, ArrayList<TrackerEntity>());
    }

    private fun queryById(id: Long, table: TableScheme) : Cursor
    {
        val query = readableDatabase.query(table.tableName, table.columnNames, "${table._ID}=?", arrayOf(id.toString()), null, null, "${table._ID} ASC")
        query.moveToFirst()
        return query
    }

    fun add(project: ProjectEntity)
    {
        val values = ContentValues()
        values.put(ProjectScheme.OBJECT_ID, project.objectId)
        values.put(ProjectScheme.NAME, project.name)
        values.put(ProjectScheme.POSITION, project.position)
        values.put(ProjectScheme.USER_ID, project.userId)

        val newRowId = writableDatabase.insert(ProjectScheme.tableName, null, values)
        project.id = newRowId
    }

    fun update(project:ProjectEntity, changedColumns: Array<String>)
    {

    }

    fun update(user: UserEntity, changedColumns: Array<String>)
    {

    }


}