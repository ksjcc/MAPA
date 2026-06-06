package com.web.yunpicturebackend.common;

import com.web.yunpicturebackend.exception.ErrorCode;

/**
 * 响应工具类
 */
public class ResultUtils {
  public static <T> BaseResponse<T> success(T data) {
    return new BaseResponse<>(0, data, "ok");
  }

  public static <T> BaseResponse<T> error(int code, String message) {
    return new BaseResponse<>(code, null, message);
  }

  public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
    return new BaseResponse<>(errorCode.getCode(), null, message);
  }

}
