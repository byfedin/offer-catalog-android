plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.spotless)
}

android {
    namespace = "dev.byfedin.offercatalog"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.byfedin.offercatalog"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        warningsAsErrors = true
        // Registry updates must not change the outcome of a build of the same commit.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
    testOptions { animationsDisabled = true }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

spotless {
    kotlin {
        target("src/**/*.kt")
        ktfmt("0.59").kotlinlangStyle()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktfmt("0.59").kotlinlangStyle()
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.navigation.compose)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}
