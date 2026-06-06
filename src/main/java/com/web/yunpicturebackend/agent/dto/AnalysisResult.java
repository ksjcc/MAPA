package com.web.yunpicturebackend.agent.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalysisResult {
  private String description;
  private List<String> tags;
  private String scene;
  private String theme;
  private List<String> colors;
  private String ocrText;
}
