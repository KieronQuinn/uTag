package com.kieronquinn.app.utag.utils.chaser

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ChangedPackages
import android.content.pm.FeatureInfo
import android.content.pm.InstrumentationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PermissionGroupInfo
import android.content.pm.PermissionInfo
import android.content.pm.ProviderInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.content.pm.SharedLibraryInfo
import android.content.pm.VersionedPackage
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.UserHandle

@Suppress("DEPRECATION") //Not actually deprecated, just warning to not extend which we need
class FmmPackageManager : PackageManager() {

    override fun getPackageInfo(packageName: String, flags: Int): PackageInfo {
        //Native library checks if the FMM app has the system UID, so fake that
        return PackageInfo().apply {
            sharedUserId = "android.uid.system"
        }
    }

    override fun getPackageInfo(versionedPackage: VersionedPackage, flags: Int): PackageInfo {
        throw RuntimeException("Not implemented")
    }

    override fun currentToCanonicalPackageNames(packageNames: Array<out String>): Array<String> {
        throw RuntimeException("Not implemented")
    }

    override fun canonicalToCurrentPackageNames(packageNames: Array<out String>): Array<String> {
        throw RuntimeException("Not implemented")
    }

    override fun getLaunchIntentForPackage(packageName: String): Intent? {
        throw RuntimeException("Not implemented")
    }

    override fun getLeanbackLaunchIntentForPackage(packageName: String): Intent? {
        throw RuntimeException("Not implemented")
    }

    override fun getPackageGids(packageName: String): IntArray {
        throw RuntimeException("Not implemented")
    }

    override fun getPackageGids(packageName: String, flags: Int): IntArray {
        throw RuntimeException("Not implemented")
    }

    override fun getPackageUid(packageName: String, flags: Int): Int {
        throw RuntimeException("Not implemented")
    }

    override fun getPermissionInfo(permName: String, flags: Int): PermissionInfo {
        throw RuntimeException("Not implemented")
    }

    override fun queryPermissionsByGroup(
        permissionGroup: String?,
        flags: Int
    ): MutableList<PermissionInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun getPermissionGroupInfo(groupName: String, flags: Int): PermissionGroupInfo {
        throw RuntimeException("Not implemented")
    }

    override fun getAllPermissionGroups(flags: Int): MutableList<PermissionGroupInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo {
        throw RuntimeException("Not implemented")
    }

    override fun getActivityInfo(component: ComponentName, flags: Int): ActivityInfo {
        throw RuntimeException("Not implemented")
    }

    override fun getReceiverInfo(component: ComponentName, flags: Int): ActivityInfo {
        throw RuntimeException("Not implemented")
    }

    override fun getServiceInfo(component: ComponentName, flags: Int): ServiceInfo {
        throw RuntimeException("Not implemented")
    }

    override fun getProviderInfo(component: ComponentName, flags: Int): ProviderInfo {
        throw RuntimeException("Not implemented")
    }

    override fun getInstalledPackages(flags: Int): MutableList<PackageInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun getPackagesHoldingPermissions(
        permissions: Array<out String>,
        flags: Int
    ): MutableList<PackageInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun checkPermission(permName: String, packageName: String): Int {
        throw RuntimeException("Not implemented")
    }

    override fun isPermissionRevokedByPolicy(permName: String, packageName: String): Boolean {
        throw RuntimeException("Not implemented")
    }

    override fun addPermission(info: PermissionInfo): Boolean {
        throw RuntimeException("Not implemented")
    }

    override fun addPermissionAsync(info: PermissionInfo): Boolean {
        throw RuntimeException("Not implemented")
    }

    override fun removePermission(permName: String) {
        throw RuntimeException("Not implemented")
    }

    override fun checkSignatures(packageName1: String, packageName2: String): Int {
        throw RuntimeException("Not implemented")
    }

    override fun checkSignatures(uid1: Int, uid2: Int): Int {
        throw RuntimeException("Not implemented")
    }

    override fun getPackagesForUid(uid: Int): Array<String>? {
        throw RuntimeException("Not implemented")
    }

    override fun getNameForUid(uid: Int): String? {
        throw RuntimeException("Not implemented")
    }

    override fun getInstalledApplications(flags: Int): MutableList<ApplicationInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun isInstantApp(): Boolean {
        throw RuntimeException("Not implemented")
    }

    override fun isInstantApp(packageName: String): Boolean {
        throw RuntimeException("Not implemented")
    }

    override fun getInstantAppCookieMaxBytes(): Int {
        throw RuntimeException("Not implemented")
    }

    override fun getInstantAppCookie(): ByteArray {
        throw RuntimeException("Not implemented")
    }

    override fun clearInstantAppCookie() {
        throw RuntimeException("Not implemented")
    }

    override fun updateInstantAppCookie(cookie: ByteArray?) {
        throw RuntimeException("Not implemented")
    }

    override fun getSystemSharedLibraryNames(): Array<String>? {
        throw RuntimeException("Not implemented")
    }

    override fun getSharedLibraries(flags: Int): MutableList<SharedLibraryInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun getChangedPackages(sequenceNumber: Int): ChangedPackages? {
        throw RuntimeException("Not implemented")
    }

    override fun getSystemAvailableFeatures(): Array<FeatureInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun hasSystemFeature(featureName: String): Boolean {
        throw RuntimeException("Not implemented")
    }

    override fun hasSystemFeature(featureName: String, version: Int): Boolean {
        throw RuntimeException("Not implemented")
    }

    override fun resolveActivity(intent: Intent, flags: Int): ResolveInfo? {
        throw RuntimeException("Not implemented")
    }

    override fun queryIntentActivities(intent: Intent, flags: Int): MutableList<ResolveInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun queryIntentActivityOptions(
        caller: ComponentName?,
        specifics: Array<out Intent>?,
        intent: Intent,
        flags: Int
    ): MutableList<ResolveInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun queryBroadcastReceivers(intent: Intent, flags: Int): MutableList<ResolveInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun resolveService(intent: Intent, flags: Int): ResolveInfo? {
        throw RuntimeException("Not implemented")
    }

    override fun queryIntentServices(intent: Intent, flags: Int): MutableList<ResolveInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun queryIntentContentProviders(intent: Intent, flags: Int): MutableList<ResolveInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun resolveContentProvider(authority: String, flags: Int): ProviderInfo? {
        throw RuntimeException("Not implemented")
    }

    override fun queryContentProviders(
        processName: String?,
        uid: Int,
        flags: Int
    ): MutableList<ProviderInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun getInstrumentationInfo(className: ComponentName, flags: Int): InstrumentationInfo {
        throw RuntimeException("Not implemented")
    }

    override fun queryInstrumentation(
        targetPackage: String,
        flags: Int
    ): MutableList<InstrumentationInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun getDrawable(
        packageName: String,
        resid: Int,
        appInfo: ApplicationInfo?
    ): Drawable? {
        throw RuntimeException("Not implemented")
    }

    override fun getActivityIcon(activityName: ComponentName): Drawable {
        throw RuntimeException("Not implemented")
    }

    override fun getActivityIcon(intent: Intent): Drawable {
        throw RuntimeException("Not implemented")
    }

    override fun getActivityBanner(activityName: ComponentName): Drawable? {
        throw RuntimeException("Not implemented")
    }

    override fun getActivityBanner(intent: Intent): Drawable? {
        throw RuntimeException("Not implemented")
    }

    override fun getDefaultActivityIcon(): Drawable {
        throw RuntimeException("Not implemented")
    }

    override fun getApplicationIcon(info: ApplicationInfo): Drawable {
        throw RuntimeException("Not implemented")
    }

    override fun getApplicationIcon(packageName: String): Drawable {
        throw RuntimeException("Not implemented")
    }

    override fun getApplicationBanner(info: ApplicationInfo): Drawable? {
        throw RuntimeException("Not implemented")
    }

    override fun getApplicationBanner(packageName: String): Drawable? {
        throw RuntimeException("Not implemented")
    }

    override fun getActivityLogo(activityName: ComponentName): Drawable? {
        throw RuntimeException("Not implemented")
    }

    override fun getActivityLogo(intent: Intent): Drawable? {
        throw RuntimeException("Not implemented")
    }

    override fun getApplicationLogo(info: ApplicationInfo): Drawable? {
        throw RuntimeException("Not implemented")
    }

    override fun getApplicationLogo(packageName: String): Drawable? {
        throw RuntimeException("Not implemented")
    }

    override fun getUserBadgedIcon(drawable: Drawable, user: UserHandle): Drawable {
        throw RuntimeException("Not implemented")
    }

    override fun getUserBadgedDrawableForDensity(
        drawable: Drawable,
        user: UserHandle,
        badgeLocation: Rect?,
        badgeDensity: Int
    ): Drawable {
        throw RuntimeException("Not implemented")
    }

    override fun getUserBadgedLabel(label: CharSequence, user: UserHandle): CharSequence {
        throw RuntimeException("Not implemented")
    }

    override fun getText(
        packageName: String,
        resid: Int,
        appInfo: ApplicationInfo?
    ): CharSequence? {
        throw RuntimeException("Not implemented")
    }

    override fun getXml(
        packageName: String,
        resid: Int,
        appInfo: ApplicationInfo?
    ): XmlResourceParser? {
        throw RuntimeException("Not implemented")
    }

    override fun getApplicationLabel(info: ApplicationInfo): CharSequence {
        throw RuntimeException("Not implemented")
    }

    override fun getResourcesForActivity(activityName: ComponentName): Resources {
        throw RuntimeException("Not implemented")
    }

    override fun getResourcesForApplication(app: ApplicationInfo): Resources {
        throw RuntimeException("Not implemented")
    }

    override fun getResourcesForApplication(packageName: String): Resources {
        throw RuntimeException("Not implemented")
    }

    override fun verifyPendingInstall(id: Int, verificationCode: Int) {
        throw RuntimeException("Not implemented")
    }

    override fun extendVerificationTimeout(
        id: Int,
        verificationCodeAtTimeout: Int,
        millisecondsToDelay: Long
    ) {
        throw RuntimeException("Not implemented")
    }

    override fun setInstallerPackageName(targetPackage: String, installerPackageName: String?) {
        throw RuntimeException("Not implemented")
    }

    override fun getInstallerPackageName(packageName: String): String? {
        throw RuntimeException("Not implemented")
    }

    override fun addPackageToPreferred(packageName: String) {
        throw RuntimeException("Not implemented")
    }

    override fun removePackageFromPreferred(packageName: String) {
        throw RuntimeException("Not implemented")
    }

    override fun getPreferredPackages(flags: Int): MutableList<PackageInfo> {
        throw RuntimeException("Not implemented")
    }

    override fun addPreferredActivity(
        filter: IntentFilter,
        match: Int,
        set: Array<out ComponentName>?,
        activity: ComponentName
    ) {
        throw RuntimeException("Not implemented")
    }

    override fun clearPackagePreferredActivities(packageName: String) {
        throw RuntimeException("Not implemented")
    }

    override fun getPreferredActivities(
        outFilters: MutableList<IntentFilter>,
        outActivities: MutableList<ComponentName>,
        packageName: String?
    ): Int {
        throw RuntimeException("Not implemented")
    }

    override fun setComponentEnabledSetting(
        componentName: ComponentName,
        newState: Int,
        flags: Int
    ) {
        throw RuntimeException("Not implemented")
    }

    override fun getComponentEnabledSetting(componentName: ComponentName): Int {
        throw RuntimeException("Not implemented")
    }

    override fun setApplicationEnabledSetting(packageName: String, newState: Int, flags: Int) {
        throw RuntimeException("Not implemented")
    }

    override fun getApplicationEnabledSetting(packageName: String): Int {
        throw RuntimeException("Not implemented")
    }

    override fun isSafeMode(): Boolean {
        throw RuntimeException("Not implemented")
    }

    override fun setApplicationCategoryHint(packageName: String, categoryHint: Int) {
        throw RuntimeException("Not implemented")
    }

    override fun getPackageInstaller(): PackageInstaller {
        throw RuntimeException("Not implemented")
    }

    override fun canRequestPackageInstalls(): Boolean {
        throw RuntimeException("Not implemented")
    }

}