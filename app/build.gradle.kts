plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "dev.rahul.quiettv"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.rahul.quiettv"
        testInstrumentationRunner = "dev.rahul.quiettv.WidgetInstrumentation"
        minSdk = 26
        targetSdk = 35
        versionCode = 25
        versionName = "0.11.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.google.zxing:core:3.5.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
