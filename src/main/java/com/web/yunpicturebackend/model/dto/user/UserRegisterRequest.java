package com.web.yunpicturebackend.model.dto.user;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserRegisterRequest {
  private static final long serialVersionUID = 8735650154179439661L;
  private String userAccount;
  private String userPassword;
  private String checkPassword;

  public String getUserAccount() {
    return userAccount;
  }

  public String getUserPassword() {
    return userPassword;
  }

  public String getCheckPassword() {
    return checkPassword;
  }
}
