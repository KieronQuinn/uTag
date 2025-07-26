plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val xposedName = "1.0.8"
val xposedCode = 108

android {
    namespace = "com.kieronquinn.app.utag.xposed.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 30

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("int", "XPOSED_CODE", xposedCode.toString())
        buildConfigField("String", "XPOSED_NAME", "\"$xposedName\"")
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
    buildFeatures {
        buildConfig = true
        aidl = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    compileOnly(libs.xposed)
    implementation(libs.play.services.location)
    implementation(libs.dexplore)
    implementation(libs.androidx.core)
}