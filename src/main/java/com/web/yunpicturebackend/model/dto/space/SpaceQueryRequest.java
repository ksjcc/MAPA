package com.web.yunpicturebackend.model.dto.space;

import java.io.Serializable;

import com.web.yunpicturebackend.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询空间请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {

  /**
   * id
   */
  private Long id;

  public Long getId() {
    return id;
  }

  /**
   * 用户 id
   */
  private Long userId;

  public Long getUserId() {
    return userId;
  }

  /**
   * 空间名称
   */
  private String spaceName;

  public String getSpaceName() {
    return spaceName;
  }

  /**
   * 空间级别：0-普通版 1-专业版 2-旗舰版
   */
  private Integer spaceLevel;

  public Integer getSpaceLevel() {
    return spaceLevel;
  }

  /**
   * 空间类型：0-私有 1-团队
   */
  private Integer spaceType;

  public Integer getSpaceType() {
    return spaceType;
  }

  private static final long serialVersionUID = 1L;
}
