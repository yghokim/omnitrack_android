package kr.ac.snu.hcil.omnitrack.core.attributes

import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.util.SparseArray
import com.afollestad.materialdialogs.MaterialDialog
import com.tbruyelle.rxpermissions.RxPermissions
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.*
import kr.ac.snu.hcil.omnitrack.utils.DialogHelper

/**
 * Created by Young-Ho on 10/7/2017.
 */
object OTAttributeManager {

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

    private val attributeCharacteristicsTable = SparseArray<OTAttributeHelper>()

    fun getAttributeHelper(type: Int): OTAttributeHelper {
        val characteristics = attributeCharacteristicsTable[type]
        if (characteristics == null) {
            val fallback = when (type) {
                TYPE_NUMBER -> OTNumberAttributeHelper()
                TYPE_TIME -> OTTimeAttributeHelper()
                TYPE_TIMESPAN -> OTTimeSpanAttributeHelper()
                TYPE_SHORT_TEXT -> OTShortTextAttributeHelper()
                TYPE_LONG_TEXT -> OTLongTextAttributeHelper()
                TYPE_LOCATION -> OTLocationAttributeHelper()
                TYPE_CHOICE -> OTChoiceAttributeHelper()
                TYPE_RATING -> OTRatingAttributeHelper()
                TYPE_IMAGE -> OTImageAttributeHelper()
                TYPE_AUDIO -> OTAudioRecordAttributeHelper()
                else -> throw Exception("Unsupported type key: ${type}")
            }
            this.attributeCharacteristicsTable.setValueAt(type, fallback)
            return fallback
        } else return characteristics
    }

    fun showPermissionCheckDialog(activity: Activity, typeId: Int, typeName: String, onGranted: (Boolean) -> Unit, onDenied: (() -> Unit)? = null): MaterialDialog? {
        val requiredPermissions = OTAttribute.getPermissionsForAttribute(typeId)
        if (requiredPermissions != null) {
            val notGrantedPermissions = requiredPermissions.filter { ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED }
            if (notGrantedPermissions.isNotEmpty()) {
                val dialog = DialogHelper.makeYesNoDialogBuilder(activity, activity.resources.getString(R.string.msg_permission_required),
                        String.format(activity.resources.getString(R.string.msg_format_permission_request_of_field), typeName),
                        cancelable = false,
                        onYes = {
                            val rxPermissions = RxPermissions(activity)
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