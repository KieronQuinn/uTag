package com.kieronquinn.app.utag.utils.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.GeneralSecurityException
import java.security.KeyStore

fun Context.createEncryptedSharedPrefDestructively(
    fileName: String, onDelete: () -> Unit = {}
): SharedPreferences {
    return try {
        createEncryptedSharedPrefs(fileName)
    } catch (e: GeneralSecurityException) {
        deleteMasterKeyEntry()
        deleteExistingPref(fileName)
        onDelete()
        createEncryptedSharedPrefs(fileName)
    }
}

@SuppressLint("ApplySharedPref")
private fun Context.deleteExistingPref(fileName: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        deleteSharedPreferences(fileName)
    } else {
        getSharedPreferences(fileName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}

private fun deleteMasterKeyEntry() {
    KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
        deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
    }
}

private fun Context.createEncryptedSharedPrefs(fileName: String): SharedPreferences {
    val masterKey = MasterKey.Builder(this, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        this,
        fileName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}