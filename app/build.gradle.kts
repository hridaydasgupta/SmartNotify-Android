plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.smartnotify.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.smartnotify.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 1. Room Database (Zero-Lag Local Storage)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // 2. WorkManager (Accurate Scheduling ke liye)
    val workVersion = "2.9.0"
    implementation("androidx.work:work-runtime:$workVersion")

    // 3. Lifecycle & ViewModel (MVVM Architecture)
    val lifecycleVersion = "2.7.0"
    implementation("androidx.lifecycle:lifecycle-viewmodel:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata:$lifecycleVersion")

    // 4. Encrypted SharedPreferences (Secure Time Storage)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}