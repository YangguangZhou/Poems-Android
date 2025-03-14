plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs)
}

android {
    namespace = "com.jerryz.poems"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jerryz.poems"
        minSdk = 31
        targetSdk = 35
        versionCode = 102
        versionName = "1.0.2"

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.room.common)
    implementation(libs.okhttp)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(libs.androidx.material3)
    implementation("androidx.compose.runtime:runtime:1.7.8")
    implementation(libs.glide)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}