package com.salesforce.j2v8inspector.sample

import android.app.Activity
import android.app.Application
import com.salesforce.j2v8inspector.ScriptSourceProvider
import com.salesforce.j2v8inspector.StethoHelper
import com.salesforce.j2v8inspector.sample.di.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import javax.inject.Inject

class App : Application(), HasActivityInjector {
    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var scriptProvider: ScriptSourceProvider

    override fun onCreate() {
        super.onCreate()

        DaggerAppComponent
                .builder()
                .application(this)
                .build()
                .inject(this)

        StethoHelper.initializeDebugger(this, scriptProvider)
    }

    override fun activityInjector(): AndroidInjector<Activity>? {
        return dispatchingAndroidInjector
    }
}
