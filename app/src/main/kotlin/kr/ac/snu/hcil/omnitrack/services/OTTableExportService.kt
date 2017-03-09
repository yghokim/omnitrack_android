package kr.ac.snu.hcil.omnitrack.services

import android.app.IntentService
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.utils.io.StringTableSheet
import rx.Observable
import rx.internal.util.SubscriptionList

/**
 * Created by Young-Ho on 3/9/2017.
 */
class OTTableExportService : IntentService("Table Export Service") {
    private val subscriptions = SubscriptionList()

    override fun onHandleIntent(intent: Intent) {

        println("table export service started.")
        val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        if (trackerId != null) {

            val table = OTApplication.app.currentUserObservable
                    .flatMap {
                        user ->
                        val tracker = user[trackerId]
                        if (tracker != null) {
                            val table = StringTableSheet()
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

            println("created table successfully.")
            println(table)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }
}