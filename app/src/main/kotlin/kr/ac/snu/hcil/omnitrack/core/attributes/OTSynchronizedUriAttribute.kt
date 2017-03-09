package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Single
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

/**
 * Created by Young-Ho Kim on 2017-03-09.
 */
abstract class OTSynchronizedUriAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, typeId: Int, propertyData: Map<String, Any?>?, connectionData: String?)
    : OTExternalFileInvolvedAttribute<SynchronizedUri>(objectId, localKey, parentTracker, columnName, isRequired, typeId, propertyData, connectionData) {

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_SYNCHRONIZED_URI

    override fun storeValueFile(value: Any?, outputStream: OutputStream): Single<Void> {
        if (value is SynchronizedUri) {
            return Single.defer<Void> {

                var needServerDownload = false

                if (value.isLocalUriValid) {
                    try {
                        val inputStream = FileInputStream(File(value.localUri.path))
                        FileHelper.dumpStreamToOther(inputStream, outputStream)
                        inputStream.close()
                        Single.just(null)
                    } catch(exception: Exception) {
                        needServerDownload = true
                    }
                } else if (value.isSynchronized) {
                    needServerDownload = true
                }

                if (needServerDownload) {
                    OTApplication.app.storageHelper.downloadFileAsStream(value.serverUri.path).flatMap {
                        stream ->
                        FileHelper.dumpStreamToOther(stream, outputStream)
                        stream.close()
                        Single.just(null)
                    }
                    /*
                    OTApplication.app.storageHelper.getDownloadUrl(value.serverUri.path).map{
                        url->

                    }*/
                } else Single.error<Void>(Exception("file is not synchronized"))
            }

        } else return Single.error<Void>(Exception("value is not a uri."))
    }

    override fun isValueContainingFileInfo(value: Any?): Boolean {
        if (value is SynchronizedUri) {
            return !value.isEmpty
        } else return false
    }
}