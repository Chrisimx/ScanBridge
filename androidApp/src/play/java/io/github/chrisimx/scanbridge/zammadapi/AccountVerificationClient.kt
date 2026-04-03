package io.github.chrisimx.scanbridge.zammadapi

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AccountVerificationClient {
    object RetrofitClient {

        private const val BASE_URL = "https://support.fireamp.eu/"

        val api: AccountVerifierApi by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AccountVerifierApi::class.java)
        }
    }
}
