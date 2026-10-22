plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "uk.co.fenixprint.stanalyser"
    compileSdk = 35
    defaultConfig {
        applicationId = "uk.co.fenixprint.stanalyser"
        minSdk = 26
        targetSdk = 32
        versionCode = 4
        versionName = "0.4"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
