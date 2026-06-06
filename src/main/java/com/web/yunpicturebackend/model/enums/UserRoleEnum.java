package com.web.yunpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRoleEnum {
  USER("user", "用户"),
  ADMIN("admin", "管理员");

  private final String text;
  private final String value;

  UserRoleEnum(String text, String value) {
    this.text = text;
    this.value = value;
  }

  public static UserRoleEnum getEnumByValue(String value) {
    if (ObjUtil.isEmpty(value)) {
      return null;
    }
    for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
      if (userRoleEnum.getValue().equals(value)) {
        return userRoleEnum;
      }
    }
    return null;
  }

}
