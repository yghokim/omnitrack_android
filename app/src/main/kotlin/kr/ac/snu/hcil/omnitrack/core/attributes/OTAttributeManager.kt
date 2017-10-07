package kr.ac.snu.hcil.omnitrack.core.attributes

import android.util.SparseArray
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.*

/**
 * Created by Young-Ho on 10/7/2017.
 */
object OTAttributeManager {
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
}