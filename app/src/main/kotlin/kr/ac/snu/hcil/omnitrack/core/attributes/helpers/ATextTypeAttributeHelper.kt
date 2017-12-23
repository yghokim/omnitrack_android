package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.AlphabeticalSorter
import kr.ac.snu.hcil.omnitrack.core.attributes.logics.TextLengthSorter
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO

/**
 * Created by Young-Ho on 10/12/2017.
 */
abstract class ATextTypeAttributeHelper(configuredContext: ConfiguredContext) : OTAttributeHelper(configuredContext) {

    override fun getSupportedSorters(attribute: OTAttributeDAO): Array<AFieldValueSorter> {
        return arrayOf(
                AlphabeticalSorter("${attribute.name} Alphabetical", attribute.localId),
                TextLengthSorter("${attribute.name} Length", attribute.localId)
        )
    }
}