package com.kieronquinn.app.utag.networking.model.github

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModRelease(
    @SerializedName("version")
    val version: String,
    @SerializedName("version_code")
    val versionCode: Int,
    @SerializedName("download")
    val download: String,
    @SerializedName("download_alt")
    val downloadAlt: String,
    @SerializedName("download_filename")
    val downloadFilename: String,
    @SerializedName("release_notes")
    val releaseNotes: String,
    @SerializedName("file_size")
    val fileSize: Long
): Parcelable
