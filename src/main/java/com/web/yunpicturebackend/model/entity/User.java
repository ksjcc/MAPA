package com.web.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@TableName(value = "user")
@Data
public class User {
  /**
   * id
   */
  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /**
   * 账号
   */
  private String userAccount;

  /**
   * 密码
   */
  private String userPassword;

  public void setUserPassword(String userPassword) {
    this.userPassword = userPassword;
  }

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

  public String getUserRole() {
    return userRole;
  }

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
   * 编辑时间
   */
  private Date editTime;

  /**
   * 创建时间
   */
  private Date createTime;

  /**
   * 更新时间
   */
  private Date updateTime;

  /**
   * 是否删除
   */
  @TableLogic
  private Integer isDelete;

  @TableField(exist = false)
  private static final long serialVersionUID = 1L;
}
