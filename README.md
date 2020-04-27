# J2V8-Debugger

This project is an add-on for the excellent [J2V8 Project](https://github.com/eclipsesource/J2V8).
This project is based on the excellent [J2V8 Debugger](https://github.com/jamie-houston/j2v8-debugger)

It allows users to debug JS running in V8 using [Chrome DevTools](https://developers.google.com/web/tools/chrome-devtools/).

Uses [Stetho](https://github.com/facebook/stetho) for communication with Chrome DevTools.

## Features
* Debugging embedded V8 in Android app using Chrome DevTools.
* Support setting/removing breakpoints, step into, step out and step over, variables inspection, etc.
* Debugging embedded V8 is similar to [Remote Debugging WebViews](https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews).
* Access debuggable V8 in the app via **chrome://inspect**.

## SetUp
Add JitPack repository in your root build.gradle at the end of repositories:

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Add dependency in *gradle.build* file of your app module
```gradle
dependencies {
    implementation ('com.alexii.j2v8debugger:j2v8-debugger:0.2.0') // {
    //     optionally J2V8 can be excluded if specific version of j2v8 is needed or defined by other libs
    //     exclude group: 'com.eclipsesource.j2v8'
    // }
}
```

## Usage

`StethoHelper` and `V8Helper` is used for set-up of Chrome DevTools and V8 for debugging.

1. Initialization Stetho in `Application` class.

Use `StethoHelper.defaultInspectorModulesProvider()` instead of default `Stetho.defaultInspectorModulesProvider()`.

```.Kotlin
                val initializer = Stetho.newInitializerBuilder(context)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(context))
                        .enableWebKitInspector(StethoHelper.defaultInspectorModulesProvider(context, scriptProvider))
                        .build();

                Stetho.initialize(initializer);
```

2. Creation of debuggable V8 instance.

Use `V8Helper.createDebuggableV8Runtime()` instead of `V8.createV8Runtime()`

```.Kotlin
 val debuggableV8Runtime : Future<V8> = V8Helper.createDebuggableV8Runtime(v8Executor, scriptName)
```

3. Clean-up of debuggable V8.

In addition to v8.close()

```.Kotlin

v8Executor.execute { v8Debugger.releaseDebuggable() }
```

See [sample project](https://github.com/alexii/j2v8-debugger/blob/master/j2v8-debugger-sample/src/main/java/com/alexii/j2v8debugging/sample/ExampleActivity.kt) for more info.

### Notes regarding J2V8 threads.
- Creation and clean-up of V8 should run on fixed V8 thread.
- Creation and clean-up of V8 Debugger should run on fixed V8 thread.
- Debugging operation like set/remove breakpoint should run on fixed V8 thread.
- Execution of any JS script/function should run on fixed V8 thread.

It's easier to implement such behaviour _(especially from lib point of view)_ if single-threaded V8 executor is used.

This way all above mentioned operations would run on such executor.

Therefore lib api like `V8Helper.createDebuggableV8Runtime(v8Executor)` is build with this concept in mind.

Later v8 executor will be passed to Chrome DevTools and used for performing debug-related operations.

If Guava is already used in project - MoreExecutors and [ListenableFuture](https://github.com/google/guava/wiki/ListenableFutureExplained) could be handy.

### Known issues
- It's not possible to set break-point while debugging in progress.

 Reason: Since V8 thread is suspended - setting new breakpoint is not possible as it must run on the same V8 thread.
 
### Useful Links
https://github.com/cyrus-and/chrome-remote-interface/wiki/Inspect-the-inspector
