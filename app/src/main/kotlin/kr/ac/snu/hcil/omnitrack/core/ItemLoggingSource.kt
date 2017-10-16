package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho on 10/17/2017.
 */

enum class ItemLoggingSource(val nameResId: Int) {
    Unspecified(R.string.msg_tracking_source_unspecified),
    Trigger(R.string.msg_tracking_source_trigger),
    Shortcut(R.string.msg_tracking_source_shortcut),
    Manual(R.string.msg_tracking_source_manual);

    val sourceText: String by lazy {
        OTApplication.app.resourcesWrapped.getString(nameResId)
    }
}