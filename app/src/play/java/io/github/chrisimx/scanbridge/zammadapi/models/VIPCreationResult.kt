package io.github.chrisimx.scanbridge.zammadapi.models

import io.github.chrisimx.scanbridge.R

enum class VIPCreationResult(val message: Int) {
    Success(R.string.created_successfully),
    TokenNotFound(R.string.vip_creation_token_is_invalid),
    ChallengeExpired(R.string.vip_creation_challenge_expired),
    InvalidSignature(R.string.google_proof_signature_is_invalid),
    GoogleProofInvalid(R.string.google_proof_is_invalid),
    WrongNonce(R.string.google_proof_has_wrong_nonce),
    WrongPackageName(R.string.google_proof_has_wrong_package_name),
    WrongGoogleResponseCode(R.string.google_proof_has_wrong_response_code),
    PurchaseAlreadyUsed(R.string.vip_creation_purchase_was_already_used),
    GoogleProofTooOld(R.string.vip_creation_google_proof_is_not_recent_enough),
    GoogleProofFromTheFuture(R.string.vip_creation_google_proof_is_from_the_future),
    AccountCouldntBeCreated(R.string.vip_creation_account_unable_to_be_created),
    EmptyEmailOrName(R.string.vip_creation_empty_email_or_name);
}

data class JsonWrappedResult<T>(
    val result: T
)
