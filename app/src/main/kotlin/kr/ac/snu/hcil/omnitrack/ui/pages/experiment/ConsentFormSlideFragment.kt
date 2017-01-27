package kr.ac.snu.hcil.omnitrack.ui.pages.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import kr.ac.snu.hcil.omnitrack.R
import us.feras.mdv.MarkdownView

/**
 * Created by Young-Ho on 1/26/2017.
 */
class ConsentFormSlideFragment : SlideFragment() {
    private lateinit var markdownView: MarkdownView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.slide_consent_form, container)
        return view
    }
}