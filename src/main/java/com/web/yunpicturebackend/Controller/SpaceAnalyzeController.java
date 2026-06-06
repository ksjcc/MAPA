package com.web.yunpicturebackend.Controller;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.web.yunpicturebackend.Service.SpaceAnalyzeService;
import com.web.yunpicturebackend.Service.SpaceService;
import com.web.yunpicturebackend.Service.UserService;
import com.web.yunpicturebackend.common.BaseResponse;
import com.web.yunpicturebackend.common.ResultUtils;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.exception.ThrowUtils;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpaceAnalyzeController {

  @Resource
  private UserService userService;

  @Resource
  private SpaceAnalyzeService spaceAnalyzeService;

  /**
   * 获取空间的使用状态
   *
   * @param spaceUsageAnalyzeRequest
   * @param request
   * @return
   */
  @PostMapping("/usage")
  public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(
      @RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    SpaceUsageAnalyzeResponse spaceUsageAnalyze = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest,
        loginUser);
    return ResultUtils.success(spaceUsageAnalyze);
  }

  /**
   * 获取空间图片分类分析
   *
   * @param spaceCategoryAnalyzeRequest
   * @param request
   * @return
   */
  @PostMapping("/category")
  public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(
      @RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze = spaceAnalyzeService
        .getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
    return ResultUtils.success(spaceCategoryAnalyze);
  }

  @PostMapping("/user")
  public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(
      @RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User user = userService.getLoginUser(request);
    List<SpaceUserAnalyzeResponse> spaceUserAnalyzeResponse = spaceAnalyzeService
        .getSpaceUserAnalyze(spaceUserAnalyzeRequest, user);
    return ResultUtils.success(spaceUserAnalyzeResponse);
  }

  @PostMapping("/rank")
  public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    List<Space> resultList = spaceAnalyzeService.getSpaceRankAnalyzeRequests(spaceRankAnalyzeRequest, loginUser);
    return ResultUtils.success(resultList);
  }

  /**
   * 获取空间图片大小分析
   *
   * @param spaceSizeAnalyzeRequest
   * @param request
   * @return
   */
  @PostMapping("/size")
  public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(
      @RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    List<SpaceSizeAnalyzeResponse> resultList = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest,
        loginUser);
    return ResultUtils.success(resultList);
  }

  /**
   * 获取空间图片标签分析
   *
   * @param spaceTagAnalyzeRequest
   * @param request
   * @return
   */
  @PostMapping("/tag")
  public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(
      @RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    List<SpaceTagAnalyzeResponse> spaceTagAnalyze = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest,
        loginUser);
    return ResultUtils.success(spaceTagAnalyze);
  }
}
