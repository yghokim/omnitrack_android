package kr.ac.snu.hcil.omnitrack.core.fields.helpers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.fields.logics.AFieldValueSorter
import kr.ac.snu.hcil.omnitrack.core.fields.logics.AlphabeticalSorter
import kr.ac.snu.hcil.omnitrack.core.fields.logics.TextLengthSorter
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO

/**
 * Created by Young-Ho on 10/12/2017.
 */
abstract class ATextTypeFieldHelper(context: Context) : OTFieldHelper(context) {

    override fun getSupportedSorters(field: OTFieldDAO): Array<AFieldValueSorter> {
        return arrayOf(
                AlphabeticalSorter("${field.name} Alphabetical", field.localId),
                TextLengthSorter("${field.name} Length", field.localId)
        )
    }
}