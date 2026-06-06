package com.web.yunpicturebackend.common;

import lombok.Data;

/**
 * 通用的分页请求类
 */
@Data
public class PageRequest {
  /**
   * 当前页号
   */
  private int current = 1;

  public int getCurrent() {
    return current;
  }

  /**
   * 页面大小
   */
  private int pageSize = 10;

  public int getPageSize() {
    return pageSize;
  }

  /**
   * 排序字段
   */
  private String sortField;

  public String getSortField() {
    return sortField;
  }

  /**
   * 排序顺序（默认升序）
   */
  private String sortOrder = "descend";

  public String getSortOrder() {
    return sortOrder;
  }
}
