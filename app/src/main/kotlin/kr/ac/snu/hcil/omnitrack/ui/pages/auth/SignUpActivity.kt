package kr.ac.snu.hcil.omnitrack.ui.pages.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_sign_up.*
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.di.global.ForGeneric
import kr.ac.snu.hcil.omnitrack.core.net.ServerError
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import retrofit2.HttpException
import javax.inject.Inject

class SignUpActivity : AppCompatActivity(R.layout.activity_sign_up) {
    enum class ESlide(@StringRes val nextStepLabel: Int = R.string.msg_auth_next_step) {
        CONSENT_FORM(R.string.msg_auth_approve_and_continue),
        CREDENTIAL_FORM,
        DEMOGRAPHIC_QUESTIONNAIRE
    }

    companion object {
        const val INVITATION_CODE = "invitation_code"
        const val CONSENT_FORM = "consent_form"
        const val DEMOGRAPHIC_SCHEMA = "demographic_schema"

        fun makeIntent(context: Context): Intent {
            return Intent(context, SignUpActivity::class.java)
        }
    }

    @field:[Inject ForGeneric]
    protected lateinit var gson: Gson

    private lateinit var viewModel: SignUpViewModel

    private val adapter = PagerAdapter(supportFragmentManager)

    private val slideList = ArrayList<ESlide>()
    private lateinit var consentFormMarkdown: String
    private lateinit var demographicSchema: String

    private val creationSubscriptions = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as OTAndroidApp).applicationComponent.inject(this)

        ui_button_next.isEnabled = false
        ui_button_next.alpha = 0.2f

        main_viewpager.adapter = adapter

        viewModel = ViewModelProviders.of(this).get(SignUpViewModel::class.java)

        if (savedInstanceState == null) {
            //start
            viewModel.initialize()
        }

        setBusyMode(true)
        this.creationSubscriptions.add(
                viewModel.slideListInfo.observeOn(AndroidSchedulers.mainThread()).subscribe { (slides, data) ->
                    ui_button_next.isEnabled = true
                    ui_button_next.alpha = 1f

                    if (slides.contains(ESlide.DEMOGRAPHIC_QUESTIONNAIRE)) {
                        demographicSchema = data!![DEMOGRAPHIC_SCHEMA]!!
                    }

                    if (slides.contains(ESlide.CONSENT_FORM)) {
                        consentFormMarkdown = data!![CONSENT_FORM]!!
                    }

                    this.slideList.clear()
                    this.slideList.addAll(slides)
                    this.adapter.notifyDataSetChanged()
                    main_viewpager.setCurrentItem(0, false)
                    updateIndicators()

                    setBusyMode(false)
                }
        )

        ui_button_previous.setOnClickListener {
            if (main_viewpager.currentItem == 0) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            } else {
                main_viewpager.currentItem--
                updateIndicators()
            }
        }

        ui_button_next.setOnClickListener {
            viewModel.tryNext(slideList[main_viewpager.currentItem])
            updateIndicators()
        }

        creationSubscriptions.add(
                viewModel.onNextApproved.subscribe { prevSlide ->
                    val prevSlideIndex = slideList.indexOf(prevSlide)
                    if (slideList.size - 1 == prevSlideIndex) {
                        //finish
                        creationSubscriptions.add(
                                viewModel.tryRegister().observeOn(AndroidSchedulers.mainThread()).subscribe({
                                    startActivity(Intent(this@SignUpActivity, HomeActivity::class.java)
                                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            .putExtra(OTApp.INTENT_EXTRA_IGNORE_SIGN_IN_CHECK, true)
                                            .putExtra(HomeActivity.INTENT_EXTRA_INITIAL_LOGIN, true)
                                    )
                                    // finish should always be called on the main thread.
                                    finish()
                                }, { err ->
                                    if (err is HttpException) {
                                        val errorCode = ServerError.extractServerErrorCode(gson, err)
                                        when (errorCode) {
                                            ServerError.ERROR_CODE_USERNAME_NOT_MATCH_RESEARCHER -> {
                                                Toast.makeText(this, "Username must match one of the researchers' E-mails.", Toast.LENGTH_LONG).show()
                                                main_viewpager.setCurrentItem(ESlide.values().indexOf(ESlide.CREDENTIAL_FORM), true)
                                            }
                                            ServerError.ERROR_CODE_ILLEGAL_INVITATION_CODE -> {
                                                Toast.makeText(this, "You entered a wrong invitation code. Please re-check.", Toast.LENGTH_LONG).show()
                                                main_viewpager.setCurrentItem(ESlide.values().indexOf(ESlide.CREDENTIAL_FORM), true)
                                            }
                                            ServerError.ERROR_CODE_USER_ALREADY_EXISTS -> {
                                                Toast.makeText(this, "You can't use this username. Please try another one.", Toast.LENGTH_LONG).show()
                                                main_viewpager.setCurrentItem(ESlide.values().indexOf(ESlide.CREDENTIAL_FORM), true)
                                            }
                                        }
                                    } else {
                                        err.printStackTrace()
                                    }
                                })
                        )
                    } else {
                        main_viewpager.currentItem = prevSlideIndex + 1
                    }
                }
        )
    }

    private fun updateIndicators() {
        if (slideList.size > 0) {
            ui_progress_indicator.text = "${main_viewpager.currentItem + 1}/${slideList.size}"
            ui_button_next.setText(if (main_viewpager.currentItem == slideList.size - 1) {
                R.string.msg_auth_sign_up_complete
            } else {
                slideList[main_viewpager.currentItem].nextStepLabel
            })
        }
    }

    private fun setBusyMode(isBusy: Boolean) {
        if (isBusy) {
            ui_loading_indicator.visibility = View.VISIBLE
            ui_main.visibility = View.INVISIBLE
        } else {
            ui_loading_indicator.visibility = View.GONE
            ui_main.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        creationSubscriptions.clear()
    }

    inner class PagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return when (slideList[position]) {
                ESlide.CONSENT_FORM -> ConsentFormSlideFragment.getInstanceFromString(consentFormMarkdown)
                ESlide.CREDENTIAL_FORM -> SignUpCredentialSlideFragment()
                ESlide.DEMOGRAPHIC_QUESTIONNAIRE -> DemographicQuestionnaireSlideFragment.getInstance(demographicSchema)
            }
        }

        override fun getCount(): Int {
            return slideList.size
        }

    }

    abstract class SlideFragment(val slide: ESlide) : Fragment() {

        protected lateinit var viewModel: SignUpViewModel

        private val attachSubscription = CompositeDisposable()

        override fun onAttach(context: Context) {
            super.onAttach(context)
            viewModel = ViewModelProviders.of(requireActivity()).get(SignUpViewModel::class.java)
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