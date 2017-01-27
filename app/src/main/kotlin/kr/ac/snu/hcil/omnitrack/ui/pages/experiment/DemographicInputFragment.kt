package kr.ac.snu.hcil.omnitrack.ui.pages.experiment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.SelectionView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ChoiceInputView
import kr.ac.snu.hcil.omnitrack.utils.UniqueStringEntryList

/**
 * Created by younghokim on 2017. 1. 27..
 */
class DemographicInputFragment : SlideFragment() {


    private lateinit var genderSelector: SelectionView
    private lateinit var occupationChoiceInputView: ChoiceInputView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.slide_demographic, container, false)

        genderSelector = view.findViewById(R.id.ui_gender_selector) as SelectionView
        genderSelector.setValues(R.array.gender_entries)

        occupationChoiceInputView = view.findViewById(R.id.ui_occupation_selector) as ChoiceInputView
        occupationChoiceInputView.multiSelectionMode = false
        occupationChoiceInputView.entries = resources.getStringArray(R.array.occupation_entries).mapIndexed { i, s ->
            UniqueStringEntryList.Entry(i, s)
        }.toTypedArray()



        return view
    }
}