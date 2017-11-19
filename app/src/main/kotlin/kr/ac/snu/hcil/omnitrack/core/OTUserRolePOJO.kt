package kr.ac.snu.hcil.omnitrack.core

import android.support.annotation.Keep

/**
 * Created by younghokim on 2017. 9. 29..
 */
@Keep
class OTUserRolePOJO {
    var role: String = ""
    var isConsentApproved = false
    var information: Any? = null
}