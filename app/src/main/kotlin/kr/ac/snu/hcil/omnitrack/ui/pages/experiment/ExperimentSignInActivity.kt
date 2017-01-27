package kr.ac.snu.hcil.omnitrack.ui.pages.experiment

import android.os.Bundle
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 2017. 1. 27..
 */
class ExperimentSignInActivity : IntroActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(
                FragmentSlide.Builder()
                        .fragment(ConsentFormSlideFragment())
                        .background(R.color.colorPointed_Light)
                        .backgroundDark(R.color.colorPointed)
                        .build()
        )

        addSlide(
                FragmentSlide.Builder()
                        .fragment(DemographicInputFragment())
                        .background(R.color.frontalBackground)
                        .backgroundDark(R.color.colorSecondary)
                        .build()
        )
    }
}