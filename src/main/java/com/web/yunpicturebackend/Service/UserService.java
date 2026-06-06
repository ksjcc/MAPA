package com.web.yunpicturebackend.Service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.dto.user.*;
import com.web.yunpicturebackend.model.vo.LoginUserVO;
import com.web.yunpicturebackend.model.vo.UserVO;

public interface UserService extends IService<User> {
  /**
   * 用户注册
   * 
   * @param userAccount   用户账户
   * @param userPassword  用户密码
   * @param checkPassword 校验密码
   * @return 新用户id
   */
  Long userRegister(String userAccount, String userPassword, String checkPassword);

  /**
   * 用户登录
   * 
   * @param userAccount  用户账户
   * @param userPassword 用户密码
   * @return 登录凭证
   */
  LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

  User getLoginUser(HttpServletRequest request);

  LoginUserVO getLoginUserVO(User loginUser);

  /**
   * 用户注销
   * 
   * @param request 请求
   * @return 是否成功
   */
  Boolean userLogout(HttpServletRequest request);

  /**
   * 获取加密后的密码
   *
   * @param userPassword
   * @return
   */
  String getEncryptPassword(String userPassword);

  /**
   * 根据用户id获取用户信息
   * 
   * @param id
   * @return
   */
  User getUserById(Long id);

  /**
   * 根据用户id获取用户脱敏后的信息
   * 
   * @param id
   * @return
   */
  UserVO getUserVOById(Long id);

  QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

  /**
   * 判断是否事管理员
   * 
   * @param user
   * @return
   */
  boolean isAdmin(User user);

  boolean exchangeVip(User user, String vipCode);

  /**
   * 获得脱敏后的用户信息列表
   *
   * @param userList
   * @return 脱敏后的用户列表
   */
  List<UserVO> getUserVOList(List<User> userList);

  UserVO getUserVO(User user);
}
