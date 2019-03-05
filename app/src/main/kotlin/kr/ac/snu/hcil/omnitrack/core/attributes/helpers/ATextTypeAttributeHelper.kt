package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AlphabeticalSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.TextLengthSorter
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO

/**
 * Created by Young-Ho on 10/12/2017.
 */
abstract class ATextTypeAttributeHelper(context: Context) : OTAttributeHelper(context) {

    override fun getSupportedSorters(attribute: OTAttributeDAO): Array<AFieldValueSorter> {
        return arrayOf(
                AlphabeticalSorter("${attribute.name} Alphabetical", attribute.localId),
                TextLengthSorter("${attribute.name} Length", attribute.localId)
        )
    }
}