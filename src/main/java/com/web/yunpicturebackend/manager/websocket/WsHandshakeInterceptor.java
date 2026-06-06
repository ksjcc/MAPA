package com.web.yunpicturebackend.manager.websocket;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.web.yunpicturebackend.Service.PictureService;
import com.web.yunpicturebackend.Service.SpaceService;
import com.web.yunpicturebackend.Service.UserService;
import com.web.yunpicturebackend.manager.auth.SpaceUserAuthManager;
import com.web.yunpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.web.yunpicturebackend.model.entity.Picture;
import com.web.yunpicturebackend.model.entity.Space;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.enums.SpaceTypeEnum;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;

/**
 * WebSocket 拦截器，建立连接前要先校验
 */
@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {
  @Resource
  private UserService userService;
  @Resource
  private PictureService pictureService;
  @Resource
  private SpaceService spaceService;
  @Resource
  private SpaceUserAuthManager spaceUserAuthManager;

  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
      Map<String, Object> attributes) throws Exception {
    if (request instanceof ServletServerHttpRequest) {
      HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
      String pictureId = httpServletRequest.getParameter("pictureId");
      if (StrUtil.isBlank(pictureId)) {
        log.error("缺少图片参数，拒绝握手");
        return false;
      }
      User loginUser = userService.getLoginUser(httpServletRequest);
      if (loginUser == null) {
        log.error("用户未登录，拒绝握手");
        return false;
      }
      Picture picture = pictureService.getById(pictureId);
      if (ObjUtil.isEmpty(picture)) {
        log.error("图片不存在，拒绝握手");
        return false;
      }
      Long spaceId = picture.getSpaceId();
      Space space = null;
      if (spaceId != null) {
        space = spaceService.getById(spaceId);
        if (ObjUtil.isEmpty(space)) {
          log.error("空间不存在，拒绝握手");
          return false;
        }
        if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
          log.error("图片所在空间不是团队空间，拒绝握手");
          return false;
        }
      }
      List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
      if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
        log.error("用户没有编辑图片权限，拒绝握手");
        return false;
      }
      // 设置用户登录信息等属性到 WebSocket 会话中
      attributes.put("user", loginUser);
      attributes.put("userId", loginUser.getId());
      attributes.put("pictureId", Long.valueOf(pictureId)); // 记得转换为 Long 类型
    }
    return true;
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
      Exception exception) {
  }
}
