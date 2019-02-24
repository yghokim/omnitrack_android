package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import android.net.Uri
import androidx.work.OneTimeWorkRequest
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.InternetObservingSettings
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.strategy.SocketInternetObservingStrategy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.annotations.RealmModule
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.LocalMediaCacheEntry
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.UploadTaskInfo
import kr.ac.snu.hcil.omnitrack.core.net.*
import kr.ac.snu.hcil.omnitrack.utils.LocaleHelper
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkNotConnectedException
import okhttp3.Cache
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017-11-01.
 */
@Module(includes = [ApplicationModule::class, ScheduledJobModule::class])
class NetworkModule {
    private val rxJava2CallAdapterFactory: RxJava2CallAdapterFactory by lazy {
        RxJava2CallAdapterFactory.create()
    }

    private val scalarsConverterFactory: ScalarsConverterFactory by lazy {
        ScalarsConverterFactory.create()
    }

    @Provides
    @Singleton
    fun provideHttpCache(context: Context): Cache {
        val cacheSize = 10 * 1024 * 1024
        return Cache(context.cacheDir, cacheSize.toLong())
    }

    @Provides
    @Singleton
    @Authorized
    fun provideOkHttpClient(context: Context, authManager: Lazy<OTAuthManager>, @DeviceId deviceId: Lazy<String>, @Sha1FingerPrint fingerPrint: String, cache: Cache): OkHttpClient {
        return OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor { chain ->
                    val bearer = "Bearer " + authManager.get().getAuthToken().blockingGet()
                    val newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", bearer)
                            .addHeader("OTDeviceId", deviceId.get())
                            .addHeader("OTFingerPrint", fingerPrint)
                            .addHeader("OTPackageName", BuildConfig.APPLICATION_ID)
                            .addHeader("OTRole", "ServiceUser")
                            .addHeader("OTLocale", LocaleHelper.getLanguageCode(context))
                            .apply {
                                if (!BuildConfig.DEFAULT_EXPERIMENT_ID.isNullOrBlank()) {
                                    this.addHeader("OTExperiment", BuildConfig.DEFAULT_EXPERIMENT_ID)
                                }
                            }
                            .build()
                    println("Provide HTTP client bounding to " + newRequest.url().toString())
                    chain.proceed(newRequest)
                }.build()
    }

    @Provides
    @Singleton
    @OkHttpMediaType(MediaTypeValue.IMAGE)
    fun provideImageMediaType(): MediaType {
        return MediaType.parse("image/*")!!
    }

    @Provides
    @Singleton
    @OkHttpMediaType(MediaTypeValue.PLAINTEXT)
    fun providePlainTextMediaType(): MediaType {
        return MediaType.parse("text/plain")!!
    }



    @Provides
    @Singleton
    @SynchronizationServer
    fun provideSynchronizationRetrofit(@Authorized client: OkHttpClient, @ForGeneric gson: Lazy<Gson>): Retrofit {
        return Retrofit.Builder()
                .client(client)
                .baseUrl(BuildConfig.OMNITRACK_SYNCHRONIZATION_SERVER_URL)
                .addCallAdapterFactory(rxJava2CallAdapterFactory)
                .addConverterFactory(scalarsConverterFactory)
                .addConverterFactory(GsonConverterFactory.create(
                        GsonBuilder()
                                .registerTypeAdapter(
                                        ISynchronizationServerSideAPI
                                                .ExperimentConsentInfo::class.java,
                                        ISynchronizationServerSideAPI
                                                .ExperimentConsentInfo.ConsentInfoTypeAdapter(gson))
                                .create()
                ))
                .build()
    }

    @Provides
    @Singleton
    @BinaryStorageServer
    fun provideBinaryStorageRetrofit(@Authorized client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
                .client(client)
                .baseUrl(BuildConfig.OMNITRACK_MEDIA_STORAGE_SERVER_URL)
                .addCallAdapterFactory(rxJava2CallAdapterFactory)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    @Provides
    @Singleton
    fun provideOfficialServerController(@SynchronizationServer retrofit: Retrofit): OTOfficialServerApiController {
        return OTOfficialServerApiController(retrofit)
    }

    @Provides
    @Singleton
    fun provideBinaryStorageController(
            @BinaryStorageServer workRequest: Provider<OneTimeWorkRequest>,
            core: IBinaryStorageCore): OTBinaryStorageController {
        return OTBinaryStorageController(workRequest, core, provideRealm())
    }

    /**
     * Change this to replace binary storage
     */
    @Provides
    @Singleton
    fun provideBinaryStorageCore(context: Context, @BinaryStorageServer retrofit: Lazy<Retrofit>): IBinaryStorageCore {
        //return OTFirebaseStorageCore()
        return OTOfficialBinaryStorageCore(context, retrofit)
    }

    @Provides
    @Singleton
    fun provideSynchronizationServerController(controller: OTOfficialServerApiController): ISynchronizationServerSideAPI {
        return controller
    }

    @Provides
    @Singleton
    fun provideLocalMediaCacheManager(context: Context, authManager: Lazy<OTAuthManager>, storageController: Lazy<OTBinaryStorageController>): OTLocalMediaCacheManager {
        return OTLocalMediaCacheManager(context, authManager, storageController)
    }

    @Provides
    @Singleton
    fun provideUserReportServerController(controller: OTOfficialServerApiController): IUserReportServerAPI {
        return controller
    }

    @Provides
    @Singleton
    fun provideUsageLogUploadController(controller: OTOfficialServerApiController): IUsageLogUploadAPI {
        return controller
    }

    private val realmConfiguration: RealmConfiguration by lazy {
        RealmConfiguration.Builder().name("media_storage.db").modules(UploadTaskQueueRealmModule()).deleteRealmIfMigrationNeeded().build()
    }

    private fun provideRealm(): Factory<Realm> {
        return object : Factory<Realm> {
            override fun get(): Realm {
                return Realm.getInstance(realmConfiguration)
            }
        }
    }

    @Provides
    @Singleton
    @ServerResponsive
    fun provideServerConnection(): Completable {
        val serverUri = Uri.parse(BuildConfig.OMNITRACK_SYNCHRONIZATION_SERVER_URL)

        return ReactiveNetwork.checkInternetConnectivity(
                InternetObservingSettings
                        .builder()
                        .host(serverUri.host)
                        .port(serverUri.port)
                        .strategy(SocketInternetObservingStrategy())
                        .timeout(2000)
                        .build())
                .subscribeOn(Schedulers.io())
                .flatMapCompletable { connected ->
                    if (connected) {
                        Completable.complete()
                    } else Completable.error(NetworkNotConnectedException())
                }
        /*
        return Completable.defer{
            val reachable = InetAddress.getByName(serverUri.host).isReachable(2000)
            if(reachable){
                Completable.complete()
            } else Completable.error(NetworkNotConnectedException())
        }.subscribeOn(Schedulers.io())*/
    }


}

enum class MediaTypeValue {
    IMAGE, BINARY, PLAINTEXT, JSON,
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class Authorized

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class SynchronizationServer

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class BinaryStorageServer

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class OkHttpMediaType(val type: MediaTypeValue)

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ServerResponsive


@RealmModule(classes = [UploadTaskInfo::class, LocalMediaCacheEntry::class])
class UploadTaskQueueRealmModule