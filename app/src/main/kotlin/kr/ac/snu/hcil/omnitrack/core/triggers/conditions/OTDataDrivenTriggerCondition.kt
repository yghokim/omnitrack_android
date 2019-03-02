package kr.ac.snu.hcil.omnitrack.core.triggers.conditions

import android.content.Context
import com.google.gson.JsonObject
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.connection.OTConnection
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import java.math.BigDecimal

class OTDataDrivenTriggerCondition : ATriggerCondition(OTTriggerDAO.CONDITION_TYPE_DATA) {

    companion object {
        const val COMPARISON_VALUE_LE_THRESHOLD = "le"
        const val COMPARISON_VALUE_GE_THRESHOLD = "ge"
        const val COMPARISON_VALUE_LT_THRESHOLD = "lt"
        const val COMPARISON_VALUE_GT_THRESHOLD = "gt"
    }

    var connection: OTConnection? = null
    var threshold: BigDecimal = BigDecimal.ZERO
    var comparison: String = COMPARISON_VALUE_GE_THRESHOLD

    override fun getSerializedString(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isConfigurationValid(context: Context): Single<Pair<Boolean, List<CharSequence>?>> {
        return connection?.makeValidationStateObservable(context)?.firstOrError()?.map { (valid, message) ->
            Pair(valid, message?.let { listOf(message) })
        } ?: Single.just(Pair<Boolean, List<CharSequence>?>(false, listOf("Condition is not set.")))
    }

    override fun writeEventLogContent(table: JsonObject) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun makeInformationText(): CharSequence {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}