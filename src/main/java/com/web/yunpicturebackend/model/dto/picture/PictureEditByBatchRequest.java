package com.web.yunpicturebackend.model.dto.picture;

import java.util.List;
import lombok.Data;
import java.io.Serializable;

/**
 * 图片批量编辑请求
 */
@Data
public class PictureEditByBatchRequest {
  /**
   * 图片 id 列表
   */
  private List<Long> pictureIdList;

  public List<Long> getPictureIdList() {
    return pictureIdList;
  }

  /**
   * 空间 id
   */
  private Long spaceId;

  public Long getSpaceId() {
    return spaceId;
  }

  /**
   * 分类
   */
  private String category;

  public String getCategory() {
    return category;
  }

  /**
   * 标签
   */
  private List<String> tags;

  public List<String> getTags() {
    return tags;
  }

  /**
   * 命名规则
   */
  private String nameRule;

  public String getNameRule() {
    return nameRule;
  }

  private static final long serialVersionUID = 1L;

}
