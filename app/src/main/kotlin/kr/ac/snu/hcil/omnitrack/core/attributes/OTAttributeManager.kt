package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import dagger.Lazy
import kr.ac.snu.hcil.android.common.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.android.common.containers.CachedObjectPoolWithIntegerKey
import kr.ac.snu.hcil.android.common.view.DialogHelper
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.*
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Young-Ho on 10/7/2017.
 */
@Singleton
class OTAttributeManager @Inject constructor(val context: Context, val authManager: Lazy<OTAuthManager>) : CachedObjectPoolWithIntegerKey<OTAttributeHelper>() {

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

    override fun createNewInstance(key: Int): OTAttributeHelper {
        return when (key) {
            TYPE_NUMBER -> OTNumberAttributeHelper(context)
            TYPE_TIME -> OTTimeAttributeHelper(context)
            TYPE_TIMESPAN -> OTTimeSpanAttributeHelper(context)
            TYPE_SHORT_TEXT -> OTShortTextAttributeHelper(context)
            TYPE_LONG_TEXT -> OTLongTextAttributeHelper(context)
            TYPE_LOCATION -> OTLocationAttributeHelper(context)
            TYPE_CHOICE -> OTChoiceAttributeHelper(context)
            TYPE_RATING -> OTRatingAttributeHelper(context)
            TYPE_IMAGE -> OTImageAttributeHelper(context)
            TYPE_AUDIO -> OTAudioRecordAttributeHelper(context)
            else -> throw Exception("Unsupported type key: $key")
        }
    }

    private val attributeLocalIdGenerator = ConcurrentUniqueLongGenerator()

    fun makeNewAttributeLocalId(createdAt: Long = System.currentTimeMillis()): String {
        val nanoStamp = attributeLocalIdGenerator.getNewUniqueLong(createdAt)

        val id = authManager.get().deviceLocalKey + "_" + nanoStamp.toString(36)
        println("new attribute local id: $id")
        return id
    }

    fun showPermissionCheckDialog(fragment: Fragment, typeId: Int, typeName: String, onGranted: (Boolean) -> Unit, onDenied: (() -> Unit)? = null): MaterialDialog? {
        val requiredPermissions = get(typeId).getRequiredPermissions(null)
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