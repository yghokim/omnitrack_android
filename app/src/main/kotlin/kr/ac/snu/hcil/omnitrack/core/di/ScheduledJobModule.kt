package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import android.os.Bundle
import com.firebase.jobdispatcher.*
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.services.OTBinaryUploadService
import kr.ac.snu.hcil.omnitrack.services.OTSynchronizationService
import kr.ac.snu.hcil.omnitrack.services.OTUsageLogUploadService
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module
class ScheduledJobModule {
    @Provides
    @Singleton
    fun providesDispatcher(context: Context): FirebaseJobDispatcher
    {
        return FirebaseJobDispatcher(GooglePlayDriver(context))
    }

    @Provides
    @Singleton
    fun providesJobBuilder(dispatcher: FirebaseJobDispatcher) = dispatcher.newJobBuilder()

    @Provides
    @Singleton
    @VersionCheck
    fun providesVersionCheckJob(builder: Job.Builder): Job
    {
        return builder.setRecurring(true)
                .setService(OTVersionCheckService::class.java)
                .setTag(OTVersionCheckService.TAG)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(3600*6, 3600*7))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                ).build()
    }

    @Provides
    @Singleton
    @ServerSyncOneShot
    fun makeOneShotBundle(): Bundle {
        return Bundle().apply { putBoolean(OTSynchronizationService.EXTRA_KEY_ONESHOT, true) }
    }



    @Provides
    @Singleton
    @ServerSync
    fun providesServerSyncJob(builder: Job.Builder): Job
    {
        return builder.setRecurring(true)
                .setService(OTSynchronizationService::class.java)
                .setTag(OTSynchronizationService.TAG)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 3600*12))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                ).build()
    }


    @Provides
    @Singleton
    @BinaryStorageServer
    fun provideBinaryUploadJob(builder: Job.Builder): Job {
        return builder.setService(OTBinaryUploadService::class.java)
                .setRecurring(false)
                .setTag(OTBinaryUploadService.TAG)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 30))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                )
                .build()
    }

    @Provides
    @Singleton
    @ServerSyncOneShot
    fun providesImmediateServerSyncJob(builder: Job.Builder, @ServerSyncOneShot oneShotBundle: Bundle): Job {
        return builder.setRecurring(false)
                .setService(OTSynchronizationService::class.java)
                .setTag(OTSynchronizationService.TAG)
                .setLifetime(Lifetime.FOREVER)
                .setExtras(oneShotBundle)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 0))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                ).build()
    }

    @Provides
    @Singleton
    @UsageLogger
    fun providesUsageLogUploadJob(builder: Job.Builder): Job {
        return builder.setService(OTUsageLogUploadService::class.java)
                .setTag(OTUsageLogUploadService.TAG)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(5, 30))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .addConstraint(Constraint.ON_ANY_NETWORK)
                .build()
    }


}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class VersionCheck

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ServerSync

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ServerSyncOneShot