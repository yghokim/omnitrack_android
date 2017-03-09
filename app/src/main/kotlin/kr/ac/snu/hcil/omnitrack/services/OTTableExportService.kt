package kr.ac.snu.hcil.omnitrack.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.utils.io.StringTableSheet
import rx.Observable
import rx.internal.util.SubscriptionList
import java.io.File

/**
 * Created by Young-Ho on 3/9/2017.
 */
class OTTableExportService : IntentService("Table Export Service") {

    companion object {
        const val EXTRA_EXPORT_URI = "export_uri"

        fun makeIntent(context: Context, tracker: OTTracker, exportUri: String): Intent {
            val intent = Intent(context, OTTableExportService::class.java)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
                    .putExtra(EXTRA_EXPORT_URI, exportUri)
            return intent
        }

    }

    private val subscriptions = SubscriptionList()

    override fun onHandleIntent(intent: Intent) {

        println("table export service started.")
        val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        val exportUri = intent.getStringExtra(EXTRA_EXPORT_URI)
        if (trackerId != null && exportUri != null) {

            var externalFilesInvolved: Boolean = false

            val table = OTApplication.app.currentUserObservable
                    .flatMap {
                        user ->
                        val tracker = user[trackerId]
                        if (tracker != null) {
                            val table = StringTableSheet()

                            externalFilesInvolved = tracker.attributes.unObservedList.find { it.isExternalFile } != null

                            var cacheDir: File? = null
                            if (externalFilesInvolved) {
                                cacheDir = this.cacheDir.resolve("export_${System.currentTimeMillis()}")
                                if (!cacheDir.exists())
                                    cacheDir.mkdirs()
                            }

                            table.columns.add("index")
                            table.columns.add("logged_at")
                            table.columns.add("source")
                            tracker.attributes.unObservedList.forEach {
                                it.onAddColumnToTable(table.columns)
                            }

                            FirebaseDbHelper.loadItems(tracker).map {
                                items ->
                                items.withIndex().forEach {
                                    itemWithIndex ->
                                    val item = itemWithIndex.value
                                    val row = ArrayList<String?>()
                                    row.add(itemWithIndex.index.toString())
                                    row.add(item.timestamp.toString())
                                    row.add(item.source.name)
                                    tracker.attributes.unObservedList.forEach {
                                        attribute ->
                                        attribute.onAddValueToTable(item.getValueOf(attribute), row)
                                    }
                                    table.rows.add(row)
                                }

                                table
                            }
                        } else {
                            Observable.error(Exception("tracker does not exists."))
                        }
                    }.toBlocking().first()

            if (externalFilesInvolved) {

            } else {
                val successful = try {
                    val outputStream = contentResolver.openOutputStream(Uri.parse(exportUri))
                    table.storeToStream(outputStream)
                    true
                } catch(ex: Exception) {
                    //fail
                    false
                }
            }

            println("created table successfully.")
            println(table)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }
}