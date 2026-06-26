plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.junit5.android)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.wakeiq"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.wakeiq"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.1"

        testInstrumentationRunner = "com.wakeiq.HiltTestRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            if (keystorePath != null && keystorePassword != null && keyAlias != null) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                keyPassword = keystorePassword
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("full") {
            dimension = "distribution"
        }
        create("foss") {
            dimension = "distribution"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ktlint {
    version.set("1.3.1")
    android.set(true)
    outputToConsole.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

detekt {
    config.setFrom(rootProject.files("detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}

kover {
    // Generated machinery (Room / Hilt / Dagger / Compose) is never our logic.
    // Kover wildcards span the package separator, but a trailing "*" is still
    // needed to catch nested synthetic classes such as AlarmDao_Impl$2, and
    // factory names without an "_Factory" suffix (Module_ProvideXFactory). Kover
    // per-variant filters replace the base filters rather than merging, so this
    // list is restated in the "logic" report below.
    val generatedCode =
        arrayOf(
            "*_Impl*",
            "*Factory*",
            "*_MembersInjector*",
            "*_GeneratedInjector",
            "Hilt_*",
            "*Hilt_*",
            "dagger.hilt.*",
            "hilt_aggregated_deps.*",
            "*ComposableSingletons*",
            "com.wakeiq.BuildConfig",
        )

    currentProject {
        // "logic" mirrors the fullDebug unit-test run; CI exercises full only.
        createVariant("logic") {
            add("fullDebug")
        }
    }

    reports {
        // Base report (koverHtmlReportFullDebug): honest "all code minus
        // generated" number, including UI and Android glue.
        filters {
            excludes {
                classes(*generatedCode)
            }
        }

        // Logic report (koverHtmlReportLogic): meaningful-logic coverage only.
        // Drops untestable UI and Android glue. The @Composable annotation alone
        // does not strip file-level composable facades, so screen/theme/nav
        // classes are excluded by name as well. Android adapters that wrap a
        // framework API behind a thin shell are marked @InstrumentedOnly and
        // excluded here; their pure logic lives in separate tested units. See
        // docs/adr/0002-coverage-exclusions.md.
        variant("logic") {
            filters {
                excludes {
                    classes(*generatedCode)
                    classes(
                        "*ScreenKt*",
                        "com.wakeiq.presentation.theme.*",
                        "com.wakeiq.presentation.navigation.*",
                        "com.wakeiq.MainActivityKt",
                        // DataStore delegate facade: pure wiring, sibling to the excluded AppPreferences.
                        "com.wakeiq.data.preferences.AppPreferencesKt",
                    )
                    annotatedBy(
                        "dagger.hilt.android.AndroidEntryPoint",
                        "dagger.hilt.android.HiltAndroidApp",
                        "dagger.Module",
                        "androidx.compose.runtime.Composable",
                        "com.wakeiq.core.InstrumentedOnly",
                    )
                }
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Logging / observability
    implementation(libs.timber)
    debugImplementation(libs.leakcanary.android)

    // Detekt formatting plugin
    detektPlugins(libs.detekt.formatting)

    // Unit tests
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.testing)

    // Instrumented tests
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
