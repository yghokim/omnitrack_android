package kr.ac.snu.hcil.omnitrack.core.dependency

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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
                if (permissionNames.asSequence().map { permission -> ContextCompat.checkSelfPermission(context, permission) }.none { it != PackageManager.PERMISSION_GRANTED }) {
                    DependencyCheckResult(DependencyState.Passed, "Permissions are all granted", "")
                } else {
                    DependencyCheckResult(DependencyState.FatalFailed, "Need some additional permissions to be granted", "Request")
                }
        )
    }

    override fun tryResolve(activity: FragmentActivity): Single<Boolean> {
        return tryResolveImpl(RxPermissions(activity))
    }

    override fun tryResolve(fragment: Fragment): Single<Boolean> {
        return tryResolveImpl(RxPermissions(fragment))
    }

    private fun tryResolveImpl(perm: RxPermissions): Single<Boolean> {
        return perm.request(*(this.permissionNames.toTypedArray()))
                .toList()
                .map { list -> list.none { it == false } }
    }


}