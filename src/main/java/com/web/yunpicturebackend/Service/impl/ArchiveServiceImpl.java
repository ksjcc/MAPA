package com.web.yunpicturebackend.Service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.web.yunpicturebackend.Service.ArchiveAnalysisService;
import com.web.yunpicturebackend.Service.ArchiveExecutorService;
import com.web.yunpicturebackend.Service.ArchiveService;
import com.web.yunpicturebackend.Service.PictureService;
import com.web.yunpicturebackend.Service.SpaceService;
import com.web.yunpicturebackend.agent.Planner.ArchivePlanner;
import com.web.yunpicturebackend.common.ArchiveStrategy;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.model.dto.archive.ArchiveExecutionResult;
import com.web.yunpicturebackend.model.dto.archive.ArchivePictureRecord;
import com.web.yunpicturebackend.model.dto.archive.ArchiveResult;
import com.web.yunpicturebackend.model.dto.archive.PictureArchiveCandidate;
import com.web.yunpicturebackend.model.entity.Picture;
import com.web.yunpicturebackend.model.entity.Space;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {
  private final ArchivePlanner archivePlanner;
  private final ArchiveAnalysisService archiveAnalysisService;
  private final ArchiveExecutorService archiveExecutorService;
  private final PictureService pictureService;
  private final SpaceService spaceService;

  @Override
  public ArchiveResult archiveSpacePictures(Long spaceId, String userRequirement) {
    if (spaceId == null || spaceId <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "spaceId 不合法");
    }
    if (StrUtil.isBlank(userRequirement)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "归档要求不能为空");
    }
    Space space = spaceService.getById(spaceId);
    if (space == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
    }
    List<Picture> pictures = pictureService.lambdaQuery()
        .eq(Picture::getSpaceId, spaceId)
        .eq(Picture::getIsDelete, 0)
        .list();
    if (CollUtil.isEmpty(pictures)) {
      return ArchiveResult.builder()
          .spaceId(spaceId)
          .userRequirement(userRequirement)
          .categoryDimension("")
          .categories(Collections.emptyList())
          .totalPictures(0)
          .archivedCount(0)
          .categorySummary(Collections.emptyMap())
          .records(Collections.emptyList())
          .build();
    }

    ArchiveStrategy strategy = archivePlanner.plan(userRequirement);
    List<String> categories = CollUtil.isEmpty(strategy.getCategories()) ? Collections.emptyList() : strategy.getCategories();

    List<PictureArchiveCandidate> candidates = pictures.stream()
        .map(this::buildCandidate)
        .map(archiveAnalysisService::analyzeCandidate)
        .map(candidate -> assignCategory(candidate, strategy))
        .filter(candidate -> StrUtil.isNotBlank(candidate.getTargetCategory()))
        .toList();

    ArchiveExecutionResult executionResult = archiveExecutorService.execute(spaceId, candidates);
    Map<String, Long> summary = buildSummary(executionResult.getRecords());

    return ArchiveResult.builder()
        .spaceId(spaceId)
        .userRequirement(userRequirement)
        .categoryDimension(StrUtil.blankToDefault(strategy.getCategoryDimension(), "综合分类"))
        .categories(categories)
        .totalPictures(pictures.size())
        .archivedCount(executionResult.getUpdatedCount())
        .categorySummary(summary)
        .records(executionResult.getRecords())
        .build();
  }

  private PictureArchiveCandidate buildCandidate(Picture picture) {
    return PictureArchiveCandidate.builder()
        .pictureId(picture.getId())
        .pictureName(StrUtil.blankToDefault(picture.getName(), ""))
        .pictureUrl(StrUtil.blankToDefault(picture.getUrl(), ""))
        .originalCategory(StrUtil.blankToDefault(picture.getCategory(), ""))
        .description(StrUtil.blankToDefault(picture.getAiDescription(), ""))
        .scene(StrUtil.blankToDefault(picture.getAiScene(), ""))
        .theme(StrUtil.blankToDefault(picture.getAiTheme(), ""))
        .build();
  }

  private PictureArchiveCandidate assignCategory(PictureArchiveCandidate candidate, ArchiveStrategy strategy) {
    String targetCategory = matchCategory(candidate, strategy);
    return PictureArchiveCandidate.builder()
        .pictureId(candidate.getPictureId())
        .pictureName(candidate.getPictureName())
        .pictureUrl(candidate.getPictureUrl())
        .originalCategory(candidate.getOriginalCategory())
        .targetCategory(targetCategory)
        .description(candidate.getDescription())
        .scene(candidate.getScene())
        .theme(candidate.getTheme())
        .tags(candidate.getTags())
        .colors(candidate.getColors())
        .build();
  }

  private String matchCategory(PictureArchiveCandidate candidate, ArchiveStrategy strategy) {
    List<String> categories = strategy == null ? Collections.emptyList() : strategy.getCategories();
    if (CollUtil.isNotEmpty(categories)) {
      String material = buildMaterial(candidate);
      for (String category : categories) {
        if (StrUtil.isBlank(category)) {
          continue;
        }
        if (StrUtil.containsIgnoreCase(material, category)) {
          return category;
        }
      }
      return categories.get(0);
    }

    if (StrUtil.containsIgnoreCase(strategy.getCategoryDimension(), "颜色") && CollUtil.isNotEmpty(candidate.getColors())) {
      return candidate.getColors().get(0);
    }
    if (StrUtil.isNotBlank(candidate.getScene())) {
      return candidate.getScene();
    }
    if (StrUtil.isNotBlank(candidate.getTheme())) {
      return candidate.getTheme();
    }
    if (CollUtil.isNotEmpty(candidate.getTags())) {
      return candidate.getTags().get(0);
    }
    return "未分类";
  }

  private String buildMaterial(PictureArchiveCandidate candidate) {
    return String.join(" ",
        List.of(
            StrUtil.blankToDefault(candidate.getPictureName(), ""),
            StrUtil.blankToDefault(candidate.getDescription(), ""),
            StrUtil.blankToDefault(candidate.getScene(), ""),
            StrUtil.blankToDefault(candidate.getTheme(), ""),
            CollUtil.isEmpty(candidate.getTags()) ? "" : String.join(" ", candidate.getTags()),
            CollUtil.isEmpty(candidate.getColors()) ? "" : String.join(" ", candidate.getColors())));
  }

  private Map<String, Long> buildSummary(List<ArchivePictureRecord> records) {
    if (CollUtil.isEmpty(records)) {
      return Collections.emptyMap();
    }
    return records.stream()
        .filter(Objects::nonNull)
        .filter(record -> StrUtil.isNotBlank(record.getToCategory()))
        .sorted(Comparator.comparing(ArchivePictureRecord::getToCategory))
        .collect(LinkedHashMap::new,
            (map, record) -> map.merge(record.getToCategory(), 1L, Long::sum),
            Map::putAll);
  }
}
