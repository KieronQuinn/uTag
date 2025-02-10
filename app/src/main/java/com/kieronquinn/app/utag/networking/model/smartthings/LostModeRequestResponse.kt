package com.kieronquinn.app.utag.networking.model.smartthings

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.R

data class LostModeRequestResponse(
    @SerializedName("enabled")
    val enabled: Boolean,
    @SerializedName("messageType")
    val messageType: MessageType,
    @SerializedName("message")
    val message: String?,
    @SerializedName("phoneNumber")
    val phoneNumber: String?,
    @SerializedName("email")
    val email: String?
) {

    enum class MessageType {
        @SerializedName("PREDEFINED")
        PREDEFINED,
        @SerializedName("CUSTOM")
        CUSTOM
    }

    fun getPredefinedMessage(): PredefinedMessage? {
        if(messageType != MessageType.PREDEFINED) return null
        return PredefinedMessage.fromMessage(message ?: return null)
    }

    /**
     *  Predefined messages whose names are sent in [message] with [MessageType.PREDEFINED], the
     *  [displayContent] is not sent to the server and therefore localised translations will be
     *  displayed where possible.
     */
    enum class PredefinedMessage(@StringRes val displayContent: Int) {
        DREAM_SACP_BODY_THIS_SMARTTAG_HAS_BEEN_LOST(
            R.string.this_smarttag_has_been_lost
        ),
        DREAM_SACP_BODY_THIS_IS_A_LOST_ITEM_THATS_IMPORTANT_TO_ME_SO_PLEASE_HELP_ME_GET_IT_BACK(
            R.string.this_is_a_lost_item
        ),
        DREAM_SACP_BODY_IM_TRACKING_THIS_ITEM_AND_TRYING_TO_FIND_IT_SO_PLEASE_CONTACT_ME(
            R.string.im_tracking_this_item
        ),
        DREAM_SACP_BODY_PLEASE_CONTACT_ME_AND_HELP_MY_LOST_PET_COME_BACK_HOME(
            R.string.help_my_lost_pet_come_back_home
        );

        companion object {
            fun fromMessage(message: String): PredefinedMessage? {
                return entries.firstOrNull { it.name == message }
            }
        }
    }

}
