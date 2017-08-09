package kr.ac.snu.hcil.omnitrack.ui.pages.experiment

import android.os.Bundle
import android.support.v7.widget.AppCompatButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import com.mukesh.countrypicker.fragments.CountryPicker
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.ExtendedSpinner
import kr.ac.snu.hcil.omnitrack.ui.components.common.choice.SelectionView

/**
 * Created by younghokim on 2017. 1. 27..
 */
class DemographicInputFragment : SlideFragment(), ExtendedSpinner.OnItemSelectedListener {

    val genderKeys: Array<String> by lazy {
        resources.getStringArray(R.array.gender_entry_keys)
    }

    val ageKeys: Array<String> by lazy {
        resources.getStringArray(R.array.age_entry_keys)
    }

    /*
    val occupationKeys: Array<String> by lazy {
        resources.getStringArray(R.array.occupation_entry_keys)
    }*/

    val selectedGenderKey: String? get() {
        return if (genderSelector.selectedIndex != -1)
            genderKeys[genderSelector.selectedIndex]
        else null
    }

    val selectedAgeKey: String? get() {
        return if (ageSpinner.selectedItemPosition != -1)
            ageKeys[ageSpinner.selectedItemPosition]
        else null
    }

    /*
    val selectedOccupationKey: String? get() {
        return if (occupationSpinner.selectedItemPosition != -1)
            occupationKeys[occupationSpinner.selectedItemPosition]
        else null
    }*/


    private lateinit var genderSelector: SelectionView
    private lateinit var ageSpinner: ExtendedSpinner
    //private lateinit var occupationSpinner: ExtendedSpinner
    private lateinit var countryButton: AppCompatButton

    private lateinit var countryPicker: CountryPicker

    private var selectedCountryName: String? = null

    var selectedCountryCode: String? = null
        private set

    private var selectedCountryFlagId: Int? = null

    private var isComplete: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.slide_demographic, container, false)

        genderSelector = view.findViewById(R.id.ui_gender_selector)
        genderSelector.setValues(R.array.gender_entries)

        ageSpinner = view.findViewById(R.id.ui_age_selector)
        ageSpinner.setItems(*resources.getStringArray(R.array.age_entries))
        ageSpinner.selectedItemPosition = -1

        /*
        occupationSpinner = view.findViewById(R.id.ui_occupation_selector) as ExtendedSpinner
        occupationSpinner.setItems(*resources.getStringArray(R.array.occupation_entries))
        occupationSpinner.selectedItemPosition = -1
*/

        countryButton = view.findViewById(R.id.ui_country_selector)

        countryPicker = CountryPicker.newInstance(getString(R.string.msg_select_country))
        countryPicker.setListener { name, code, dialCode, flagDrawableResID ->
            selectedCountryName = name
            selectedCountryCode = code
            selectedCountryFlagId = flagDrawableResID

            countryButton.text = name
            countryButton.setCompoundDrawablesRelativeWithIntrinsicBounds(flagDrawableResID, 0, 0, 0)

            countryPicker.dismiss()
        }

        val userCountry = countryPicker.getUserCountryInfo(context)
        selectedCountryCode = userCountry.code
        selectedCountryName = userCountry.name
        selectedCountryFlagId = userCountry.flag
        countryButton.text = selectedCountryName
        countryButton.setCompoundDrawablesRelativeWithIntrinsicBounds(userCountry.flag, 0, 0, 0)

        countryButton.setOnClickListener {
            countryPicker.show(fragmentManager, "COUNTRY_PICKER")
        }


        ageSpinner.onItemSelectedListener = this
        /*
        occupationSpinner.onItemSelectedListener = this
        */

        /*
        occupationChoiceInputView = view.findViewById(R.id.ui_occupation_selector) as ChoiceInputView
        occupationChoiceInputView.multiSelectionMode = false
        occupationChoiceInputView.entries = resources.getStringArray(R.array.occupation_entries).mapIndexed { i, s ->
            UniqueStringEntryList.Entry(i, s)
        }.toTypedArray()
*/


        return view
    }

    override fun canGoForward(): Boolean {
        return isComplete
    }

    override fun onItemSelected(spinner: ExtendedSpinner, position: Int) {
        checkInputsValid()
    }

    private fun checkInputsValid() {
        if (ageSpinner.selectedItem != null /*&& occupationSpinner.selectedItem != null*/ && selectedCountryCode != null) {
            println("Demographic information complete!")
            isComplete = true
            val activity = activity
            if (activity is ExperimentSignInActivity) {
                activity.buttonNextFunction = IntroActivity.BUTTON_NEXT_FUNCTION_NEXT_FINISH
                activity.buttonNext.alpha = 1f
            }
            updateNavigation()
        } else {
            isComplete = false
            updateNavigation()
        }
    }

}