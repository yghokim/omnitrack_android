package kr.ac.snu.hcil.omnitrack.services

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.support.v7.widget.AppCompatCheckBox
import android.view.LayoutInflater
import android.webkit.MimeTypeMap
import android.widget.RadioButton
import android.widget.Toast
import br.com.goncalves.pugnotification.notification.PugNotification
import com.afollestad.materialdialogs.MaterialDialog
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTExternalFileInvolvedAttribute
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import kr.ac.snu.hcil.omnitrack.utils.io.StringTableSheet
import rx.Observable
import rx.Single
import rx.internal.util.SubscriptionList
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Young-Ho on 3/9/2017.
 */
class OTTableExportService : Service() {

    enum class TableFileType(val extension: String) { CSV("csv"), EXCEL("xls")
    }

    companion object {

        val TAG = OTTableExportService::class.java.simpleName.toString()

        private val uniqueNotificationIdPointer = AtomicInteger()

        fun makeUniqueNotificationId(): Int {
            return uniqueNotificationIdPointer.addAndGet(1)
        }

        const val EXTRA_EXPORT_URI = "export_uri"
        const val EXTRA_EXPORT_CONFIG_INCLUDE_FILE = "export_config_include_files"
        const val EXTRA_EXPORT_CONFIG_TABLE_FILE_TYPE = "export_config_table_file_type"


        fun makeIntent(context: Context, tracker: OTTracker, exportUri: String, includeFiles: Boolean, tableFileType: TableFileType): Intent {
            val intent = Intent(context, OTTableExportService::class.java)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
                    .putExtra(EXTRA_EXPORT_URI, exportUri)
                    .putExtra(EXTRA_EXPORT_CONFIG_INCLUDE_FILE, includeFiles)
                    .putExtra(EXTRA_EXPORT_CONFIG_TABLE_FILE_TYPE, tableFileType.toString())
            return intent
        }

        fun makeConfigurationDialog(context: Context, tracker: OTTracker, onConfigured: (includeFiles: Boolean, tableFileType: TableFileType) -> Unit): MaterialDialog.Builder {

            val view = LayoutInflater.from(context).inflate(R.layout.dialog_export_configuration, null, false)

            val includeFilesCheckbox = view.findViewById(R.id.ui_include_external_files) as AppCompatCheckBox
            if (tracker.isExternalFilesInvolved) {
                includeFilesCheckbox.isEnabled = true
                includeFilesCheckbox.isChecked = true
            } else {
                includeFilesCheckbox.isEnabled = false
                includeFilesCheckbox.isChecked = false
                includeFilesCheckbox.alpha = 0.3f
            }
            val excelRadioButton = view.findViewById(R.id.ui_radio_export_type_excel) as RadioButton
            val csvRadioButton = view.findViewById(R.id.ui_radio_export_type_csv) as RadioButton

            val builder = MaterialDialog.Builder(context)
                    .title(context.getString(R.string.msg_configure_export))
                    .cancelable(true)
                    .customView(view, false)
                    .positiveColorRes(R.color.colorPointed)
                    .negativeColorRes(R.color.colorRed_Light)
                    .positiveText(R.string.msg_ok)
                    .negativeText(R.string.msg_cancel)
                    .onPositive { materialDialog, dialogAction ->
                        onConfigured.invoke(includeFilesCheckbox.isChecked, if (excelRadioButton.isChecked) {
                            TableFileType.EXCEL
                        } else {
                            TableFileType.CSV
                        })
                    }

            return builder
        }

    }

    private val subscriptions = SubscriptionList()

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        if (!(wakeLock?.isHeld ?: false)) {
            wakeLock?.acquire()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (wakeLock?.isHeld() ?: false) {
            wakeLock?.release()
        }

        OTApplication.app.setTrackerItemExportInProgress(false)

        subscriptions.clear()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        println("table export service started.")
        val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        if (OTApplication.app.isTrackerItemExportInProgress()) {
            println("another export task is in progress")
            Toast.makeText(this, "Another export task is in progress.", Toast.LENGTH_LONG).show()
            return START_NOT_STICKY
        }
        val exportUriString = intent.getStringExtra(EXTRA_EXPORT_URI)
        if (trackerId != null && exportUriString != null) {
            val exportUri = Uri.parse(exportUriString)

            OTApplication.app.setTrackerItemExportInProgress(true)
            OTTaskNotificationManager.setTaskProgressNotification(this, TAG, 100, getString(R.string.msg_export_title_progress), "downloading", OTTaskNotificationManager.PROGRESS_INDETERMINATE)

            var loadedTracker: OTTracker? = null

            val externalFilesInvolved: Boolean = intent.getBooleanExtra(EXTRA_EXPORT_CONFIG_INCLUDE_FILE, false)
            val tableType = TableFileType.valueOf(intent.getStringExtra(EXTRA_EXPORT_CONFIG_TABLE_FILE_TYPE))

            println("include external files:${externalFilesInvolved}")
            println("table file type: ${tableType}")

            var cacheDirectory: File? = null

            val table = StringTableSheet()
            var involvedFileList: MutableList<String>? = null

            fun finish(successful: Boolean) {
                println("export observable completed")

                cacheDirectory?.let {
                    if (it.exists()) {
                        if (it.deleteRecursively()) {
                            println("export cache files successfully removed.")
                        }
                    }
                }

                OTTaskNotificationManager.dismissNotification(this, 100, TAG)

                if (successful && loadedTracker != null) {

                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(if (externalFilesInvolved) {
                        "zip"
                    } else {
                        tableType.extension
                    })
                    println("mimeType is ${mimeType}")

                    PugNotification.with(this)
                            .load()
                            .title(String.format(getString(R.string.msg_export_success_notification_message), loadedTracker?.name))
                            .`when`(System.currentTimeMillis())
                            .tag(TAG)
                            .identifier(makeUniqueNotificationId())
                            .color(R.color.colorPrimary)
                            .button(0, getString(R.string.msg_open)) {

                                val choiceIntent = Intent.createChooser(Intent(Intent.ACTION_VIEW).putExtra(Intent.EXTRA_STREAM, exportUri).setDataAndType(exportUri, mimeType).apply { this.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION },
                                        getString(R.string.msg_open_exported_file))

                                PendingIntent.getActivity(this, makeUniqueNotificationId(),
                                        choiceIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                            }
                            .button(0, getString(R.string.msg_share)) {

                                val choiceIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).setDataAndType(exportUri, mimeType).putExtra(Intent.EXTRA_STREAM, exportUri).apply { this.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION },
                                        getString(R.string.msg_share_exported_file))

                                PendingIntent.getActivity(this, makeUniqueNotificationId(),
                                        choiceIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                            }
                            .smallIcon(R.drawable.done)
                            .largeIcon(R.drawable.icon_cloud_download)
                            .simple()
                            .build()
                }

                OTApplication.app.setTrackerItemExportInProgress(false)
                stopSelf(startId)
            }

            val subscription =
            OTApplication.app.currentUserObservable
                    .flatMap {
                        user ->
                        val tracker = user[trackerId]
                        if (tracker != null) {
                            loadedTracker = tracker

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

                            val tableObservable = FirebaseDbHelper.loadItems(tracker).doOnNext {
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
                                    val tablePath = cacheDirectory?.resolve("table.${tableType.extension}")
                                    if(tablePath!=null)
                                    {
                                        val fileOutputStream = FileOutputStream(tablePath)
                                        when (tableType) {
                                            TableFileType.CSV -> {
                                                table.storeCsvToStream(fileOutputStream)
                                                fileOutputStream.close()
                                            }
                                            TableFileType.EXCEL -> {
                                                table.storeExcelToStream(fileOutputStream)
                                            }
                                        }

                                        involvedFileList?.add(tablePath.path)
                                    }
                                }

                            }

                            if (externalFilesInvolved) {
                                tableObservable.flatMap {
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

                                                        if (!cacheFilePath.exists()) {
                                                            cacheFilePath.createNewFile()
                                                        }
                                                        storeObservables.add(attr.storeValueFile(itemValue, Uri.parse(cacheFilePath.path)).onErrorReturn { ex -> Uri.EMPTY })
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (storeObservables.isNotEmpty()) {
                                        Single.zip(storeObservables) {
                                            uris ->
                                            println("uris")
                                            uris.filter { it != Uri.EMPTY }.map { it.toString() }
                                        }.toObservable().doOnNext {
                                            uris ->
                                            involvedFileList?.addAll(uris)
                                        }
                                    } else {
                                        tableObservable
                                    }

                                }
                            } else {
                                tableObservable
                            }
                        } else {
                            Observable.error(Exception("tracker does not exists."))
                        }
                    }.subscribe({
                println("file making task finished")

                val successful: Boolean = if (externalFilesInvolved) {
                    involvedFileList?.let {
                        list ->
                        println("Involved files: ")
                        for (path in list) {
                            println(path)
                        }

                        try {
                            val outputStream = contentResolver.openOutputStream(exportUri)
                            kr.ac.snu.hcil.omnitrack.utils.io.ZipUtil.zip(cacheDirectory!!.absolutePath, outputStream)
                        } catch(ex: Exception) {
                            //fail
                            ex.printStackTrace()
                            false
                        }
                    } ?: false
                } else {
                    try {
                        println("store table itself to output")
                        val outputStream = contentResolver.openOutputStream(exportUri)
                        when (tableType) {
                            TableFileType.EXCEL -> {
                                table.storeExcelToStream(outputStream)
                            }
                            TableFileType.CSV -> {
                                table.storeCsvToStream(outputStream)
                                outputStream.close()
                            }
                        }
                        true
                    } catch(ex: Exception) {
                        //fail
                        ex.printStackTrace()
                        false
                    }
                }


                if (successful) {
                    println("created table successfully.")
                    println(table)

                    finish(true)
                } else {
                    finish(false)
                }
            }, { ex -> ex.printStackTrace(); finish(false) })

            subscriptions.add(subscription)
            return START_NOT_STICKY
        } else {
            stopSelf(startId)
            return START_NOT_STICKY
        }
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}