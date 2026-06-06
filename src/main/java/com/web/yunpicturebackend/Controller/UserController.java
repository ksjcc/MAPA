package com.web.yunpicturebackend.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web.yunpicturebackend.Service.UserService;
import com.web.yunpicturebackend.annotation.AuthCheck;
import com.web.yunpicturebackend.common.BaseResponse;
import com.web.yunpicturebackend.common.DeleteRequest;
import com.web.yunpicturebackend.common.ResultUtils;
import com.web.yunpicturebackend.constant.UserConstant;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.exception.ThrowUtils;
import com.web.yunpicturebackend.model.vo.LoginUserVO;
import com.web.yunpicturebackend.model.vo.UserVO;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.dto.user.*;

@RestController
@RequestMapping("/user")
@Tag(name = "用户", description = "用户注册、登录、信息管理接口")
public class UserController {

  @Resource
  private UserService userservice;

  /**
   * 用户注册
   * 
   * @param userRegisterRequest
   * @return 新用户id
   */
  @PostMapping("/register")
  @Operation(summary = "用户注册", description = "根据账户、密码、校验密码注册新用户")
  public BaseResponse<Long> userRegister(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户注册请求", required = true) @RequestBody UserRegisterRequest userRegisterRequest) {
    ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
    String userAccount = userRegisterRequest.getUserAccount();
    String userPassword = userRegisterRequest.getUserPassword();
    String checkPassword = userRegisterRequest.getCheckPassword();
    Long result = userservice.userRegister(userAccount, userPassword, checkPassword);
    return ResultUtils.success(result);
  }

  /**
   * 用户登录
   * 
   * @param userLoginRequest
   * @param request
   * @return
   */
  @PostMapping("/login")
  @Operation(summary = "用户登录", description = "使用账户与密码登录并返回登录信息")
  public BaseResponse<LoginUserVO> Loginuser(
      @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户登录请求", required = true) UserLoginRequest userLoginRequest,
      @Parameter(description = "HTTP请求") HttpServletRequest request) {
    ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
    String userAccount = userLoginRequest.getUserAccount();
    String userPassword = userLoginRequest.getUserPassword();
    LoginUserVO loginUserVO = userservice.userLogin(userAccount, userPassword, request);
    return ResultUtils.success(loginUserVO);
  }

  @GetMapping("/get/login")
  @Operation(summary = "获取当前登录用户", description = "从会话中获取登录用户并返回脱敏信息")
  public BaseResponse<LoginUserVO> getLoginUser(@Parameter(description = "HTTP请求") HttpServletRequest request) {
    com.web.yunpicturebackend.model.entity.User loginUser = userservice.getLoginUser(request);
    return ResultUtils.success(userservice.getLoginUserVO(loginUser));
  }

  @PostMapping("/logout")
  @Operation(summary = "用户注销", description = "移除登录态，退出登录")
  public BaseResponse<Boolean> userLogout(@Parameter(description = "HTTP请求") HttpServletRequest request) {
    ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
    Boolean result = userservice.userLogout(request);
    return ResultUtils.success(result);
  }

  @PostMapping("/add")
  @Operation(summary = "新增用户", description = "管理员新增用户，设置默认密码")
  public BaseResponse<Long> addUser(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户添加请求", required = true) @RequestBody UserAddRequest userAddRequest) {
    ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
    User user = new User();
    BeanUtils.copyProperties(userAddRequest, user);
    final String DEFAULT_PASSWORD = "12345678";
    String encryptPassword = userservice.getEncryptPassword(DEFAULT_PASSWORD);
    user.setUserPassword(encryptPassword);
    boolean result = userservice.save(user);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    return ResultUtils.success(user.getId());
  }

  @GetMapping("/get")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  @Operation(summary = "根据ID获取用户", description = "管理员根据用户ID获取用户信息")
  public BaseResponse<User> getUserById(@Parameter(description = "用户ID", required = true) @RequestParam Long id) {
    ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
    User user = userservice.getUserById(id);
    ThrowUtils.throwIf(user == null, ErrorCode.OPERATION_ERROR);
    return ResultUtils.success(user);
  }

  @GetMapping("/get/vo")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  @Operation(summary = "根据ID获取用户VO", description = "管理员获取用户脱敏信息")
  public BaseResponse<UserVO> getUserVOById(@Parameter(description = "用户ID", required = true) @RequestParam Long id) {
    BaseResponse<User> userResponse = getUserById(id);
    User user = userResponse.getData();
    return ResultUtils.success(userservice.getUserVOById(user.getId()));
  }

  @PostMapping("/update")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  @Operation(summary = "删除用户", description = "管理员根据ID删除用户")
  public BaseResponse<Boolean> updateUser(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户删除请求", required = true) @RequestBody DeleteRequest deleteRequest) {
    if (deleteRequest == null || deleteRequest.getId() <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    boolean b = userservice.removeById(deleteRequest.getId());
    ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
    return ResultUtils.success(b);
  }

  @PostMapping("/list/page/vo")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  @Operation(summary = "分页查询用户VO", description = "管理员根据查询条件分页获取用户脱敏信息")
  public BaseResponse<Page<UserVO>> listUserVOByPage(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户查询请求", required = true) @RequestBody UserQueryRequest userQueryRequest) {
    ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
    long current = userQueryRequest.getCurrent();
    long pageSize = userQueryRequest.getPageSize();
    Page<User> userPage = userservice.page(new Page<>(current, pageSize),
        userservice.getQueryWrapper(userQueryRequest));
    Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
    List<UserVO> userVOList = userservice.getUserVOList(userPage.getRecords());
    userVOPage.setRecords(userVOList);
    return ResultUtils.success(userVOPage);
  }

}
