package kr.ac.snu.hcil.omnitrack.core.di

import dagger.Component
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Singleton
@Component(modules = arrayOf(TriggerSystemModule::class, SynchronizationModule::class, BackendDatabaseModule::class))
interface TriggerSystemComponent {

}