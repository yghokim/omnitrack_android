package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.os.Bundle
import android.support.v4.app.DialogFragment
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.disposables.SerialDisposable

/**
 * Created by younghokim on 2017. 4. 14..
 * Inspired by https://github.com/ogaclejapan/Qiitanium/blob/master/qiitanium/src/main/java/com/ogaclejapan/qiitanium/presentation/fragment/AppDialogFragment.java
 */
abstract class RxBoundDialogFragment : DialogFragment() {

    private val subscription = SerialDisposable()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscription.set(onBind(savedInstanceState))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onUnbind()
    }

    protected abstract fun onBind(savedInstanceState: Bundle?): Disposable

    protected open fun onUnbind() {
        subscription.set(Disposables.empty())
    }
}