
package io.github.chrisimx.scanbridge;

import com.google.android.vending.licensing.LicenseValidationResultCode
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ResponseData

/**
 * Non-caching policy. All requests will be sent to the licensing service,
 * and no local caching is performed.
 * <p>
 * Using a non-caching policy ensures that there is no local preference data
 * for malicious users to tamper with. As a side effect, applications
 * will not be permitted to run while offline. Developers should carefully
 * weigh the risks of using this Policy over one which implements caching,
 * such as ServerManagedPolicy.
 * <p>
 * Access to the application is only allowed if a LICENSED response is.
 * received. All other responses (including RETRY) will deny access.
 */

class SimpleStrictLicensePolicy : Policy {
    var lastResponse: LicensingRequestResult? = null
        private set

    /**
     * Process a new response from the license server. Since we aren't
     * performing any caching, this equates to reading the LicenseResponse.
     *
     * @param response the result from validating the server response
     * @param rawData the raw server response data
     */
    override fun processServerResponse(response: Int, rawData: ResponseData?, signature: String?, actualError: LicenseValidationResultCode) {
        if (response == Policy.LICENSED) {
            if (rawData == null || signature == null) { // Should be impossible
                lastResponse = null
                return
            }
            lastResponse = LicensingRequestResult.OwnershipProof(rawData, signature)
        } else {
            lastResponse = LicensingRequestResult.Error(actualError)
        }
    }

    /**
     * {@inheritDoc}
     *
     * This implementation allows access if and only if a LICENSED response
     * was received the last time the server was contacted.
     */
    override fun allowAccess(): Boolean {
        return lastResponse is LicensingRequestResult.OwnershipProof
    }

    override fun getLicensingUrl(): String? {
        return null
    }
}

sealed class LicensingRequestResult {
    data class OwnershipProof(val responseData: ResponseData, val signature: String) : LicensingRequestResult()
    data class Error(val errorCode: LicenseValidationResultCode) : LicensingRequestResult()
}
