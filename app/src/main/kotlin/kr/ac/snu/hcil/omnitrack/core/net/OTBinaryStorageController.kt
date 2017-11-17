package kr.ac.snu.hcil.omnitrack.core.net

import android.content.Context
import android.net.Uri
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.Job
import dagger.Lazy
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import java.util.*
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by younghokim on 2017. 11. 15..
 */
class OTBinaryStorageController(val context: Context, val core: IBinaryStorageCore, val realmProvider: Provider<Realm>) {

    @Inject
    lateinit var dispatcher: Lazy<FirebaseJobDispatcher>

    private val jobProvider: Provider<Job>

    init {
        val component = (context.applicationContext as OTApp).scheduledJobComponent
        component.inject(this)
        jobProvider = component.getBinaryUploadJob()
    }

    fun registerNewUploadTask(localPath: String, serverPath: String) {
        realmProvider.get().use { realm ->
            realm.executeTransactionIfNotIn {
                val dbObject = realm.where(UploadTaskInfo::class.java)
                        .equalTo("serverPath", serverPath).findFirst() ?: realm.createObject(UploadTaskInfo::class.java, UUID.randomUUID().toString())
                dbObject.localFilePath = localPath
                dbObject.serverPath = serverPath
                if (dbObject.trialCount.isNull) {
                    dbObject.trialCount.set(0L)
                }
            }
        }
        dispatcher.get().mustSchedule(jobProvider.get())
    }

    fun makeServerPath(userId: String, trackerId: String, itemId: String, attributeLocalId: String, fileIdentifier: String): String {
        return core.makeServerPath(userId, trackerId, itemId, attributeLocalId, fileIdentifier)
    }

    fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri> {
        return core.downloadFileTo(pathString, localUri)
    }
}