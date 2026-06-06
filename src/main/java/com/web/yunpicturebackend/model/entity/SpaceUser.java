package com.web.yunpicturebackend.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName(value = "space_user")
@Data
public class SpaceUser implements Serializable {
  /**
   * id
   */
  @TableId(type = IdType.AUTO)
  private Long id;

  public Long getId() {
    return id;
  }

  /**
   * 空间 id
   */
  private Long spaceId;

  public Long getSpaceId() {
    return spaceId;
  }

  /**
   * 用户 id
   */
  private Long userId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  /**
   * 空间角色：viewer/editor/admin
   */
  private String spaceRole;

  public String getSpaceRole() {
    return spaceRole;
  }

  public void setSpaceRole(String spaceRole) {
    this.spaceRole = spaceRole;
  }

  /**
   * 创建时间
   */
  private Date createTime;

  /**
   * 更新时间
   */
  private Date updateTime;

  @TableField(exist = false)
  private static final long serialVersionUID = 1L;
}
