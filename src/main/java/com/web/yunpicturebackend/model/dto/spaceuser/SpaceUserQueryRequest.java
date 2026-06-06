package com.web.yunpicturebackend.model.dto.spaceuser;

import java.io.Serializable;

import lombok.Data;

/**
 * 空间用户查询请求
 */
@Data
public class SpaceUserQueryRequest implements Serializable {

  /**
   * ID
   */
  private Long id;

  public Long getId() {
    return id;
  }

  /**
   * 空间 ID
   */
  private Long spaceId;

  public Long getSpaceId() {
    return spaceId;
  }

  /**
   * 用户 ID
   */
  private Long userId;

  public Long getUserId() {
    return userId;
  }

  /**
   * 空间角色：viewer/editor/admin
   */
  private String spaceRole;

  public String getSpaceRole() {
    return spaceRole;
  }

  private static final long serialVersionUID = 1L;
}
