package kr.ac.snu.hcil.omnitrack.core.externals

import android.content.Context
import io.reactivex.Observable
import kr.ac.snu.hcil.android.common.TextHelper
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO

/**
 * Created by Young-Ho Kim on 16. 7. 28
 */
abstract class OTServiceMeasureFactory(context: Context, val parentService: OTExternalService, factoryTypeName: String) : OTMeasureFactory(context, factoryTypeName) {

    override fun getCategoryName(): String {
        return context.resources.getString(parentService.nameResourceId)
    }

    override val typeCode: String by lazy {
        "${parentService.identifier}_$factoryTypeName"
    }

    override fun makeAvailabilityCheckObservable(field: OTFieldDAO): Observable<Pair<Boolean, List<CharSequence>?>> {
        return parentService.onStateChanged.map { state ->
            val invalidMessages = ArrayList<CharSequence>()
            val valid = isAvailableToRequestValue(field, invalidMessages)
            return@map Pair(valid, invalidMessages)
        }
    }

    override fun isAvailableToRequestValue(field: OTFieldDAO, invalidMessages: MutableList<CharSequence>?): Boolean {
        if (parentService.state == OTExternalService.ServiceState.ACTIVATED) {
            return true
        } else {
            invalidMessages?.add(TextHelper.fromHtml(String.format(
                    "<font color=\"blue\">${context.resources.getString(R.string.msg_service_is_not_activated_format)}</font>",
                    context.resources.getString(parentService.nameResourceId))))
            return false
        }
    }

    fun <T : OTExternalService> getService(): T {
        @Suppress("UNCHECKED_CAST")
        return parentService as T
    }

    override fun onMakeFormattedName(): String {
        return "${super.onMakeFormattedName()} | ${context.resources.getString(parentService.nameResourceId)}"
    }
}