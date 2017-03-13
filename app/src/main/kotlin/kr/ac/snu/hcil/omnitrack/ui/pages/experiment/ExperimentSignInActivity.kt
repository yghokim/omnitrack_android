package kr.ac.snu.hcil.omnitrack.ui.pages.experiment

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPager
import android.widget.ImageButton
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 2017. 1. 27..
 */
class ExperimentSignInActivity : IntroActivity() {

    internal lateinit var buttonNext: ImageButton

    private lateinit var demographicSlide: FragmentSlide
    private lateinit var purposeSlide: FragmentSlide

    private var isComponentsCreated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buttonNext = findViewById(R.id.mi_button_next) as ImageButton
        buttonBackFunction = BUTTON_BACK_FUNCTION_BACK
        buttonNextFunction = BUTTON_NEXT_FUNCTION_NEXT
        isButtonBackVisible = false

        addSlide(
                FragmentSlide.Builder()
                        .fragment(ConsentFormSlideFragment())
                        .background(R.color.colorPointed_Light)
                        .backgroundDark(R.color.colorPointed)
                        .build()
        )

        demographicSlide = FragmentSlide.Builder()
                .fragment(DemographicInputFragment())
                .background(R.color.frontalBackground)
                .backgroundDark(R.color.colorSecondary)
                .canGoForward(false)
                .build()

        addSlide(demographicSlide)

        purposeSlide = FragmentSlide.Builder()
                .fragment(PurposeChoiceFragment())
                .background(R.color.frontalBackground)
                .backgroundDark(R.color.colorSecondary)
                .canGoForward(false)
                .build()

        addSlide(purposeSlide)

        addSlide(
                SimpleSlide.Builder()
                        .background(R.color.colorPrimary)
                        .backgroundDark(R.color.colorPrimaryDark)
                        //.image(R.drawable.icon)
                        .title(getString(R.string.msg_signup_complete))
                        .description(getString(R.string.msg_signup_complete_description))
                        .build()
        )

        addOnNavigationBlockedListener { position, direction ->

            val contentView = findViewById(android.R.id.content)
            if (contentView != null) {
                val slide = getSlide(position)
                if (slide === demographicSlide) {

                    Snackbar.make(contentView, R.string.msg_demographic_complete_form, Snackbar.LENGTH_LONG).show()
                } else if (slide === purposeSlide) {
                    Snackbar.make(contentView, getString(R.string.msg_choose_one_or_more), Snackbar.LENGTH_LONG).show()
                }
            }
        }

        addOnPageChangeListener(
                object : ViewPager.OnPageChangeListener {
                    override fun onPageScrollStateChanged(state: Int) {

                    }

                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

                    }

                    override fun onPageSelected(position: Int) {
                        println("page changed to ${position}")
                        val slide = getSlide(position)
                        if (slide === demographicSlide || slide === purposeSlide) {
                            if (slide.canGoForward()) {
                                buttonNext.alpha = 1f
                            } else {
                                buttonNext.alpha = 0.2f
                            }
                        } else {
                            buttonNext.alpha = 1f
                        }
                    }

                }
        )

        isComponentsCreated = true

    }

    fun setButtonNextBackgroundTint(color: Int) {
        ViewCompat.setBackgroundTintList(buttonNext, ColorStateList.valueOf(color));
    }

    fun setButtonNextBackgroundTintRes(@ColorRes colorRes: Int) {
        ViewCompat.setBackgroundTintList(buttonNext, ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
    }

    override fun onSendActivityResult(result: Int): Intent? {
        return Intent().apply {
            if (isComponentsCreated) {
                val demographicFragment = demographicSlide.fragment as? DemographicInputFragment
                if (demographicFragment != null) {
                    putExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_AGE_GROUP, demographicFragment.selectedAgeKey)
                    putExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_GENDER, demographicFragment.selectedGenderKey)
                    //putExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_OCCUPATION, fragment.selectedOccupationKey)
                    putExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_COUNTRY, demographicFragment.selectedCountryCode)
                }

                val purposeFragment = purposeSlide.fragment as? PurposeChoiceFragment
                if (purposeFragment != null) {
                    for (purpose in purposeFragment.selectedPurposes) {
                        println(purpose)
                    }
                    putExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_PURPOSES, ArrayList<String>(purposeFragment.selectedPurposes.toList()))
                }
            }
        }
    }
}