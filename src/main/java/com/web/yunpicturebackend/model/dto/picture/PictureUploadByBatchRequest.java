package com.web.yunpicturebackend.model.dto.picture;

import java.io.Serializable;

import lombok.Data;

/**
 * 批量导入图片请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

  /**
   * 搜索词
   */
  private String searchText;

  public String getSearchText() {
    return searchText;
  }

  /**
   * 抓取数量
   */
  private Integer count = 10;

  public Integer getCount() {
    return count;
  }

  /**
   * 图片名称前缀
   */
  private String namePrefix;

  public String getNamePrefix() {
    return namePrefix;
  }

  private static final long serialVersionUID = 1L;
}
