package kr.ac.snu.hcil.omnitrack.ui.pages.experiment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.slide_invitation_code_prompt.view.*
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.net.OTOfficialServerApiController
import javax.inject.Inject

class InvitationCodePromptSlideFragment : ExperimentSignUpActivity.SlideFragment(ExperimentSignUpActivity.ESlide.INVITATION_CODE_PROMPT) {

    val insertedInvitationCode: String?
        get() = view?.ui_invitation_code_input?.text?.toString()?.trim()

    @Inject
    protected lateinit var serverApiController: OTOfficialServerApiController

    private val createViewSubscriptions = CompositeDisposable()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun onNextTried() {
        val code = view?.ui_invitation_code_input?.text?.toString()?.trim() ?: ""
        createViewSubscriptions.add(
                serverApiController.verifyInvitationCode(code, BuildConfig.DEFAULT_EXPERIMENT_ID)
                        .subscribe({ verified ->
                            if (verified) {
                                viewModel.verifiedInvitationCode = code
                                goNext()

                                Toast.makeText(context, "Verified your access code.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Access code is not verified.", Toast.LENGTH_SHORT).show()
                            }
                        }, { ex ->
                            ex.printStackTrace()
                        })
        )
    }


    override fun onDestroyView() {
        super.onDestroyView()
        createViewSubscriptions.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.slide_invitation_code_prompt, container, false)

        view.ui_invitation_code_input.requestFocus()
        view.ui_invitation_code_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        return view
    }
}