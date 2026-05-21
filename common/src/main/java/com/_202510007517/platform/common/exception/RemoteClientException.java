package com._202510007517.platform.common.exception;

public class RemoteClientException extends RuntimeException {

    private final int code;
    private final String bodyJson;

    public RemoteClientException(int code, String message, String bodyJson) {
        super(message);
        this.code = code;
        this.bodyJson = bodyJson;
    }

    public int getCode() {
        return code;
    }

    public String getBodyJson() {
        return bodyJson;
    }
}
