package com.web.yunpicturebackend.model.vo;

import java.util.List;

import lombok.Data;

/**
 * 图片标签分类
 */
@Data
public class PictureTagCategory {

  /**
   * 标签列表
   */
  private List<String> tagList;

  public void setTagList(List<String> tagList) {
    this.tagList = tagList;
  }

  /**
   * 分类列表
   */
  private List<String> categoryList;

  public void setCategoryList(List<String> categoryList) {
    this.categoryList = categoryList;
  }
}
