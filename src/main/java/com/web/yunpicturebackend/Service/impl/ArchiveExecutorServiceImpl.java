package com.web.yunpicturebackend.Service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.yunpicturebackend.Service.ArchiveExecutorService;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.mapper.PictureMapper;
import com.web.yunpicturebackend.model.dto.archive.ArchiveExecutionResult;
import com.web.yunpicturebackend.model.dto.archive.ArchivePictureRecord;
import com.web.yunpicturebackend.model.dto.archive.PictureArchiveCandidate;
import com.web.yunpicturebackend.model.entity.Picture;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class ArchiveExecutorServiceImpl extends ServiceImpl<PictureMapper, Picture> implements ArchiveExecutorService {

  @Override
  public ArchiveExecutionResult execute(Long spaceId, List<PictureArchiveCandidate> candidates) {
    if (spaceId == null || spaceId <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "spaceId 不合法");
    }
    if (CollUtil.isEmpty(candidates)) {
      return ArchiveExecutionResult.builder()
          .updatedCount(0)
          .records(Collections.emptyList())
          .build();
    }
    List<ArchivePictureRecord> records = candidates.stream()
        .filter(candidate -> candidate.getPictureId() != null)
        .filter(candidate -> StrUtil.isNotBlank(candidate.getTargetCategory()))
        .map(candidate -> {
          Picture update = new Picture();
          update.setId(candidate.getPictureId());
          update.setCategory(candidate.getTargetCategory());
          boolean updated = this.lambdaUpdate()
              .eq(Picture::getId, candidate.getPictureId())
              .eq(Picture::getSpaceId, spaceId)
              .set(Picture::getCategory, candidate.getTargetCategory())
              .update();
          if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "归档执行失败，pictureId=" + candidate.getPictureId());
          }
          return ArchivePictureRecord.builder()
              .pictureId(candidate.getPictureId())
              .pictureName(candidate.getPictureName())
              .fromCategory(candidate.getOriginalCategory())
              .toCategory(candidate.getTargetCategory())
              .build();
        })
        .toList();
    return ArchiveExecutionResult.builder()
        .updatedCount(records.size())
        .records(records)
        .build();
  }
}
