package com.kieronquinn.app.utag.networking.model.smartthings

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class Agreement(
    @SerializedName("type")
    val type: String,
    @SerializedName("agreed")
    val agreed: Boolean
) {

    companion object: KoinComponent {
        private val gson by inject<Gson>()

        fun getAgreement(): String {
            return listOf(
                Agreement("service.location", true),
                Agreement("service.pp", true)
            ).let {
                Base64.encodeToString(gson.toJson(it).toByteArray(), Base64.NO_WRAP)
            }
        }
    }

}
