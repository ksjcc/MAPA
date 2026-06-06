package com.web.yunpicturebackend.model.dto.user;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class UserAddRequest {
  private String userName;
  private String userAccount;
  private String userAvatar;
  private String userProfile;
  private String userRole;
  private final long serialVersionUID = 1L;
}
