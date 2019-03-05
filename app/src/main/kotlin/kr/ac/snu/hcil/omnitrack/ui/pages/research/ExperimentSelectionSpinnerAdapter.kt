package kr.ac.snu.hcil.omnitrack.ui.pages.research

import android.content.Context
import com.jaredrummler.materialspinner.MaterialSpinnerAdapter
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.research.ExperimentInfo

/**
 * Created by Young-Ho on 2/20/2018.
 */
class ExperimentSelectionSpinnerAdapter(context: Context, infoList: List<ExperimentInfo>) : MaterialSpinnerAdapter<Any>(context, listOf(NoSelectionEntry(context)) + infoList) {
    class NoSelectionEntry(val context: Context) {
        override fun toString(): String {
            return context.resources.getString(R.string.msg_none)
        }
    }
}