package kr.ac.snu.hcil.omnitrack.ui.pages.experiment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_experiment_signup.*
import kr.ac.snu.hcil.omnitrack.R

class ExperimentSignUpActivity : AppCompatActivity(R.layout.activity_experiment_signup) {
    enum class ESlide {
        INVITATION_CODE_PROMPT,
        CONSENT_FORM,
        DEMOGRAPHIC_QUESTIONNAIRE
    }

    companion object {
        const val INVITATION_CODE = "invitation_code"
        const val ASK_INVITATION_CODE = "ask_invitation_code"
        const val CONSENT_FORM = "consent_form"
        const val DEMOGRAPHIC_SCHEMA = "demographic_schema"
        const val DEMOGRAPHIC_DATA = "demographic_data"

        fun makeIntent(context: Context, askInvitationCode: Boolean, consentForm: String?, demographicSchema: String?): Intent {
            return Intent(context, ExperimentSignUpActivity::class.java).apply {
                putExtra(ASK_INVITATION_CODE, askInvitationCode)
                if (consentForm != null) {
                    putExtra(CONSENT_FORM, consentForm)
                }

                if (demographicSchema != null) {
                    putExtra(DEMOGRAPHIC_SCHEMA, demographicSchema.toString())
                }
            }
        }
    }

    private lateinit var viewModel: ExperimentSignUpViewModel

    private val adapter = PagerAdapter(supportFragmentManager)

    private lateinit var slides: Array<ESlide>
    private lateinit var consentFormMarkdown: String
    private lateinit var demographicSchema: String

    private val creationSubscriptions = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val slideList = ArrayList<ESlide>()

        if (intent.getBooleanExtra(ASK_INVITATION_CODE, false)) {
            slideList.add(ESlide.INVITATION_CODE_PROMPT)
        }

        if (intent.hasExtra(CONSENT_FORM)) {
            slideList.add(ESlide.CONSENT_FORM)
            consentFormMarkdown = intent.getStringExtra(CONSENT_FORM)
            /*
            consentFormSlide = FragmentSlide.Builder()
                    .fragment(ConsentFormSlideFragment.getInstanceFromString(intent.getStringExtra(CONSENT_FORM)))
                    .background(R.color.colorPointed_Light)
                    .backgroundDark(R.color.colorPointed)
                    .build()*/
        }

        if (intent.hasExtra(DEMOGRAPHIC_SCHEMA)) {
            slideList.add(ESlide.DEMOGRAPHIC_QUESTIONNAIRE)
            demographicSchema = intent.getStringExtra(DEMOGRAPHIC_SCHEMA)
        }

        slides = slideList.toTypedArray()

        main_viewpager.adapter = adapter

        viewModel = ViewModelProviders.of(this).get(ExperimentSignUpViewModel::class.java)

        ui_fab.setOnClickListener {
            viewModel.tryNext(slides[main_viewpager.currentItem])
        }

        creationSubscriptions.add(
                viewModel.onNextApproved.subscribe { prevSlide ->
                    val prevSlideIndex = slides.indexOf(prevSlide)
                    if (slides.size - 1 == prevSlideIndex) {
                        //finish
                        setResult(Activity.RESULT_OK, onSendActivityResult())
                        finish()
                    } else {
                        main_viewpager.currentItem = prevSlideIndex + 1
                    }
                }
        )

        setResult(Activity.RESULT_CANCELED)
    }

    override fun onDestroy() {
        super.onDestroy()
        creationSubscriptions.clear()
    }

    fun onSendActivityResult(): Intent {
        return Intent().apply {
            putExtra(INVITATION_CODE, viewModel.verifiedInvitationCode)
            putExtra(DEMOGRAPHIC_SCHEMA, viewModel.demographicAnswers?.toString())
        }
    }

    inner class PagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return when (slides[position]) {
                ESlide.CONSENT_FORM -> ConsentFormSlideFragment.getInstanceFromString(consentFormMarkdown)
                ESlide.INVITATION_CODE_PROMPT -> InvitationCodePromptSlideFragment()
                ESlide.DEMOGRAPHIC_QUESTIONNAIRE -> DemographicQuestionnaireSlideFragment.getInstance(demographicSchema)
                else -> throw Exception("Unsupported slide type")
            }
        }

        override fun getCount(): Int {
            return slides.size
        }

    }

    abstract class SlideFragment(val slide: ESlide) : Fragment() {

        protected lateinit var viewModel: ExperimentSignUpViewModel

        private val attachSubscription = CompositeDisposable()

        override fun onAttach(context: Context) {
            super.onAttach(context)
            viewModel = ViewModelProviders.of(requireActivity()).get(ExperimentSignUpViewModel::class.java)
            attachSubscription.add(
                    viewModel.onNextTried.filter { it == slide }.subscribe {
                        onNextTried()
                    }
            )
        }

        protected abstract fun onNextTried()

        protected fun goNext() {
            this.viewModel.goNext(slide)
        }

        override fun onDetach() {
            super.onDetach()
            attachSubscription.clear()

        }
    }
}