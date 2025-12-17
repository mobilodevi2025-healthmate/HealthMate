plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Room için KSP
    alias(libs.plugins.ksp)

    // Hilt için Plugin
    alias(libs.plugins.hilt)

    // Hilt derleyicisi (Annotation Processor) için KAPT gereklidir
    id("kotlin-kapt")
}

android {
    namespace = "com.mobil.healthmate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mobil.healthmate"
        minSdk = 29
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Varsayılan Android Kütüphaneleri ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- HILT (DEPENDENCY INJECTION) - KRİTİK BÖLÜM ---
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler) // Hilt için kapt kullanıyoruz (En kararlı yöntem)

    // --- DİĞER EKLENTİLER ---

    // 1. Navigation
    implementation(libs.androidx.navigation.compose)

    // 2. Room Database (Veritabanı)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler) // Room için KSP kullanıyoruz (Kotlin 2.0 ile uyumlu)

    // 3. Retrofit & Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // 4. Coil (Resim Yükleme)
    implementation(libs.coil.compose)

    // 5. WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // --- Test Kütüphaneleri ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}