package com.salesforce.j2v8inspector.sample.di

import android.content.Context
import com.salesforce.j2v8inspector.ScriptSourceProvider
import com.salesforce.j2v8inspector.sample.App
import com.salesforce.j2v8inspector.sample.SimpleScriptProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Singleton

/**
 * This is where you will inject application-wide dependencies.
 */
@Module(includes = [AppModule.Declarations::class])
class AppModule {

    @Module
    interface Declarations {
        @Binds
        fun provideContext(application: App): Context

        @Binds
        fun provideSimpleScriptProvider(simpleScriptProvider: SimpleScriptProvider): ScriptSourceProvider
    }

    @Singleton
    @Provides
    fun provideScriptSourceProvider(): SimpleScriptProvider {
        return SimpleScriptProvider()
    }

    @Singleton
    @Provides
    fun provideV8ExecutorService(): ExecutorService {
        return Executors.newSingleThreadExecutor();
    }
}
