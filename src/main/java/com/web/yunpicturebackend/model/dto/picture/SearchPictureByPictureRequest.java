package com.web.yunpicturebackend.model.dto.picture;

import java.io.Serializable;

import lombok.Data;

/**
 * 以图搜图请求
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {

  /**
   * 图片 id
   */
  private Long pictureId;

  public Long getPictureId() {
    return pictureId;
  }

  private static final long serialVersionUID = 1L;
}
