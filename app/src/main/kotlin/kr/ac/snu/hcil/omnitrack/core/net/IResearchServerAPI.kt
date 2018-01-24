package kr.ac.snu.hcil.omnitrack.core.net

import android.support.annotation.Keep
import android.support.v7.util.DiffUtil
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
data class ParticipantInfo(
        val user: String,
        val isDenied: Boolean,
        val isConsentApproved: Boolean,
        val dropped: Boolean
)

@Keep
data class ExperimentInvitation(
        val code: String,
        val experiment: Experiment,
        val participants: List<ParticipantInfo>
) {
    class DiffCallback(val oldList: List<ExperimentInvitation>, val newList: List<ExperimentInvitation>) : DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].code == newList[newItemPosition].code
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.code == new.code && old.experiment._id == new.experiment._id
        }

    }
}

@Keep
data class Experiment(val _id: String, val name: String)


@Keep
data class DropoutBody(val reason: String?)

interface IResearchServerAPI {

    fun approveExperimentInvitation(invitationCode: String): Single<ExperimentCommandResult>
    fun rejectExperimentInvitation(invitationCode: String): Completable
    fun dropOutFromExperiment(experimentId: String, reason: CharSequence?): Single<ExperimentCommandResult>

    fun retrieveJoinedExperiments(after: Long = Long.MIN_VALUE): Single<List<ExperimentInfo>>

    fun retrievePublicInvitations(): Single<List<ExperimentInvitation>>
}