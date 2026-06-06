package com.web.yunpicturebackend.model.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;

import lombok.Data;
import java.util.Date;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "图片实体")
@Data
public class Picture implements Serializable {
  private String aiTags;

  private String aiDescription;

  private String aiScene;

  private String aiColors;

  private String aiTheme;

  private String aiOcrText;
  /**
   * id
   */
  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /**
   * 图片 url
   */
  private String url;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * 缩略图 url
   */
  private String thumbnailUrl;

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  /**
   * 图片名称
   */
  private String name;

  public void setName(String name) {
    this.name = name;
  }

  /**
   * 简介
   */
  private String introduction;

  public String getIntroduction() {
    return introduction;
  }

  public void setIntroduction(String introduction) {
    this.introduction = introduction;
  }

  /**
   * 分类
   */
  private String category;

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  /**
   * 标签（JSON 数组）
   */
  private String tags;

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  /**
   * 图片体积
   */
  private Long picSize;

  public Long getPicSize() {
    return picSize;
  }

  public void setPicSize(Long picSize) {
    this.picSize = picSize;
  }

  /**
   * 图片宽度
   */
  private Integer picWidth;

  public Integer getPicWidth() {
    return picWidth;
  }

  public void setPicWidth(Integer picWidth) {
    this.picWidth = picWidth;
  }

  /**
   * 图片高度
   */
  private Integer picHeight;

  public Integer getPicHeight() {
    return picHeight;
  }

  public void setPicHeight(Integer picHeight) {
    this.picHeight = picHeight;
  }

  /**
   * 图片比例
   */
  private Double picScale;

  public Double getPicScale() {
    return picScale;
  }

  public void setPicScale(Double picScale) {
    this.picScale = picScale;
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

  public void setPicColor(String picColor) {
    this.picColor = picColor;
  }

  /**
   * 创建用户 id
   */
  private Long userId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  /**
   * 空间 id
   */
  private Long spaceId;

  public Long getSpaceId() {
    return spaceId;
  }

  public void setSpaceId(Long spaceId) {
    this.spaceId = spaceId;
  }

  /**
   * 审核状态：0-待审核; 1-通过; 2-拒绝
   */
  private Integer reviewStatus;

  public void setReviewStatus(Integer reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  /**
   * 审核信息
   */
  private String reviewMessage;

  public void setReviewMessage(String reviewMessage) {
    this.reviewMessage = reviewMessage;
  }

  /**
   * 审核人 ID
   */
  private Long reviewerId;

  public void setReviewerId(Long reviewerId) {
    this.reviewerId = reviewerId;
  }

  /**
   * 审核时间
   */
  private Date reviewTime;

  public void setReviewTime(Date reviewTime) {
    this.reviewTime = reviewTime;
  }

  /**
   * 创建时间
   */
  private Date createTime;

  /**
   * 编辑时间
   */
  private Date editTime;

  public void setEditTime(Date editTime) {
    this.editTime = editTime;
  }

  /**
   * 更新时间
   */
  private Date updateTime;

  /**
   * 是否删除
   */
  @TableLogic
  private Integer isDelete;

  @TableField(exist = false)
  private static final long serialVersionUID = 1L;
}
