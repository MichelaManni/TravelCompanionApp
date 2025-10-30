plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Plugin necessario per Room (gestisce le annotazioni)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.travelcompanionapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.travelcompanionapp"
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // === ANDROID CORE === //
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation.android)

    // === GOOGLE PLAY SERVICES === //
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // ⭐ AGGIUNTO: Google Maps per Compose
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // === JETPACK COMPOSE === //
    val composeBom = "2024.09.00"
    implementation(platform("androidx.compose:compose-bom:$composeBom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // ViewModel per Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    // === LOTTIE (per le animazioni) ===
    val lottieVersion = "6.4.1"
    implementation("com.airbnb.android:lottie-compose:$lottieVersion")

    // === COMPOSE NAVIGATION ===
    val navVersion = "2.7.7"
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // === RIMOZIONE OSMDROID === //
    // Rimosso: implementation("org.osmdroid:osmdroid-android:6.1.18")

    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // === ROOM DATABASE === //
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // === COROUTINES ===
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // === TESTING === //
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBom"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // === DEBUG === //
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}