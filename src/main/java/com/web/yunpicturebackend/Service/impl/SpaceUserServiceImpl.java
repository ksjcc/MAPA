package com.web.yunpicturebackend.Service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.web.yunpicturebackend.Service.SpaceService;
import com.web.yunpicturebackend.Service.SpaceUserService;
import com.web.yunpicturebackend.Service.UserService;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.exception.ThrowUtils;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy; // 添加此行

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.yunpicturebackend.mapper.SpaceUserMapper;
import com.web.yunpicturebackend.model.dto.space.SpaceQueryRequest;
import com.web.yunpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.web.yunpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.web.yunpicturebackend.model.entity.Space;
import com.web.yunpicturebackend.model.entity.SpaceUser;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.web.yunpicturebackend.model.vo.SpaceUserVO;
import com.web.yunpicturebackend.model.vo.SpaceVO;
import com.web.yunpicturebackend.model.vo.UserVO;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserService {

  @Resource
  private UserService userService;
  @Lazy // 添加此行
  @Resource
  private SpaceService spaceService;

  @Override
  public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
    ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
    SpaceUser spaceUser = new SpaceUser();
    BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
    validSpaceUser(spaceUser, true);
    boolean result = this.save(spaceUser);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    return spaceUser.getId();
  }

  @Override
  public void validSpaceUser(SpaceUser spaceUser, boolean add) {
    ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
    Long spaceId = spaceUser.getSpaceId();
    Long userId = spaceUser.getUserId();
    if (add) {
      ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
      User user = userService.getById(userId);
      ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
      Space space = spaceService.getById(spaceId);
      ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
    }
    // 校验空间角色是否存在
    String spaceRole = spaceUser.getSpaceRole();
    SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
    if (spaceRoleEnum != null && spaceRoleEnum == null) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
    }
  }

  @Override
  public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
    // 对象转封装类
    SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
    Long userId = spaceUser.getUserId();
    if (userId != null && userId > 0) {
      User user = userService.getById(userId);
      UserVO userVO = UserVO.objToVo(user);
      spaceUserVO.setUser(userVO);
    }
    // 关联查询空间信息
    Long spaceId = spaceUser.getSpaceId();
    if (spaceId != null && spaceId > 0) {
      Space space = spaceService.getById(spaceId);
      SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
      spaceUserVO.setSpace(spaceVO);
    }
    return spaceUserVO;
  }

  @Override
  public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
    // 判断输入列表是否为空
    if (CollUtil.isEmpty(spaceUserList)) {
      return Collections.emptyList();
    }
    // 对象列表 => 封装对象列表
    List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
    // 1. 收集需要关联查询的用户 ID 和空间 ID
    Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
    Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
    // 2. 批量查询用户和空间
    Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
        .collect(Collectors.groupingBy(User::getId));
    Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
        .collect(Collectors.groupingBy(Space::getId));
    // 3. 填充 SpaceUserVO 的用户和空间信息
    spaceUserVOList.forEach(spaceUserVO -> {
      Long userId = spaceUserVO.getUserId();
      Long spaceId = spaceUserVO.getSpaceId();
      User user = new User();
      if (userIdUserListMap.containsKey(userId)) {
        user = userIdUserListMap.get(userId).get(0);
      }
      spaceUserVO.setUser(userService.getUserVO(user));
      Space space = null;
      if (spaceIdSpaceListMap.containsKey(spaceId)) {
        space = spaceIdSpaceListMap.get(spaceId).get(0);
      }
      spaceUserVO.setSpace(SpaceVO.objToVo(space));
    });
    return spaceUserVOList;
  }

  @Override
  public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
    QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
    if (spaceUserQueryRequest == null) {
      return queryWrapper;
    }
    // 从对象中取值
    Long id = spaceUserQueryRequest.getId();
    Long spaceId = spaceUserQueryRequest.getSpaceId();
    Long userId = spaceUserQueryRequest.getUserId();
    String spaceRole = spaceUserQueryRequest.getSpaceRole();
    queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
    queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
    queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
    queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
    return queryWrapper;
  }
}
