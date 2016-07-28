package kr.ac.snu.hcil.omnitrack.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.NamedObject
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import java.text.AttributedCharacterIterator
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "omnitrack.db", null, 1) {

    abstract class TableScheme {

        val _ID = "_id"
        val LOGGED_AT = "logged_at"
        val UPDATED_AT = "updated_at"

        abstract val tableName: String

        abstract val intrinsicColumnNames: Array<String>

        val columnNames: Array<String> by lazy {
            Array<String>((intrinsicColumnNames?.size) + 3) {
                index ->
                when (index) {
                    0 -> _ID
                    1 -> LOGGED_AT
                    2 -> UPDATED_AT
                    else -> intrinsicColumnNames[index - 3]
                }
            }
        }


        val creationQueryString: String by lazy {
            "CREATE TABLE ${tableName} (${_ID} INTEGER PRIMARY KEY, ${creationColumnContentString}, ${LOGGED_AT} INTEGER, ${UPDATED_AT} INTEGER);"
        }

        abstract val creationColumnContentString: String

        open val indexCreationQueryString: String = ""

        fun makeIndexQueryString(unique: Boolean, name: String, vararg columns: String): String {
            return "CREATE${if (unique) {
                " UNIQUE"
            } else {
                ""
            }} INDEX ${tableName}_$name ON $tableName (${columns.joinToString(", ")});"
        }

        fun makeForeignKeyStatementString(column: String, foreignTable: String): String {
            return "$column INTEGER REFERENCES $foreignTable"
        }
    }

    abstract class TableWithNameScheme : TableScheme() {
        val NAME = "name"
        val OBJECT_ID = "object_id"

        override val intrinsicColumnNames: Array<String> = arrayOf(NAME, OBJECT_ID)

        override val creationColumnContentString: String = "${NAME} TEXT, ${OBJECT_ID} TEXT"

        override val indexCreationQueryString: String = makeIndexQueryString(true, name = "obj_id_unique", columns = OBJECT_ID)
    }

    object UserScheme : TableWithNameScheme() {
        override val tableName: String
            get() = "omnitrack_users"

        val EMAIL = "email"
        val ATTR_ID_SEED = "attribute_id_seed"

        override val intrinsicColumnNames = super.intrinsicColumnNames + arrayOf(EMAIL, ATTR_ID_SEED)

        override val creationColumnContentString: String = super.creationColumnContentString + ", ${EMAIL} TEXT, ${ATTR_ID_SEED} INTEGER"
    }

    object TrackerScheme : TableWithNameScheme() {
        override val tableName: String
            get() = "omnitrack_trackers"

        val USER_ID = "user_id"
        val POSITION = "position"
        val COLOR = "color"

        override val intrinsicColumnNames = super.intrinsicColumnNames + arrayOf(USER_ID, POSITION, COLOR)

        override val creationColumnContentString: String = super.creationColumnContentString + ", ${makeForeignKeyStatementString(USER_ID, UserScheme.tableName)}, ${COLOR} INTEGER, ${POSITION} INTEGER"
    }

    object AttributeScheme : TableWithNameScheme() {
        override val tableName: String = "omnitrack_attributes"

        val TRACKER_ID = "tracker_id"
        val POSITION = "position"
        val SETTING_DATA = "setting_data"
        val TYPE = "type"

        override val intrinsicColumnNames: Array<String> = super.intrinsicColumnNames + arrayOf(TRACKER_ID, TYPE, POSITION, SETTING_DATA)

        override val creationColumnContentString = super.creationColumnContentString + ", ${makeForeignKeyStatementString(TRACKER_ID, TrackerScheme.tableName)}, ${AttributeScheme.POSITION} INTEGER, ${AttributeScheme.TYPE} INTEGER, ${AttributeScheme.SETTING_DATA} TEXT"
    }

    object TriggerScheme : TableWithNameScheme() {
        override val tableName: String = "omnitrack_triggers"
        val USER_ID = "user_id"
        val TRACKER_OBJECT_ID = "tracker_object_id"
        val POSITION = "position"
        val PROPERTIES = "properties"
        val TYPE = "type"
        val IS_ON = "is_on"

        override val intrinsicColumnNames: Array<String> = super.intrinsicColumnNames + arrayOf(TRACKER_OBJECT_ID, TYPE, POSITION, IS_ON, PROPERTIES)

        override val creationColumnContentString = super.creationColumnContentString + ", ${makeForeignKeyStatementString(USER_ID, UserScheme.tableName)}, ${TriggerScheme.TRACKER_OBJECT_ID} TEXT, ${TriggerScheme.POSITION} INTEGER, ${TriggerScheme.IS_ON} INTEGER, ${TriggerScheme.TYPE} INTEGER, ${TriggerScheme.PROPERTIES} TEXT"
    }

    object ItemScheme : TableScheme() {
        override val tableName: String = "omnitrack_items"

        val TRACKER_ID = "tracker_id"
        val VALUES_JSON = "values_json"

        val KEY_TIME_TIMESTAMP = "key_time_timestamp"
        val KEY_TIME_GRANULARITY = "key_time_granularity"
        val KEY_TIME_TIMEZONE = "key_time_timezone"

        override val intrinsicColumnNames: Array<String> = arrayOf(TRACKER_ID, VALUES_JSON, KEY_TIME_TIMESTAMP, KEY_TIME_GRANULARITY, KEY_TIME_TIMEZONE)
        override val creationColumnContentString: String = "${makeForeignKeyStatementString(TRACKER_ID, TrackerScheme.tableName)}, $VALUES_JSON TEXT, $KEY_TIME_TIMESTAMP INTEGER, $KEY_TIME_GRANULARITY TEXT, $KEY_TIME_TIMEZONE TEXT"

        override val indexCreationQueryString: String =
                makeIndexQueryString(false, "tracker_and_timestamp", TRACKER_ID, KEY_TIME_TIMESTAMP)
    }

    override fun onCreate(db: SQLiteDatabase) {
        println("Create Database Tables")
        Log.d("OMNITRACK", "Create Database Tables")

        val tables = arrayOf(UserScheme, TrackerScheme, AttributeScheme, TriggerScheme, ItemScheme)

        for (scheme in tables) {
            db.execSQL(scheme.creationQueryString)
            db.execSQL(scheme.indexCreationQueryString)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun findUserById(id: Long): OTUser? {
        val result = queryById(id, UserScheme)
        if (result.count == 0) {
            result.close()
            return null
        } else {
            val objectId = result.getString(result.getColumnIndex(UserScheme.OBJECT_ID));
            val name = result.getString(result.getColumnIndex(UserScheme.NAME));
            val email = result.getString(result.getColumnIndex(UserScheme.EMAIL));
            val seed = result.getLong(result.getColumnIndex(UserScheme.ATTR_ID_SEED))
            val entity = OTUser(objectId, id, name, email, seed, findTrackersOfUser(id))
            result.close()
            return entity
        }
    }

    fun findTrackersOfUser(userId: Long): List<OTTracker>? {

        val query: Cursor = readableDatabase.query(TrackerScheme.tableName, TrackerScheme.columnNames, "${TrackerScheme.USER_ID}=?", arrayOf(userId.toString()), null, null, "${TrackerScheme.POSITION} ASC")
        val result = ArrayList<OTTracker>()
        if (query.moveToFirst()) {
            do {
                result.add(extractTrackerEntity(query))
            } while (query.moveToNext())

            query.close()
            return result
        } else return null
    }

    fun findAttributesOfTracker(trackerId: Long): List<OTAttribute<out Any>>? {

        val query: Cursor = readableDatabase.query(AttributeScheme.tableName, AttributeScheme.columnNames, "${AttributeScheme.TRACKER_ID}=?", arrayOf(trackerId.toString()), null, null, "${AttributeScheme.POSITION} ASC")

        if (query.moveToFirst()) {
            val result = ArrayList<OTAttribute<out Any>>()
            do {
                result.add(extractAttributeEntity(query))
            } while (query.moveToNext())

            query.close()
            return result
        } else return null
    }

    fun findTriggersOfUser(userId: Long): List<OTTrigger>? {
        val query: Cursor = readableDatabase.query(TriggerScheme.tableName, TriggerScheme.columnNames, "${TriggerScheme.USER_ID}=?", arrayOf(userId.toString()), null, null, "${TriggerScheme.POSITION} ASC")
        if (query.moveToFirst()) {
            val result = ArrayList<OTTrigger>()
            do {
                result.add(extractTriggerEntity(query))
            } while (query.moveToNext())

            query.close()
            return result
        } else return null

    }

    fun extractTriggerEntity(cursor: Cursor): OTTrigger {
        val id = cursor.getLong(cursor.getColumnIndex(TriggerScheme._ID))
        val name = cursor.getString(cursor.getColumnIndex(TriggerScheme.NAME))
        val objectId = cursor.getString(cursor.getColumnIndex(TriggerScheme.OBJECT_ID))
        val type = cursor.getInt(cursor.getColumnIndex(TriggerScheme.TYPE))
        val trackerObjectId = cursor.getString(cursor.getColumnIndex(TriggerScheme.TRACKER_OBJECT_ID))
        val serializedProperties = cursor.getString(cursor.getColumnIndex(TriggerScheme.PROPERTIES))
        val isOn = when (cursor.getInt(cursor.getColumnIndex(TriggerScheme.IS_ON))) {0 -> false
            1 -> true
            else -> false
        }

        return OTTrigger.makeInstance(objectId, id, type, name, trackerObjectId, isOn, serializedProperties)
    }

    fun extractTrackerEntity(cursor: Cursor): OTTracker {
        val id = cursor.getLong(cursor.getColumnIndex(TrackerScheme._ID))
        val name = cursor.getString(cursor.getColumnIndex(TrackerScheme.NAME))
        val objectId = cursor.getString(cursor.getColumnIndex(TrackerScheme.OBJECT_ID))
        val color = cursor.getInt(cursor.getColumnIndex(TrackerScheme.COLOR))


        return OTTracker(objectId, id, name, color, findAttributesOfTracker(id))
    }

    fun extractAttributeEntity(cursor: Cursor): OTAttribute<out Any> {
        val id = cursor.getLong(cursor.getColumnIndex(AttributeScheme._ID))
        val name = cursor.getString(cursor.getColumnIndex(AttributeScheme.NAME))
        val objectId = cursor.getString(cursor.getColumnIndex(AttributeScheme.OBJECT_ID))
        val type = cursor.getInt(cursor.getColumnIndex(AttributeScheme.TYPE))
        val settingData = cursor.getString(cursor.getColumnIndex(AttributeScheme.SETTING_DATA))

        return OTAttribute.createAttribute(objectId, id, name, type, settingData)
    }

    private fun queryById(id: Long, table: TableScheme): Cursor {
        val query = readableDatabase.query(table.tableName, table.columnNames, "${table._ID}=?", arrayOf(id.toString()), null, null, "${table._ID} ASC")
        query.moveToFirst()
        return query
    }

    fun deleteObjects(scheme: TableScheme, vararg ids: Long) {
        val ids = ids.map { "${scheme._ID}=${it.toString()}" }.toTypedArray()
        if (ids.size > 0) {
            writableDatabase.delete(scheme.tableName, ids.joinToString(" OR "), null)
        }
    }

    private fun saveObject(objRef: IDatabaseStorable, values: ContentValues, scheme: TableScheme): Boolean {
        val now = System.currentTimeMillis()
        values.put(scheme.UPDATED_AT, now)

        val db = writableDatabase
        val transactionMode = !db.inTransaction()

        if (transactionMode) db.beginTransaction()

        try {
            if (objRef.dbId != null) //update
            {
                val numAffected = db.update(scheme.tableName, values, "${scheme._ID}=?", arrayOf(objRef.dbId.toString()))
                if (numAffected == 1) {
                    println("updated db object : ${scheme.tableName}, id: ${objRef.dbId}, updated at ${now}")
                } else { // something wrong
                    throw Exception("Something is wrong saving user in Db")
                }
            } else { //new
                if (!values.containsKey(scheme.LOGGED_AT)) values.put(scheme.LOGGED_AT, now)
                val newRowId = db.insert(scheme.tableName, null, values)
                if (newRowId != -1L) {
                    objRef.dbId = newRowId
                    println("added db object : ${scheme.tableName}, id: $newRowId, logged at ${now}")
                } else {
                    throw Exception("Object insertion failed - ${scheme.tableName}")
                }
            }

            if (transactionMode)
                db.setTransactionSuccessful()
            return true
        } catch(e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            if (transactionMode)
                db.endTransaction()
        }
    }

    private fun baseContentValuesOfNamed(objRef: NamedObject, scheme: TableWithNameScheme): ContentValues {
        val values = ContentValues()
        values.put(scheme.NAME, objRef.name)
        values.put(scheme.OBJECT_ID, objRef.objectId)

        return values
    }

    fun save(trigger: OTTrigger, owner: OTUser, position: Int) {
        val values = baseContentValuesOfNamed(trigger, TriggerScheme)

        values.put(TriggerScheme.USER_ID, owner.dbId)
        values.put(TriggerScheme.TYPE, trigger.typeId)
        values.put(TriggerScheme.PROPERTIES, trigger.getSerializedProperties())
        values.put(TriggerScheme.TRACKER_OBJECT_ID, trigger.trackerObjectId)
        values.put(TriggerScheme.POSITION, position)
        values.put(TriggerScheme.IS_ON, trigger.isOn)

        saveObject(trigger, values, TriggerScheme)
    }

    fun save(attribute: OTAttribute<out Any>, position: Int) {
        val values = baseContentValuesOfNamed(attribute, AttributeScheme)

        values.put(AttributeScheme.POSITION, position)
        values.put(AttributeScheme.TYPE, attribute.typeId)
        values.put(AttributeScheme.TRACKER_ID, attribute.owner?.dbId ?: null)
        values.put(AttributeScheme.SETTING_DATA, attribute.getSerializedProperties())

        saveObject(attribute, values, AttributeScheme)
    }

    fun save(tracker: OTTracker, position: Int) {
        val values = baseContentValuesOfNamed(tracker, TrackerScheme)
        values.put(TrackerScheme.POSITION, position)
        values.put(TrackerScheme.COLOR, tracker.color)
        values.put(TrackerScheme.USER_ID, tracker.owner?.dbId ?: null)

        if (saveObject(tracker, values, TrackerScheme)) {
            writableDatabase.beginTransaction()

            deleteObjects(AttributeScheme, *tracker.fetchRemovedAttributeIds())

            for (child in tracker.attributes.iterator().withIndex()) {
                save(child.value, child.index)
            }

            writableDatabase.setTransactionSuccessful()
            writableDatabase.endTransaction()
        }
    }

    fun save(user: OTUser) {
        val values = baseContentValuesOfNamed(user, UserScheme)
        values.put(UserScheme.EMAIL, user.email)
        values.put(UserScheme.ATTR_ID_SEED, user.attributeIdSeed)


        if (saveObject(user, values, UserScheme)) {

            writableDatabase.beginTransaction()
            deleteObjects(TrackerScheme, *user.fetchRemovedTrackerIds())

            for (child in user.trackers.iterator().withIndex()) {
                save(child.value, child.index)
            }
            writableDatabase.setTransactionSuccessful()
            writableDatabase.endTransaction()
        }
    }

    //Item API===============================

    fun save(item: OTItem, tracker: OTTracker) {
        val values = ContentValues()

        values.put(ItemScheme.TRACKER_ID, tracker.dbId)
        values.put(ItemScheme.VALUES_JSON, item.getSerializedValueTable(tracker))
        if (item.timestamp != -1L) {
            values.put(ItemScheme.LOGGED_AT, item.timestamp)
        }

        saveObject(item, values, ItemScheme)
    }

    fun getItems(tracker: OTTracker, listOut: ArrayList<OTItem>): Int {
        val cursor = readableDatabase.query(ItemScheme.tableName, ItemScheme.columnNames, "${ItemScheme.TRACKER_ID}=?", arrayOf(tracker.dbId.toString()), null, null, "${ItemScheme.LOGGED_AT} DESC")

        var count = 0
        if (cursor.moveToFirst()) {
            do {
                listOut.add(extractItemEntity(cursor, tracker))
                count++
            } while (cursor.moveToNext())
        }

        return count
    }


    fun extractItemEntity(cursor: Cursor, tracker: OTTracker): OTItem {
        val id = cursor.getLong(cursor.getColumnIndex(ItemScheme._ID))
        val serializedValues = cursor.getString(cursor.getColumnIndex(ItemScheme.VALUES_JSON))
        //TODO keytime columns
        //val KEY_TIME_TIMESTAMP = "key_time_timestamp"
        //val KEY_TIME_GRANULARITY = "key_time_granularity"
        //val KEY_TIME_TIMEZONE = "key_time_timezone"
        val timestamp = cursor.getLong(cursor.getColumnIndex(ItemScheme.LOGGED_AT))

        return OTItem(id, tracker.objectId, serializedValues, timestamp)
    }
}