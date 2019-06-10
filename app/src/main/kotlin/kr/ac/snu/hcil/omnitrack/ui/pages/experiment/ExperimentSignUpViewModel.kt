package kr.ac.snu.hcil.omnitrack.ui.pages.experiment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.gson.JsonObject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class ExperimentSignUpViewModel(app: Application) : AndroidViewModel(app) {

    var verifiedInvitationCode: String? = null
    var demographicAnswers: JsonObject? = null

    private val onNextTrySubject = PublishSubject.create<ExperimentSignUpActivity.ESlide>()

    private val onNextApprovedSubject = PublishSubject.create<ExperimentSignUpActivity.ESlide>()

    val onNextTried: Subject<ExperimentSignUpActivity.ESlide> get() = onNextTrySubject

    val onNextApproved: Subject<ExperimentSignUpActivity.ESlide> get() = onNextApprovedSubject

    fun tryNext(currentSlide: ExperimentSignUpActivity.ESlide) {
        onNextTrySubject.onNext(currentSlide)
    }

    fun goNext(currentSlide: ExperimentSignUpActivity.ESlide) {
        onNextApprovedSubject.onNext(currentSlide)
    }
}