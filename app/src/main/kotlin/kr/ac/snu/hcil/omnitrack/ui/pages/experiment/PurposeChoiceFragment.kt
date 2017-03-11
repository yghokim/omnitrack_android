package kr.ac.snu.hcil.omnitrack.ui.pages.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.ChoiceFormView

/**
 * Created by younghokim on 2017. 3. 11..
 */
class PurposeChoiceFragment : SlideFragment() {

    lateinit var purposeChoiceView: ChoiceFormView

    val selectedPurposes: Array<String> get() = purposeChoiceView.selectedEntries.map {
        if (it.isCustom) {
            "other: ${it.text}"
        } else {
            it.id
        }
    }.toTypedArray()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.slide_purposes, container, false)

        purposeChoiceView = view.findViewById(R.id.ui_purpose_selection) as ChoiceFormView
        purposeChoiceView.allowMultipleSelection = true


        val entries = resources.getStringArray(R.array.purpose_entries).map { it.split("|") }.map { ChoiceFormView.Entry(it[0], it[1]) }.toMutableList()
        entries.add(ChoiceFormView.Entry("other", "", true))

        purposeChoiceView.entries = entries.toTypedArray()

        purposeChoiceView.valueChanged += {
            sender, args ->
            updateNavigation()
        }

        return view
    }

    override fun canGoForward(): Boolean {
        println("selection is ${purposeChoiceView.isSelectionEmpty}")
        return !purposeChoiceView.isSelectionEmpty
    }

    override fun updateNavigation() {

        val activity = activity
        if (activity is ExperimentSignInActivity) {
            if (canGoForward()) {
                activity.buttonNextFunction = IntroActivity.BUTTON_NEXT_FUNCTION_NEXT_FINISH
                activity.buttonNext.alpha = 1f
            } else {

                activity.buttonNext.alpha = 0.2f
            }
        }

        super.updateNavigation()
    }

}