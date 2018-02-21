package kr.ac.snu.hcil.omnitrack.ui.pages.research

import android.content.Context
import com.jaredrummler.materialspinner.MaterialSpinnerAdapter
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.ExperimentInfo

/**
 * Created by Young-Ho on 2/20/2018.
 */
class ExperimentSelectionSpinnerAdapter(context: Context, infoList: List<ExperimentInfo>) : MaterialSpinnerAdapter<Any>(context, listOf(NO_SELECTION) + infoList) {
    companion object {
        val NO_SELECTION = NoSelectionEntry()
    }

    class NoSelectionEntry {
        override fun toString(): String {
            return OTApp.getString(R.string.msg_none)
        }
    }
}