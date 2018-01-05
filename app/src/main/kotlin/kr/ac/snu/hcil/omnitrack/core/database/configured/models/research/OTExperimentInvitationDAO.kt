package kr.ac.snu.hcil.omnitrack.core.database.configured.models.research

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Created by younghokim on 2018. 1. 3..
 */
open class OTExperimentInvitationDAO : RealmObject() {
    @PrimaryKey
    var code: String = ""

    var title: String = ""
    var content: String = ""

    fun getInfo(): InvitationInfo {
        return InvitationInfo(code, title, content)
    }
}

data class InvitationInfo(val code: String, val title: String, val content: String)