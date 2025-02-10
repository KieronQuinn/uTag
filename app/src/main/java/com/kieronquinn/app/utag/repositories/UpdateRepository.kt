package com.kieronquinn.app.utag.repositories

import android.content.Context
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.model.Release
import com.kieronquinn.app.utag.networking.model.github.ModRelease
import com.kieronquinn.app.utag.networking.services.GitHubRawService
import com.kieronquinn.app.utag.networking.services.GitHubReleasesService
import com.kieronquinn.app.utag.repositories.UpdateRepository.Companion.CONTENT_TYPE_APK
import com.kieronquinn.app.utag.utils.extensions.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit

interface UpdateRepository {

    companion object {
        const val CONTENT_TYPE_APK = "application/vnd.android.package-archive"
    }

    suspend fun getUpdate(currentTag: String = BuildConfig.TAG_NAME): Release?
    suspend fun getModRelease(): ModRelease?

}

class UpdateRepositoryImpl(context: Context, retrofit: Retrofit): UpdateRepository {

    private val gitHubReleasesService = GitHubReleasesService.createService(retrofit, context)
    private val gitHubRawService = GitHubRawService.createService(retrofit, context)

    override suspend fun getUpdate(currentTag: String): Release? = withContext(Dispatchers.IO) {
        val releasesResponse = try {
            gitHubReleasesService.getReleases().execute()
        }catch (e: Exception) {
            return@withContext null
        }
        if(!releasesResponse.isSuccessful) return@withContext null
        val newestRelease = releasesResponse.body()?.firstOrNull() ?: return@withContext null
        if(newestRelease.tag == null || newestRelease.tag == currentTag) return@withContext null
        //Found a new release
        val versionName = newestRelease.versionName ?: return@withContext null
        val asset = newestRelease.assets?.firstOrNull { it.contentType == CONTENT_TYPE_APK }
            ?: return@withContext null
        val downloadUrl = asset.downloadUrl ?: return@withContext null
        val fileName = asset.fileName ?: return@withContext null
        val gitHubUrl = newestRelease.gitHubUrl ?: return@withContext null
        val body = newestRelease.body ?: return@withContext null
        return@withContext Release(
            newestRelease.tag, versionName, downloadUrl, fileName, gitHubUrl, body
        )
    }

    override suspend fun getModRelease(): ModRelease? {
        return gitHubRawService.getRelease().get(name = "modRelease")
    }

}