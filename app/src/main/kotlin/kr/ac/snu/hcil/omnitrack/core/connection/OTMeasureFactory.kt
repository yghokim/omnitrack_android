package kr.ac.snu.hcil.omnitrack.core.connection

import android.content.Context
import android.text.Html
import com.google.gson.stream.JsonReader
import io.reactivex.Observable
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.INameDescriptionResourceProvider
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO

/**
 * Created by Young-Ho Kim on 16. 7. 28
 */
abstract class OTMeasureFactory(val context: Context, val factoryTypeName: String) : INameDescriptionResourceProvider {

    abstract val typeCode: String

    open val requiredPermissions: Array<String> = arrayOf()

    abstract fun getAttributeType(): Int

    abstract val isRangedQueryAvailable: Boolean
    abstract val isDemandingUserInput: Boolean
    abstract val minimumGranularity: OTTimeRangeQuery.Granularity?

    /*** Typename in TypeStringSerializer
     *
     */
    abstract val dataTypeName: String

    abstract fun makeMeasure(): OTMeasure
    abstract fun makeMeasure(reader: JsonReader): OTMeasure
    abstract fun makeMeasure(serialized: String): OTMeasure
    abstract fun serializeMeasure(measure: OTMeasure): String

    fun getFormattedName(): CharSequence {
        //val html = "<b>${context.resources.getString(nameResourceId)}</b> | ${context.resources.getString(parentService.nameResourceId)}"
        val html = onMakeFormattedName()
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    abstract fun makeAvailabilityCheckObservable(attribute: OTAttributeDAO): Observable<Pair<Boolean, List<CharSequence>?>>
    abstract fun isAvailableToRequestValue(attribute: OTAttributeDAO, invalidMessages: MutableList<CharSequence>? = null): Boolean

    abstract fun getCategoryName(): String

    protected open fun onMakeFormattedName(): String {
        return "<b>${context.resources.getString(nameResourceId)}</b>"
    }

    abstract class OTMeasure(private val factory: OTMeasureFactory) {

        open val factoryCode: String get() = this.factory.typeCode

        open fun getFormattedName(): CharSequence {
            return factory.getFormattedName()
        }

        abstract fun getValueRequest(builder: OTItemBuilderWrapperBase?, query: OTTimeRangeQuery?): Single<Nullable<out Any>>

        fun <T : OTMeasureFactory> getFactory(): T {
            @Suppress("UNCHECKED_CAST")
            return factory as T
        }
    }

    abstract class OTRangeQueriedMeasure(factory: OTMeasureFactory) : OTMeasure(factory) {

        abstract fun getValueRequest(start: Long, end: Long): Single<Nullable<out Any>>

        override fun getValueRequest(builder: OTItemBuilderWrapperBase?, query: OTTimeRangeQuery?): Single<Nullable<out Any>> {
            val range = query!!.getRange(builder)
            return getValueRequest(range.first, range.second)
        }
    }
}