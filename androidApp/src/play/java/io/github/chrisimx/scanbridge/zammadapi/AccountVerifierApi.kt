package io.github.chrisimx.scanbridge.zammadapi

import io.github.chrisimx.scanbridge.zammadapi.models.AccountCreationRequest
import io.github.chrisimx.scanbridge.zammadapi.models.GoogleChallenge
import io.github.chrisimx.scanbridge.zammadapi.models.JsonWrappedResult
import io.github.chrisimx.scanbridge.zammadapi.models.VIPCreationResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AccountVerifierApi {

    @POST("/customerExtraApi/userCreate")
    suspend fun createAccount(@Body body: AccountCreationRequest): JsonWrappedResult<VIPCreationResult>

    @GET("/customerExtraApi/googleChallenge")
    suspend fun getGoogleChallenge(): GoogleChallenge
}
