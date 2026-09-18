plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android { namespace = "com.nova.voice"; compileSdk = 35
    defaultConfig { applicationId = "com.nova.voice"; minSdk = 31; targetSdk = 35; versionCode = 1; versionName = "1.0" }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
