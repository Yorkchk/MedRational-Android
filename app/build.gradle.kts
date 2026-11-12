import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
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


// Push notifications (Firebase Cloud Messaging) and crash reports (Crashlytics) need
// app/google-services.json from the Firebase console. Without it the app still builds, and both are
// simply off (see data/push/WorkshopPush.kt and data/crash/CrashReporting.kt)
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
} else {
    logger.warn("app/google-services.json is missing: push and crash reporting are disabled in this build")
}

// Backend URLs come from local.properties (not committed) so they never live in source, one per
// environment flavor. The dev default targets the backend on the host machine as seen from the
// Android emulator; uat and prod have none, so building them without their URL fails (see below).
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val envUrlKeys = mapOf("dev" to "api.devUrl", "uat" to "api.uatUrl", "prod" to "api.prodUrl")
fun envUrl(env: String): String? = localProps.getProperty(envUrlKeys.getValue(env))
    ?: "http://10.0.2.2:8080/".takeIf { env == "dev" }

// Release signing (the Play upload key). CI passes it as env vars (publish-uat-apk.yml, release.yml);
// locally it can live in local.properties. With any value missing, release builds stay unsigned, so
// PR CI (assembleDevRelease) needs no secrets.
fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: localProps.getProperty(prop)?.takeIf { it.isNotBlank() }
val releaseSigning = listOf(
    signingValue("SIGNING_STORE_FILE", "signing.storeFile"),
    signingValue("SIGNING_STORE_PASSWORD", "signing.storePassword"),
    signingValue("SIGNING_KEY_ALIAS", "signing.keyAlias"),
    signingValue("SIGNING_KEY_PASSWORD", "signing.keyPassword"),
).takeIf { values -> values.all { it != null } }?.map { it!! }

// The CD workflows set the version (-PversionCode=<commits on main> -PversionName=...); local builds
// keep the defaults
val appVersionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
val appVersionName = (findProperty("versionName") as String?) ?: "1.0"

android {
    namespace = "com.example.medrational_android"
    compileSdk = 36

    defaultConfig {
        // The Play Store ID, permanent once published (Play refuses com.example.*). The code's
        // namespace above stays com.example.medrational_android; the two are independent.
        applicationId = "ma.scome.swisra"
        minSdk = 24
        // Google Play's minimum target for new apps and updates (API 36 since 31 August 2026)
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The environment (which backend and push topic) is a flavor, independent of the build type, so
    // uatRelease is a prod-like build for UAT. dev and uat get their own application ID and launcher
    // name, so testers keep the real app installed next to them; each ID must be an Android app in
    // the Firebase project, listed in google-services.json. The topic is picked in WorkshopPush.
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            manifestPlaceholders["appLabel"] = "SWISRA Dev"
            // dev never reports crashes (CrashReporting), so CI's devRelease build on every PR
            // doesn't upload R8 mapping files; uat and prod release builds do
            extensions.findByType<CrashlyticsExtension>()?.mappingFileUploadEnabled = false
        }
        create("uat") {
            dimension = "env"
            applicationIdSuffix = ".uat"
            manifestPlaceholders["appLabel"] = "SWISRA UAT"
        }
        create("prod") {
            dimension = "env"
            // No suffix: the Play Store app's ID
            manifestPlaceholders["appLabel"] = "SWISRA"
        }
    }
    productFlavors.configureEach {
        buildConfigField("String", "BASE_URL", "\"${envUrl(name).orEmpty()}\"")
    }

    releaseSigning?.let { (storeFile, storePassword, keyAlias, keyPassword) ->
        signingConfigs {
            create("release") {
                this.storeFile = rootProject.file(storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
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

// Fails a uat or prod build up front when its URL is missing, instead of shipping an app with no
// backend. Dev builds (and CI) never need one.
gradle.taskGraph.whenReady {
    val missing = envUrlKeys.filter { (env, _) ->
        envUrl(env) == null && allTasks.any { it.project == project && it.name.contains(env.replaceFirstChar(Char::uppercase)) }
    }
    if (missing.isNotEmpty()) {
        throw GradleException("Set ${missing.values.joinToString(" and ")} in local.properties to build ${missing.keys.joinToString(" and ")}")
    }
    // uat and prod only allow HTTPS (network_security_config.xml), so an http:// URL would build an
    // app that can't reach its backend: refuse it here
    val insecure = envUrlKeys.filter { (env, _) ->
        env != "dev" && envUrl(env)?.startsWith("https://") == false &&
            allTasks.any { it.project == project && it.name.contains(env.replaceFirstChar(Char::uppercase)) }
    }
    if (insecure.isNotEmpty()) {
        throw GradleException("${insecure.values.joinToString(" and ")} in local.properties must start with https://")
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
    implementation(libs.firebase.crashlytics)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.material:material-icons-extended")
    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil for asynchronous image loading in Compose
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Animated GIF decoding
    implementation("io.coil-kt:coil-gif:2.7.0")

    // Security (for encrypted storage of Admin JWT tokens)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}