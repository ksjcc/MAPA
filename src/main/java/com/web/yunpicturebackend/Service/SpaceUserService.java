package com.web.yunpicturebackend.Service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.web.yunpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.web.yunpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.web.yunpicturebackend.model.entity.SpaceUser;
import com.web.yunpicturebackend.model.vo.SpaceUserVO;

public interface SpaceUserService extends IService<SpaceUser> {
  /**
   * 创建空间成员
   *
   * @param spaceUserAddRequest
   * @return
   */
  long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

  /**
   * 校验空间成员
   *
   * @param spaceUser
   * @param add       是否为创建时检验
   */
  void validSpaceUser(SpaceUser spaceUser, boolean add);

  /**
   * 获取空间成员包装类（单条）
   *
   * @param spaceUser
   * @param request
   * @return
   */
  SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

  /**
   * 获取空间成员包装类（列表）
   *
   * @param spaceUserList
   * @return
   */
  List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

  /**
   * 获取查询对象
   *
   * @param spaceUserQueryRequest
   * @return
   */
  QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
