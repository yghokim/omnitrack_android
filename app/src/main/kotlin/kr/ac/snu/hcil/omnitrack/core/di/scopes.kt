package kr.ac.snu.hcil.omnitrack.core.di

import javax.inject.Scope

/**
 * Created by younghokim on 2017-11-02.
 */
@Scope
@Retention(AnnotationRetention.RUNTIME) annotation class Configured

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class ForFragment

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class ForActivity