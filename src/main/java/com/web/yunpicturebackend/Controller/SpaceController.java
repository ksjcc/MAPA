package com.web.yunpicturebackend.Controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.web.yunpicturebackend.Service.SpaceService;
import com.web.yunpicturebackend.Service.UserService;
import com.web.yunpicturebackend.annotation.AuthCheck;
import com.web.yunpicturebackend.common.BaseResponse;
import com.web.yunpicturebackend.common.DeleteRequest;
import com.web.yunpicturebackend.common.ResultUtils;
import com.web.yunpicturebackend.constant.UserConstant;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.exception.ThrowUtils;
import com.web.yunpicturebackend.manager.auth.SpaceUserAuthManager;
import com.web.yunpicturebackend.model.dto.space.SpaceAddRequest;
import com.web.yunpicturebackend.model.dto.space.SpaceLevel;
import com.web.yunpicturebackend.model.dto.space.SpaceQueryRequest;
import com.web.yunpicturebackend.model.dto.space.SpaceUpdateRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceRankAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceUserAnalyzeRequest;
import com.web.yunpicturebackend.model.entity.Space;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.enums.SpaceLevelEnum;
import com.web.yunpicturebackend.model.vo.SpaceVO;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceUserAnalyzeResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {
  @Resource
  private UserService userService;

  @Resource
  private SpaceService spaceService;

  @Resource
  private SpaceUserAuthManager spaceUserAuthManager;

  @PostMapping("/add")
  public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
    ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    long newId = spaceService.addSpace(spaceAddRequest, loginUser);
    return ResultUtils.success(newId);
  }

  @PostMapping("/update")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(spaceUpdateRequest == null, ErrorCode.PARAMS_ERROR);
    Space space = new Space();
    BeanUtils.copyProperties(spaceUpdateRequest, space);
    // 自动填充数据
    spaceService.fillSpaceBySpaceLevel(space);
    // 数据校证
    spaceService.validSpace(space, false);
    // 判断是否纯在
    long id = spaceUpdateRequest.getId();
    Space oldSpace = spaceService.getById(id);
    ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
    // 操作数据库更新
    boolean result = spaceService.updateById(space);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    return ResultUtils.success(result);
  }

  /**
   * 根据 id 获取空间（封装类）
   */
  @GetMapping("/get/vo")
  public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
    ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
    // 查询数据库
    Space space = spaceService.getById(id);
    ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
    SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
    User loginUser = userService.getLoginUser(request);
    List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
    spaceVO.setPermissionList(permissionList);
    // 获取封装类
    return ResultUtils.success(spaceVO);
  }

  /**
   * 分页获取空间列表（仅管理员可用）
   */
  @PostMapping("/list/page")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
    long current = spaceQueryRequest.getCurrent();
    long size = spaceQueryRequest.getPageSize();
    // 查询数据库
    Page<Space> spacePage = spaceService.page(new Page<>(current, size),
        spaceService.getQueryWrapper(spaceQueryRequest));
    return ResultUtils.success(spacePage);
  }

  public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
    if (deleteRequest == null || deleteRequest.getId() <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    User loginUser = userService.getLoginUser(request);
    Long id = deleteRequest.getId();
    // 判断是否存在
    Space oldSpace = spaceService.getById(id);
    ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
    // 仅本人或者管理员可删除
    spaceService.checkSpaceAuth(loginUser, oldSpace);
    // 操作数据库
    boolean result = spaceService.removeById(id);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    return ResultUtils.success(true);
  }

  /**
   * 获取空间级别列表，便于前端展示
   *
   * @return
   */
  @GetMapping("/list/level")
  public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
    List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
        .map(spaceLevelEnum -> new SpaceLevel(
            spaceLevelEnum.getValue(),
            spaceLevelEnum.getText(),
            spaceLevelEnum.getMaxCount(),
            spaceLevelEnum.getMaxSize()))
        .collect(Collectors.toList());
    return ResultUtils.success(spaceLevelList);
  }
}
