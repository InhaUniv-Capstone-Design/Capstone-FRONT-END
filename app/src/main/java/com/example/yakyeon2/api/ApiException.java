package com.example.yakyeon2.api;

public final class ApiException extends Exception {

    private final int status;
    private final String code;
    private final boolean network;
    private final boolean responseInvalid;
    private final boolean sessionEnded;

    private ApiException(
            int status,
            String code,
            String message,
            boolean network,
            boolean responseInvalid,
            boolean sessionEnded
    ) {
        super(message == null ? "" : message);
        this.status = status;
        this.code = code;
        this.network = network;
        this.responseInvalid = responseInvalid;
        this.sessionEnded = sessionEnded;
    }

    public static ApiException disconnected() {
        return new ApiException(0, null, "", true, false, false);
    }

    public static ApiException response(int status) {
        return new ApiException(status, null, "", false, true, false);
    }

    public static ApiException http(int status, String code, String message) {
        return new ApiException(status, code, message, false, false, ApiCodes.endsSession(code));
    }

    public static ApiException local() {
        return new ApiException(0, null, "", false, false, false);
    }

    public ApiException asSessionEnded() {
        return new ApiException(status, code, getMessage(), network, responseInvalid, true);
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public boolean network() {
        return network;
    }

    public boolean hasCode(String expected) {
        return expected != null && expected.equals(code);
    }

    public boolean endsSession() {
        return sessionEnded;
    }

    public ApiFailure.Kind failure() {
        if (network) {
            return ApiFailure.Kind.NETWORK;
        }
        if (responseInvalid) {
            return ApiFailure.Kind.RESPONSE;
        }
        return ApiFailure.kind(status, code);
    }
}
