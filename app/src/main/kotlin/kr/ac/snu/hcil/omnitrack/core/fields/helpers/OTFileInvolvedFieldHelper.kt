package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import android.content.Context
import android.net.Uri
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.file.FileHelper
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.net.OTLocalMediaCacheManager
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.core.types.OTServerFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 10. 13..
 */
abstract class OTFileInvolvedFieldHelper(context: Context) : OTFieldHelper(context) {


    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_SERVERFILE

    @Inject
    lateinit var localCacheManager: OTLocalMediaCacheManager

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun isExternalFile(field: OTFieldDAO): Boolean {
        return true
    }

    override fun onAddColumnToTable(field: OTFieldDAO, out: MutableList<String>) {
        out.add("${getAttributeUniqueName(field)}_filepath")
    }

    fun storeValueFile(field: OTFieldDAO, value: Any?, outputUri: Uri): Single<Uri> {
        if (value is OTServerFile) {
            return localCacheManager.getCachedUri(value, outputUri, false).flatMap { (refreshed, resultUri) ->
                if (!refreshed || resultUri != outputUri) {
                    val inputStream = FileInputStream(File(resultUri.path))
                        val outputStream = FileOutputStream(outputUri.path)
                        FileHelper.dumpStreamToOther(inputStream, outputStream)
                        inputStream.close()
                    println("copied local cached file - $resultUri.")
                    }
                return@flatMap Single.just(outputUri)
            }
        } else return Single.error<Uri>(Exception("value is not a uri."))
    }

    fun isValueContainingFileInfo(field: OTFieldDAO, value: Any?): Boolean {
        return value is OTServerFile
    }

    abstract fun makeRelativeFilePathFromValue(field: OTFieldDAO, value: Any?, uniqKey: String?): String

    override fun onAddValueToTable(field: OTFieldDAO, value: Any?, out: MutableList<String?>, uniqKey: String?) {
        if (isValueContainingFileInfo(field, value)) {
            out.add(makeRelativeFilePathFromValue(field, value, uniqKey))
        } else out.add(null)
    }
}