package kr.ac.snu.hcil.omnitrack.core.di.global

import android.content.Context
import com.firebase.jobdispatcher.*
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by Young-Ho on 11/3/2017.
 */
@Module()
class JobDispatcherModule {
    @Provides
    @Singleton
    fun providesDispatcher(context: Context): FirebaseJobDispatcher {
        return FirebaseJobDispatcher(GooglePlayDriver(context))
    }

    @Provides
    @Singleton
    fun providesJobBuilder(dispatcher: FirebaseJobDispatcher): Job.Builder {
        return dispatcher.newJobBuilder()
    }

    @Provides
    @Singleton
    @VersionCheck
    fun providesVersionCheckJob(builder: Job.Builder): Job {
        return builder.setRecurring(true)
                .setService(OTVersionCheckService::class.java)
                .setTag(OTVersionCheckService.TAG)
                .setLifetime(Lifetime.FOREVER)
                .setReplaceCurrent(true)
                .setTrigger(Trigger.executionWindow(3600 * 6, 3600 * 7))
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(
                        Constraint.ON_ANY_NETWORK
                ).build()
    }


}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class VersionCheck
