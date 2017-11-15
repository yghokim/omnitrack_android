package kr.ac.snu.hcil.omnitrack.core.net

import android.content.Context
import android.net.Uri
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.Job
import dagger.Lazy
import io.reactivex.Single
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.core.database.UploadTaskInfo
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

    fun registerNewUploadTask(outUri: SynchronizedUri, itemId: String, trackerId: String, userId: String) {
        realmProvider.get().use { realm ->
            val dbObject = UploadTaskInfo()
            dbObject.id = UUID.randomUUID().toString()

            //realm.createObject(UploadTaskInfo::class.java, UUID.randomUUID().toString())
            dbObject.itemId = itemId
            dbObject.trackerId = trackerId
            dbObject.userId = userId
            dbObject.localUri = outUri.localUri.toString()
            dbObject.serverUri = outUri.serverUri.toString()

            realm.executeTransaction {
                realm.copyToRealmOrUpdate(dbObject)
            }
        }
        dispatcher.get().mustSchedule(jobProvider.get())
    }

    fun makeFilePath(itemId: String, trackerId: String, userId: String, fileName: String): Uri {
        return core.makeFilePath(itemId, trackerId, userId, fileName)
    }

    fun downloadFileTo(pathString: String, localUri: Uri): Single<Uri> {
        return core.downloadFileTo(pathString, localUri)
    }
}