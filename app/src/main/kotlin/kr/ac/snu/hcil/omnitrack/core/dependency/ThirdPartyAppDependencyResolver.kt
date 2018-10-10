package kr.ac.snu.hcil.omnitrack.core.dependency

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.annotation.StringRes
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.TextHelper

/**
 * Created by younghokim on 2017. 5. 17..
 */

typealias TextEmitter = (context: Context) -> CharSequence
class ThirdPartyAppDependencyResolver(val packageName: String, val appName: TextEmitter, val mandatory: Boolean) : OTSystemDependencyResolver() {


    class Builder(private val context: Context) {
        private var packageName: String? = null
        private var appName: TextEmitter? = null
        private var mandatory: Boolean = false


        private var fatalFailedMessage: TextEmitter? = null
        private var nonFatalFailedMessage: TextEmitter? = null
        private var passedMessage: TextEmitter? = null

        private var playStoreTossMessage: TextEmitter? = null

        fun setPackageName(packageName: String): Builder {
            this.packageName = packageName
            return this
        }

        fun setAppName(appName: String): Builder {
            this.appName = { appName }

            return this
        }

        fun setAppName(@StringRes appNameRes: Int): Builder {
            this.appName = { context.getString(appNameRes) }
            return this
        }

        fun isMandatory(isMandatory: Boolean): Builder {
            this.mandatory = isMandatory
            return this
        }

        fun fatalFailedMessage(message: TextEmitter): Builder {
            this.fatalFailedMessage = message
            return this
        }

        fun nonFatalFailedMessage(message: TextEmitter): Builder {
            this.nonFatalFailedMessage = message
            return this
        }

        fun passedMessage(message: TextEmitter): Builder {
            this.passedMessage = message
            return this
        }

        fun playStoreTossMessage(message: TextEmitter): Builder {
            this.playStoreTossMessage = message
            return this
        }

        fun build(): ThirdPartyAppDependencyResolver {
            return ThirdPartyAppDependencyResolver(packageName ?: "PackageNameNull", appName ?: { "AppNameNull" }, mandatory)
                    .apply {
                        this.passedMessage = this@Builder.passedMessage
                        this.nonFatalFailedMessage = this@Builder.nonFatalFailedMessage
                        this.fatalFailedMessage = this@Builder.fatalFailedMessage
                        this.playStoreTossMessage = this@Builder.playStoreTossMessage
                    }
        }
    }

    private var fatalFailedMessage: TextEmitter? = null
    private var nonFatalFailedMessage: TextEmitter? = null
    private var passedMessage: TextEmitter? = null

    private var playStoreTossMessage: TextEmitter? = null

    override fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult> {
        return Single.defer {
            val pm = context.packageManager
            try {
                pm.getPackageInfo(packageName, 0)
                return@defer Single.just(DependencyState.Passed)
            } catch(e: PackageManager.NameNotFoundException) {
                return@defer if (mandatory) Single.just(DependencyState.FatalFailed) else Single.just(DependencyState.NonFatalFailed)
            }
        }.map { state ->
            when (state) {
                DependencyState.Passed -> DependencyCheckResult(state, passedMessage?.invoke(context) ?: TextHelper.fromHtml(TextHelper.formatWithResources(context, R.string.msg_format_third_party_app_dependency_passed_message, appName.invoke(context))), playStoreTossMessage?.invoke(context) ?: context.getString(R.string.msg_go_to_store))
                DependencyState.FatalFailed -> DependencyCheckResult(state, fatalFailedMessage?.invoke(context) ?: TextHelper.fromHtml(TextHelper.formatWithResources(context, R.string.msg_format_third_party_app_dependency_non_fatal_failed_message, appName.invoke(context))), playStoreTossMessage?.invoke(context) ?: context.getString(R.string.msg_go_to_store))
                DependencyState.NonFatalFailed -> DependencyCheckResult(state, nonFatalFailedMessage?.invoke(context) ?: TextHelper.fromHtml(TextHelper.formatWithResources(context, R.string.msg_format_third_party_app_dependency_fatal_failed_message, appName.invoke(context))), playStoreTossMessage?.invoke(context) ?: context.getString(R.string.msg_go_to_store))
            }
        }
    }

    override fun tryResolve(activity: Activity): Single<Boolean> {
        return Single.defer {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            } catch(ex: ActivityNotFoundException) {
                ex.printStackTrace()
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            } finally {

            }

            Single.just(false)
        }
    }


}