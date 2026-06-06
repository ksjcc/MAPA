package com.web.yunpicturebackend.model.dto.archive;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArchiveResult {
  private Long spaceId;
  private String userRequirement;
  private String categoryDimension;
  private List<String> categories;
  private Integer totalPictures;
  private Integer archivedCount;
  private Map<String, Long> categorySummary;
  private List<ArchivePictureRecord> records;
}
