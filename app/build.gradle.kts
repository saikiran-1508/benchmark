import java.util.Properties

plugins {
    id("com.android.application")
    // ✅ THESE WERE MISSING IN YOUR SCREENSHOT:
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.26"
    id("com.google.gms.google-services") version "4.4.4"

}

// Read the Gemini key from local.properties (never commit API keys to git).
// Add a line like: GEMINI_API_KEY=AIza... — the app falls back to an
// offline voice parser when the key is missing.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.example.benchmark"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.benchmark"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY") ?: ""}\"")

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // --- 1. CORE ANDROID ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    // --- 2. COMPOSE UI ---
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // --- 3. NAVIGATION (This fixes the red errors in Navigation.kt) ---
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // --- 4. PROFILE CHARTS & ICONS ---
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")

    // --- 5. FIREBASE ---
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // --- 6. GEMINI AI ---
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // --- 7. NETWORKING ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // --- 8. DATABASE (ROOM) ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // --- 9. TESTING ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}