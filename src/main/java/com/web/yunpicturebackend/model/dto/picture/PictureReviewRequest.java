package com.web.yunpicturebackend.model.dto.picture;

import java.io.Serializable;

import lombok.Data;

/**
 * 图片审核请求
 */
@Data
public class PictureReviewRequest implements Serializable {
  /**
   * id
   */
  private Long id;

  public Long getId() {
    return id;
  }

  /**
   * 审核状态：0-待审核; 1-通过; 2-拒绝
   */
  private Integer reviewStatus;

  public Integer getReviewStatus() {
    return reviewStatus;
  }

  /**
   * 审核信息
   */
  private String reviewMessage;

  public String getReviewMessage() {
    return reviewMessage;
  }

  private static final long serialVersionUID = 1L;
}
