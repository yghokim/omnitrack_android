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

        addSlide(
                SimpleSlide.Builder()
                        .background(R.color.colorPrimary)
                        .backgroundDark(R.color.colorPrimaryDark)
                        .image(R.drawable.abc_btn_check_material)
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
                        if (slide === demographicSlide) {
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

    }

    fun setButtonNextBackgroundTint(color: Int) {
        ViewCompat.setBackgroundTintList(buttonNext, ColorStateList.valueOf(color));
    }

    fun setButtonNextBackgroundTintRes(@ColorRes colorRes: Int) {
        ViewCompat.setBackgroundTintList(buttonNext, ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
    }

    override fun onSendActivityResult(result: Int): Intent? {
        return Intent().apply {
            val fragment = demographicSlide.fragment as? DemographicInputFragment
            if (fragment != null) {
                putExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_AGE_GROUP, fragment.selectedAgeKey)
                putExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_GENDER, fragment.selectedGenderKey)
                putExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_OCCUPATION, fragment.selectedOccupationKey)
                putExtra(OTApplication.ACCOUNT_DATASET_EXPERIMENT_KEY_COUNTRY, fragment.selectedCountryInfo?.key)
            }
        }
    }
}