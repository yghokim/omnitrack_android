package kr.ac.snu.hcil.omnitrack.core.di.configured

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
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfiguration
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import javax.inject.Qualifier

/**
 * Created by younghokim on 2017. 12. 10..
 */
@Module(includes = [ConfiguredModule::class])
class FirebaseModule {
    @Provides
    @Configured
    fun provideFirebaseApp(context: Context, configuration: OTConfiguration): FirebaseApp {

        return try {
            FirebaseApp.getInstance()!!
        } catch (ex: Exception) {
            FirebaseApp.initializeApp(context, FirebaseOptions
                    .Builder()
                    .setApiKey(configuration.firebaseGoogleApiKey)
                    .setApplicationId(BuildConfig.FIREBASE_CLIENT_ID)
                    .setGcmSenderId(configuration.firebaseCloudMessagingSenderId)
                    .setProjectId(configuration.firebaseProjectId)
                    .build())
        }
    }

    @Provides
    @Configured
    fun provideFirebaseAuth(fbApp: FirebaseApp): FirebaseAuth {
        return FirebaseAuth.getInstance(fbApp)
    }

    @Provides
    @Configured
    fun provideFirebaseInstanceId(fbApp: FirebaseApp): FirebaseInstanceId {
        return FirebaseInstanceId.getInstance(fbApp)
    }

    @Provides
    @Configured
    @FirebaseInstanceIdToken
    fun provideFirebaseInstanceIdToken(configuration: OTConfiguration, fbInstanceId: FirebaseInstanceId): Single<String> {
        return Single.defer {
            return@defer Single.just(fbInstanceId.getToken(configuration.firebaseCloudMessagingSenderId, "FCM"))
        }.subscribeOn(Schedulers.io())
    }
}


@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class FirebaseInstanceIdToken
