package com.web.yunpicturebackend.model.dto.archive;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArchiveExecutionResult {
  private Integer updatedCount;
  private List<ArchivePictureRecord> records;
}
