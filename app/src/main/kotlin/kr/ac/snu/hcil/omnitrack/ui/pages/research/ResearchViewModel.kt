package kr.ac.snu.hcil.omnitrack.ui.pages.research

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import dagger.internal.Factory
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.ExperimentInfo
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.InvitationInfo
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.research.OTExperimentDAO
import kr.ac.snu.hcil.omnitrack.core.di.configured.Research
import kr.ac.snu.hcil.omnitrack.core.research.ResearchManager
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
import javax.inject.Inject

/**
 * Created by younghokim on 2018. 1. 4..
 */
class ResearchViewModel(application: Application) : AndroidViewModel(application) {

    @Inject
    protected lateinit var researchManager: ResearchManager
    private lateinit var userId: String

    @field:[Inject Research]
    protected lateinit var realmFactory: Factory<Realm>

    private val realm: Realm

    val invitationLoadingStatus = BehaviorSubject.create<Boolean>()
    val invitationListSubject = BehaviorSubject.create<List<InvitationInfo>>()

    val experimentLoadingStatus = BehaviorSubject.create<Boolean>()
    val experimentListSubject = BehaviorSubject.create<List<ExperimentInfo>>()

    private val subscriptions = CompositeDisposable()

    init {
        getApplication<OTApp>().currentConfiguredContext.researchComponent.inject(this)
        realm = realmFactory.get()
    }

    fun initialize(userId: String) {
        if (!this::userId.isInitialized || this.userId != userId) {
            this.userId = userId
            subscriptions.add(
                    researchManager.updateExperimentsFromServer().doOnSubscribe {
                        experimentLoadingStatus.onNextIfDifferAndNotNull(true)
                    }.doOnComplete {
                        experimentLoadingStatus.onNextIfDifferAndNotNull(false)
                    }.subscribe())

            subscriptions.add(
                    realm.where(OTExperimentDAO::class.java).sort("joinedAt", Sort.DESCENDING).findAllAsync().asFlowable().filter { it.isLoaded && it.isValid }.subscribe { results ->
                        experimentListSubject.onNext(results.map { it.getInfo() })
                    }
            )
        }
    }

    fun insertInvitationCode(invitationCode: String) {
        subscriptions.add(
                researchManager.approveInvitation(invitationCode).subscribe { result ->
                    println("invitation approval success: ${result}")
                }
        )
    }

    fun withdrawFromExperiment(experimentId: String, reason: String?) {
        subscriptions.add(
                researchManager.dropoutFromExperiment(experimentId, reason).subscribe {
                    println("withdrew from the experiment.")
                }
        )
    }

    override fun onCleared() {
        subscriptions.clear()
        realm.close()
        super.onCleared()
    }

}