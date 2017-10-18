package kr.ac.snu.hcil.omnitrack.core.dependency

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Single

/**
 * Created by younghokim on 2017. 5. 18..
 */
class PermissionDependencyResolver(vararg permissions: String) : OTSystemDependencyResolver() {

    private val permissionNames = ArrayList<String>()

    init {
        this.permissionNames.addAll(permissions)
    }

    override fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult> {
        return Single.just(
                if (
                permissionNames.map { permission -> ContextCompat.checkSelfPermission(context, permission) }
                        .filter { it != PackageManager.PERMISSION_GRANTED }
                        .isEmpty()) {
                    DependencyCheckResult(DependencyState.Passed, "Permissions are all granted", "")
                } else {
                    DependencyCheckResult(DependencyState.FatalFailed, "Need some additional permissions to be granted", "Request")
                }
        )
    }

    override fun tryResolve(activity: Activity): Single<Boolean> {
        return RxPermissions(activity)
                .request(*(this.permissionNames.toTypedArray()))
                .toList()
                .map { list -> list.filter { it == false }.isEmpty() }
    }


}