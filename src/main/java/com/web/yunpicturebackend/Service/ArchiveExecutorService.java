package com.web.yunpicturebackend.Service;

import java.util.List;

import com.web.yunpicturebackend.model.dto.archive.ArchiveExecutionResult;
import com.web.yunpicturebackend.model.dto.archive.PictureArchiveCandidate;

public interface ArchiveExecutorService {
  ArchiveExecutionResult execute(Long spaceId, List<PictureArchiveCandidate> candidates);
}
