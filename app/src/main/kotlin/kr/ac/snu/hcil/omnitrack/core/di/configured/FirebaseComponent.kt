package kr.ac.snu.hcil.omnitrack.core.di.configured

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import dagger.Subcomponent
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.di.Configured

/**
 * Created by younghokim on 2017. 12. 18..
 */
@Configured
@Subcomponent(modules = [FirebaseModule::class])
interface FirebaseComponent {

    fun getFirebaseApp(): FirebaseApp
    fun getFirebaseInstanceId(): FirebaseInstanceId
    fun getFirebaseAuth(): FirebaseAuth

    @FirebaseInstanceIdToken
    fun getFirebaseInstanceIdToken(): Single<String>

    @Subcomponent.Builder
    interface Builder {
        fun plus(module: FirebaseModule): Builder
        fun plus(module: ConfiguredModule): Builder
        fun build(): FirebaseComponent
    }
}