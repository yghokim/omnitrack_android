package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI

/**
 * Created by younghokim on 2017. 9. 27..
 */
class OTSyncManager(val context: Context, val synchronizationServerController: ISynchronizationServerSideAPI) {


    fun performSynchronization() {

    }

    fun performSynchronizationOf(dataType: ESyncDataType) {
        context.startService(OTSynchronizationService.makePerformSynchronizationSessionIntent(context, dataType, SyncDirection.BIDIRECTIONAL))
    }
}