plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safeargs)
}

import java.util.Properties

val aiProperties = Properties().apply {
    val envFile = rootProject.file("ai.env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

val aiBaseUrl = aiProperties.getProperty("AI_BASE_URL") ?: "https://one.jerryz.com.cn/v1"
val aiApiKey = aiProperties.getProperty("AI_API_KEY") ?: ""
val aiModel = aiProperties.getProperty("AI_MODEL") ?: "qwen3-max-preview"

android {
    namespace = "com.jerryz.poems"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jerryz.poems"
        minSdk = 31
        targetSdk = 35
        versionCode = 110
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AI_BASE_URL", "\"${aiBaseUrl.escapeForBuildConfig()}\"")
        buildConfigField("String", "AI_API_KEY", "\"${aiApiKey.escapeForBuildConfig()}\"")
        buildConfigField("String", "AI_MODEL", "\"${aiModel.escapeForBuildConfig()}\"")
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
        buildConfig = true
    }
}

configurations.all {
    resolutionStrategy {
        // 根据你已有的 activity-compose 版本，强制所有 activity 库使用 1.10.1
        force("androidx.activity:activity:1.10.1")
        force("androidx.activity:activity-ktx:1.10.1")
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
    implementation(libs.androidx.recyclerview)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runtime)
    implementation(libs.jpinyin)
    implementation(libs.glide)
    implementation(libs.androidx.preference.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
