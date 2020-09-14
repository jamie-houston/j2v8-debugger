package com.salesforce.j2v8inspector.sample.di

import com.salesforce.j2v8inspector.sample.ExampleActivity

import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Binds sub-components within the app.
 */
@Module
abstract class BuildersModule {

    @ContributesAndroidInjector
    internal abstract fun bindExampleActivity(): ExampleActivity
}
