package com.web.yunpicturebackend.Service.impl;

import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.enums.UserRoleEnum;
import com.web.yunpicturebackend.model.dto.user.*;
import com.web.yunpicturebackend.model.vo.LoginUserVO;
import com.web.yunpicturebackend.model.vo.UserVO;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.web.yunpicturebackend.Service.UserService;
import com.web.yunpicturebackend.constant.UserConstant;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.mapper.UserMapper;
import com.web.yunpicturebackend.manager.auth.StpKit;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
  @Override
  public Long userRegister(String userAccount, String userPassword, String checkPassword) {
    // 1. 校验
    if (userAccount == null || userPassword == null || checkPassword == null) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
    }
    if (userAccount.length() < 4) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
    }
    if (userPassword.length() < 8 || checkPassword.length() < 8) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
    }
    // 密码和校验密码相同
    if (!userPassword.equals(checkPassword)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
    }
    // 2.检查重复
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("userAccount", userAccount);
    long count = this.baseMapper.selectCount(queryWrapper);
    if (count > 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
    }
    // 3. 加密
    String encryptPassword = this.getEncryptPassword(userPassword);
    // 4. 插入数据
    User user = new User();
    user.setUserAccount(userAccount);
    user.setUserPassword(encryptPassword);
    boolean saveResult = this.save(user);
    if (!saveResult) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败");
    }
    return user.getId();
  }

  @Override
  public LoginUserVO userLogin(String userAccount, String userPassWord, HttpServletRequest request) {
    // 1. 校验
    if (userAccount == null || userPassWord == null) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
    }
    if (userAccount.length() < 4) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
    }
    if (userPassWord.length() < 8) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
    }
    String encryptPassword = this.getEncryptPassword(userPassWord);
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("userAccount", userAccount);
    queryWrapper.eq("userPassword", encryptPassword);
    User user = this.baseMapper.selectOne(queryWrapper);
    if (user == null) {
      String legacyEncryptPassword = DigestUtils.md5DigestAsHex((userPassWord).getBytes());
      queryWrapper = new QueryWrapper<>();
      queryWrapper.eq("userAccount", userAccount);
      queryWrapper.eq("userPassword", legacyEncryptPassword);
      user = this.baseMapper.selectOne(queryWrapper);
      if (user != null) {
        user.setUserPassword(encryptPassword);
        this.updateById(user);
      }
    }
    if (user == null) {
      log.info("user login failed,userAccount cannot login");
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
    }
    // request.getSession() 获取当前会话对象 HttpSession
    // setAttribute(UserConstant.USER_LOGIN_STATE, user) 把已登录用户对象保存到会话中，作为“登录态”。
    // 后续可通过 request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE)
    // 取出登录用户，例如同文件的 getLoginUser
    request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
    StpKit.SPACE.login(user.getId());
    StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
    return this.getLoginUserVO(user);
  }

  @Override
  public User getLoginUser(HttpServletRequest request) {
    Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
    User currentuser = (User) userObj;
    if (currentuser == null || currentuser.getId() == null) {
      throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }
    return currentuser;
  }

  @Override
  public LoginUserVO getLoginUserVO(User loginUser) {
    if (loginUser == null) {
      return null;
    }
    LoginUserVO loginUserVO = new LoginUserVO();
    BeanUtils.copyProperties(loginUser, loginUserVO);
    return loginUserVO;
  }

  @Override
  public String getEncryptPassword(String userPassword) {
    final String SALT = "yunpicture";
    return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
  }

  @Override
  public Boolean userLogout(HttpServletRequest request) {
    Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
    if (userObj == null) {
      throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }
    request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
    return true;
  }

  @Override
  public User getUserById(Long id) {
    User user = this.baseMapper.selectById(id);
    if (user == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
    }
    return user;
  }

  @Override
  public UserVO getUserVOById(Long id) {
    User user = this.baseMapper.selectById(id);
    if (user == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
    }
    UserVO userVO = new UserVO();
    BeanUtils.copyProperties(user, userVO);
    return userVO;
  }

  @Override
  public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
    if (userQueryRequest == null) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
    }
    Long id = userQueryRequest.getId();
    String userName = userQueryRequest.getUserName();
    String userAccount = userQueryRequest.getUserAccount();
    String userProfile = userQueryRequest.getUserProfile();
    String userRole = userQueryRequest.getUserRole();
    String sortField = userQueryRequest.getSortField();
    String sortOrder = userQueryRequest.getSortOrder();
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
    queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
    queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
    queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
    queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
    queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
    return queryWrapper;
  }

  @Override
  public boolean isAdmin(User user) {
    return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
  }

  @Override
  public boolean exchangeVip(User user, String vipCode) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'exchangeVip'");
  }

  @Override
  public List<UserVO> getUserVOList(List<User> userList) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getUserVOList'");
  }

  @Override
  public UserVO getUserVO(User user) {
    if (user == null) {
      return null;
    }
    UserVO userVO = new UserVO();
    BeanUtil.copyProperties(user, userVO);
    return userVO;
  }
}
