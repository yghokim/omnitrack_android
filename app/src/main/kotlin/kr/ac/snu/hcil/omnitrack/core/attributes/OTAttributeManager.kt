package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.pm.PackageManager
import android.util.SparseArray
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.*
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.utils.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper
import javax.inject.Inject

/**
 * Created by Young-Ho on 10/7/2017.
 */
@Configured
class OTAttributeManager @Inject constructor(val configuredContext: ConfiguredContext, val authManager: Lazy<OTAuthManager>) {

    companion object {
        const val VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE = 0
        const val VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_SINGLELINE = 1

        const val TYPE_NUMBER = 0
        const val TYPE_TIME = 1
        const val TYPE_TIMESPAN = 2
        const val TYPE_SHORT_TEXT = 3
        const val TYPE_LONG_TEXT = 4
        const val TYPE_LOCATION = 5
        const val TYPE_CHOICE = 6
        const val TYPE_RATING = 7
        const val TYPE_IMAGE = 8
        const val TYPE_AUDIO = 9

    }

    private val attributeCharacteristicsTable = SparseArray<OTAttributeHelper>()

    fun getAttributeHelper(type: Int): OTAttributeHelper {
        val characteristics = attributeCharacteristicsTable[type]
        if (characteristics == null) {
            val fallback = when (type) {
                TYPE_NUMBER -> OTNumberAttributeHelper(configuredContext)
                TYPE_TIME -> OTTimeAttributeHelper(configuredContext)
                TYPE_TIMESPAN -> OTTimeSpanAttributeHelper(configuredContext)
                TYPE_SHORT_TEXT -> OTShortTextAttributeHelper(configuredContext)
                TYPE_LONG_TEXT -> OTLongTextAttributeHelper(configuredContext)
                TYPE_LOCATION -> OTLocationAttributeHelper(configuredContext)
                TYPE_CHOICE -> OTChoiceAttributeHelper(configuredContext)
                TYPE_RATING -> OTRatingAttributeHelper(configuredContext)
                TYPE_IMAGE -> OTImageAttributeHelper(configuredContext)
                TYPE_AUDIO -> OTAudioRecordAttributeHelper(configuredContext)
                else -> throw Exception("Unsupported type key: $type")
            }
            this.attributeCharacteristicsTable.setValueAt(type, fallback)
            return fallback
        } else return characteristics
    }

    private val attributeLocalIdGenerator = ConcurrentUniqueLongGenerator()

    fun makeNewAttributeLocalId(createdAt: Long = System.currentTimeMillis()): String {
        val nanoStamp = attributeLocalIdGenerator.getNewUniqueLong(createdAt)

        val id = authManager.get().getDeviceLocalKey() + "_" + nanoStamp.toString(36)
        println("new attribute local id: $id")
        return id
    }

    fun showPermissionCheckDialog(fragment: Fragment, typeId: Int, typeName: String, onGranted: (Boolean) -> Unit, onDenied: (() -> Unit)? = null): MaterialDialog? {
        val requiredPermissions = getAttributeHelper(typeId).getRequiredPermissions(null)
        if (requiredPermissions != null) {
            val notGrantedPermissions = requiredPermissions.filter { ContextCompat.checkSelfPermission(fragment.requireContext(), it) != PackageManager.PERMISSION_GRANTED }
            if (notGrantedPermissions.isNotEmpty()) {
                val dialog = DialogHelper.makeYesNoDialogBuilder(fragment.requireActivity(), fragment.resources.getString(R.string.msg_permission_required),
                        String.format(fragment.resources.getString(R.string.msg_format_permission_request_of_field), typeName),
                        cancelable = false,
                        onYes = {
                            val rxPermissions = RxPermissions(fragment)
                            rxPermissions.request(*requiredPermissions)
                                    .subscribe { granted ->
                                        if (granted) {
                                            onGranted.invoke(true)
                                        } else {
                                            onDenied?.invoke()
                                        }
                                    }
                        },
                        onCancel = null,
                        yesLabel = R.string.msg_allow_permission,
                        noLabel = R.string.msg_cancel
                )
                return dialog.show()
            } else {
                onGranted.invoke(false)
                return null
            }
        } else {
            onGranted.invoke(false)
            return null
        }
    }
}