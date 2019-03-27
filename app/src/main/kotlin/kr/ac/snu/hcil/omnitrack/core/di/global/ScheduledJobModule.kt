package kr.ac.snu.hcil.omnitrack.core.di.global

import androidx.work.*
import dagger.Module
import dagger.Provides
import dagger.internal.Factory
import kr.ac.snu.hcil.omnitrack.core.workers.*
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 18..
 */
@Module()
class ScheduledJobModule {

    private val networkConstraints: Constraints by lazy {
        Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()
    }

    @Provides
    @Singleton
    @ServerFullSync
    fun providesServerSyncRequest(): PeriodicWorkRequest {
        return PeriodicWorkRequest.Builder(OTSynchronizationWorker::class.java, 3, TimeUnit.HOURS)
                .setConstraints(
                        Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .setRequiresCharging(true)
                                .build()
                ).setBackoffCriteria(BackoffPolicy.LINEAR, 20, TimeUnit.SECONDS)
                .setInputData(Data.Builder().putBoolean(OTSynchronizationWorker.EXTRA_KEY_FULLSYNC, true).build())
                .addTag(OTSynchronizationWorker.TAG)
                .addTag(OTSynchronizationWorker.EXTRA_KEY_FULLSYNC)
                .build()
    }

    @Provides
    @Singleton
    @ServerSyncOneShot
    fun providesImmediateServerSyncRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequest.Builder(OTSynchronizationWorker::class.java)
                .setConstraints(networkConstraints)
                .setInitialDelay(1, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(Data.Builder().putBoolean(OTSynchronizationWorker.EXTRA_KEY_ONESHOT, true).build())
                .addTag(OTSynchronizationWorker.EXTRA_KEY_ONESHOT)
                .build()
        /*
        return builder
                .setTag("${OTSynchronizationService.TAG};${OTSynchronizationService.EXTRA_KEY_ONESHOT}")
                .setRecurring(false)
                .setService(OTSynchronizationService::class.java)
                .setLifetime(Lifetime.FOREVER)
                //.setExtras(oneShotBundle.get())
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 0))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                ).build()*/
    }

    @Provides
    @Singleton
    @BinaryStorageServer
    fun provideBinaryUploadRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequest.Builder(OTBinaryUploadWorker::class.java)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setConstraints(networkConstraints)
                .addTag(OTBinaryUploadWorker.TAG)
                .build()
    }

    @Provides
    @Singleton
    @UsageLogger
    fun providesUsageLogUploadRequest(): OneTimeWorkRequest {

        return OneTimeWorkRequest.Builder(OTUsageLogUploadWorker::class.java)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, OneTimeWorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS, TimeUnit.SECONDS)
                .setInitialDelay(3, TimeUnit.SECONDS)
                .setConstraints(networkConstraints)
                .addTag(OTUsageLogUploadWorker.TAG)
                .build()
    }

    @Provides
    @Singleton
    @InformationUpload
    fun providesInformationUploadRequestBuilderFactory(): Factory<OneTimeWorkRequest.Builder> {
        return object : Factory<OneTimeWorkRequest.Builder> {
            override fun get(): OneTimeWorkRequest.Builder {

                return OneTimeWorkRequest.Builder(OTInformationUploadWorker::class.java)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                        .setConstraints(networkConstraints)
            }
        }


        /*return builder.setService(OTInformationUploadService::class.java)
                .setTag("OTInformationUploadService")
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 20))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .addConstraint(Constraint.ON_ANY_NETWORK)*/
    }

    @Provides
    @Singleton
    @VersionCheck
    fun providesVersionCheckRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequest.Builder(OTVersionCheckWorker::class.java)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setConstraints(networkConstraints)
                .build()
        /*
        return builder.setRecurring(true)
                .setService(OTVersionCheckWorker::class.java)
                .setTag(OTVersionCheckWorker.TAG)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(3600 * 6, 3600 * 7))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                ).build()*/
    }

    @Provides
    @Singleton
    @ResearchSync
    fun provideResearchSyncRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequest.Builder(OTResearchSynchronizationWorker::class.java)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setConstraints(networkConstraints)
                .build()
        /*
        return builder
                .setTag("OTResearchSynchronizationWorker")
                .setService(OTResearchSynchronizationWorker::class.java)
                .setRecurring(false)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 10))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .addConstraint(Constraint.ON_ANY_NETWORK)
                .build()*/
    }

}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class VersionCheck

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ServerFullSync

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ServerSyncOneShot

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class InformationUpload

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ResearchSync
