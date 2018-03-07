package kr.ac.snu.hcil.omnitrack.core.di.configured

import android.os.Bundle
import com.firebase.jobdispatcher.*
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfiguration
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.services.*
import javax.inject.Provider
import javax.inject.Qualifier

/**
 * Created by younghokim on 2017. 12. 18..
 */
@Module(includes = [ConfiguredModule::class])
class ScheduledJobModule {

    @Provides
    @ConfiguredObject
    fun makeConfiguredBundle(config: OTConfiguration): Bundle {
        return Bundle().apply { putString(OTApp.INTENT_EXTRA_CONFIGURATION_ID, config.id) }
    }

    @Provides
    @Configured
    @ServerSyncOneShot
    fun makeOneShotBundle(@ConfiguredObject bundle: Bundle): Bundle {
        return bundle.apply { putBoolean(OTSynchronizationService.EXTRA_KEY_ONESHOT, true) }
    }

    @Provides
    @Configured
    @ServerSync
    fun providesServerSyncJob(configuration: OTConfiguration, @ConfiguredObject bundle: Bundle, builder: Job.Builder): Job
    {
        return builder
                .setRecurring(true)
                .setService(OTSynchronizationService::class.java)
                .setTag("${OTSynchronizationService.TAG};PeriodicFullSync;${configuration.id}")
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(4800, 3600 * 2))
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                )
                .setExtras(bundle.apply { putBoolean(OTSynchronizationService.EXTRA_KEY_FULLSYNC, true) }).build()
    }

    @Provides
    @Configured
    @ServerSyncOneShot
    fun providesImmediateServerSyncJob(configuration: OTConfiguration, builder: Job.Builder, @ServerSyncOneShot oneShotBundle: Provider<Bundle>): Job {
        return builder
                .setRecurring(false)
                .setService(OTSynchronizationService::class.java)
                .setTag("${OTSynchronizationService.TAG};OneShot;${configuration.id}")
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
    fun provideBinaryUploadJob(configuration: OTConfiguration, builder: Job.Builder): Job {
        return builder.setService(OTBinaryUploadService::class.java)
                .setRecurring(false)
                .setTag(OTBinaryUploadService.makeTag(configuration.id, OTBinaryUploadService.ACTION_UPLOAD))
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
    @Configured
    @UsageLogger
    fun providesUsageLogUploadJob(configuration: OTConfiguration, builder: Job.Builder): Job {
        return builder.setService(OTUsageLogUploadService::class.java)
                .setTag("OTUsageLogUploadService;${configuration.id}")
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setRecurring(false)
                .setTrigger(Trigger.executionWindow(5, 30))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .addConstraint(Constraint.ON_ANY_NETWORK)
                .build()
    }

    @Provides
    @Configured
    @InformationUpload
    fun providesInformationUploadJobBuilder(builder: Job.Builder): Job.Builder {
        return builder.setService(OTInformationUploadService::class.java)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 20))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .addConstraint(Constraint.ON_ANY_NETWORK)
    }

    @Provides
    @Configured
    @ResearchSync
    fun provideResearchSyncJob(configuration: OTConfiguration, builder: Job.Builder): Job {
        return builder
                .setService(OTResearchSynchronizationService::class.java)
                .setRecurring(false)
                .setTag("OTResearchSynchronizationService;${configuration.id}")
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
@Retention(AnnotationRetention.RUNTIME) annotation class ConfiguredObject

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class InformationUpload

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ResearchSync

