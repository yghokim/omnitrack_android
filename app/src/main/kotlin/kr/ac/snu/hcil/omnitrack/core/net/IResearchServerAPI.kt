package kr.ac.snu.hcil.omnitrack.core.net

import android.support.annotation.Keep
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.ExperimentInfo

/**
 * Created by younghokim on 2018. 1. 3..
 */

@Keep
data class ExperimentCommandResult(
        val success: Boolean,
        val error: String?,
        val injectionExists: Boolean,
        val experiment: ExperimentInfo?
)


@Keep
data class DropoutBody(val reason: String?)

interface IResearchServerAPI {

    fun approveExperimentInvitation(invitationCode: String): Single<ExperimentCommandResult>
    fun rejectExperimentInvitation(invitationCode: String): Completable
    fun dropOutFromExperiment(experimentId: String, reason: CharSequence?): Single<ExperimentCommandResult>

    fun retrieveJoinedExperiments(after: Long = Long.MIN_VALUE): Single<List<ExperimentInfo>>
}