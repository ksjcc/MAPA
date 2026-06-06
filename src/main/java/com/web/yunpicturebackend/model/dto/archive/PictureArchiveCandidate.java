package com.web.yunpicturebackend.model.dto.archive;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PictureArchiveCandidate {
  private Long pictureId;
  private String pictureName;
  private String pictureUrl;
  private String originalCategory;
  private String targetCategory;
  private String description;
  private String scene;
  private String theme;
  private List<String> tags;
  private List<String> colors;
}
