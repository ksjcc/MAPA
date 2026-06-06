package com.web.yunpicturebackend.apo;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.enums.UserRoleEnum;
import com.web.yunpicturebackend.model.dto.user.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.web.yunpicturebackend.Service.UserService;
import com.web.yunpicturebackend.annotation.AuthCheck;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;

@Aspect
@Component
public class AuthInterceptor {
  @Resource
  private UserService userService;

  @Around("@annotation(authCheck)")
  public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
    String mustRole = authCheck.mustRole();
    RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
    HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
    User loginUser = userService.getLoginUser(request);
    UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
    if (mustRoleEnum == null) {
      return joinPoint.proceed();
    }
    UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
    if (userRoleEnum == null) {
      throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
    }
    if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
      throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
    }
    return joinPoint.proceed();
  }
}
