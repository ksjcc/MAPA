package com.web.yunpicturebackend.agent.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PictureSearchResult {

  private Long pictureId;

  private String name;

  private String url;

  private String tags;

  private String category;
}
