package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.os.Bundle
import android.support.v4.app.DialogFragment
import rx.Subscription
import rx.subscriptions.SerialSubscription
import rx.subscriptions.Subscriptions

/**
 * Created by younghokim on 2017. 4. 14..
 * Inspired by https://github.com/ogaclejapan/Qiitanium/blob/master/qiitanium/src/main/java/com/ogaclejapan/qiitanium/presentation/fragment/AppDialogFragment.java
 */
abstract class RxBoundDialogFragment : DialogFragment() {

    private val subscription = SerialSubscription()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscription.set(onBind(savedInstanceState))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        onUnbind()
    }

    protected abstract fun onBind(savedInstanceState: Bundle?): Subscription

    protected open fun onUnbind() {
        subscription.set(Subscriptions.empty())
    }
}