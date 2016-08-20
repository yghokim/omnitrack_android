package kr.ac.snu.hcil.omnitrack.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.AsyncTask
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Ion
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Created by younghokim on 16. 8. 20..
 */

class CacheHelper(context: Context) : SQLiteOpenHelper(context, "cache.db", null, 1) {

    interface ICachedBitmapListener {
        fun onBitmapRetrieved(uri: Uri?)
    }

    object BitmapCacheScheme : TableScheme() {

        val KEY = "key"
        val FILEPATH = "filepath"

        override val creationColumnContentString: String = "$KEY TEXT, $FILEPATH TEXT"

        override val intrinsicColumnNames: Array<String> = arrayOf(KEY, FILEPATH)

        override val tableName: String = "bitmap_caches"

        override val indexCreationQueryString: String = makeIndexQueryString(true, "key_unique", KEY)

    }


    override fun onCreate(db: SQLiteDatabase) {
        val tables = arrayOf(BitmapCacheScheme)

        for (scheme in tables) {
            db.execSQL(scheme.creationQueryString)
            db.execSQL(scheme.indexCreationQueryString)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //Caching

    private fun queryBitmapCache(key: String, vararg columns: String): Cursor {
        return readableDatabase.query(BitmapCacheScheme.tableName, columns, "${BitmapCacheScheme.KEY}=?", arrayOf(key), null, null, null)
    }

    fun getBitmapCachePath(key: String): String? {
        val cursor = queryBitmapCache(key, BitmapCacheScheme.FILEPATH)
        if (cursor.count == 0) {
            return null
        } else {
            cursor.moveToFirst()
            return cursor.getString(cursor.getColumnIndex(BitmapCacheScheme.FILEPATH))
        }
    }

    fun putBitmapCachePath(key: String, path: String) {
        val cursor = queryBitmapCache(key, BitmapCacheScheme.FILEPATH)

        val values = ContentValues(2)
        values.put(BitmapCacheScheme.KEY, key)
        values.put(BitmapCacheScheme.FILEPATH, path)

        val db = writableDatabase
        db.beginTransaction()
        try {
            if (cursor.count == 0) {
                //insert
                val id = db.insertOrThrow(BitmapCacheScheme.tableName, null, values)

            } else {
                //update
                val numAffected = db.update(BitmapCacheScheme.tableName, values, "${BitmapCacheScheme.KEY}=?", arrayOf(key))
            }
        } catch(e: SQLException) {
            e.printStackTrace()
            db.endTransaction()
            return
        }
        db.setTransactionSuccessful()
        db.endTransaction()


    }


    fun downloadBitmapAsync(context: Context, downloadUrl: String, handler: ICachedBitmapListener) {
        BitmapRetrievalTask(context, downloadUrl, handler).execute()
    }

    inner class BitmapRetrievalTask(val context: Context, val downloadUrl: String, val handler: ICachedBitmapListener) : AsyncTask<Void?, Void?, Uri?>(), FutureCallback<ByteArray> {

        private var isLoading = false
        private var image: ByteArray? = null

        override fun doInBackground(vararg p0: Void?): Uri? {
            val cache = getBitmapCachePath(downloadUrl)

            //println("getting image from $downloadUrl")

            if (cache != null) {
                return Uri.fromFile(File(cache))
            } else {
                try {
                    //download image
                    isLoading = true
                    Ion.with(context)
                            .load(downloadUrl)
                            .asByteArray()
                            .setCallback(this)

                    while (!isCancelled && isLoading) {

                    }

                    if (image != null) {


                        val cacheFile = File(context.cacheDir, "cache_${System.currentTimeMillis()}_${UUID.randomUUID()}.png")
                        println("cache image to ${cacheFile.absolutePath}")
                        val fileStream = FileOutputStream(cacheFile)

                        /*
                        imageUri?.compress(Bitmap.CompressFormat.PNG, 100, fileStream)
*/
                        fileStream.write(image)
                        fileStream.close()

                        putBitmapCachePath(downloadUrl, cacheFile.absolutePath)

                        return Uri.fromFile(cacheFile)
                    } else return null
                } catch(e: Exception) {
                    e.printStackTrace()
                    return null
                }
            }
        }

        override fun onCompleted(e: Exception?, result: ByteArray?) {
            isLoading = false
            image = result
        }


        override fun onPostExecute(result: Uri?) {
            handler.onBitmapRetrieved(result)
        }

    }

}