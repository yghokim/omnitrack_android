package kr.ac.snu.hcil.omnitrack.ui.pages.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.di.global.ServerResponsive
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationServerSideAPI
import javax.inject.Inject
import javax.inject.Provider


typealias SignUpSlideListInfo = Pair<Array<SignUpActivity.ESlide>, Map<String, String?>?>

class SignUpViewModel(app: Application) : AndroidViewModel(app) {


    @field:[Inject ServerResponsive]
    protected lateinit var checkServerConnection: Provider<Completable>

    @Inject
    protected lateinit var authManager: OTAuthManager

    @Inject
    protected lateinit var serverApiController: ISynchronizationServerSideAPI

    init {
        (app as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.clear()
    }

    private val subscriptions = CompositeDisposable()

    var invitationCode: String? = null
    var demographicAnswers: JsonObject? = null
    var username: String? = null
    var password: String? = null

    private val slideListInfoSubject = BehaviorSubject.createDefault<SignUpSlideListInfo>(SignUpSlideListInfo(emptyArray(), null))

    private val onNextTrySubject = PublishSubject.create<SignUpActivity.ESlide>()

    private val onNextApprovedSubject = PublishSubject.create<SignUpActivity.ESlide>()

    val onNextTried: Subject<SignUpActivity.ESlide> get() = onNextTrySubject

    val onNextApproved: Subject<SignUpActivity.ESlide> get() = onNextApprovedSubject

    val slideListInfo: Subject<SignUpSlideListInfo> get() = slideListInfoSubject

    fun tryNext(currentSlide: SignUpActivity.ESlide) {
        onNextTrySubject.onNext(currentSlide)
    }

    fun goNext(currentSlide: SignUpActivity.ESlide) {
        onNextApprovedSubject.onNext(currentSlide)
    }

    fun initialize() {
        subscriptions.add(
                checkServerConnection.get().toSingle { true }.flatMap {
                    if (BuildConfig.DEFAULT_EXPERIMENT_ID == null) {
                        //master mode
                        Single.just(Pair(arrayOf(SignUpActivity.ESlide.CREDENTIAL_FORM), null))
                    } else {
                        //study mode
                        serverApiController.getExperimentConsentInfo(BuildConfig.DEFAULT_EXPERIMENT_ID).map {
                            if (it.receiveConsentInApp) {
                                Pair(arrayOf(SignUpActivity.ESlide.CONSENT_FORM, SignUpActivity.ESlide.CREDENTIAL_FORM, SignUpActivity.ESlide.DEMOGRAPHIC_QUESTIONNAIRE), mapOf(
                                        SignUpActivity.DEMOGRAPHIC_SCHEMA to it.demographicFormSchema,
                                        SignUpActivity.CONSENT_FORM to it.consent
                                ))
                            } else Pair(arrayOf(SignUpActivity.ESlide.CREDENTIAL_FORM), null)
                        }
                    }
                }.subscribe({ result ->
                    slideListInfoSubject.onNext(result)
                }, { err ->
                    err.printStackTrace()
                }))
    }

    fun tryRegister(): Completable {
        return authManager.register(username!!, password!!, invitationCode, demographicAnswers)
    }
}