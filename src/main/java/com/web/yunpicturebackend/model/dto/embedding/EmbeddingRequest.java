package com.web.yunpicturebackend.model.dto.embedding;

import lombok.Data;

@Data
public class EmbeddingRequest {
  private String imageUrl;

  private String text;

}
