package com.web.yunpicturebackend.model.dto.embedding;

import java.util.List;

import lombok.Data;

@Data
public class EmbeddingResponse {
  private List<Float> vector;
}
