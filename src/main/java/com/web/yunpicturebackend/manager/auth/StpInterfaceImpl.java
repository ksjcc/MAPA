package com.web.yunpicturebackend.manager.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.web.yunpicturebackend.Service.PictureService;
import com.web.yunpicturebackend.Service.SpaceService;
import com.web.yunpicturebackend.Service.SpaceUserService;
import com.web.yunpicturebackend.Service.UserService;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.web.yunpicturebackend.model.entity.Picture;
import com.web.yunpicturebackend.model.entity.Space;
import com.web.yunpicturebackend.model.entity.SpaceUser;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.web.yunpicturebackend.model.enums.SpaceTypeEnum;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import static com.web.yunpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component
public class StpInterfaceImpl implements StpInterface {
  @Value("${service.servlet.context-path}")
  private String contextPath;

  @Resource
  private UserService userService;

  @Resource
  private SpaceService spaceService;

  @Resource
  private SpaceUserService spaceUserService;

  @Resource
  private PictureService pictureService;

  @Resource
  private SpaceUserAuthManager spaceUserAuthManager;

  @Override
  public List<String> getPermissionList(Object loginId, String loginType) {
    if (!StpKit.SPACE_TYPE.equals(loginType)) {
      return new ArrayList<>();
    }
    // 管理员权限，表示权限校验通过
    List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
    // 获取上下文对象
    SpaceUserAuthContext authContext = getAuthContextByRequest();
    if (isAllFieldsNull(authContext)) {
      return ADMIN_PERMISSIONS;
    }
    User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
    if (loginUser == null) {
      throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
    }
    Long UserId = loginUser.getId();
    SpaceUser spaceUser = authContext.getSpaceUser();
    if (spaceUser != null) {
      return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
    }
    Long spaceUserId = authContext.getSpaceUserId();
    if (spaceUserId != null) {
      spaceUser = spaceUserService.getById(spaceUserId);
      if (spaceUser == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
      }
      // 取出当前登录用户对应的 spaceUser
      SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
          .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
          .eq(SpaceUser::getUserId, UserId)
          .one();
      if (loginSpaceUser == null) {
        return new ArrayList<>();
      }
      return spaceUserAuthManager.getPermissionsByRole(spaceUserService.getById(spaceUserId).getSpaceRole());
    }
    Long spaceId = authContext.getSpaceId();
    if (spaceId == null) {
      // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
      Long pictureId = authContext.getPictureId();
      // 图片 id 也没有，则默认通过权限校验
      if (pictureId == null) {
        return ADMIN_PERMISSIONS;
      }
      Picture picture = pictureService.lambdaQuery()
          .eq(Picture::getId, pictureId)
          .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
          .one();
      if (picture == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
      }
      spaceId = picture.getSpaceId();
      // 公共图库，仅本人或管理员可操作
      if (spaceId == null) {
        if (picture.getUserId().equals(UserId) || userService.isAdmin(loginUser)) {
          return ADMIN_PERMISSIONS;
        } else {
          // 不是自己的图片，仅可查看
          return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
      }
    }
    // 获取 Space 对象
    Space space = spaceService.getById(spaceId);
    if (space == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
    }
    // 根据 Space 类型判断权限
    if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
      // 私有空间，仅本人或管理员有权限
      if (space.getUserId().equals(UserId) || userService.isAdmin(loginUser)) {
        return ADMIN_PERMISSIONS;
      } else {
        return new ArrayList<>();
      }
    } else {
      // 团队空间，查询 SpaceUser 并获取角色和权限
      spaceUser = spaceUserService.lambdaQuery()
          .eq(SpaceUser::getSpaceId, spaceId)
          .eq(SpaceUser::getUserId, UserId)
          .one();
      if (spaceUser == null) {
        return new ArrayList<>();
      }
      return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
    }
  }

  @Override
  public List<String> getRoleList(Object loginId, String loginType) {
    return new ArrayList<>();
  }

  private SpaceUserAuthContext getAuthContextByRequest() {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
        .getRequest();
    String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
    SpaceUserAuthContext authRequest;
    // 获取请求参数
    if (ContentType.JSON.getValue().equals(contentType)) {
      String body = ServletUtil.getBody(request);
      authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
    } else {
      Map<String, String> paramMap = ServletUtil.getParamMap(request);
      authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
    }
    // 根据请求路径区分 id 字段的含义
    Long id = authRequest.getId();
    if (ObjUtil.isNotNull(id)) {
      // 获取到请求路径的业务前缀，/api/picture/aaa?a=1
      String requestURI = request.getRequestURI();
      // 先替换掉上下文，剩下的就是前缀
      String partURI = requestURI.replace(contextPath + "/", "");
      // 获取前缀的第一个斜杠前的字符串
      String moduleName = StrUtil.subBefore(partURI, "/", false);
      switch (moduleName) {
        case "picture":
          authRequest.setPictureId(id);
          break;
        case "spaceUser":
          authRequest.setSpaceUserId(id);
          break;
        case "space":
          authRequest.setSpaceId(id);
          break;
        default:
      }
    }
    return authRequest;
  }

  private boolean isAllFieldsNull(Object object) {
    if (object == null) {
      return true; // 对象本身为空
    }
    // 获取所有字段并判断是否所有字段都为空
    return Arrays.stream(ReflectUtil.getFields(object.getClass()))
        // 获取字段值
        .map(field -> ReflectUtil.getFieldValue(object, field))
        // 检查是否所有字段都为空
        .allMatch(ObjectUtil::isEmpty);
  }
}
