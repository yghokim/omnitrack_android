package kr.ac.snu.hcil.omnitrack.core.injections

import dagger.Component
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 5. 17..
 */
@Singleton
@Component(modules = arrayOf(OTApplicationModule::class))
interface AppComponent {
}