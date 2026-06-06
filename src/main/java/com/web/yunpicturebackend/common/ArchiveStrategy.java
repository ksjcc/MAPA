package com.web.yunpicturebackend.common;

import java.util.List;

import lombok.Data;

@Data
public class ArchiveStrategy {
  /**
   * 分类维度
   */
  private String categoryDimension;

  /**
   * 分类规则
   */
  private List<String> categories;
}
