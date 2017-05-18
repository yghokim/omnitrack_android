package kr.ac.snu.hcil.omnitrack.core.dependency

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.annotation.StringRes
import rx.Single

/**
 * Created by younghokim on 2017. 5. 17..
 */
class ThirdPartyAppDependencyResolver(val packageName: String, val appName: String, val mandatory: Boolean) : OTSystemDependencyResolver() {

    class Builder(private val context: Context) {
        private var packageName: String? = null
        private var appName: String? = null
        private var mandatory: Boolean = false


        private var fatalFailedMessage: String? = null
        private var nonFatalFailedMessage: String? = null
        private var passedMessage: String? = null

        private var playStoreTossMessage: String? = null

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

        fun fatalFailedMessage(message: String): Builder {
            this.fatalFailedMessage = message
            return this
        }

        fun nonFatalFailedMessage(message: String): Builder {
            this.nonFatalFailedMessage = message
            return this
        }

        fun passedMessage(message: String): Builder {
            this.passedMessage = message
            return this
        }

        fun playStoreTossMessage(message: String): Builder {
            this.playStoreTossMessage = message
            return this
        }

        fun build(): ThirdPartyAppDependencyResolver {
            return ThirdPartyAppDependencyResolver(packageName ?: "PackageNameNull", appName ?: "AppNameNull", mandatory)
        }
    }

    private var fatalFailedMessage: String? = null
    private var nonFatalFailedMessage: String? = null
    private var passedMessage: String? = null

    private var playStoreTossMessage: String? = null

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
                DependencyState.Passed -> DependencyCheckResult(state, passedMessage ?: "", "")
                DependencyState.FatalFailed -> DependencyCheckResult(state, fatalFailedMessage ?: "", "")
                DependencyState.NonFatalFailed -> DependencyCheckResult(state, nonFatalFailedMessage ?: "", "")
            }
        }
    }

    override fun tryResolve(activity: Activity): Single<Boolean> {
        return Single.defer {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=kr.ac.snu.hcil.omnitrack")))
            } catch(ex: ActivityNotFoundException) {
                ex.printStackTrace()
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=kr.ac.snu.hcil.omnitrack")))
            } finally {

            }

            Single.just(false)
        }
    }


}