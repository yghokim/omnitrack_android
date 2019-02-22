package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import dagger.Module
import dagger.Provides
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.BuildConfig
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 10..
 */
@Module(includes = [ApplicationModule::class])
class FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseApp(context: Context): FirebaseApp {

        return try {
            FirebaseApp.getInstance()!!
        } catch (ex: Exception) {
            FirebaseApp.initializeApp(context, FirebaseOptions
                    .Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_CLIENT_ID)
                    .setGcmSenderId(BuildConfig.FIREBASE_CLOUD_MESSAGING_SENDER_ID)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .build())
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(fbApp: FirebaseApp): FirebaseAuth {
        return FirebaseAuth.getInstance(fbApp)
    }

    @Provides
    @Singleton
    fun provideFirebaseInstanceId(fbApp: FirebaseApp): FirebaseInstanceId {
        return FirebaseInstanceId.getInstance(fbApp)
    }

    @Provides
    @Singleton
    @FirebaseInstanceIdToken
    fun provideFirebaseInstanceIdToken(fbInstanceId: FirebaseInstanceId): Single<String> {
        return Single.defer {
            return@defer Single.just(fbInstanceId.getToken(BuildConfig.FIREBASE_CLOUD_MESSAGING_SENDER_ID, "FCM"))
        }.subscribeOn(Schedulers.io())
    }
}


@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class FirebaseInstanceIdToken
