package com.web.yunpicturebackend.model.dto.picture;

import java.io.Serializable;

import com.web.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;

import lombok.Data;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {
  /**
   * 图片 id
   */
  private Long pictureId;
  /**
   * 扩图参数
   */
  private CreateOutPaintingTaskRequest.Parameters parameters;
  private static final long serialVersionUID = 1L;
}
