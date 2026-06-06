package com.web.yunpicturebackend.model.dto.user;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class UserLoginRequest {
  private static final long serialVersionUID = 8735650154179439661L;

  /**
   * 账号
   */
  private String userAccount;

  /**
   * 密码
   */
  private String userPassword;

  public String getUserAccount() {
    return userAccount;
  }

  public String getUserPassword() {
    return userPassword;
  }
}
