package kr.ac.snu.hcil.omnitrack.ui.pages.research

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import dagger.internal.Factory
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.models.research.ExperimentInfo
import kr.ac.snu.hcil.omnitrack.core.database.models.research.OTExperimentDAO
import kr.ac.snu.hcil.omnitrack.core.di.global.Research
import kr.ac.snu.hcil.omnitrack.core.net.ExperimentInvitation
import kr.ac.snu.hcil.omnitrack.core.research.ResearchManager
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
    val invitationListSubject = BehaviorSubject.create<List<ExperimentInvitation>>()

    val experimentLoadingStatus = BehaviorSubject.create<Boolean>()
    val experimentListSubject = BehaviorSubject.create<List<ExperimentInfo>>()

    private val networkSubscription = SerialDisposable()

    private val subscriptions = CompositeDisposable()

    init {
        getApplication<OTApp>().applicationComponent.inject(this)
        realm = realmFactory.get()
    }

    fun initialize(userId: String) {
        if (!this::userId.isInitialized || this.userId != userId) {
            this.userId = userId


            subscriptions.add(
                    realm.where(OTExperimentDAO::class.java).sort("joinedAt", Sort.DESCENDING).findAllAsync().asFlowable().filter { it.isLoaded && it.isValid }.subscribe { results ->
                        experimentListSubject.onNext(results.map { it.getInfo() })
                    }
            )

            startWatchingNetworkForRefresh()
        }
    }

    fun startWatchingNetworkForRefresh() {
        this.networkSubscription.set(
                ReactiveNetwork.observeInternetConnectivity().subscribe { connectivity ->
                    println("connectivity changed : " + connectivity)
                    if (connectivity == true) {
                        reload()
                    }
                }
        )
    }

    fun stopWatchingNetworkForRefresh() {
        this.networkSubscription.set(null)
    }

    fun reload() {
        subscriptions.add(
                researchManager.updateExperimentsFromServer().doOnSubscribe {
                    experimentLoadingStatus.onNextIfDifferAndNotNull(true)
                }.doOnComplete {
                            experimentLoadingStatus.onNextIfDifferAndNotNull(false)
                        }.subscribe())

        subscriptions.add(
                researchManager.loadPublicInvitations().doOnSubscribe {
                    invitationLoadingStatus.onNextIfDifferAndNotNull(true)
                }.doOnEvent { ev, err ->
                            invitationLoadingStatus.onNextIfDifferAndNotNull(false)
                        }.map { invitation ->
                            invitation.filter { it.participants.isEmpty() || it.participants.find { participant -> participant.dropped == true } != null }
                        }.subscribe({ invitations ->
                            invitationListSubject.onNextIfDifferAndNotNull(invitations)
                        }, { ex ->
                            ex.printStackTrace()
                        })
        )
    }

    fun withdrawFromExperiment(experimentId: String, reason: String?) {
        subscriptions.add(
                researchManager.dropoutFromExperiment(experimentId, reason).doOnSubscribe {
                    this.experimentLoadingStatus.onNextIfDifferAndNotNull(true)
                }.doOnComplete {
                            this.experimentLoadingStatus.onNextIfDifferAndNotNull(false)
                        }.subscribe {
                    println("withdrew from the experiment.")
                    reload()
                }
        )
    }

    override fun onCleared() {
        subscriptions.clear()
        realm.close()
        super.onCleared()
    }
}