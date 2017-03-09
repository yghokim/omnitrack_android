package kr.ac.snu.hcil.omnitrack.core.attributes

import android.net.Uri
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Single
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.Callable

/**
 * Created by Young-Ho Kim on 2017-03-09.
 */
abstract class OTSynchronizedUriAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, typeId: Int, propertyData: Map<String, Any?>?, connectionData: String?)
    : OTExternalFileInvolvedAttribute<SynchronizedUri>(objectId, localKey, parentTracker, columnName, isRequired, typeId, propertyData, connectionData) {

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_SYNCHRONIZED_URI

    override fun storeValueFile(value: Any?, outputUri: Uri): Single<Uri> {
        if (value is SynchronizedUri) {
            return Single.defer<Uri>(Callable<Single<Uri>> {

                fun tryServerDownload(): Single<Uri> {
                    return OTApplication.app.storageHelper.downloadFileTo(value.serverUri.path, outputUri).flatMap {
                        uri ->
                        Single.just<Uri>(uri)
                    }
                }

                if (value.isLocalUriValid) {
                    try {
                        val inputStream = FileInputStream(File(value.localUri.path))
                        val outputStream = FileOutputStream(outputUri.path)
                        FileHelper.dumpStreamToOther(inputStream, outputStream)
                        inputStream.close()
                        println("copied local cached file - ${value.localUri}.")
                        return@Callable Single.just<Uri>(outputUri)
                    } catch(exception: Exception) {
                        println("reading local file error")
                        exception.printStackTrace()
                        return@Callable tryServerDownload()
                    }
                } else if (value.isSynchronized) {

                    println("File is not cached - need server download. - ${value.serverUri}")
                    return@Callable tryServerDownload()
                } else return@Callable Single.error<Uri>(Exception("file is not synchronized"))
            })
        } else return Single.error<Uri>(Exception("value is not a uri."))
    }

    override fun isValueContainingFileInfo(value: Any?): Boolean {
        if (value is SynchronizedUri) {
            return !value.isEmpty
        } else return false
    }
}