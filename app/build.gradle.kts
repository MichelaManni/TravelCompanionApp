plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Plugin necessario per Room (gestisce le annotazioni)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.travelcompanion"
    compileSdk = 36 // Versione di SDK per la compilazione

    defaultConfig {
        applicationId = "com.example.travelcompanion"
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

    // === CORREZIONE DELL'ERRORE DI SINTASSI (KTS) ===
    compileOptions {
        // La sintassi corretta in KTS per queste opzioni è l'assegnazione diretta nel blocco
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

    // === JETPACK COMPOSE === //
    val composeBom = "2024.09.00" // Versione di Compose (BOM)
    implementation(platform("androidx.compose:compose-bom:$composeBom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // ViewModel per Compose: fornisce viewModelScope e la funzione viewModel()
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")

    // === DIPENDENZA AGGIUNTA: COMPOSE NAVIGATION ===
    // Risolve l'errore di navigazione (NavHost, rememberNavController, composable)
    val navVersion = "2.7.7"
    implementation("androidx.navigation:navigation-compose:$navVersion")

    // === ROOM DATABASE === //
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion") // Per le Coroutine Flow
    kapt("androidx.room:room-compiler:$roomVersion") // Processore di annotazioni

    // === COROUTINES (Fondamentali per Flow e ViewModel) ===
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