package kr.ac.snu.hcil.omnitrack.ui.pages.connectionwizard

import android.content.Context
import com.tech.freak.wizardpager.model.PageList

/**
 * Created by Young-Ho on 8/12/2016.
 */
class ConnectionWizardModel : com.tech.freak.wizardpager.model.AbstractWizardModel {
    override fun onNewRootPageList(): PageList {
        return PageList(
        )
    }

    constructor(context: Context?) : super(context)


}