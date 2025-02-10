// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.navigation.safe.args) apply false
    alias(libs.plugins.google.maps.secrets) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.parcelize) apply false
    alias(libs.plugins.play.services) apply false
    alias(libs.plugins.play.services.oss) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}