package kr.ac.snu.hcil.omnitrack.services.messaging

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper

/**
 * Created by younghokim on 2017. 4. 12..
 */
class OTFirebaseInstanceIdService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        super.onTokenRefresh()
        println("Firebase cloud messaging instance id refreshed. token: ${FirebaseInstanceId.getInstance().token}")
        FirebaseDbHelper.refreshInstanceIdToServerIfExists(false)
    }
}