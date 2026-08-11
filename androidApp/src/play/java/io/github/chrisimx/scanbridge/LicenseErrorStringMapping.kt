package io.github.chrisimx.scanbridge

import android.content.Context
import com.google.android.vending.licensing.LicenseValidationResultCode
import org.jetbrains.compose.resources.getString
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.application_license_check_permission_missing
import scanbridge.composeui.generated.resources.application_license_public_key_invalid
import scanbridge.composeui.generated.resources.google_license_server_failure
import scanbridge.composeui.generated.resources.google_license_server_over_quota
import scanbridge.composeui.generated.resources.google_license_server_unreachable
import scanbridge.composeui.generated.resources.google_license_user_undetermined
import scanbridge.composeui.generated.resources.google_licensing_server_invalid_response
import scanbridge.composeui.generated.resources.google_play_services_not_running
import scanbridge.composeui.generated.resources.licensed
import scanbridge.composeui.generated.resources.licensed_but_data_got_lost
import scanbridge.composeui.generated.resources.licensed_package_has_invalid_name
import scanbridge.composeui.generated.resources.licensed_package_not_on_google_play
import scanbridge.composeui.generated.resources.licensing_unknown_server_response_code
import scanbridge.composeui.generated.resources.not_licensed

suspend fun LicenseValidationResultCode.asLocalizedString(): String = when (this) {
    LicenseValidationResultCode.LICENSED -> getString(Res.string.licensed)
    LicenseValidationResultCode.NOT_LICENSED -> getString(Res.string.not_licensed)
    LicenseValidationResultCode.LICENSED_OLD_KEY -> getString(Res.string.licensed)
    LicenseValidationResultCode.ERROR_NOT_MARKET_MANAGED -> getString(Res.string.licensed_package_not_on_google_play)
    LicenseValidationResultCode.ERROR_SERVER_FAILURE -> getString(Res.string.google_license_server_failure)
    LicenseValidationResultCode.ERROR_OVER_QUOTA -> getString(Res.string.google_license_server_over_quota)
    LicenseValidationResultCode.ERROR_CONTACTING_SERVER -> getString(Res.string.google_license_server_unreachable)
    LicenseValidationResultCode.ERROR_INVALID_PACKAGE_NAME -> getString(Res.string.licensed_package_has_invalid_name)
    LicenseValidationResultCode.ERROR_NON_MATCHING_UID -> getString(Res.string.google_license_user_undetermined)
    LicenseValidationResultCode.GOOGLE_PLAY_SERVICE_CONNECTION_FAILED -> getString(Res.string.google_play_services_not_running)
    LicenseValidationResultCode.RESPONSE_DATA_UNEXPECTEDLY_NULL -> getString(Res.string.licensed_but_data_got_lost)
    LicenseValidationResultCode.ERROR_INVALID_PUBLIC_KEY -> getString(Res.string.application_license_public_key_invalid)
    LicenseValidationResultCode.ERROR_MISSING_PERMISSION -> getString(Res.string.application_license_check_permission_missing)
    LicenseValidationResultCode.ERROR_INVALID_SERVER_RESPONSE -> getString(Res.string.google_licensing_server_invalid_response)
    LicenseValidationResultCode.UNKNOWN_RESPONSE_CODE -> getString(Res.string.licensing_unknown_server_response_code)
}
