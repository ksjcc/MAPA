package com.web.yunpicturebackend.model.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
 * 空间
 * 
 * @TableName space
 */
@Data
public class Space implements Serializable {
  /**
   * id
   */
  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  public Long getId() {
    return id;
  }

  /**
   * 空间名称
   */
  private String spaceName;

  public String getSpaceName() {
    return spaceName;
  }

  public void setSpaceName(String spaceName) {
    this.spaceName = spaceName;
  }

  /**
   * 空间级别：0-普通版 1-专业版 2-旗舰版
   */
  private Integer spaceLevel;

  public Integer getSpaceLevel() {
    return spaceLevel;
  }

  public void setSpaceLevel(Integer spaceLevel) {
    this.spaceLevel = spaceLevel;
  }

  /**
   * 空间类型：0-私有 1-团队
   */
  private Integer spaceType;

  public Integer getSpaceType() {
    return spaceType;
  }

  public void setSpaceType(Integer spaceType) {
    this.spaceType = spaceType;
  }

  /**
   * 空间图片的最大总大小
   */
  private Long maxSize;

  public Long getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(Long maxSize) {
    this.maxSize = maxSize;
  }

  /**
   * 空间图片的最大数量
   */
  private Long maxCount;

  public Long getMaxCount() {
    return maxCount;
  }

  public void setMaxCount(Long maxCount) {
    this.maxCount = maxCount;
  }

  /**
   * 当前空间下图片的总大小
   */
  private Long totalSize;

  public Long getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(Long totalSize) {
    this.totalSize = totalSize;
  }

  /**
   * 当前空间下的图片数量
   */
  private Long totalCount;

  public Long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Long totalCount) {
    this.totalCount = totalCount;
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
   * 创建时间
   */
  private Date createTime;

  /**
   * 编辑时间
   */
  private Date editTime;

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
