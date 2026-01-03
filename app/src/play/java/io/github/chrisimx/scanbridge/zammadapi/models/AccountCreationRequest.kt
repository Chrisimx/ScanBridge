package io.github.chrisimx.scanbridge.zammadapi.models

data class AccountCreationRequest(
    val name: String,
    val email: String,
    val token: String,
    val google_proof: String,
    val google_proof_signature: String
)
