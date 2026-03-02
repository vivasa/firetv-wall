plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.clock.firetv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.clock.firetv"
        minSdk = 22
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.4")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("io.coil-kt:coil:2.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // NewPipeExtractor for YouTube stream URL extraction
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.0")

    // OkHttp for NewPipeExtractor's Downloader
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // NanoHTTPD for companion web server
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // Media3 ExoPlayer for native video playback
    val media3Version = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")
}
