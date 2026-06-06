package com.web.yunpicturebackend.Service;

import com.web.yunpicturebackend.agent.dto.AnalysisResult;

public interface PictureAnalysisService {
  AnalysisResult analysis(String imageurl);

  void analyzeAndSave(Long pictureId);
}
