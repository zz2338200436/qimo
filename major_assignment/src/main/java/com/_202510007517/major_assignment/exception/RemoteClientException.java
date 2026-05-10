package com._202510007517.major_assignment.exception;

/**
 * 下游服务返回 4xx 时在本地封装的异常类型。
 * <p>
 * 对齐 Design §Error Handling §2 / §3：{@code GlobalFeignErrorDecoder} 在遇到
 * {@code [400, 499]} 响应时抛出此异常，由 {@code GlobalExceptionHandler} 透传原始
 * HTTP 状态码与下游 {@code ResponseResult.message}（Property 25）。
 * </p>
 */
public class RemoteClientException extends RuntimeException {

    private final int code;
    private final String bodyJson;

    public RemoteClientException(int code, String message, String bodyJson) {
        super(message);
        this.code = code;
        this.bodyJson = bodyJson;
    }

    /** 下游响应码，范围 {@code [400, 499]}；与抛出时的 HTTP 状态码一致。 */
    public int getCode() {
        return code;
    }

    /** 下游原始响应体 JSON，可能为空字符串但不为 {@code null}。 */
    public String getBodyJson() {
        return bodyJson;
    }
}
