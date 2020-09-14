plugins {
    id("com.android.library")
    id("kotlin-android")
    `maven-publish`
}

android {
    compileSdkVersion(29)

    defaultConfig {
        minSdkVersion(23)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            isTestCoverageEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // J2V8 Runtime
    api("com.eclipsesource.j2v8:j2v8:6.2.0@aar")
    api("com.facebook.stetho:stetho:1.5.1")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.72")
    implementation("androidx.annotation:annotation:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")

    testImplementation("org.junit.jupiter:junit-jupiter:5.6.1")
    testImplementation("org.json:json:20190722")
    testImplementation("com.google.guava:guava:25.1-android")
    testImplementation("io.mockk:mockk:1.10.0")
}

repositories {
    mavenCentral()
}
