package kr.ac.snu.hcil.omnitrack.utils

import android.text.Html
import android.text.Spanned
import java.util.*

/**
 * Created by younghokim on 16. 8. 16..
 */
object TextHelper {


    fun stringWithFallback(value: CharSequence?, nullFallback: CharSequence, blankFallback: CharSequence): CharSequence {
        return if (value != null) {
            if (value.isEmpty()) {
                blankFallback
            } else value
        } else {
            nullFallback
        }
    }

    fun stringWithFallback(value: CharSequence?, nullOrBlankFallback: CharSequence): CharSequence {
        return stringWithFallback(value, nullOrBlankFallback, nullOrBlankFallback)
    }

    fun fromHtml(source: String): Spanned {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(source)
        }
    }

    data class CountryInfo(val localizedName: String, val key: String) {
        override fun toString(): String {
            return localizedName
        }
    }


    val countryNames: Array<CountryInfo> by lazy {
        val locales = Locale.getAvailableLocales()
        val list = ArrayList<CountryInfo>()
        val nameSet = HashSet<String>()
        for (loc in locales) {
            val country = loc.displayCountry
            if (country.length > 0 && !nameSet.contains(country)) {
                list.add(CountryInfo(country, loc.country))
                nameSet.add(country)
            }
        }

        list.sortWith(Comparator<kr.ac.snu.hcil.omnitrack.utils.TextHelper.CountryInfo> { a, b ->
            a.localizedName.compareTo(b.localizedName)
        })

        list.toTypedArray()
    }
}