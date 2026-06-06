package com.web.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 按照颜色搜索图片请求
 */
@Data
public class SearchPictureByColorRequest {

  /**
   * 图片主色调
   */
  private String picColor;

  public String getPicColor() {
    return picColor;
  }

  /**
   * 空间 id
   */
  private Long spaceId;

  public Long getSpaceId() {
    return spaceId;
  }

  private static final long serialVersionUID = 1L;
}
