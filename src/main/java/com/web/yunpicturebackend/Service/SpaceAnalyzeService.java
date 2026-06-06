package com.web.yunpicturebackend.Service;

import java.util.List;

import com.web.yunpicturebackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceRankAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceSizeAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceUserAnalyzeRequest;
import com.web.yunpicturebackend.model.entity.Space;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceSizeAnalyzeResponse;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceUserAnalyzeResponse;

public interface SpaceAnalyzeService {

  SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

  List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
      User loginUser);

  List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRquest, User loginUser);

  List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

  List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

  List<Space> getSpaceRankAnalyzeRequests(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
      User loginUser);

}
