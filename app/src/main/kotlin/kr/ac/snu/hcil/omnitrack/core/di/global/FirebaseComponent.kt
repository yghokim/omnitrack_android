package kr.ac.snu.hcil.omnitrack.core.di.global

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import dagger.Component
import io.reactivex.Single
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 18..
 */
@Singleton
@Component(modules = [FirebaseModule::class])
interface FirebaseComponent {
    fun getFirebaseApp(): FirebaseApp
    fun getFirebaseInstanceId(): FirebaseInstanceId
    fun getFirebaseAuth(): FirebaseAuth

    @FirebaseInstanceIdToken
    fun getFirebaseInstanceIdToken(): Single<String>

}