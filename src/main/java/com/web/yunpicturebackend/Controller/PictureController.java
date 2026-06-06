package com.web.yunpicturebackend.Controller;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.beans.BeanUtils;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.web.yunpicturebackend.Service.PictureService;
import com.web.yunpicturebackend.Service.SpaceService;
import com.web.yunpicturebackend.Service.UserService;
import com.web.yunpicturebackend.annotation.AuthCheck;
import com.web.yunpicturebackend.annotation.SaSpaceCheckPermission;
import com.web.yunpicturebackend.api.aliyunai.AliYunAiApi;
import com.web.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.web.yunpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.web.yunpicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.web.yunpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.web.yunpicturebackend.common.BaseResponse;
import com.web.yunpicturebackend.common.DeleteRequest;
import com.web.yunpicturebackend.common.ResultUtils;
import com.web.yunpicturebackend.constant.UserConstant;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.exception.ThrowUtils;
import com.web.yunpicturebackend.manager.auth.StpKit;
import com.web.yunpicturebackend.manager.auth.SpaceUserAuthManager;
import com.web.yunpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.web.yunpicturebackend.model.dto.picture.CreatePictureOutPaintingTaskRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureEditByBatchRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureEditRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureReviewRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureUpdateRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.web.yunpicturebackend.model.dto.picture.SearchPictureByColorRequest;
import com.web.yunpicturebackend.model.dto.picture.SearchPictureByPictureRequest;
import com.web.yunpicturebackend.model.vo.PictureTagCategory;
import com.web.yunpicturebackend.model.vo.PictureVO;
import com.github.benmanes.caffeine.cache.Cache;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.web.yunpicturebackend.model.entity.Picture;
import com.web.yunpicturebackend.model.entity.Space;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.enums.PictureReviewStatusEnum;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/picture")
@Tag(name = "图片接口")
public class PictureController {
  @Resource
  private UserService userService;
  @Resource
  private PictureService pictureService;
  @Resource
  private SpaceService spaceService;
  @Resource
  private StringRedisTemplate stringRedisTemplate;
  @Resource
  private SpaceUserAuthManager spaceUserAuthManager;
  @Resource
  private AliYunAiApi aliyunAiApi;

  private final Cache<String, String> LOCAL_CACHE = Caffeine.<String, String>newBuilder()
      .initialCapacity(1024)
      .maximumSize(10_000L) // 最大 10000 条
      // 缓存 5 分钟后移除
      .expireAfterWrite(Duration.ofMinutes(5))
      .build();

  /**
   * 上传图片（可重新上传）
   */
  @Operation(summary = "上传图片（可重新上传）")
  @PostMapping("/upload")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
  // @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<PictureVO> uploadPicture(
      @Parameter(description = "图片文件", required = true) @RequestPart("file") MultipartFile multipartFile,
      @Parameter(description = "图片上传请求") PictureUploadRequest pictureUploadRequest,
      HttpServletRequest request) {
    User loginUser = userService.getLoginUser(request);
    PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
    return ResultUtils.success(pictureVO);
  }

  /**
   * 通过 URL 上传图片（可重新上传）
   */
  @Operation(summary = "通过 URL 上传图片（可重新上传）")
  @PostMapping("/upload/url")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
  public BaseResponse<PictureVO> uploadPictureByUrl(
      @Parameter(description = "图片上传请求", required = true) @RequestBody PictureUploadRequest pictureUploadRequest,
      HttpServletRequest request) {
    User loginUser = userService.getLoginUser(request);
    String fileUrl = pictureUploadRequest.getFileUrl();
    PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
    return ResultUtils.success(pictureVO);
  }

  @Operation(summary = "删除图片")
  @PostMapping("/delete")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
  public BaseResponse<Boolean> deletePicture(
      @Parameter(description = "删除请求", required = true) @RequestBody DeleteRequest deleteRequest,
      HttpServletRequest request) {
    if (deleteRequest == null || deleteRequest.getId() <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    User loginUser = userService.getLoginUser(request);
    pictureService.deletePicture(deleteRequest.getId(), loginUser);
    return ResultUtils.success(true);
  }

  /**
   * 更新图片（仅管理员可用）
   *
   * @param pictureUpdateRequest
   * @param request
   * @return
   */
  @Operation(summary = "更新图片（仅管理员可用）")
  @PostMapping("/update")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Boolean> updatePicture(
      @Parameter(description = "图片更新请求", required = true) @RequestBody PictureUpdateRequest pictureUpdateRequest,
      HttpServletRequest request) {
    if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    // 将实体类和 DTO 进行转换
    Picture picture = new Picture();
    BeanUtils.copyProperties(pictureUpdateRequest, picture);
    // 注意将 list 转为 string
    picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
    // 数据校验
    pictureService.validPicture(picture);
    // 判断是否存在
    long id = pictureUpdateRequest.getId();
    Picture oldPicture = pictureService.getById(id);
    ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
    // 补充审核参数
    User loginUser = userService.getLoginUser(request);
    pictureService.fillReviewParams(oldPicture, loginUser);
    // 操作数据库
    boolean result = pictureService.updateById(picture);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    return ResultUtils.success(true);
  }

  /**
   * 根据 id 获取图片（仅管理员可用）
   */
  @Operation(summary = "根据 id 获取图片（仅管理员可用）")
  @GetMapping("/get")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Picture> getPictureById(@Parameter(description = "图片 ID", required = true) long id,
      HttpServletRequest request) {
    ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
    // 查询数据库
    Picture picture = pictureService.getById(id);
    ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
    // 获取封装类
    return ResultUtils.success(picture);
  }

  /**
   * 根据 id 获取图片（封装类）
   */
  @Operation(summary = "根据 id 获取图片（封装类）")
  @GetMapping("/get/vo")
  public BaseResponse<PictureVO> getPictureVOById(@Parameter(description = "图片 ID", required = true) long id,
      HttpServletRequest request) {
    ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
    // 查询数据库
    Picture picture = pictureService.getById(id);
    ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
    // 空间权限校验
    Long spaceId = picture.getSpaceId();
    Space space = null;
    User loginUser = userService.getLoginUser(request); // 先获取登录用户
    if (spaceId != null) {
      boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
      ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
      space = spaceService.getById(spaceId);
      ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
    }
    // 获取权限列表
    List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
    PictureVO pictureVO = pictureService.getPictureVO(picture, request);
    pictureVO.setPermissionList(permissionList);
    // 获取封装类
    return ResultUtils.success(pictureVO);
  }

  /**
   * 分页获取图片列表（仅管理员可用）
   */
  @Operation(summary = "分页获取图片列表（仅管理员可用）")
  @PostMapping("/list/page")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Page<Picture>> listPictureByPage(
      @Parameter(description = "图片查询请求", required = true) @RequestBody PictureQueryRequest pictureQueryRequest) {
    long current = pictureQueryRequest.getCurrent();
    long size = pictureQueryRequest.getPageSize();
    // 查询数据库
    Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
        pictureService.getQueryWrapper(pictureQueryRequest));
    return ResultUtils.success(picturePage);
  }

  /**
   * 分页获取图片列表（封装类）
   */
  @Operation(summary = "分页获取图片列表（封装类）")
  @PostMapping("/list/page/vo")
  public BaseResponse<Page<PictureVO>> listPictureVOByPage(
      @Parameter(description = "图片查询请求", required = true) @RequestBody PictureQueryRequest pictureQueryRequest,
      HttpServletRequest request) {
    long current = pictureQueryRequest.getCurrent();
    long size = pictureQueryRequest.getPageSize();
    // 限制爬虫
    ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
    // 空间权限校验
    Long spaceId = pictureQueryRequest.getSpaceId();
    if (spaceId == null) {
      // 公开图库
      // 普通用户默认只能看到审核通过的数据
      pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
      pictureQueryRequest.setNullSpaceId(true);
    } else {
      boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
      ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
    }
    // 查询数据库
    Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
        pictureService.getQueryWrapper(pictureQueryRequest));
    // 获取封装类
    return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
  }

  /**
   * 分页获取图片列表（封装类，有缓存）
   */
  @Deprecated
  @Operation(summary = "分页获取图片列表（封装类，有缓存）")
  @PostMapping("/list/page/vo/cache")
  public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(
      @Parameter(description = "图片查询请求", required = true) @RequestBody PictureQueryRequest pictureQueryRequest,
      HttpServletRequest request) {
    long current = pictureQueryRequest.getCurrent();
    long size = pictureQueryRequest.getPageSize();
    // 限制爬虫
    ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
    // 普通用户默认只能看到审核通过的数据
    pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
    // 查询缓存，缓存中没有，再查询数据库
    // 构建缓存的 key
    String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
    String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
    String cacheKey = String.format("yunpicture:listPictureVOByPage:%s", hashKey);
    // 1. 先从本地缓存中查询
    String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
    if (cachedValue != null) {
      // 如果缓存命中，返回结果
      Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
      return ResultUtils.success(cachedPage);
    }
    // 2. 本地缓存未命中，查询 Redis 分布式缓存
    ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
    cachedValue = opsForValue.get(cacheKey);
    if (cachedValue != null) {
      // 如果缓存命中，更新本地缓存，返回结果
      LOCAL_CACHE.put(cacheKey, cachedValue);
      Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
      return ResultUtils.success(cachedPage);
    }
    // 3. 查询数据库
    Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
        pictureService.getQueryWrapper(pictureQueryRequest));
    Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
    // 4. 更新缓存
    // 更新 Redis 缓存
    String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
    // 设置缓存的过期时间，5 - 10 分钟过期，防止缓存雪崩
    int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
    opsForValue.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
    // 写入本地缓存
    LOCAL_CACHE.put(cacheKey, cacheValue);
    // 获取封装类
    return ResultUtils.success(pictureVOPage);
  }

  /**
   * 编辑图片（给用户使用）
   */
  @Operation(summary = "编辑图片（给用户使用）")
  @PostMapping("/edit")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
  public BaseResponse<Boolean> editPicture(
      @Parameter(description = "图片编辑请求", required = true) @RequestBody PictureEditRequest pictureEditRequest,
      HttpServletRequest request) {
    if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    User loginUser = userService.getLoginUser(request);
    pictureService.editPicture(pictureEditRequest, loginUser);
    return ResultUtils.success(true);
  }

  @Operation(summary = "获取图片标签和分类")
  @GetMapping("/tag_category")
  public BaseResponse<PictureTagCategory> listPictureTagCategory() {
    PictureTagCategory pictureTagCategory = new PictureTagCategory();
    List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
    List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
    pictureTagCategory.setTagList(tagList);
    pictureTagCategory.setCategoryList(categoryList);
    return ResultUtils.success(pictureTagCategory);
  }

  /**
   * 审核图片
   */
  @Operation(summary = "审核图片")
  @PostMapping("/review")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Boolean> doPictureReview(
      @Parameter(description = "图片审核请求", required = true) @RequestBody PictureReviewRequest pictureReviewRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    pictureService.doPictureReview(pictureReviewRequest, loginUser);
    return ResultUtils.success(true);
  }

  /**
   * 批量抓取并创建图片
   */
  @Operation(summary = "批量抓取并创建图片")
  @PostMapping("/upload/batch")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Integer> uploadPictureByBatch(
      @Parameter(description = "批量图片上传请求", required = true) @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
    return ResultUtils.success(uploadCount);
  }

  /**
   * 以图搜图
   */
  @Operation(summary = "以图搜图")
  @PostMapping("/search/picture")
  public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(
      @Parameter(description = "以图搜图请求", required = true) @RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
    ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
    Long pictureId = searchPictureByPictureRequest.getPictureId();
    ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
    Picture picture = pictureService.getById(pictureId);
    ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
    List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(picture.getUrl());
    return ResultUtils.success(resultList);
  }

  /**
   * 按照颜色搜索
   */
  @Operation(summary = "按照颜色搜索")
  @PostMapping("/search/color")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
  public BaseResponse<List<PictureVO>> searchPictureByColor(
      @Parameter(description = "按照颜色搜索图片请求", required = true) @RequestBody SearchPictureByColorRequest searchPictureByColorRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
    String picColor = searchPictureByColorRequest.getPicColor();
    Long spaceId = searchPictureByColorRequest.getSpaceId();
    User loginUser = userService.getLoginUser(request);
    List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
    return ResultUtils.success(pictureVOList);
  }

  /**
   * 批量编辑图片
   */
  @Operation(summary = "批量编辑图片")
  @PostMapping("/edit/batch")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
  public BaseResponse<Boolean> editPictureByBatch(
      @Parameter(description = "批量编辑图片请求", required = true) @RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
    return ResultUtils.success(true);
  }

  public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
      @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    CreateOutPaintingTaskResponse response = pictureService
        .createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
    return ResultUtils.success(response);
  }

  @GetMapping("/out_painting/get_task")
  public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
    ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
    GetOutPaintingTaskResponse task = aliyunAiApi.getOutPaintingTask(taskId);
    return ResultUtils.success(task);
  }
  // /**
  // * 创建 AI 扩图任务
  // */
  // @PostMapping("/out_painting/create_task")
  // @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
  // public BaseResponse<CreateOutPaintingTaskResponse>
  // createPictureOutPaintingTask(
  // @RequestBody CreatePictureOutPaintingTaskRequest
  // createPictureOutPaintingTaskRequest,
  // HttpServletRequest request) {
  // if (createPictureOutPaintingTaskRequest == null ||
  // createPictureOutPaintingTaskRequest.getPictureId() == null) {
  // throw new BusinessException(ErrorCode.PARAMS_ERROR);
  // }
  // User loginUser = userService.getLoginUser(request);
  // CreateOutPaintingTaskResponse response = pictureService
  // .createPictureOutPaintingTask(createPictureOutPaintingTaskRequest,
  // loginUser);
  // return ResultUtils.success(response);
  // }

  // /**
  // * 查询 AI 扩图任务
  // */
  // @GetMapping("/out_painting/get_task")
  // public BaseResponse<GetOutPaintingTaskResponse>
  // getPictureOutPaintingTask(String taskId) {
  // ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
  // GetOutPaintingTaskResponse task = aliyunAiApi.getOutPaintingTask(taskId);
  // return ResultUtils.success(task);
  // }
}
