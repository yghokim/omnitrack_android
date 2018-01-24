package kr.ac.snu.hcil.omnitrack.core.research

import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.ExperimentInfo
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.OTExperimentDAO
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.di.configured.Research
import kr.ac.snu.hcil.omnitrack.core.net.ExperimentInvitation
import kr.ac.snu.hcil.omnitrack.core.net.IResearchServerAPI
import javax.inject.Inject

/**
 * Created by younghokim on 2018. 1. 4..
 */
@Configured
class ResearchManager @Inject constructor(val serverApiController: IResearchServerAPI, @Research val researchRealmFactory: Factory<Realm>) {

    fun dropoutFromExperiment(experimentId: String, reason: String?): Completable {
        return serverApiController.dropOutFromExperiment(experimentId, reason).flatMapCompletable { result ->
            if (result.success && result.experiment != null) {
                return@flatMapCompletable Completable.defer {
                    researchRealmFactory.get().use { realm ->
                        realm.executeTransaction {
                            saveExperimentInfo(result.experiment, realm)
                        }
                    }
                    return@defer Completable.complete()
                }.subscribeOn(Schedulers.io())
            } else {
                return@flatMapCompletable Completable.complete()
            }
        }
    }

    fun approveInvitation(invitationCode: String): Single<Boolean> {
        return serverApiController.approveExperimentInvitation(invitationCode).flatMap { result ->
            if (result.success && result.experiment != null) {
                return@flatMap Single.defer {
                    researchRealmFactory.get().use { realm ->
                        realm.executeTransaction {
                            saveExperimentInfo(result.experiment, realm)
                        }
                        println("Joined to new experiment: ${result.experiment.name}")
                    }
                    return@defer Single.just(true)
                }.subscribeOn(Schedulers.io())
            } else {
                return@flatMap Single.just(false)
            }
        }
    }

    private fun saveExperimentInfo(experiment: ExperimentInfo, realm: Realm) {
        val dao = realm.where(OTExperimentDAO::class.java).equalTo("id", experiment.id).findFirst() ?: realm.createObject(OTExperimentDAO::class.java, experiment.id)
        dao.name = experiment.name
        dao.joinedAt = experiment.joinedAt
        dao.droppedAt = experiment.droppedAt
    }

    fun updateExperimentsFromServer(): Completable {
        return serverApiController.retrieveJoinedExperiments().flatMapCompletable { experiments ->
            return@flatMapCompletable Completable.defer {
                    researchRealmFactory.get().use { realm ->
                        realm.executeTransaction {
                            if (experiments.isNotEmpty()) {
                                realm.where(OTExperimentDAO::class.java)
                                        .not().`in`("id", experiments.map { it.id }.toTypedArray())
                                        .findAll().deleteAllFromRealm()

                                experiments.forEach { experimentInfo ->
                                    saveExperimentInfo(experimentInfo, realm)
                                }
                            } else {
                                realm.where(OTExperimentDAO::class.java).findAll().deleteAllFromRealm()
                            }
                        }
                    }
                    return@defer Completable.complete()
                }
        }
    }

    fun loadPublicInvitations(): Single<List<ExperimentInvitation>> {
        return serverApiController.retrievePublicInvitations()
    }

    fun getExperimentsInLocal(): Single<List<ExperimentInfo>> {
        return Single.defer {
            researchRealmFactory.get().use { realm ->
                return@use Single.just(realm.where(OTExperimentDAO::class.java).sort("joinedAt", Sort.DESCENDING).findAll().map { dao -> dao.getInfo() })
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }
}