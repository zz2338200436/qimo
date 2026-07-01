package com._202510007517.platform.common.exception;

public class RemoteServerException extends RuntimeException {

    private static final int DEFAULT_CODE = 502;

    private final int code;
    private final String bodyJson;

    public RemoteServerException(int code, String message, String bodyJson) {
        super(message);
        this.code = code <= 0 ? DEFAULT_CODE : code;
        this.bodyJson = bodyJson;
    }

    public RemoteServerException(String message, String bodyJson) {
        this(DEFAULT_CODE, message, bodyJson);
    }

    public int getCode() {
        return code;
    }

    public String getBodyJson() {
        return bodyJson;
    }
}
