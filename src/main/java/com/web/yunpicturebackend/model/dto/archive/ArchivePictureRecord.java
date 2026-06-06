package com.web.yunpicturebackend.model.dto.archive;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArchivePictureRecord {
  private Long pictureId;
  private String pictureName;
  private String fromCategory;
  private String toCategory;
}
