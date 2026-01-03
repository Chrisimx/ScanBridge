package io.github.chrisimx.scanbridge

import android.content.Context
import com.google.android.vending.licensing.LicenseChecker
import com.google.android.vending.licensing.LicenseCheckerCallback
import com.google.android.vending.licensing.LicenseValidationResultCode
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

const val LICENSE_PUBLIC_KEY_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsFR5gQWj/KOokXop/zmpi2O+MH7hMjh/1GsbY0JN1v6yecycK6JXG+TCwTWwLWYELQEEZ9D0tYRR3mwxSJRU2NCCSLVjRKMJWE2m5Z45LqkvrDPVUqNcgxlT9E81XmftCSw3R6eMEXcwlJHO10USDDdPgMPByI26Hyigi+MZU1o8jRGafQOwm2QLfaTarYWYfugsmpWKs86YaqKXyp7qsWJrDdf2x6+HeKXQhLcKDTKocCVbrEc6VoZySS8xZLb/thlx7ujnZ3H+88dfy1bhdjxyrvwVQ2ho35y5FvOAYVs8/PgX4C6DDOJ3rdXEi0lD4HWgtpEtnJ8uDlRzR0l6dwIDAQAB"
class OwnershipProofService(val application: Context) {
    val policy = SimpleStrictLicensePolicy()
    val licenseChecker = LicenseChecker(application, policy, LICENSE_PUBLIC_KEY_BASE64)
    suspend fun requestOwnershipProof(nonce: Int): LicensingRequestResult =
        suspendCancellableCoroutine { continuation ->
            val callbackObject = CustomLicenseCallback(policy, continuation)
            licenseChecker.checkAccess(nonce, callbackObject)
        }
}

class CustomLicenseCallback(val policy: SimpleStrictLicensePolicy,
                            val continuation: Continuation<LicensingRequestResult>) : LicenseCheckerCallback {
    override fun allow(reason: LicenseValidationResultCode) {
        val lastResponse = policy.lastResponse

        if (lastResponse is LicensingRequestResult.OwnershipProof) {
            Timber.d("License valid")
            continuation.resume(lastResponse)
            return
        } else if (lastResponse is LicensingRequestResult.Error) {
            Timber.e("License lastResponse is error even though license is marked valid")
            continuation.resume(LicensingRequestResult.Error(reason))
            return
        } else {
            Timber.e("License lastResponse null even though license is marked valid")
            continuation.resume(
                LicensingRequestResult.Error(
                    LicenseValidationResultCode.RESPONSE_DATA_UNEXPECTEDLY_NULL,
                )
            )
            return
        }
    }

    override fun dontAllow(reason: LicenseValidationResultCode) {
        Timber.d("Invalid license. Error $reason")
        continuation.resume(LicensingRequestResult.Error(reason))
    }

    override fun applicationError(errorCode: LicenseValidationResultCode) {
        Timber.d("Application error: $errorCode")
        continuation.resume(LicensingRequestResult.Error(errorCode))
    }

}
