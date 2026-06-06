package com.web.yunpicturebackend.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.web.yunpicturebackend.common.ResultUtils;
import com.web.yunpicturebackend.common.BaseResponse;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  @ExceptionHandler(NotLoginException.class)
  public BaseResponse<?> notLoginException(NotLoginException e) {
    log.error("未登录异常", e);
    return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
  }

  @ExceptionHandler(NotPermissionException.class)
  public BaseResponse<?> notPermissionException(NotPermissionException e) {
    log.error("未权限异常", e);
    return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
  }

  @ExceptionHandler(BusinessException.class)
  public BaseResponse<?> businessException(BusinessException e) {
    log.error("业务异常", e);
    return ResultUtils.error(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(RuntimeException.class)
  public BaseResponse<?> businessExceptionHandler(RuntimeException e) {
    log.error("业务时间异常", e);
    return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
  }
}
