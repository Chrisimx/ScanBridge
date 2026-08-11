package io.github.chrisimx.scanbridge.zammadapi.models

import io.github.chrisimx.scanbridge.R
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.created_successfully
import scanbridge.composeui.generated.resources.google_proof_has_wrong_nonce
import scanbridge.composeui.generated.resources.google_proof_has_wrong_package_name
import scanbridge.composeui.generated.resources.google_proof_has_wrong_response_code
import scanbridge.composeui.generated.resources.google_proof_is_invalid
import scanbridge.composeui.generated.resources.google_proof_signature_is_invalid
import scanbridge.composeui.generated.resources.vip_creation_account_unable_to_be_created
import scanbridge.composeui.generated.resources.vip_creation_challenge_expired
import scanbridge.composeui.generated.resources.vip_creation_empty_email_or_name
import scanbridge.composeui.generated.resources.vip_creation_google_proof_is_from_the_future
import scanbridge.composeui.generated.resources.vip_creation_google_proof_is_not_recent_enough
import scanbridge.composeui.generated.resources.vip_creation_purchase_was_already_used
import scanbridge.composeui.generated.resources.vip_creation_token_is_invalid

enum class VIPCreationResult {
    Success,
    TokenNotFound,
    ChallengeExpired,
    InvalidSignature,
    GoogleProofInvalid,
    WrongNonce,
    WrongPackageName,
    WrongGoogleResponseCode,
    PurchaseAlreadyUsed,
    GoogleProofTooOld,
    GoogleProofFromTheFuture,
    AccountCouldntBeCreated,
    EmptyEmailOrName,
}

data class JsonWrappedResult<T>(val result: T)

suspend fun VIPCreationResult.toLocalizedString(): String {
    return when (this) {
        VIPCreationResult.Success ->
            getString(Res.string.created_successfully)

        VIPCreationResult.TokenNotFound ->
            getString(Res.string.vip_creation_token_is_invalid)

        VIPCreationResult.ChallengeExpired ->
            getString(Res.string.vip_creation_challenge_expired)

        VIPCreationResult.InvalidSignature ->
            getString(Res.string.google_proof_signature_is_invalid)

        VIPCreationResult.GoogleProofInvalid ->
            getString(Res.string.google_proof_is_invalid)

        VIPCreationResult.WrongNonce ->
            getString(Res.string.google_proof_has_wrong_nonce)

        VIPCreationResult.WrongPackageName ->
            getString(Res.string.google_proof_has_wrong_package_name)

        VIPCreationResult.WrongGoogleResponseCode ->
            getString(Res.string.google_proof_has_wrong_response_code)

        VIPCreationResult.PurchaseAlreadyUsed ->
            getString(Res.string.vip_creation_purchase_was_already_used)

        VIPCreationResult.GoogleProofTooOld ->
            getString(Res.string.vip_creation_google_proof_is_not_recent_enough)

        VIPCreationResult.GoogleProofFromTheFuture ->
            getString(Res.string.vip_creation_google_proof_is_from_the_future)

        VIPCreationResult.AccountCouldntBeCreated ->
            getString(Res.string.vip_creation_account_unable_to_be_created)

        VIPCreationResult.EmptyEmailOrName ->
            getString(Res.string.vip_creation_empty_email_or_name)
    }
}
