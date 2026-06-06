package com.web.yunpicturebackend.model.dto.picture;

import java.io.Serializable;
import java.util.List;

import com.web.yunpicturebackend.common.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 图片查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryRequest extends PageRequest implements Serializable {

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

  public String getName() {
    return name;
  }

  /**
   * 简介
   */
  private String introduction;

  public String getIntroduction() {
    return introduction;
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
   * 文件体积
   */
  private Long picSize;

  public Long getPicSize() {
    return picSize;
  }

  /**
   * 图片宽度
   */
  private Integer picWidth;

  public Integer getPicWidth() {
    return picWidth;
  }

  /**
   * 图片高度
   */
  private Integer picHeight;

  public Integer getPicHeight() {
    return picHeight;
  }

  /**
   * 图片比例
   */
  private Double picScale;

  public Double getPicScale() {
    return picScale;
  }

  /**
   * 图片格式
   */
  private String picFormat;

  public String getPicFormat() {
    return picFormat;
  }

  /**
   * 图片主色调
   */
  private String picColor;

  public String getPicColor() {
    return picColor;
  }

  /**
   * 搜索词（同时搜名称、简介等）
   */
  private String searchText;

  public String getSearchText() {
    return searchText;
  }

  /**
   * 用户 id
   */
  private Long userId;

  public Long getUserId() {
    return userId;
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

  /**
   * 审核人 ID
   */
  private Long reviewerId;

  public Long getReviewerId() {
    return reviewerId;
  }

  /**
   * 审核时间
   */
  private Date reviewTime;

  /**
   * 空间 id
   */
  private Long spaceId;

  public Long getSpaceId() {
    return spaceId;
  }

  /**
   * 是否只查询 spaceId 为 null 的数据
   */
  private boolean nullSpaceId;

  public boolean isNullSpaceId() {
    return nullSpaceId;
  }

  public void setNullSpaceId(boolean nullSpaceId) {
    this.nullSpaceId = nullSpaceId;
  }

  /*
   * 开始编辑时间
   */
  private Date startEditTime;

  public Date getStartEditTime() {
    return startEditTime;
  }

  /*
   * 结束编辑时间
   */
  private Date endEditTime;

  public Date getEndEditTime() {
    return endEditTime;
  }

  public void setReviewStatus(Integer reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  private static final long serialVersionUID = 1L;
}
