package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.net.Uri
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.OTServerFile
import kr.ac.snu.hcil.omnitrack.core.net.OTLocalMediaCacheManager
import kr.ac.snu.hcil.omnitrack.utils.io.FileHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 10. 13..
 */
abstract class OTFileInvolvedAttributeHelper(configuredContext: ConfiguredContext) : OTAttributeHelper(configuredContext) {


    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_SERVERFILE

    @Inject
    protected lateinit var localCacheManager: OTLocalMediaCacheManager

    init {
        configuredContext.configuredAppComponent.inject(this)
    }

    override fun isExternalFile(attribute: OTAttributeDAO): Boolean {
        return true
    }

    override fun onAddColumnToTable(attribute: OTAttributeDAO, out: MutableList<String>) {
        out.add("${getAttributeUniqueName(attribute)}_filepath")
    }

    fun storeValueFile(attribute: OTAttributeDAO, value: Any?, outputUri: Uri): Single<Uri> {
        if (value is OTServerFile) {
            return localCacheManager.getCachedUri(value, outputUri, false).flatMap { (refreshed, resultUri) ->
                if (!refreshed || resultUri != outputUri) {
                    val inputStream = FileInputStream(File(resultUri.path))
                        val outputStream = FileOutputStream(outputUri.path)
                        FileHelper.dumpStreamToOther(inputStream, outputStream)
                        inputStream.close()
                    println("copied local cached file - ${resultUri}.")
                    }
                return@flatMap Single.just(outputUri)
            }
        } else return Single.error<Uri>(Exception("value is not a uri."))
    }

    fun isValueContainingFileInfo(attribute: OTAttributeDAO, value: Any?): Boolean {
        return value is OTServerFile
    }

    abstract fun makeRelativeFilePathFromValue(attribute: OTAttributeDAO, value: Any?, uniqKey: String?): String

    override fun onAddValueToTable(attribute: OTAttributeDAO, value: Any?, out: MutableList<String?>, uniqKey: String?) {
        if (isValueContainingFileInfo(attribute, value)) {
            out.add(makeRelativeFilePathFromValue(attribute, value, uniqKey))
        } else out.add(null)
    }
}