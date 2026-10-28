import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Unit-test coverage: koverHtmlReportDebug / koverXmlReportDebug (app/build/reports/kover)
    alias(libs.plugins.kover)
}

// Generated code and pure styling would only dilute the coverage numbers
kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*",
                    "*ComposableSingletons*",
                    "*_Factory",
                )
                packages("com.example.medrational_android.ui.theme")
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }
    }
}

// Push notifications (Firebase Cloud Messaging) need app/google-services.json from the Firebase
// console. Without it the app still builds, and push is simply off (see data/push/WorkshopPush.kt)
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
} else {
    logger.warn("app/google-services.json is missing: push notifications are disabled in this build")
}

// Backend URLs come from local.properties (not committed) so they never live in source.
// Defaults target the backend on the host machine as seen from the Android emulator.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val debugBaseUrl = localProps.getProperty("api.baseUrl", "http://10.0.2.2:8080/")
val releaseBaseUrl = localProps.getProperty("api.releaseBaseUrl", debugBaseUrl)

android {
    namespace = "com.example.medrational_android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.medrational_android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"$releaseBaseUrl\"")
            isMinifyEnabled = true
            isShrinkResources = true
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
        buildConfig = true
    }
    // The language is picked in the app (French by default, English from the profile), not from
    // the phone's settings, so every install needs both: Play must not split strings by language
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    // Per-app language (French by default, English from the profile)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-extended")
    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil for asynchronous image loading in Compose
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Animated GIF decoding
    implementation("io.coil-kt:coil-gif:2.7.0")

    // Security (for encrypted storage of Admin JWT tokens)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}