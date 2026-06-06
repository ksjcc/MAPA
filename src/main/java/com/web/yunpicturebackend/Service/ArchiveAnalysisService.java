package com.web.yunpicturebackend.Service;

import com.web.yunpicturebackend.model.dto.archive.PictureArchiveCandidate;

public interface ArchiveAnalysisService {
  PictureArchiveCandidate analyzeCandidate(PictureArchiveCandidate candidate);
}
