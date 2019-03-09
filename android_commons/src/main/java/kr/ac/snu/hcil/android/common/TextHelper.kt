package kr.ac.snu.hcil.android.common

import android.content.Context
import android.text.Html
import android.text.Spanned
import androidx.annotation.StringRes
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

    fun formatWithResources(context: Context, @StringRes formatResId: Int, vararg args: Any): CharSequence {
        return String.format(context.getString(formatResId), *(args.map {
            if (it is Int) {
                context.getString(it)
            } else {
                it.toString()
            }
        }.toTypedArray()))
    }

    fun fromHtml(source: CharSequence): Spanned {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(source.toString(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(source.toString())
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
            if (country.isNotEmpty() && !nameSet.contains(country)) {
                list.add(CountryInfo(country, loc.country))
                nameSet.add(country)
            }
        }

        list.sortWith(Comparator { a, b ->
            a.localizedName.compareTo(b.localizedName)
        })

        list.toTypedArray()
    }
}