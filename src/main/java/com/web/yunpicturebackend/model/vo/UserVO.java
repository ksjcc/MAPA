package com.web.yunpicturebackend.model.vo;

import java.util.Date;

import org.springframework.beans.BeanUtils;

import com.web.yunpicturebackend.model.entity.User;

import lombok.Data;

@Data
public class UserVO {
  /**
   * id
   */
  private Long id;

  /**
   * 账号
   */
  private String userAccount;

  /**
   * 用户昵称
   */
  private String userName;

  /**
   * 用户头像
   */
  private String userAvatar;

  /**
   * 用户简介
   */
  private String userProfile;

  /**
   * 用户角色：user/admin
   */
  private String userRole;

  /**
   * 会员过期时间
   */
  private Date vipExpireTime;

  /**
   * 会员兑换码
   */
  private String vipCode;

  /**
   * 会员编号
   */
  private Long vipNumber;

  /**
   * 创建时间
   */
  private Date createTime;

  private static final long serialVersionUID = 1L;

  public static User voToObj(UserVO userVO) {
    if (userVO == null) {
      return null;
    }
    User user = new User();
    BeanUtils.copyProperties(userVO, user);
    return user;
  }

  public static UserVO objToVo(User user) {
    if (user == null) {
      return null;
    }
    UserVO userVO = new UserVO();
    BeanUtils.copyProperties(user, userVO);
    return userVO;
  }
}
