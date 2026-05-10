package com._202510007517.major_assignment.exception;

/**
 * 下游服务返回 5xx 时在本地封装的异常类型。
 * <p>
 * 对齐 Design §Error Handling §2 / §3：{@code GlobalFeignErrorDecoder} 在遇到
 * {@code [500, 599]} 响应时抛出此异常，由 {@code GlobalExceptionHandler} 映射为 502。
 * </p>
 */
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

    /** 下游响应码（原始 {@code [500, 599]} 值）；在未提供时兜底为 {@value #DEFAULT_CODE}。 */
    public int getCode() {
        return code;
    }

    /** 下游原始响应体 JSON。 */
    public String getBodyJson() {
        return bodyJson;
    }
}
