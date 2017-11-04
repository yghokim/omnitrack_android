package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import android.icu.util.TimeUnit
import com.firebase.jobdispatcher.*
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.services.OTSynchronizationService
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import javax.inject.Qualifier
import javax.xml.datatype.DatatypeConstants.MINUTES

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module
class ScheduledJobModule {
    @Provides
    @ApplicationScope
    fun providesDispatcher(context: Context): FirebaseJobDispatcher
    {
        return FirebaseJobDispatcher(GooglePlayDriver(context))
    }

    @Provides
    @ApplicationScope
    fun providesJobBuilder(dispatcher: FirebaseJobDispatcher) = dispatcher.newJobBuilder()

    @Provides
    @ApplicationScope
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
    @ApplicationScope
    @ServerSync
    fun providesServerSyncJob(builder: Job.Builder): Job
    {
        return builder.setRecurring(true)
                .setService(OTSynchronizationService::class.java)
                .setTag(OTVersionCheckService.TAG)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(0, 3600*12))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                ).build()
    }

}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class VersionCheck

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ServerSync



