import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.navigation.safe.args)
    alias(libs.plugins.google.maps.secrets)
    alias(libs.plugins.ksp)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.play.services.oss)
    alias(libs.plugins.play.services)
    alias(libs.plugins.firebase.crashlytics)
}

fun getKeystoreProperties(): Properties? {
    var properties: Properties? = Properties()
    properties?.setProperty("keyAlias", "")
    properties?.setProperty("keyPassword", "")
    properties?.setProperty("storeFile", "")
    properties?.setProperty("storePassword", "")
    try {
        val propertiesFile = rootProject.file("local.properties")
        if (propertiesFile.exists()) {
            properties?.load(FileInputStream(propertiesFile))
        }
    } catch (ignored: Exception) {
        properties = null
        println("Unable to read keystore")
    }
    return properties
}

val tagName = "1.0.5"
val tagCode = 105

android {
    namespace = "com.kieronquinn.app.utag"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kieronquinn.app.utag"
        minSdk = 30
        targetSdk = 35
        versionCode = tagCode
        versionName = tagName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "TAG_NAME", "\"${tagName}\"")
    }

    signingConfigs {
        create("release") {
            val keystore = getKeystoreProperties()
            if (keystore != null) {
                storeFile = file(keystore.getProperty("storeFile"))
                storePassword  = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            //Minify is enabled but obfuscation is disabled
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        aidl = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/*"
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    project.tasks.preBuild.dependsOn("fmm")
}

/**
 *  Checks for required FMM libraries for Chaser, which are not included on Git. The setup details
 *  where to get them from for building.
 */
tasks.register("fmm") {
    doFirst {
        val libsPath = "${project.rootDir}/app/src/main/jniLibs"
        val requiredFiles = setOf(
            File("$libsPath/arm64-v8a/libfmm_ct.so"),
            File("$libsPath/armeabi-v7a/libfmm_ct.so")
        )
        if(requiredFiles.any { !it.exists() }) {
            throw GradleException("FMM libraries are missing, check the build setup in the README")
        }
    }
}

configurations.all {
    exclude(group = "androidx.appcompat", module = "appcompat")
    exclude(group = "androidx.core", module = "core")
    exclude(group = "androidx.customview", module = "customview")
    exclude(group = "androidx.viewpager", module = "viewpager")
    exclude(group = "androidx.fragment", module = "fragment")
    exclude(group = "androidx.drawerlayout", module = "drawerlayout")
    exclude(group = "com.google.android.material", module = "material")
    resolutionStrategy {
        //Versions from https://github.com/OneUIProject/oneui-core/blob/sesl4/manifest.gradle
        force("androidx.activity:activity:1.2.4")
    }
}

secrets {
    propertiesFileName = "secrets.properties"
}

dependencies {
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.splash.screen)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.work)
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.apache.commons.codec)
    implementation(libs.apache.commons.lang)
    implementation(libs.apache.commons.csv)
    implementation(libs.gson)
    implementation(libs.bundles.oneui)
    implementation(libs.koin)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.process.phoenix)
    implementation(libs.bundles.retrofit)
    implementation(libs.okhttp)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.play.services.oss)
    implementation(libs.glide)
    implementation(libs.lottie)
    implementation(libs.kotlin.date.range)
    implementation(libs.room)
    implementation(libs.better.link.movement.method)
    implementation(libs.otp.view)
    implementation(libs.flexbox)
    implementation(libs.markwon)
    implementation(libs.markwon.tables)
    implementation(libs.maps.utils)
    implementation(libs.smartspacer)
    implementation(libs.kotlin.reflect)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    ksp(libs.room.compiler)
    implementation(project(":xposed-core"))
    implementation(project(":uwb"))
}