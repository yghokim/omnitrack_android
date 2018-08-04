package kr.ac.snu.hcil.omnitrack.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatCheckBox
import android.view.LayoutInflater
import android.webkit.MimeTypeMap
import android.widget.RadioButton
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import dagger.Lazy
import dagger.internal.Factory
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTFileInvolvedAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import kr.ac.snu.hcil.omnitrack.utils.io.StringTableSheet
import org.jetbrains.anko.notificationManager
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

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

        fun makeConfigurationDialog(context: Context, configuredContext: ConfiguredContext, tracker: OTTrackerDAO, onConfigured: (includeFiles: Boolean, tableFileType: TableFileType) -> Unit): MaterialDialog.Builder {

            val view = LayoutInflater.from(context).inflate(R.layout.dialog_export_configuration, null, false)

            val includeFilesCheckbox = view.findViewById<AppCompatCheckBox>(R.id.ui_include_external_files)
            if (tracker.isExternalFilesInvolved(configuredContext)) {
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

    lateinit var realm: Realm

    @field:[Inject Backend]
    lateinit var realmProvider: Factory<Realm>


    @Inject
    lateinit var dbManager: Lazy<BackendDbManager>

    @Inject
    lateinit var logger: Lazy<IEventLogger>

    private val subscriptions = CompositeDisposable()

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
        realm.close()
    }

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).currentConfiguredContext.configuredAppComponent.inject(this)
        realm = realmProvider.get()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        println("table export service started.")
        val trackerId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER)
        val exportUriString = intent.getStringExtra(EXTRA_EXPORT_URI)
        if (trackerId != null && exportUriString != null) {
            val exportUri = Uri.parse(exportUriString)

            Toast.makeText(this@OTTableExportService, R.string.msg_export_title_progress, Toast.LENGTH_SHORT).show()
            val notification = OTTaskNotificationManager.makeTaskProgressNotificationBuilder(this@OTTableExportService, getString(R.string.msg_export_title_progress), "downloading", OTTaskNotificationManager.PROGRESS_INDETERMINATE).build()
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

                logger.get().logExport(trackerId)

                cacheDirectory?.let {
                    if (it.exists()) {
                        if (it.deleteRecursively()) {
                            println("export cache files successfully removed.")
                        }
                    }
                }

                OTTaskNotificationManager.dismissNotification(this@OTTableExportService, NOTIFY_ID, TAG)

                if (successful && loadedTracker != null) {

                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(if (externalFilesInvolved) {
                        "zip"
                    } else {
                        tableType.extension
                    })

                    val notificationId = makeUniqueNotificationId()

                    val notification =
                            NotificationCompat.Builder(this@OTTableExportService, OTNotificationManager.CHANNEL_ID_SYSTEM)
                                    .setContentTitle(String.format(getString(R.string.msg_export_success_notification_message), loadedTracker?.name))
                                    .setShowWhen(true)
                                    .setWhen(System.currentTimeMillis())
                                    .setColor(ContextCompat.getColor(this@OTTableExportService, R.color.colorPrimary))
                                    .setSmallIcon(R.drawable.icon_simple_check)
                                    .addAction(0, getString(R.string.msg_open), PendingIntent.getActivity(this@OTTableExportService, notificationId,
                                            Intent.createChooser(Intent(Intent.ACTION_VIEW).putExtra(Intent.EXTRA_STREAM, exportUri).setDataAndType(exportUri, mimeType).apply { this.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION },
                                                    getString(R.string.msg_open_exported_file)), PendingIntent.FLAG_CANCEL_CURRENT))
                                    .addAction(0, getString(R.string.msg_share),
                                            PendingIntent.getActivity(this@OTTableExportService, notificationId,
                                                    Intent.createChooser(Intent(Intent.ACTION_SEND).setDataAndType(exportUri, mimeType).putExtra(Intent.EXTRA_STREAM, exportUri).apply { this.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION },
                                                            getString(R.string.msg_share_exported_file)), PendingIntent.FLAG_UPDATE_CURRENT)
                                    )
                                    .build()
                    notificationManager.notify(TAG, notificationId, notification)
                }
                stopSelf(startId)
            }

            subscriptions.add(
                    Single.defer<Boolean> {
                        val configuredContext = (application as OTApp).currentConfiguredContext
                        val tracker = dbManager.get().getUnManagedTrackerDao(trackerId, realm)
                        val attributes = tracker?.attributes?.filter { it.isHidden == false && it.isInTrashcan == false }
                                ?: emptyList()
                        loadedTracker = tracker
                        if (tracker == null) {
                            return@defer Single.just(true)
                        } else {
                            if (externalFilesInvolved) {
                                cacheDirectory = this@OTTableExportService.cacheDir.resolve("export_${System.currentTimeMillis()}")
                                cacheDirectory?.let {
                                    if (!it.exists())
                                        cacheDirectory?.mkdirs()
                                }

                                involvedFileList = ArrayList<String>()
                            }

                            table.columns.add("index")
                            table.columns.add("logged_at")
                            table.columns.add("source")

                            attributes.forEach {
                                it.getHelper(configuredContext).onAddColumnToTable(it, table.columns)
                            }


                            val items = dbManager.get().makeItemsQuery(trackerId, null, null, realm).findAll()
                            items.withIndex().forEach { itemWithIndex ->
                                val item = itemWithIndex.value
                                val row = ArrayList<String?>()
                                row.add(itemWithIndex.index.toString())
                                row.add(item.timestamp.toString())
                                row.add(item.loggingSource.name)
                                attributes.forEach { attribute ->
                                    attribute.getHelper(configuredContext).onAddValueToTable(attribute, item.getValueOf(attribute.localId), row, itemWithIndex.index.toString())
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
                                attributes.filter { it.getHelper(configuredContext).isExternalFile(it) && it.getHelper(configuredContext) is OTFileInvolvedAttributeHelper }.forEach { attr ->
                                    val helper = attr.getHelper(configuredContext) as OTFileInvolvedAttributeHelper
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
                            involvedFileList?.let { list ->
                                println("Involved files: ")
                                for (path in list) {
                                    println(path)
                                }

                                try {
                                    val outputStream = contentResolver.openOutputStream(exportUri)
                                    kr.ac.snu.hcil.omnitrack.utils.io.ZipUtil.zip(cacheDirectory!!.absolutePath, outputStream)
                                } catch (ex: Exception) {
                                    //fail
                                    ex.printStackTrace()
                                    false
                                }
                            } == true
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
                            } catch (ex: Exception) {
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



            return START_REDELIVER_INTENT
        } else {
            stopSelf(startId)
            return START_NOT_STICKY
        }
    }
}