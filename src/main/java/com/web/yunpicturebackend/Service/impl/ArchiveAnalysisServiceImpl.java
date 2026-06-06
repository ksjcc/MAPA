package com.web.yunpicturebackend.Service.impl;

import java.util.Collections;

import org.springframework.stereotype.Service;

import com.web.yunpicturebackend.Service.ArchiveAnalysisService;
import com.web.yunpicturebackend.agent.dto.AnalysisResult;
import com.web.yunpicturebackend.model.dto.archive.PictureArchiveCandidate;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArchiveAnalysisServiceImpl implements ArchiveAnalysisService {
  private final PictureAnalysisSerivceImpl pictureAnalysisService;

  @Override
  public PictureArchiveCandidate analyzeCandidate(PictureArchiveCandidate candidate) {
    if (candidate == null || StrUtil.isBlank(candidate.getPictureUrl())) {
      return candidate;
    }
    try {
      AnalysisResult result = pictureAnalysisService.analyzePictureDirect(candidate.getPictureUrl());
      return PictureArchiveCandidate.builder()
          .pictureId(candidate.getPictureId())
          .pictureName(candidate.getPictureName())
          .pictureUrl(candidate.getPictureUrl())
          .originalCategory(candidate.getOriginalCategory())
          .targetCategory(candidate.getTargetCategory())
          .description(StrUtil.blankToDefault(result.getDescription(), ""))
          .scene(StrUtil.blankToDefault(result.getScene(), ""))
          .theme(StrUtil.blankToDefault(result.getTheme(), ""))
          .tags(CollUtil.isEmpty(result.getTags()) ? Collections.emptyList() : result.getTags())
          .colors(CollUtil.isEmpty(result.getColors()) ? Collections.emptyList() : result.getColors())
          .description(StrUtil.blankToDefault(result.getDescription(), "") + (StrUtil.isBlank(result.getOcrText()) ? "" : " OCR:" + result.getOcrText()))
          .build();
    } catch (Exception e) {
      log.warn("图片分析失败，归档时回退到基础信息，pictureId={}", candidate.getPictureId(), e);
      return candidate;
    }
  }
}
