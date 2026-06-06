package com.web.yunpicturebackend.common;

import java.io.Serializable;

import com.web.yunpicturebackend.exception.ErrorCode;

/**
 * 通用返回类
 *
 * @param <T>
 */
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 7898594227894461723L;

    private final int code;
    private final T data;
    private final String message;

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }

    public int getCode() {
        return code;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
