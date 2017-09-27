package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import rx.Single

/**
 * Created by younghokim on 2017. 9. 27..
 */
interface IServerSideAPI {
    fun pullItemsAfter(timestamp: Long): Single<List<OTItemPOJO>>
    fun pushItemsDirty(items: List<OTItemPOJO>): Single<List<String>>

}