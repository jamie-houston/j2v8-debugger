package com.salesforce.j2v8debugging.sample

import android.app.Activity
import android.app.Application
import com.salesforce.j2v8debugger.BuildConfig
import com.salesforce.j2v8debugger.ScriptSourceProvider
import com.salesforce.j2v8debugger.StethoHelper
import com.salesforce.j2v8debugger.utils.LogUtils
import com.salesforce.j2v8debugging.sample.di.DaggerAppComponent
import com.facebook.stetho.Stetho
import com.salesforce.j2v8debugger.V8Debugger
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import timber.log.Timber
import javax.inject.Inject

class App : Application(), HasActivityInjector {
    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var scriptProvider: ScriptSourceProvider

    @Inject
    lateinit var v8Debugger: V8Debugger

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        if (BuildConfig.DEBUG) LogUtils.enabled = true

        DaggerAppComponent
                .builder()
                .application(this)
                .build()
                .inject(this)

        val context = this
        val initializer = Stetho.newInitializerBuilder(context)
                .enableDumpapp(Stetho.defaultDumperPluginsProvider(context))
                .enableWebKitInspector(StethoHelper.defaultInspectorModulesProvider(context, scriptProvider, v8Debugger))
                .build()
        Stetho.initialize(initializer)

        Timber.w("[Alex_Stetho] initialize")
    }

    override fun activityInjector(): AndroidInjector<Activity>? {
        return dispatchingAndroidInjector
    }

}
