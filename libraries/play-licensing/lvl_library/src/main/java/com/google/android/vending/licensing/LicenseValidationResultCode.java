package com.google.android.vending.licensing;

// Server response codes.
public enum LicenseValidationResultCode {
    LICENSED(0x0),
    NOT_LICENSED(0x1),
    LICENSED_OLD_KEY(0x2),
    ERROR_NOT_MARKET_MANAGED(0x3),
    ERROR_SERVER_FAILURE(0x4),
    ERROR_OVER_QUOTA(0x5),
    ERROR_CONTACTING_SERVER(0x101),
    ERROR_INVALID_PACKAGE_NAME(0x102),
    ERROR_NON_MATCHING_UID(0x103),
    GOOGLE_PLAY_SERVICE_CONNECTION_FAILED(0x851),
    RESPONSE_DATA_UNEXPECTEDLY_NULL(0x341),
    ERROR_INVALID_PUBLIC_KEY(0x501),
    ERROR_MISSING_PERMISSION(0x502),
    ERROR_INVALID_SERVER_RESPONSE(0x503),
    UNKNOWN_RESPONSE_CODE(0x999);

    private final int code;
    LicenseValidationResultCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
    public static LicenseValidationResultCode fromCode(int code) {
        for (LicenseValidationResultCode error : values()) {
            if (error.code == code) {
                return error;
            }
        }
        return LicenseValidationResultCode.UNKNOWN_RESPONSE_CODE;
    }
}