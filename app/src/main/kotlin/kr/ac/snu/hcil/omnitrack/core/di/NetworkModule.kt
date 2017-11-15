package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.net.*
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017-11-01.
 */
@Module()
class NetworkModule {
    private val rxJava2CallAdapterFactory: RxJava2CallAdapterFactory by lazy {
        RxJava2CallAdapterFactory.create()
    }
    private val gsonConverterFactory: GsonConverterFactory by lazy {
        GsonConverterFactory.create()
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
    fun provideOkHttpClient(authManager: Lazy<OTAuthManager>, cache: Cache): OkHttpClient {
        return OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor { chain ->
                    val bearer = "Bearer " + authManager.get().getAuthToken().blockingGet()
                    val newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", bearer)
                            .build()
                    chain.proceed(newRequest)
                }.build()
    }

    @Provides
    @Singleton
    @SynchronizationServer
    fun provideSynchronizationRetrofit(@Authorized client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
                .client(client)
                .baseUrl(BuildConfig.OMNITRACK_SYNCHRONIZATION_SERVER_URL)
                .addCallAdapterFactory(rxJava2CallAdapterFactory)
                .addConverterFactory(gsonConverterFactory)
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
                .addConverterFactory(gsonConverterFactory)
                .build()
    }

    @Provides
    @Singleton
    fun provideOfficialServerController(@SynchronizationServer retrofit: Retrofit): OTOfficialServerApiController {
        return OTOfficialServerApiController(retrofit)
    }

    @Provides
    @Singleton
    fun provideBinaryStorageController(context: Context, core: IBinaryStorageCore): OTBinaryStorageController {
        return OTBinaryStorageController(context, core)
    }

    /**
     * Change this to replace binary storage
     */
    @Provides
    @Singleton
    fun provideBinaryStorageCore(): IBinaryStorageCore {
        return OTFirebaseStorageCore()
    }

    @Provides
    @Singleton
    fun provideSynchronizationServerController(controller: OTOfficialServerApiController): ISynchronizationServerSideAPI {
        return controller
    }

    @Provides
    @Singleton
    fun provideUserReportServerController(controller: OTOfficialServerApiController): IUserReportServerAPI {
        return controller
    }
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class Authorized

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class SynchronizationServer

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class BinaryStorageServer
