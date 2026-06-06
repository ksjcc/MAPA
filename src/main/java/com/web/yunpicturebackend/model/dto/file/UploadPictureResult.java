package com.web.yunpicturebackend.model.dto.file;

import lombok.Data;

@Data
public class UploadPictureResult {

  /**
   * 图片地址
   */
  private String url;

  public String getUrl() {
    return url;
  }

  /**
   * 缩略图 url
   */
  private String thumbnailUrl;

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  /**
   * 图片名称
   */
  private String picName;

  public String getPicName() {
    return picName;
  }

  /**
   * 文件体积
   */
  private Long picSize;

  public Long getPicSize() {
    return picSize;
  }

  /**
   * 图片宽度
   */
  private int picWidth;

  public int getPicWidth() {
    return picWidth;
  }

  /**
   * 图片高度
   */
  private int picHeight;

  public int getPicHeight() {
    return picHeight;
  }

  /**
   * 图片宽高比
   */
  private Double picScale;

  public Double getPicScale() {
    return picScale;
  }

  /**
   * 图片格式
   */
  private String picFormat;

  /**
   * 图片主色调
   */
  private String picColor;

  public String getPicColor() {
    return picColor;
  }
}
