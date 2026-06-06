package com.web.yunpicturebackend.Service.impl;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.web.yunpicturebackend.Service.PictureAnalysisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PictureAnalysisAsyncService {
  private final PictureAnalysisService pictureAnalysisService;

  @Async
  public void analyzeAndSave(Long pictureId) {
    try {
      pictureAnalysisService.analyzeAndSave(pictureId);
    } catch (Exception e) {
      log.warn("图片 AI 分析回写失败，pictureId={}", pictureId, e);
    }
  }
}
