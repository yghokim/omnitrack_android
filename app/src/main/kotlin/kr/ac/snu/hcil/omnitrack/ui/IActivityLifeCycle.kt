package kr.ac.snu.hcil.omnitrack.ui

import android.os.Bundle

/**
 * Created by younghokim on 16. 8. 14..
 *
 * ViewHolder example
 * https://gist.github.com/alunsford3/5d7c1bb5a67b90b4e1f3
 *
 */
interface IActivityLifeCycle {
    fun onCreate(savedInstanceState: Bundle?)

    fun onSaveInstanceState(outState: Bundle?)

    fun onResume()

    fun onPause()

    fun onDestroy()

    fun onLowMemory()
}