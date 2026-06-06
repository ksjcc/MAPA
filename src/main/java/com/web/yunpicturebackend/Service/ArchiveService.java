package com.web.yunpicturebackend.Service;

import com.web.yunpicturebackend.model.dto.archive.ArchiveResult;

public interface ArchiveService {
  ArchiveResult archiveSpacePictures(Long spaceId, String userRequirement);
}
