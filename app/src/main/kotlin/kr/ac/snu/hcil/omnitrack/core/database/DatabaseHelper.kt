package kr.ac.snu.hcil.omnitrack.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kr.ac.snu.hcil.omnitrack.core.OTProject
import kr.ac.snu.hcil.omnitrack.core.OTUser
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
            return  super.getCreationColumnContentString() + ", ${USER_ID} INTEGER, ${POSITION} INTEGER"
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

    fun findUserById(id: Long) : OTUser?{
        val result = queryById(id, UserScheme)
        if(result.count == 0)
        {
            result.close()
            return null
        }
        else{
            val id = result.getLong(result.getColumnIndex(UserScheme._ID));
            val objectId = result.getString(result.getColumnIndex(UserScheme.OBJECT_ID));
            val name = result.getString(result.getColumnIndex(UserScheme.NAME));
            val email = result.getString(result.getColumnIndex(UserScheme.EMAIL));
            val entity = OTUser(objectId, id, name, email, findProjectsOfUser(id))
            result.close()
            return entity
        }
    }

    fun findProjectsOfUser(userId : Long) : List<OTProject>? {

        val query : Cursor = readableDatabase.query(ProjectScheme.tableName, ProjectScheme.columnNames,  "${ProjectScheme.USER_ID}=?", arrayOf(userId.toString()), null, null, "${ProjectScheme.POSITION} ASC")
        query.moveToFirst()
        if(query.count == 0)
        {
            query.close()
            return null;
        }
        else{
            val result = ArrayList<OTProject>()
            while(query.moveToNext())
            {
                result.add(extractProjectEntity(query))
            }

            query.close()
            return result
        }
    }

    fun extractProjectEntity(cursor: Cursor) : OTProject
    {
        val id = cursor.getLong(cursor.getColumnIndex(ProjectScheme._ID))
        val name = cursor.getString(cursor.getColumnIndex(ProjectScheme.NAME))
        val objectId = cursor.getString(cursor.getColumnIndex(ProjectScheme.OBJECT_ID))

        return OTProject(objectId, id, name); //TODO: implement putting trackers
    }

    private fun queryById(id: Long, table: TableScheme) : Cursor
    {
        val query = readableDatabase.query(table.tableName, table.columnNames, "${table._ID}=?", arrayOf(id.toString()), null, null, "${table._ID} ASC")
        query.moveToFirst()
        return query
    }

    fun save(project: OTProject, position: Int)
    {
        val values = ContentValues()
        values.put(ProjectScheme.OBJECT_ID, project.objectId)
        values.put(ProjectScheme.NAME, project.name)
        values.put(ProjectScheme.POSITION, position)
        values.put(ProjectScheme.USER_ID, project.owner?.dbId ?: null)

        if(project.dbId != null) // update
        {
            val numAffected = writableDatabase.update(ProjectScheme.tableName, values, "${ProjectScheme._ID}=?", arrayOf(project.dbId.toString()))
            if(numAffected==1)
            {

            }
            else{ // something wrong
                throw Exception("Something is wrong saving project in Db")
            }

        }else{ // create new
            val newRowId = writableDatabase.insert(ProjectScheme.tableName, null, values)
            project.dbId = newRowId
        }

        //TODO store trackers
    }

    fun save(user : OTUser)
    {
        val values = ContentValues()
        values.put(UserScheme.NAME, user.name)
        values.put(UserScheme.EMAIL, user.email)
        values.put(UserScheme.OBJECT_ID, user.objectId)

        if(user.dbId != null) // update
        {
            val numAffected = writableDatabase.update(UserScheme.tableName, values, "${UserScheme._ID}=?", arrayOf(user.dbId.toString()))
            if(numAffected==1)
            {

            }
            else{ // something wrong
                throw Exception("Something is wrong saving user in Db")
            }
        }
        else{ //create

            val newRowId = writableDatabase.insert(UserScheme.tableName, null, values)
            user.dbId = newRowId
        }

        writableDatabase.delete(ProjectScheme.tableName, "${ProjectScheme._ID}=?", user.fetchRemovedProjectIds().map{ it.toString() }.toTypedArray())

        for(child in user.projects.iterator().withIndex())
        {
            save(child.value, child.index)
        }

    }


}