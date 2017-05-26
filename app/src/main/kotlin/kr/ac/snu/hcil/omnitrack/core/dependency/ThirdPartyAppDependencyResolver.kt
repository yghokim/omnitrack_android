package kr.ac.snu.hcil.omnitrack.core.dependency

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.annotation.StringRes
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import rx.Single

/**
 * Created by younghokim on 2017. 5. 17..
 */
class ThirdPartyAppDependencyResolver(val packageName: String, val appName: String, val mandatory: Boolean) : OTSystemDependencyResolver() {

    class Builder(private val context: Context) {
        private var packageName: String? = null
        private var appName: String? = null
        private var mandatory: Boolean = false


        private var fatalFailedMessage: CharSequence? = null
        private var nonFatalFailedMessage: CharSequence? = null
        private var passedMessage: CharSequence? = null

        private var playStoreTossMessage: CharSequence? = null

        fun setPackageName(packageName: String): Builder {
            this.packageName = packageName
            return this
        }

        fun setAppName(appName: String): Builder {
            this.appName = appName
            return this
        }

        fun setAppName(@StringRes appNameRes: Int): Builder {
            this.appName = context.getString(appNameRes)
            return this
        }

        fun isMandatory(isMandatory: Boolean): Builder {
            this.mandatory = isMandatory
            return this
        }

        fun fatalFailedMessage(message: CharSequence): Builder {
            this.fatalFailedMessage = message
            return this
        }

        fun nonFatalFailedMessage(message: CharSequence): Builder {
            this.nonFatalFailedMessage = message
            return this
        }

        fun passedMessage(message: CharSequence): Builder {
            this.passedMessage = message
            return this
        }

        fun playStoreTossMessage(message: CharSequence): Builder {
            this.playStoreTossMessage = message
            return this
        }

        fun build(): ThirdPartyAppDependencyResolver {
            return ThirdPartyAppDependencyResolver(packageName ?: "PackageNameNull", appName ?: "AppNameNull", mandatory)
                    .apply {
                        this.passedMessage = this@Builder.passedMessage ?: TextHelper.fromHtml(TextHelper.formatWithResources(context, R.string.msg_format_third_party_app_dependency_passed_message, appName))
                        this.nonFatalFailedMessage = this@Builder.nonFatalFailedMessage ?: TextHelper.fromHtml(TextHelper.formatWithResources(context, R.string.msg_format_third_party_app_dependency_non_fatal_failed_message, appName))
                        this.fatalFailedMessage = this@Builder.fatalFailedMessage ?: TextHelper.fromHtml(TextHelper.formatWithResources(context, R.string.msg_format_third_party_app_dependency_fatal_failed_message, appName))
                        this.playStoreTossMessage = this@Builder.playStoreTossMessage ?: context.getString(R.string.msg_go_to_store)
                    }
        }
    }

    private var fatalFailedMessage: CharSequence? = null
    private var nonFatalFailedMessage: CharSequence? = null
    private var passedMessage: CharSequence? = null

    private var playStoreTossMessage: CharSequence? = null

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
                DependencyState.Passed -> DependencyCheckResult(state, passedMessage ?: "", playStoreTossMessage ?: context.getString(R.string.msg_go_to_store))
                DependencyState.FatalFailed -> DependencyCheckResult(state, fatalFailedMessage ?: "", playStoreTossMessage ?: context.getString(R.string.msg_go_to_store))
                DependencyState.NonFatalFailed -> DependencyCheckResult(state, nonFatalFailedMessage ?: "", playStoreTossMessage ?: context.getString(R.string.msg_go_to_store))
            }
        }
    }

    override fun tryResolve(activity: Activity): Single<Boolean> {
        return Single.defer {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${packageName}")))
            } catch(ex: ActivityNotFoundException) {
                ex.printStackTrace()
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")))
            } finally {

            }

            Single.just(false)
        }
    }


}