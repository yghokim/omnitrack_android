package kr.ac.snu.hcil.omnitrack.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.support.v7.widget.AppCompatCheckBox
import android.view.LayoutInflater
import android.webkit.MimeTypeMap
import android.widget.RadioButton
import android.widget.Toast
import br.com.goncalves.pugnotification.notification.PugNotification
import com.afollestad.materialdialogs.MaterialDialog
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTFileInvolvedAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.EventLoggingManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import kr.ac.snu.hcil.omnitrack.utils.io.StringTableSheet
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Young-Ho on 3/9/2017.
 */
class OTTableExportService : WakefulService(TAG) {

    enum class TableFileType(val extension: String) { CSV("csv"), EXCEL("xls")
    }

    companion object {

        val TAG = OTTableExportService::class.java.simpleName.toString()
        val NOTIFY_ID: Int = 110523424

        private val uniqueNotificationIdPointer = AtomicInteger()

        fun makeUniqueNotificationId(): Int {
            return uniqueNotificationIdPointer.addAndGet(1)
        }

        const val EXTRA_EXPORT_URI = "export_uri"
        const val EXTRA_EXPORT_CONFIG_INCLUDE_FILE = "export_config_include_files"
        const val EXTRA_EXPORT_CONFIG_TABLE_FILE_TYPE = "export_config_table_file_type"


        fun makeIntent(context: Context, trackerId: String, exportUri: String, includeFiles: Boolean, tableFileType: TableFileType): Intent {
            val intent = Intent(context, OTTableExportService::class.java)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
                    .putExtra(EXTRA_EXPORT_URI, exportUri)
                    .putExtra(EXTRA_EXPORT_CONFIG_INCLUDE_FILE, includeFiles)
                    .putExtra(EXTRA_EXPORT_CONFIG_TABLE_FILE_TYPE, tableFileType.toString())
            return intent
        }

        fun makeConfigurationDialog(context: Context, tracker: OTTrackerDAO, onConfigured: (includeFiles: Boolean, tableFileType: TableFileType) -> Unit): MaterialDialog.Builder {

            val view = LayoutInflater.from(context).inflate(R.layout.dialog_export_configuration, null, false)

            val includeFilesCheckbox = view.findViewById<AppCompatCheckBox>(R.id.ui_include_external_files)
            if (tracker.isExternalFilesInvolved) {
                includeFilesCheckbox.isEnabled = true
                includeFilesCheckbox.isChecked = true
            } else {
                includeFilesCheckbox.isEnabled = false
                includeFilesCheckbox.isChecked = false
                includeFilesCheckbox.alpha = 0.3f
            }
            val excelRadioButton = view.findViewById<RadioButton>(R.id.ui_radio_export_type_excel)
            val csvRadioButton = view.findViewById<RadioButton>(R.id.ui_radio_export_type_csv)

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

    private val subscriptions = CompositeDisposable()

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        println("table export service started.")
        val trackerId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)
        val exportUriString = intent.getStringExtra(EXTRA_EXPORT_URI)
        if (trackerId != null && exportUriString != null) {
            val exportUri = Uri.parse(exportUriString)

            Toast.makeText(this, R.string.msg_export_title_progress, Toast.LENGTH_SHORT).show()
            val notification = OTTaskNotificationManager.makeTaskProgressNotificationBuilder(this, getString(R.string.msg_export_title_progress), "downloading", OTTaskNotificationManager.PROGRESS_INDETERMINATE).build()
            startForeground(NOTIFY_ID, notification)

            var loadedTracker: OTTrackerDAO? = null

            val externalFilesInvolved: Boolean = intent.getBooleanExtra(EXTRA_EXPORT_CONFIG_INCLUDE_FILE, false)
            val tableType = TableFileType.valueOf(intent.getStringExtra(EXTRA_EXPORT_CONFIG_TABLE_FILE_TYPE))

            println("include external files:${externalFilesInvolved}")
            println("table file type: ${tableType}")

            var cacheDirectory: File? = null

            val table = StringTableSheet()
            var involvedFileList: MutableList<String>? = null

            fun finish(successful: Boolean) {
                println("export observable completed")

                EventLoggingManager.logExport(trackerId)

                cacheDirectory?.let {
                    if (it.exists()) {
                        if (it.deleteRecursively()) {
                            println("export cache files successfully removed.")
                        }
                    }
                }

                OTTaskNotificationManager.dismissNotification(this, NOTIFY_ID, TAG)

                if (successful && loadedTracker != null) {

                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(if (externalFilesInvolved) {
                        "zip"
                    } else {
                        tableType.extension
                    })

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
                            .smallIcon(R.drawable.icon_simple_check)
                            .largeIcon(R.drawable.icon_simple_check)
                            .simple()
                            .build()
                }
                stopSelf(startId)
            }

            subscriptions.add(
                    Single.defer<Boolean> {
                        val realm = OTApp.instance.databaseManager.getRealmInstance()
                        val tracker = OTApp.instance.databaseManager.getUnManagedTrackerDao(trackerId, realm)
                        realm.close()
                        loadedTracker = tracker
                        if (tracker == null) {
                            return@defer Single.just(true)
                        } else {
                            if (externalFilesInvolved) {
                                cacheDirectory = this.cacheDir.resolve("export_${System.currentTimeMillis()}")
                                cacheDirectory?.let {
                                    if (!it.exists())
                                        cacheDirectory?.mkdirs()
                                }

                                involvedFileList = ArrayList<String>()
                            }

                            table.columns.add("index")
                            table.columns.add("logged_at")
                            table.columns.add("source")

                            tracker.attributes.forEach {
                                it.getHelper().onAddColumnToTable(it, table.columns)
                            }

                            val items = OTApp.instance.databaseManager.makeItemsQuery(trackerId, null, null, realm).findAll()
                            items.withIndex().forEach { itemWithIndex ->
                                val item = itemWithIndex.value
                                val row = ArrayList<String?>()
                                row.add(itemWithIndex.index.toString())
                                row.add(item.timestamp.toString())
                                row.add(item.loggingSource.name)
                                tracker.attributes.forEach { attribute ->
                                    attribute.getHelper().onAddValueToTable(attribute, item.getValueOf(attribute.localId), row, itemWithIndex.index.toString())
                                    //TODO add value
                                    //attribute.onAddValueToTable(item.getValueOf(attribute), row, itemWithIndex.index.toString())
                                }
                                table.rows.add(row)
                            }

                            if (!externalFilesInvolved) {
                                return@defer Single.just(true)
                            } else {
                                val tablePath = cacheDirectory?.resolve("table.${tableType.extension}")
                                if (tablePath != null) {
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

                                val storeObservables = ArrayList<Single<Uri>>()
                                tracker.attributes.filter { it.getHelper().isExternalFile(it) && it.getHelper() is OTFileInvolvedAttributeHelper }.forEach { attr ->
                                    val helper = attr.getHelper() as OTFileInvolvedAttributeHelper
                                    items.withIndex().forEach { itemWithIndex ->
                                        val itemValue = itemWithIndex.value.getValueOf(attr.localId)
                                        if (itemValue != null && helper.isValueContainingFileInfo(attr, itemValue)) {
                                            val cacheFilePath = cacheDirectory?.resolve(helper.makeRelativeFilePathFromValue(attr, itemValue, itemWithIndex.index.toString()))
                                            if (cacheFilePath != null) {
                                                val cacheFileLocation = cacheFilePath.parentFile
                                                if (!cacheFileLocation.exists()) {
                                                    cacheFileLocation.mkdirs()
                                        }

                                                if (!cacheFilePath.exists()) {
                                                    cacheFilePath.createNewFile()
                                        }
                                                storeObservables.add(helper.storeValueFile(attr, itemValue, Uri.parse(cacheFilePath.path)).onErrorReturn { ex -> Uri.EMPTY })
                                    }
                                }
                            }
                        }

                                if (storeObservables.isNotEmpty()) {
                                    return@defer Single.zip(storeObservables) { uris ->
                                        println("uris")
                                        uris.filter { it != Uri.EMPTY }.map { it.toString() }
                                    }.doOnSuccess { uris ->
                                        involvedFileList?.addAll(uris)
                                    }.map { true }
                                } else {
                                    return@defer Single.just(true)
                                }
                            }
                        }
                    }.onErrorReturn { err ->
                        err.printStackTrace()
                        false
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
            )



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