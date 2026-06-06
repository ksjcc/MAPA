package com.web.yunpicturebackend.model.dto.picture;

import java.io.Serializable;

import lombok.Data;

/**
 * 图片上传请求
 */
@Data
public class PictureUploadRequest implements Serializable {
  /**
   * 图片 id（用于修改）
   */
  private Long id;

  public Long getId() {
    return id;
  }

  /**
   * 文件地址
   */
  private String fileUrl;

  public String getFileUrl() {
    return fileUrl;
  }

  /**
   * 图片名称
   */
  private String picName;

  public String getPicName() {
    return picName;
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
