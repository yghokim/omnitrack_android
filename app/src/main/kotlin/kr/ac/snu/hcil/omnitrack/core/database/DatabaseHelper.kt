package kr.ac.snu.hcil.omnitrack.core.database

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.utils.toBoolean
import kr.ac.snu.hcil.omnitrack.utils.toInt
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-07-11
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "omnitrack.db", null, 1) {

    companion object{
        const val SAVE_RESULT_NEW = 1
        const val SAVE_RESULT_EDIT = 2
        const val SAVE_RESULT_FAIL = 0

    }

    abstract class TableWithNameScheme(isObjectIdUnique: Boolean = true) : TableScheme() {
        val NAME = "name"
        val OBJECT_ID = "object_id"

        override val intrinsicColumnNames: Array<String> = arrayOf(NAME, OBJECT_ID)

        override val creationColumnContentString: String = "${NAME} TEXT, ${OBJECT_ID} TEXT"

        init {
            if (isObjectIdUnique)
                appendIndexQueryString(true, name = "obj_id_unique", columns = OBJECT_ID)
        }
    }
/*
    object UserScheme : TableWithNameScheme() {
        override val tableName: String
            get() = "omnitrack_users"

        val EMAIL = "email"
        val ATTR_ID_SEED = "attribute_id_seed"

        override val intrinsicColumnNames = super.intrinsicColumnNames + arrayOf(EMAIL, ATTR_ID_SEED)

        override val creationColumnContentString: String = super.creationColumnContentString + ", ${EMAIL} TEXT, ${ATTR_ID_SEED} INTEGER"
    }*/

    object TrackerScheme : TableWithNameScheme() {
        override val tableName: String
            get() = "omnitrack_trackers"

        val USER_ID = "user_id"
        val POSITION = "position"
        val COLOR = "color"
        val ATTR_ID_SEED = "attribute_id_seed"
        val IS_ON_SHORTCUT = "is_on_shortcut"

        override val intrinsicColumnNames = super.intrinsicColumnNames + arrayOf(USER_ID, POSITION, COLOR, IS_ON_SHORTCUT, ATTR_ID_SEED)

        override val creationColumnContentString: String = super.creationColumnContentString + ", ${USER_ID} TEXT, ${COLOR} INTEGER, ${POSITION} INTEGER, ${IS_ON_SHORTCUT} INTEGER, ${ATTR_ID_SEED} INTEGER"

        init {
            appendIndexQueryString(name = "user_id_index", columns = USER_ID, unique = false)
        }
    }

    object AttributeScheme : TableWithNameScheme(false) {
        override val tableName: String = "omnitrack_attributes"

        val TRACKER_ID = "tracker_id"
        val POSITION = "position"
        val PROPERTY_DATA = "property_data"
        val CONNECTION_DATA = "connection_data"
        val TYPE = "type"
        val IS_REQUIRED = "is_required"

        override val intrinsicColumnNames: Array<String> = super.intrinsicColumnNames + arrayOf(TRACKER_ID, TYPE, POSITION, IS_REQUIRED, PROPERTY_DATA, CONNECTION_DATA)

        override val creationColumnContentString = super.creationColumnContentString + ", ${makeForeignKeyStatementString(TRACKER_ID, TrackerScheme.tableName)}, ${AttributeScheme.POSITION} INTEGER, ${AttributeScheme.IS_REQUIRED} INTEGER, ${AttributeScheme.TYPE} INTEGER, ${AttributeScheme.PROPERTY_DATA} TEXT, ${AttributeScheme.CONNECTION_DATA} TEXT"

        init {
            appendIndexQueryString(true, "tracker_and_obj_id_unique", TRACKER_ID, OBJECT_ID)
        }
    }

    object TriggerScheme : TableWithNameScheme() {
        override val tableName: String = "omnitrack_triggers"
        val USER_ID = "user_id"
        val TRACKER_OBJECT_IDS = "tracker_object_ids"
        val POSITION = "position"
        val PROPERTIES = "properties"
        val TYPE = "type"
        val IS_ON = "is_on"
        val ACTION = "action"
        val LAST_TRIGGERED_TIME = "last_triggered_time"

        override val intrinsicColumnNames: Array<String> = super.intrinsicColumnNames + arrayOf(TRACKER_OBJECT_IDS, TYPE, POSITION, IS_ON, ACTION, LAST_TRIGGERED_TIME, PROPERTIES)

        override val creationColumnContentString = super.creationColumnContentString + ", ${USER_ID} TEXT, ${TriggerScheme.TRACKER_OBJECT_IDS} TEXT, ${TriggerScheme.POSITION} INTEGER, ${TriggerScheme.IS_ON} INTEGER, ${TriggerScheme.ACTION} INTEGER, ${TriggerScheme.LAST_TRIGGERED_TIME} INTEGER, ${TriggerScheme.TYPE} INTEGER, ${TriggerScheme.PROPERTIES} TEXT"
    }

    object ItemScheme : TableScheme() {
        override val tableName: String = "omnitrack_items"

        val TRACKER_ID = "tracker_id"
        val VALUES_JSON = "values_json"

        val SOURCE_TYPE = "source_type"

        val KEY_TIME_TIMESTAMP = "key_time_timestamp"
        val KEY_TIME_GRANULARITY = "key_time_granularity"
        val KEY_TIME_TIMEZONE = "key_time_timezone"

        override val intrinsicColumnNames: Array<String> = arrayOf(TRACKER_ID, SOURCE_TYPE, VALUES_JSON, KEY_TIME_TIMESTAMP, KEY_TIME_GRANULARITY, KEY_TIME_TIMEZONE)
        override val creationColumnContentString: String = "${makeForeignKeyStatementString(TRACKER_ID, TrackerScheme.tableName)}, $SOURCE_TYPE INTEGER, $VALUES_JSON TEXT, $KEY_TIME_TIMESTAMP INTEGER, $KEY_TIME_GRANULARITY TEXT, $KEY_TIME_TIMEZONE TEXT"

        init {
            appendIndexQueryString(false, "tracker_and_timestamp", TRACKER_ID, KEY_TIME_TIMESTAMP)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        println("Create Database Tables")

        val tables = arrayOf(TrackerScheme, AttributeScheme, TriggerScheme, ItemScheme)

        for (scheme in tables) {
            db.execSQL(scheme.creationQueryString)
            scheme.indexCreationQueries.forEach {
                db.execSQL(it)
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /*
    fun findUserById(id: Long): OTUser? {
        val result = queryById(id, UserScheme)
        if (result.count == 0) {
            result.close()
            return null
        } else {
            val objectId = result.getString(result.getColumnIndex(UserScheme.OBJECT_ID))
            val name = result.getString(result.getColumnIndex(UserScheme.NAME))
            val email = result.getString(result.getColumnIndex(UserScheme.EMAIL))
            val seed = result.getLong(result.getColumnIndex(UserScheme.ATTR_ID_SEED))
            val entity = OTUser(objectId, id, name, email, seed, findTrackersOfUser(id))
            result.close()
            return entity
        }
    }*/

    fun findTrackersOfUser(userId: String): List<OTTracker>? {

        val query: Cursor = readableDatabase.query(TrackerScheme.tableName, TrackerScheme.columnNames, "${TrackerScheme.USER_ID}=?", arrayOf(userId), null, null, "${TrackerScheme.POSITION} ASC")
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

    fun findTriggersOfUser(user: OTUser): List<OTTrigger>? {
        val query: Cursor = readableDatabase.query(TriggerScheme.tableName, TriggerScheme.columnNames, "${TriggerScheme.USER_ID}=?", arrayOf(user.objectId), null, null, "${TriggerScheme.POSITION} ASC")
        if (query.moveToFirst()) {
            val result = ArrayList<OTTrigger>()
            do {
                result.add(extractTriggerEntity(query, user))
            } while (query.moveToNext())

            query.close()
            return result
        } else return null

    }

    fun extractTriggerEntity(cursor: Cursor, user: OTUser): OTTrigger {
        val id = cursor.getLong(cursor.getColumnIndex(TriggerScheme._ID))
        val name = cursor.getString(cursor.getColumnIndex(TriggerScheme.NAME))
        val objectId = cursor.getString(cursor.getColumnIndex(TriggerScheme.OBJECT_ID))
        val type = cursor.getInt(cursor.getColumnIndex(TriggerScheme.TYPE))
        val trackerObjectIds = cursor.getString(cursor.getColumnIndex(TriggerScheme.TRACKER_OBJECT_IDS))
        val serializedProperties = cursor.getString(cursor.getColumnIndex(TriggerScheme.PROPERTIES))
        val isOn = cursor.getInt(cursor.getColumnIndex(TriggerScheme.IS_ON)).toBoolean()

        val action = cursor.getInt(cursor.getColumnIndex(TriggerScheme.ACTION))

        val lastTriggeredTime = cursor.getLong(cursor.getColumnIndex(TriggerScheme.LAST_TRIGGERED_TIME))

        val trigger = OTTrigger.makeInstance(objectId, id, type, user, name, trackerObjectIds.split(";").toTypedArray(), isOn, action, lastTriggeredTime, serializedProperties)
        trigger.isDirtySinceLastSync = false
        return trigger
    }

    fun extractTrackerEntity(cursor: Cursor): OTTracker {
        val id = cursor.getLong(cursor.getColumnIndex(TrackerScheme._ID))
        val name = cursor.getString(cursor.getColumnIndex(TrackerScheme.NAME))
        val objectId = cursor.getString(cursor.getColumnIndex(TrackerScheme.OBJECT_ID))
        val color = cursor.getInt(cursor.getColumnIndex(TrackerScheme.COLOR))
        val isOnShortcut = cursor.getInt(cursor.getColumnIndex(TrackerScheme.IS_ON_SHORTCUT))
        val attributeIdSeed = cursor.getLong(cursor.getColumnIndex(TrackerScheme.ATTR_ID_SEED))

        val tracker = OTTracker(objectId, id, name, color, isOnShortcut.toBoolean(), attributeIdSeed, findAttributesOfTracker(id))
        tracker.isDirtySinceLastSync = false

        return tracker
    }

    fun extractAttributeEntity(cursor: Cursor): OTAttribute<out Any> {
        val id = cursor.getLong(cursor.getColumnIndex(AttributeScheme._ID))
        val name = cursor.getString(cursor.getColumnIndex(AttributeScheme.NAME))
        val objectId = cursor.getString(cursor.getColumnIndex(AttributeScheme.OBJECT_ID))
        val type = cursor.getInt(cursor.getColumnIndex(AttributeScheme.TYPE))
        val isRequired = cursor.getInt(cursor.getColumnIndex(AttributeScheme.IS_REQUIRED)).toBoolean()
        val settingData = cursor.getString(cursor.getColumnIndex(AttributeScheme.PROPERTY_DATA))
        val connectionData = cursor.getString(cursor.getColumnIndex(AttributeScheme.CONNECTION_DATA))

        val attribute = OTAttribute.createAttribute(objectId, id, name, isRequired, type, settingData, connectionData)
        attribute.isDirtySinceLastSync = false

        return attribute
    }

    private fun queryById(id: Long, table: TableScheme): Cursor {
        val query = readableDatabase.query(table.tableName, table.columnNames, "${table._ID}=?", arrayOf(id.toString()), null, null, "${table._ID} ASC")
        query.moveToFirst()
        return query
    }

    fun deleteObjects(scheme: TableScheme, vararg ids: Long) {
        val idStringArray = ids.map { "${scheme._ID}=${it.toString()}" }.toTypedArray()
        if (idStringArray.size > 0) {
            writableDatabase.delete(scheme.tableName, idStringArray.joinToString(" OR "), null)
        }
    }

    private fun saveObject(objRef: IDatabaseStorable, values: ContentValues, scheme: TableScheme): Int {
        val now = System.currentTimeMillis()
        values.put(scheme.UPDATED_AT, now)

        val db = writableDatabase
        val transactionMode = !db.inTransaction()

        if (transactionMode) db.beginTransaction()

        try {
            val result: Int
            if (objRef.dbId != null) //update
            {
                val numAffected = db.update(scheme.tableName, values, "${scheme._ID}=?", arrayOf(objRef.dbId.toString()))
                if (numAffected == 1) {
                    println("updated db object : ${scheme.tableName}, id: ${objRef.dbId}, updated at ${now}")
                } else { // something wrong
                    throw Exception("Something is wrong saving user in Db")
                }

                result = SAVE_RESULT_EDIT
            } else { //new
                if (!values.containsKey(scheme.LOGGED_AT)) values.put(scheme.LOGGED_AT, now)
                val newRowId = db.insert(scheme.tableName, null, values)
                if (newRowId != -1L) {
                    objRef.dbId = newRowId
                    println("added db object : ${scheme.tableName}, id: $newRowId, logged at ${now}")
                } else {
                    throw Exception("Object insertion failed - ${scheme.tableName}")
                }

                result = SAVE_RESULT_NEW
            }

            if (transactionMode)
                db.setTransactionSuccessful()
            return result
        } catch(e: Exception) {
            e.printStackTrace()
            return SAVE_RESULT_FAIL
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
        if (trigger.isDirtySinceLastSync) {
            val values = baseContentValuesOfNamed(trigger, TriggerScheme)

            values.put(TriggerScheme.USER_ID, owner.objectId)
            println("saving trigger with user id ${owner.objectId}")
            values.put(TriggerScheme.TYPE, trigger.typeId)
            values.put(TriggerScheme.PROPERTIES, trigger.getSerializedProperties())
            values.put(TriggerScheme.TRACKER_OBJECT_IDS, trigger.trackers.map { it.objectId }.joinToString(";"))
            values.put(TriggerScheme.POSITION, position)
            values.put(TriggerScheme.ACTION, trigger.action)
            values.put(TriggerScheme.IS_ON, trigger.isOn.toInt())

            println("storing isOn : ${trigger.isOn}")

            saveObject(trigger, values, TriggerScheme)
            trigger.isDirtySinceLastSync = false
        }else{
            println("trigger is not dirty. Do not save. $position")
        }
    }

    fun save(attribute: OTAttribute<out Any>, position: Int) {
        if (attribute.isDirtySinceLastSync) {
            val values = baseContentValuesOfNamed(attribute, AttributeScheme)

            values.put(AttributeScheme.POSITION, position)
            values.put(AttributeScheme.TYPE, attribute.typeId)
            values.put(AttributeScheme.TRACKER_ID, attribute.owner?.dbId)
            values.put(AttributeScheme.IS_REQUIRED, attribute.isRequired.toInt())
            values.put(AttributeScheme.PROPERTY_DATA, attribute.getSerializedProperties())
            values.put(AttributeScheme.CONNECTION_DATA, attribute.valueConnection?.getSerializedString())

            saveObject(attribute, values, AttributeScheme)
            attribute.isDirtySinceLastSync = false
        }
    }

    fun save(tracker: OTTracker, position: Int) {
        if (tracker.isDirtySinceLastSync) {
            val values = baseContentValuesOfNamed(tracker, TrackerScheme)
            values.put(TrackerScheme.POSITION, position)
            values.put(TrackerScheme.COLOR, tracker.color)
            values.put(TrackerScheme.USER_ID, tracker.owner?.objectId)
            values.put(TrackerScheme.IS_ON_SHORTCUT, tracker.isOnShortcut.toInt())
            values.put(TrackerScheme.ATTR_ID_SEED, tracker.attributeIdSeed)

            saveObject(tracker, values, TrackerScheme)
            tracker.isDirtySinceLastSync = false
        }
        writableDatabase.beginTransaction()

        deleteObjects(AttributeScheme, *tracker.fetchRemovedAttributeIds())

        for (child in tracker.attributes.iterator().withIndex()) {
            save(child.value, child.index)
        }

        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
    }

    fun save(user: OTUser) {
        /*
        if (user.isDirtySinceLastSync) {
            val values = baseContentValuesOfNamed(user, UserScheme)
            values.put(UserScheme.EMAIL, user.email)
            values.put(UserScheme.ATTR_ID_SEED, user.attributeIdSeed)


            saveObject(user, values, UserScheme)
            user.isDirtySinceLastSync = false
        }
        */

        writableDatabase.beginTransaction()

        val removedTrackerIds = user.fetchRemovedTrackerIds()

        if (!removedTrackerIds.isEmpty()) {
            writableDatabase.delete(AttributeScheme.tableName, removedTrackerIds.map { "${AttributeScheme.TRACKER_ID} = ${it.toString()}" }.joinToString(" OR "), null)
            deleteObjects(TrackerScheme, *removedTrackerIds)
        }

        for (child in user.trackers.iterator().withIndex()) {
            save(child.value, child.index)
        }

        for (triggerEntry in user.triggerManager.withIndex()) {
            save(triggerEntry.value, user, triggerEntry.index)
        }
        deleteObjects(DatabaseHelper.TriggerScheme, *user.triggerManager.fetchRemovedTriggerIds())

        writableDatabase.setTransactionSuccessful()
        writableDatabase.endTransaction()
    }

    //Item API===============================

    fun save(item: OTItem, tracker: OTTracker, notifyIntent: Boolean = true): Int {
        val values = ContentValues()

        values.put(ItemScheme.TRACKER_ID, tracker.dbId)
        values.put(ItemScheme.SOURCE_TYPE, item.source.ordinal)
        values.put(ItemScheme.VALUES_JSON, item.getSerializedValueTable(tracker))
        if (item.timestamp != -1L) {
            values.put(ItemScheme.LOGGED_AT, item.timestamp)
        }

        println("item id: ${item.dbId}")

        val result = saveObject(item, values, ItemScheme)

        if(result != SAVE_RESULT_FAIL) {
            if(notifyIntent) {
                val intent = Intent(when (result) {
                    SAVE_RESULT_NEW -> OTApplication.BROADCAST_ACTION_ITEM_ADDED
                    SAVE_RESULT_EDIT -> OTApplication.BROADCAST_ACTION_ITEM_EDITED
                    else -> throw IllegalArgumentException("")
                })

                intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
                intent.putExtra(OTApplication.INTENT_EXTRA_DB_ID_ITEM, item.dbId)

                OTApplication.app.sendBroadcast(intent)
            }
            return result
        }
        else{
            println("Item insert failed - $item")
            return result
        }
    }

    fun save(items: Collection<OTItem>, tracker: OTTracker)
    {
        var success = 0
        var fail = 0
        for(item in items)
        {
            val result = save(item, tracker, false)
            if(result == SAVE_RESULT_FAIL)
            {
                fail++
            }
            else success++
        }

        if(success >0 )
        {
            val intent = Intent(OTApplication.BROADCAST_ACTION_ITEM_ADDED)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            OTApplication.app.sendBroadcast(intent)
        }
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

    fun getItems(tracker: OTTracker, timeRange: TimeSpan, listOut: ArrayList<OTItem>, timestampAsc: Boolean = false): Int {
        val cursor = readableDatabase.query(ItemScheme.tableName, ItemScheme.columnNames, "${ItemScheme.TRACKER_ID}=?  AND ${ItemScheme.LOGGED_AT} BETWEEN ? AND ?", arrayOf(tracker.dbId.toString(), timeRange.from.toString(), timeRange.to.toString()), null, null, "${ItemScheme.LOGGED_AT} ${if(timestampAsc){"ASC"} else "DESC"}")

        var count = 0
        if (cursor.moveToFirst()) {
            do {
                listOut.add(extractItemEntity(cursor, tracker))
                count++
            } while (cursor.moveToNext())
        }

        return count
    }

    fun getItem(id: Long, tracker: OTTracker): OTItem? {
        val cursor = readableDatabase.query(ItemScheme.tableName, ItemScheme.columnNames, "${ItemScheme._ID}=?", arrayOf(id.toString()), null, null, null, "1")
        if (cursor.moveToFirst()) {
            return extractItemEntity(cursor, tracker)
        } else return null
    }

    fun removeItem(item: OTItem){
        deleteObjects(DatabaseHelper.ItemScheme, item.dbId!!)
        val intent = Intent(OTApplication.BROADCAST_ACTION_ITEM_REMOVED)

        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, item.trackerObjectId)

        OTApplication.app.sendBroadcast(intent)
    }


    fun extractItemEntity(cursor: Cursor, tracker: OTTracker): OTItem {
        val id = cursor.getLong(cursor.getColumnIndex(ItemScheme._ID))
        val serializedValues = cursor.getString(cursor.getColumnIndex(ItemScheme.VALUES_JSON))
        //TODO keytime columns
        //val KEY_TIME_TIMESTAMP = "key_time_timestamp"
        //val KEY_TIME_GRANULARITY = "key_time_granularity"
        //val KEY_TIME_TIMEZONE = "key_time_timezone"


        val source = OTItem.LoggingSource.values()[cursor.getInt(cursor.getColumnIndex(ItemScheme.SOURCE_TYPE))]
        val timestamp = cursor.getLong(cursor.getColumnIndex(ItemScheme.LOGGED_AT))

        return OTItem(id, tracker.objectId, serializedValues, timestamp, source)
    }

    fun getLogCountDuring(tracker: OTTracker, from: Long, to: Long): Int
    {
        val numRows = DatabaseUtils.queryNumEntries(readableDatabase, ItemScheme.tableName, "${ItemScheme.TRACKER_ID}=? AND ${ItemScheme.LOGGED_AT} BETWEEN ? AND ?", arrayOf(tracker.dbId.toString(), from.toString(), to.toString()))
        return numRows.toInt()
    }

    fun getLogCountOfDay(tracker: OTTracker): Observable<Int> {
        return Observable.defer {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val first = cal.timeInMillis

            cal.add(Calendar.DAY_OF_YEAR, 1)
            val second = cal.timeInMillis -20

            Observable.just(getLogCountDuring(tracker, first, second))
        }.subscribeOn(Schedulers.computation())
    }

    fun getTotalItemCount(tracker: OTTracker): Observable<Int> {
        return Observable.defer {
            val numRows = DatabaseUtils.queryNumEntries(readableDatabase, ItemScheme.tableName, "${ItemScheme.TRACKER_ID}=?", arrayOf(tracker.dbId.toString()))
            Observable.just(numRows.toInt())
        }.subscribeOn(Schedulers.computation())
    }

    fun getLastLoggingTime(tracker: OTTracker): Observable<Long?> {
        return Observable.defer {
            val cursor = readableDatabase.query(ItemScheme.tableName, arrayOf(ItemScheme.LOGGED_AT), "${ItemScheme.TRACKER_ID}=?", arrayOf(tracker.dbId.toString()), null, null, "${ItemScheme.LOGGED_AT} DESC", "1")
            if (cursor.moveToFirst()) {
                val value = cursor.getLong(cursor.getColumnIndex(ItemScheme.LOGGED_AT))
                cursor.close()
                Observable.just(value)
            } else {
                cursor.close()
                Observable.just<Long?>(null)
            }
        }.subscribeOn(Schedulers.computation())
    }

}