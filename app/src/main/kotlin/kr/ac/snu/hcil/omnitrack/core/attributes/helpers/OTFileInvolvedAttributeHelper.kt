package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.net.Uri
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Callable

/**
 * Created by younghokim on 2017. 10. 13..
 */
abstract class OTFileInvolvedAttributeHelper : OTAttributeHelper() {

    override fun isExternalFile(attribute: OTAttributeDAO): Boolean {
        return true
    }

    override fun onAddColumnToTable(attribute: OTAttributeDAO, out: MutableList<String>) {
        out.add("${getAttributeUniqueName(attribute)}_filepath")
    }

    fun storeValueFile(attribute: OTAttributeDAO, value: Any?, outputUri: Uri): Single<Uri> {
        if (value is SynchronizedUri) {
            return Single.defer<Uri>(Callable<Single<Uri>> {

                fun tryServerDownload(): Single<Uri> {
                    return OTApp.instance.storageHelper.downloadFileTo(value.serverUri.path, outputUri).flatMap { uri ->
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
                    } catch (exception: Exception) {
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

    fun isValueContainingFileInfo(attribute: OTAttributeDAO, value: Any?): Boolean {
        if (value is SynchronizedUri) {
            return !value.isEmpty
        } else return false
    }

    abstract fun makeRelativeFilePathFromValue(attribute: OTAttributeDAO, value: Any?, uniqKey: String?): String

    override fun onAddValueToTable(attribute: OTAttributeDAO, value: Any?, out: MutableList<String?>, uniqKey: String?) {
        if (isValueContainingFileInfo(attribute, value)) {
            out.add(makeRelativeFilePathFromValue(attribute, value, uniqKey))
        } else out.add(null)
    }
}