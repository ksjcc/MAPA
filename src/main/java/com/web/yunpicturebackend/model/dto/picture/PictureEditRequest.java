package com.web.yunpicturebackend.model.dto.picture;

import java.util.List;
import lombok.Data;

@Data
public class PictureEditRequest {

  /**
   * id
   */
  private Long id;

  public Long getId() {
    return id;
  }

  /**
   * 图片名称
   */
  private String name;

  /**
   * 简介
   */
  private String introduction;

  /**
   * 分类
   */
  private String category;

  /**
   * 标签
   */
  private List<String> tags;

  public List<String> getTags() {
    return tags;
  }

  private static final long serialVersionUID = 1L;
}
