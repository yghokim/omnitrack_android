package kr.ac.snu.hcil.omnitrack.core.net

import io.reactivex.Single

/**
 * Created by younghokim on 2017. 11. 28..
 */
interface IUsageLogUploadAPI {
    fun uploadLocalUsageLogs(serializedLogs: List<String>): Single<List<Long>>
}