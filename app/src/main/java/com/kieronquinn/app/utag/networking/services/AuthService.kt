package com.kieronquinn.app.utag.networking.services

import com.kieronquinn.app.utag.Application.Companion.CLIENT_ID_LOGIN
import com.kieronquinn.app.utag.networking.model.auth.AuthenticateResponse
import com.kieronquinn.app.utag.networking.model.auth.AuthoriseResponse
import com.kieronquinn.app.utag.networking.model.auth.TokenResponse
import com.kieronquinn.app.utag.utils.extensions.ospClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface AuthService {

    companion object {
        private const val PATH_TOKEN = "/auth/oauth2/token"
        private const val PATH_AUTHENTICATE = "/auth/oauth2/authenticate"
        private const val PATH_AUTHORISE = "/auth/oauth2/v2/authorize"

        fun createService(retrofit: Retrofit): AuthService {
            return retrofit.newBuilder()
                .client(ospClient())
                .build()
                .create(AuthService::class.java)
        }

        private fun String.applyPrefixIfNeeded(): String {
            return if(!startsWith("https://")) {
                "https://$this"
            }else this
        }

        fun AuthService.authenticate(
            baseUrl: String,
            code: String,
            codeVerifier: String,
            username: String,
            id: String
        ) = authenticateInternal(
            url = baseUrl.applyPrefixIfNeeded() + PATH_AUTHENTICATE,
            code = code,
            codeVerifier = codeVerifier,
            username = username,
            physicalAddressText = id
        )

        fun AuthService.authorise(
            baseUrl: String,
            clientId: String,
            userauthToken: String,
            codeChallenge: String,
            physicalAddressText: String,
            scope: String,
            loginId: String
        ) = authoriseInternal(
            url = baseUrl.applyPrefixIfNeeded() + PATH_AUTHORISE,
            clientId = clientId,
            userauthToken = userauthToken,
            codeChallenge = codeChallenge,
            physicalAddressText = physicalAddressText,
            scope = scope,
            loginId = loginId
        )

        fun AuthService.token(
            baseUrl: String,
            clientId: String,
            code: String,
            codeVerifier: String,
            physicalAddressText: String
        ) = tokenInternal(
            url = baseUrl.applyPrefixIfNeeded() + PATH_TOKEN,
            clientId = clientId,
            code = code,
            codeVerifier = codeVerifier,
            physicalAddressText = physicalAddressText
        )

        fun AuthService.token(
            baseUrl: String,
            clientId: String,
            refreshToken: String
        ) = tokenInternal(
            url = baseUrl.applyPrefixIfNeeded() + PATH_TOKEN,
            clientId = clientId,
            refreshToken = refreshToken
        )

    }

    @POST
    @FormUrlEncoded
    fun authenticateInternal(
        @Url url: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("serviceType") serviceType: String = "M",
        @Field("code") code: String,
        @Field("client_id") clientId: String = CLIENT_ID_LOGIN,
        @Field("code_verifier") codeVerifier: String,
        @Field("username") username: String,
        @Field("physical_address_text") physicalAddressText: String
    ): Call<AuthenticateResponse>

    @GET
    fun authoriseInternal(
        @Url url: String,
        @Query("response_type") responseType: String = "code",
        @Query("serviceType") serviceType: String = "M",
        @Query("client_id") clientId: String,
        @Query("code_challenge_method") codeChallengeMethod: String = "S256",
        @Query("childAccountSupported") childAccountSupported: String = "Y",
        @Query("userauth_token") userauthToken: String,
        @Query("code_challenge") codeChallenge: String,
        @Query("physical_address_text") physicalAddressText: String,
        @Query("scope") scope: String,
        @Query("login_id") loginId: String
    ): Call<AuthoriseResponse>

    @POST
    @FormUrlEncoded
    fun tokenInternal(
        @Url url: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("physical_address_text") physicalAddressText: String
    ): Call<TokenResponse>

    @POST
    @FormUrlEncoded
    fun tokenInternal(
        @Url url: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String
    ): Call<TokenResponse>

}