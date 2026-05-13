package io.github.chrisimx.scanbridge

import android.content.Context
import com.google.android.vending.licensing.LicenseValidationResultCode

fun LicenseValidationResultCode.asLocalizedString(context: Context): String = when (this) {
    LicenseValidationResultCode.LICENSED -> context.getString(R.string.licensed)
    LicenseValidationResultCode.NOT_LICENSED -> context.getString(R.string.not_licensed)
    LicenseValidationResultCode.LICENSED_OLD_KEY -> context.getString(R.string.licensed)
    LicenseValidationResultCode.ERROR_NOT_MARKET_MANAGED -> context.getString(R.string.licensed_package_not_on_google_play)
    LicenseValidationResultCode.ERROR_SERVER_FAILURE -> context.getString(R.string.google_license_server_failure)
    LicenseValidationResultCode.ERROR_OVER_QUOTA -> context.getString(R.string.google_license_server_over_quota)
    LicenseValidationResultCode.ERROR_CONTACTING_SERVER -> context.getString(R.string.google_license_server_unreachable)
    LicenseValidationResultCode.ERROR_INVALID_PACKAGE_NAME -> context.getString(R.string.licensed_package_has_invalid_name)
    LicenseValidationResultCode.ERROR_NON_MATCHING_UID -> context.getString(R.string.google_license_user_undetermined)
    LicenseValidationResultCode.GOOGLE_PLAY_SERVICE_CONNECTION_FAILED -> context.getString(R.string.google_play_services_not_running)
    LicenseValidationResultCode.RESPONSE_DATA_UNEXPECTEDLY_NULL -> context.getString(R.string.licensed_but_data_got_lost)
    LicenseValidationResultCode.ERROR_INVALID_PUBLIC_KEY -> context.getString(R.string.application_license_public_key_invalid)
    LicenseValidationResultCode.ERROR_MISSING_PERMISSION -> context.getString(R.string.application_license_check_permission_missing)
    LicenseValidationResultCode.ERROR_INVALID_SERVER_RESPONSE -> context.getString(R.string.google_licensing_server_invalid_response)
    LicenseValidationResultCode.UNKNOWN_RESPONSE_CODE -> context.getString(R.string.licensing_unknown_server_response_code)
}
