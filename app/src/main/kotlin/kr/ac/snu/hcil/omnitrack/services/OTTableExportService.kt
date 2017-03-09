package kr.ac.snu.hcil.omnitrack.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTExternalFileInvolvedAttribute
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.utils.io.StringTableSheet
import org.apache.commons.compress.archivers.zip.ZipUtil
import rx.Observable
import rx.Single
import rx.internal.util.SubscriptionList
import rx.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
            var cacheDirectory: File? = null

            val table = StringTableSheet()
            var involvedFileList: MutableList<String>? = null

            OTApplication.app.currentUserObservable
                    .flatMap {
                        user ->
                        val tracker = user[trackerId]
                        if (tracker != null) {

                            externalFilesInvolved = tracker.attributes.unObservedList.find { it.isExternalFile } != null

                            if (externalFilesInvolved) {
                                cacheDirectory = this.cacheDir.resolve("export_${System.currentTimeMillis()}")
                                cacheDirectory?.let{
                                    if(!it.exists())
                                        cacheDirectory?.mkdirs()
                                }

                                involvedFileList = ArrayList<String>()
                            }

                            table.columns.add("index")
                            table.columns.add("logged_at")
                            table.columns.add("source")
                            tracker.attributes.unObservedList.forEach {
                                it.onAddColumnToTable(table.columns)
                            }

                            FirebaseDbHelper.loadItems(tracker).doOnNext {
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
                                        attribute.onAddValueToTable(item.getValueOf(attribute), row, itemWithIndex.index.toString())
                                    }
                                    table.rows.add(row)
                                }

                                if(externalFilesInvolved)
                                {
                                    val tablePath = cacheDirectory?.resolve("table.csv")
                                    if(tablePath!=null)
                                    {
                                        val fileOutputStream = FileOutputStream(tablePath)
                                        table.storeToStream(fileOutputStream)
                                        fileOutputStream.close()

                                        involvedFileList?.add(tablePath.path)
                                    }
                                }

                            }.flatMap {
                                items ->
                                val storeObservables = ArrayList<Single<Uri>>()
                                tracker.attributes.unObservedList.filter { it is OTExternalFileInvolvedAttribute }.forEach {
                                    attr ->
                                    if (attr is OTExternalFileInvolvedAttribute) {
                                        items.withIndex().forEach {
                                            itemWithIndex ->
                                            val itemValue = itemWithIndex.value.getValueOf(attr)
                                            if (itemValue != null && attr.isValueContainingFileInfo(itemValue)) {
                                                val cacheFilePath = cacheDirectory?.resolve(attr.makeRelativeFilePathFromValue(itemValue, itemWithIndex.index.toString()))
                                                if (cacheFilePath != null) {
                                                    val cacheFileLocation = cacheFilePath.parentFile
                                                    if (!cacheFileLocation.exists()) {
                                                        cacheFileLocation.mkdirs()
                                                    }

                                                    if(!cacheFilePath.exists())
                                                    {
                                                        cacheFilePath.createNewFile()
                                                    }
                                                    storeObservables.add(attr.storeValueFile(itemValue, Uri.parse(cacheFilePath.path)).onErrorReturn { ex-> Uri.EMPTY })
                                                }
                                            }
                                        }
                                    }
                                }

                                Single.zip(storeObservables){
                                    uris->
                                    uris.filter{ it != Uri.EMPTY }.map{it.toString()}
                                }.toObservable().doOnNext {
                                    uris->
                                    involvedFileList?.addAll(uris)
                                }

                            }
                        } else {
                            Observable.error(Exception("tracker does not exists."))
                        }
                    }.toBlocking().first()

            println("file making task finished")

            val successful: Boolean = if (externalFilesInvolved) {
                involvedFileList?.let{
                    list->
                    println("Involved files: ")
                    for(path in list)
                    {
                        println(path)
                    }

                    try {
                        val outputStream = contentResolver.openOutputStream(Uri.parse(exportUri))
                        kr.ac.snu.hcil.omnitrack.utils.io.ZipUtil.zip(cacheDirectory!!.absolutePath, outputStream)
                    } catch(ex: Exception) {
                        //fail
                        ex.printStackTrace()
                        false
                    }
                } ?: false
            } else {
                try {
                    val outputStream = contentResolver.openOutputStream(Uri.parse(exportUri))
                    table.storeToStream(outputStream)
                    true
                } catch(ex: Exception) {
                    //fail
                    ex.printStackTrace()
                    false
                }
            }


            if(successful)
            {
                println("created table successfully.")
                println(table)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }
}