package kr.ac.snu.hcil.omnitrack.core.database.synchronization.official

import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import rx.Single

/**
 * Created by younghokim on 2017. 9. 28..
 * Retrofit service interface to communicate between OmniTrack Official server
 */
interface OTOfficialServerService {

    @GET("api/items/changes")
    fun getItemServerChanges(@Query("user") userId: String, @Query("timestamp") timestamp: String): Single<List<OTItemPOJO>>

    @POST("api/items/changes")
    fun postItemLocalChanges(@Query("user") userId: String, @Body list: List<OTItemPOJO>): Single<List<SyncResultEntry>>
}