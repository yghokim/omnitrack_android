package kr.ac.snu.hcil.omnitrack.core.di.configured

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.work.*
import com.firebase.jobdispatcher.*
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.services.*
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Qualifier

/**
 * Created by younghokim on 2017. 12. 18..
 */
@Module(includes = [ConfiguredModule::class])
class ScheduledJobModule {

    @Provides
    @Configured
    @ServerSyncOneShot
    fun makeOneShotBundle(): Bundle {
        return bundleOf(OTSynchronizationService.EXTRA_KEY_ONESHOT to true)
    }

    @Provides
    @Configured
    @ServerSync
    fun providesServerSyncJob(builder: Job.Builder): Job
    {
        return builder
                .setTag("${OTSynchronizationService.TAG};${OTSynchronizationService.EXTRA_KEY_FULLSYNC}")
                .setRecurring(true)
                .setService(OTSynchronizationService::class.java)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(4800, 3600 * 2))
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                )
                .setExtras(bundleOf(OTSynchronizationService.EXTRA_KEY_FULLSYNC to true)).build()
    }

    @Provides
    @Configured
    @ServerSyncOneShot
    fun providesImmediateServerSyncJob(builder: Job.Builder, @ServerSyncOneShot oneShotBundle: Provider<Bundle>): Job {
        return builder
                .setTag("${OTSynchronizationService.TAG};${OTSynchronizationService.EXTRA_KEY_ONESHOT}")
                .setRecurring(false)
                .setService(OTSynchronizationService::class.java)
                .setLifetime(Lifetime.FOREVER)
                .setExtras(oneShotBundle.get())
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 0))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                ).build()
    }

    @Provides
    @Configured
    @BinaryStorageServer
    fun provideBinaryUploadRequest(): OneTimeWorkRequest {
        val constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()

        return OneTimeWorkRequestBuilder<OTBinaryUploadWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .addTag(OTBinaryUploadWorker.TAG)
                .build()
    }

    @Provides
    @Configured
    @UsageLogger
    fun providesUsageLogUploadRequest(): OneTimeWorkRequest {
        val constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()

        return OneTimeWorkRequestBuilder<OTUsageLogUploadWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .addTag(OTUsageLogUploadWorker.TAG)
                .build()
    }

    @Provides
    @Configured
    @InformationUpload
    fun providesInformationUploadJobBuilder(builder: Job.Builder): Job.Builder {
        return builder.setService(OTInformationUploadService::class.java)
                .setTag("OTInformationUploadService")
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 20))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .addConstraint(Constraint.ON_ANY_NETWORK)
    }

    @Provides
    @Configured
    @ResearchSync
    fun provideResearchSyncJob(builder: Job.Builder): Job {
        return builder
                .setTag("OTResearchSynchronizationService")
                .setService(OTResearchSynchronizationService::class.java)
                .setRecurring(false)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 10))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .addConstraint(Constraint.ON_ANY_NETWORK)
                .build()
    }
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ServerSync

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ServerSyncOneShot

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class InformationUpload

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ResearchSync

