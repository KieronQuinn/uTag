package com.kieronquinn.app.utag.xposed.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 *  Based on https://github.com/LSPosed/LSPatch/blob/master/patch-loader/src/main/java/org/lsposed/lspatch/loader/SigBypass.java
 *
 *  Moved into module directly due to the need to modify the Manifest, which breaks the automatic
 *  injection.
 */
@Suppress("DEPRECATION")
object SigBypass {

    private const val TAG = "LSPatch-SigBypass"
    private const val ORIGINAL_SIGNATURE = "308202e4308201cc020101300d06092a864886f70d010105050030373116301406035504030c0d416e64726f69642044656275673110300e060355040a0c07416e64726f6964310b30090603550406130255533020170d3232303631393030313934325a180f32303532303631313030313934325a30373116301406035504030c0d416e64726f69642044656275673110300e060355040a0c07416e64726f6964310b300906035504061302555330820122300d06092a864886f70d01010105000382010f003082010a0282010100a61a521b62e8893d2bcb7d043a2636cd4e5e47d1c1969651bf15820101c1f0f041901270dc1b02087c76164be23c2605894263b30e4cc05a2fd0f7a43240bb1ac657e81371778d887bd6ec51f1d1c58bdf5a3abffa6f4fc933f8359ac9965778800e2b7cd75db3fd13e9bb966b801491d067c6463cb0df337861297813365988deefbf5f355bc7e688ca9f96a7328799be8656e0b7e1dab62b44ad99d7210f22e21b0e84eec3a3fccb3b2870a36eb16ca1f80f775d43c78cbbff3a1cfa40eaac61f012f3599eddc55b609f50f913ce42d998175eba987e18292ad4654d95bd67f859c53b3f24502e95aae7ebc883dd3af2097554b07453f5cf308dc5b60f71b10203010001300d06092a864886f70d010105050003820101009cb2f73ec4123d68ef6e7a8a46d4bdce74a919494df0985dd11d1eed8f07aad31a1cee26f3cd970a18774b0526189928ec0ad37c6f78849ea38680c1e88d3569f0bf6da3128c3246651c147348f67230b9c9ced2cb96050eb391fca17d7f5cb25ee39593ab681f37c7edddd791c7ff0201c1a7b6745884a6010d986cac7362551d93814825d5bf3868c5747cf529febb4a87d080448594abd44806ac19fe8514e7e772f693aeff73ca53496bd9035701c9e86318d369c67f10656edda8745ce29aad0a989ba00c1c279b6c5a459c417339dd46ea2a3fd9c4955bf224f9120bb3a0d85704fb2a0095dc66bc4a4cbd3a388042b9ffadc54f0ca121ca731c8afa0d"
    private val signatures: MutableMap<String?, String?> = HashMap()

    private fun replaceSignature(
        context: Context,
        packageInfo: PackageInfo
    ) {
        val hasSignature =
            (packageInfo.signatures != null && packageInfo.signatures!!.size != 0) || packageInfo.signingInfo != null
        if (hasSignature) {
            val packageName = packageInfo.packageName
            var replacement = signatures[packageName]
            if (replacement == null && !signatures.containsKey(packageName)) {
                try {
                    val metaData = context.packageManager.getApplicationInfo(
                        packageName,
                        PackageManager.GET_META_DATA
                    ).metaData
                    var encoded: String? = null
                    if (metaData != null) encoded = metaData.getString("lspatch")
                    if (encoded != null) {
                        val json = String(
                            Base64.decode(
                                encoded,
                                Base64.DEFAULT
                            ), StandardCharsets.UTF_8
                        )
                        replacement = ORIGINAL_SIGNATURE
                    }
                } catch (ignored: PackageManager.NameNotFoundException) {}
                signatures.put(packageName, replacement)
            }
            if (replacement != null) {
                if (packageInfo.signatures != null && packageInfo.signatures!!.size > 0) {
                    Log.d(TAG, "Replace signature info for `$packageName` (method 1)")
                    packageInfo.signatures!![0] = Signature(replacement)
                }
                if (packageInfo.signingInfo != null) {
                    Log.d(TAG, "Replace signature info for `$packageName` (method 2)")
                    val signaturesArray = packageInfo.signingInfo!!.apkContentsSigners
                    if (signaturesArray != null && signaturesArray.size > 0) {
                        signaturesArray[0] = Signature(replacement)
                    }
                }
            }
        }
    }

    private fun hookPackageParser(context: Context, classLoader: ClassLoader) {
        val clazz = XposedHelpers.findClass("android.content.pm.PackageParser", classLoader)
        XposedBridge.hookAllMethods(
            clazz,
            "generatePackageInfo",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val packageInfo = param.result as PackageInfo?
                    if (packageInfo == null) return
                    replaceSignature(context, packageInfo)
                }
            })
    }

    private fun proxyPackageInfoCreator(context: Context) {
        val originalCreator = PackageInfo.CREATOR
        val proxiedCreator: Parcelable.Creator<PackageInfo?> =
            object : Parcelable.Creator<PackageInfo?> {
                override fun createFromParcel(source: Parcel?): PackageInfo {
                    val packageInfo = originalCreator.createFromParcel(source)
                    replaceSignature(context, packageInfo)
                    return packageInfo
                }

                override fun newArray(size: Int): Array<PackageInfo?>? {
                    return originalCreator.newArray(size)
                }
            }
        XposedHelpers.setStaticObjectField(
            PackageInfo::class.java,
            "CREATOR",
            proxiedCreator
        )
        try {
            val mCreators = XposedHelpers.getStaticObjectField(
                Parcel::class.java,
                "mCreators"
            ) as MutableMap<*, *>
            mCreators.clear()
        } catch (ignore: NoSuchFieldError) {
        } catch (e: Throwable) {
            Log.w(TAG, "fail to clear Parcel.mCreators", e)
        }
        try {
            val sPairedCreators = XposedHelpers.getStaticObjectField(
                Parcel::class.java,
                "sPairedCreators"
            ) as MutableMap<*, *>
            sPairedCreators.clear()
        } catch (ignore: NoSuchFieldError) {
        } catch (e: Throwable) {
            Log.w(TAG, "fail to clear Parcel.sPairedCreators", e)
        }
    }

    @Throws(IOException::class)
    fun doSigBypass(context: Context, classLoader: ClassLoader) {
        hookPackageParser(context, classLoader)
        proxyPackageInfoCreator(context)
    }
}
