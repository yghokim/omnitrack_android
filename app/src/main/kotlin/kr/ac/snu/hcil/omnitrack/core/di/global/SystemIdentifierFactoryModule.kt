package kr.ac.snu.hcil.omnitrack.core.di.global

import dagger.Module
import dagger.Provides
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 12. 22..
 */
@Module()
class SystemIdentifierFactoryModule {
    @Provides
    @Singleton
    @ReminderNotification
    fun provideReminderNotificationIdSeed(): AtomicInteger {
        return AtomicInteger()
    }

}

@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class ReminderNotification
