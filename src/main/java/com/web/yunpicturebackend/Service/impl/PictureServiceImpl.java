package com.web.yunpicturebackend.Service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;
import java.awt.Color;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.web.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.web.yunpicturebackend.manager.CosManager;
import com.web.yunpicturebackend.manager.upload.PictureUploadTemplate;
import com.web.yunpicturebackend.manager.upload.UrlPictureUpload;
import com.web.yunpicturebackend.manager.vector.VectorAsyncService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.web.yunpicturebackend.Service.PictureService;
import com.web.yunpicturebackend.Service.SpaceService;
import com.web.yunpicturebackend.Service.UserService;
import com.web.yunpicturebackend.api.aliyunai.AliYunAiApi;
import com.web.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.web.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.web.yunpicturebackend.common.BaseResponse;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.exception.ThrowUtils;
import com.web.yunpicturebackend.mapper.PictureMapper;
import com.web.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.web.yunpicturebackend.model.dto.picture.CreatePictureOutPaintingTaskRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureEditByBatchRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureEditRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureReviewRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureUpdateRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.web.yunpicturebackend.model.entity.Picture;
import com.web.yunpicturebackend.model.entity.Space;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.vo.PictureVO;
import com.web.yunpicturebackend.model.vo.UserVO;
import com.web.yunpicturebackend.util.ColorSimilarUtils;
import com.web.yunpicturebackend.util.ColorTransformUtils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {
  @Resource
  private PictureUploadTemplate pictureUploadTemplate;
  @Resource
  private SpaceService spaceService;
  @Resource
  private UrlPictureUpload urlPictureUpload;
  @Resource
  private PictureUploadTemplate filePictureUpload;
  @Resource
  private TransactionTemplate transactionTemplate;
  @Resource
  private UserService userService;
  @Resource
  private CosManager cosManager;
  @Resource
  private AliYunAiApi aliYunAiApi;
  @Resource
  private VectorAsyncService vectorAsyncService;
  @Resource
  private PictureAnalysisAsyncService pictureAnalysisAsyncService;

  @Override
  public List<Picture> searchByKeyWord(String keyword, Long spaceId) {

    LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<>();

    wrapper.eq(Picture::getIsDelete, 0);

    if (spaceId != null) {
      wrapper.eq(Picture::getSpaceId, spaceId);
    }

    wrapper.and(q -> q
        .like(Picture::getName, keyword)
        .or()
        .like(Picture::getIntroduction, keyword)
        .or()
        .like(Picture::getTags, keyword));

    wrapper.last("limit 20");

    return this.list(wrapper);
  }

  @Override
  public void validPicture(Picture picture) {
    ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
    Long id = picture.getId();
    String url = picture.getUrl();
    String introduction = picture.getIntroduction();
    ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "图片id不能为空");
    if (StrUtil.isNotBlank(url)) {
      ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
    }
    if (StrUtil.isNotBlank(introduction)) {
      ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
    }
  }

  @Override
  public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
    ThrowUtils.throwIf(!(inputSource instanceof MultipartFile) && !(inputSource instanceof String),
        ErrorCode.PARAMS_ERROR, "文件格式错误");
    Long spaceid = pictureUploadRequest.getSpaceId();
    if (spaceid != null) {
      Space space = spaceService.getById(spaceid);
      ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
      if (space.getTotalCount() >= space.getMaxCount()) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
      }
      if (space.getTotalSize() >= space.getMaxSize()) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
      }
    }
    Long pictureId = null;
    if (pictureUploadRequest != null) {
      pictureId = pictureUploadRequest.getId();
    }
    if (pictureId != null) {
      Picture oldPicture = this.getById(pictureId);
      ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
      if (spaceid == null) {
        if (oldPicture.getSpaceId() != null) {
          spaceid = oldPicture.getSpaceId();
        }
      } else {
        if (ObjUtil.notEqual(spaceid, oldPicture.getSpaceId())) {
          throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
        }
      }
    }
    String uploadPathPrefix;
    if (spaceid == null) {
      uploadPathPrefix = String.format("public/%s", loginUser.getId());
    } else {
      uploadPathPrefix = String.format("space/%s", spaceid);
    }
    PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
    if (inputSource instanceof String) {
      pictureUploadTemplate = urlPictureUpload;
    }
    UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
    Picture picture = new Picture();
    picture.setSpaceId(spaceid);
    picture.setUrl(uploadPictureResult.getUrl());
    picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
    String picName = uploadPictureResult.getPicName();
    if (pictureUploadRequest != null && StrUtil.isNotBlank(picName)) {
      picName = pictureUploadRequest.getPicName();
    }
    picture.setName(picName);
    picture.setPicSize(uploadPictureResult.getPicSize());
    picture.setPicWidth(uploadPictureResult.getPicWidth());
    picture.setPicHeight(uploadPictureResult.getPicHeight());
    picture.setPicScale(uploadPictureResult.getPicScale());
    picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
    picture.setUserId(loginUser.getId());
    this.fillReviewParams(picture, loginUser);
    if (pictureId != null) {
      picture.setId(pictureId);
      picture.setEditTime(new Date());
    }
    Picture oldPictureFinal = pictureId == null ? null : this.getById(pictureId);
    // 开启事务
    Long finalSpaceId = spaceid;
    transactionTemplate.execute(status -> {
      boolean result = this.saveOrUpdate(picture);
      ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片保存失败");
      if (finalSpaceId != null) {
        long sizeDelta = picture.getPicSize() == null ? 0L : picture.getPicSize();
        long countDelta = 1L;
        if (oldPictureFinal != null) {
          sizeDelta -= Optional.ofNullable(oldPictureFinal.getPicSize()).orElse(0L);
          countDelta = 0L;
        }
        boolean update = spaceService.lambdaUpdate()
            .eq(Space::getId, finalSpaceId)
            .setSql("totalSize = totalSize + " + sizeDelta)
            .setSql("totalCount = totalCount + " + countDelta)
            .update();
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
      }
      return picture;
    });
    vectorAsyncService.embedding(picture.getId());
    pictureAnalysisAsyncService.analyzeAndSave(picture.getId());
    if (oldPictureFinal != null && !Objects.equals(oldPictureFinal.getUrl(), picture.getUrl())) {
      this.clearPictureFile(oldPictureFinal);
    }
    return PictureVO.objToVo(picture);
  }

  @Override
  public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
    QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
    if (pictureQueryRequest == null) {
      return queryWrapper;
    }
    // 从对象中取值
    Long id = pictureQueryRequest.getId();
    String name = pictureQueryRequest.getName();
    String introduction = pictureQueryRequest.getIntroduction();
    String category = pictureQueryRequest.getCategory();
    List<String> tags = pictureQueryRequest.getTags();
    Long picSize = pictureQueryRequest.getPicSize();
    Integer picWidth = pictureQueryRequest.getPicWidth();
    Integer picHeight = pictureQueryRequest.getPicHeight();
    Double picScale = pictureQueryRequest.getPicScale();
    String picFormat = pictureQueryRequest.getPicFormat();
    String searchText = pictureQueryRequest.getSearchText();
    Long userId = pictureQueryRequest.getUserId();
    Integer reviewStatus = pictureQueryRequest.getReviewStatus();
    String reviewMessage = pictureQueryRequest.getReviewMessage();
    Long reviewerId = pictureQueryRequest.getReviewerId();
    Long spaceId = pictureQueryRequest.getSpaceId();
    Date startEditTime = pictureQueryRequest.getStartEditTime();
    Date endEditTime = pictureQueryRequest.getEndEditTime();
    boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
    String sortField = pictureQueryRequest.getSortField();
    String sortOrder = pictureQueryRequest.getSortOrder();
    if (StrUtil.isNotBlank(searchText)) {
      queryWrapper.and(
          qw -> qw.like("name", searchText)
              .or()
              .like("introduction", searchText));
    }
    queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
    queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
    queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
    queryWrapper.isNull(nullSpaceId, "spaceId");
    queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
    queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
    queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
    queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
    queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
    queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
    queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
    queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
    queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
    queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
    queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
    queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
    queryWrapper.le(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
    if (CollUtil.isNotEmpty(tags)) {
      /* and (tag like "%\"Java\"%" and like "%\"Python\"%") */
      for (String tag : tags) {
        queryWrapper.like("tags", "%\"" + tag + "\"%");
      }
    }
    queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
    return queryWrapper;
  }

  @Override
  public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
    PictureVO pictureVO = PictureVO.objToVo(picture);
    Long userId = picture.getUserId();
    if (userId != null && userId > 0) {
      User user = userService.getById(userId);
      UserVO userVO = userService.getUserVO(user);
      pictureVO.setUser(userVO);
    }
    return pictureVO;
  }

  @Override
  public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
    List<Picture> pictureList = picturePage.getRecords();
    Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
    // 对象列表 => 封装对象列表
    List<PictureVO> pictureVOList = pictureList.stream()
        .map(PictureVO::objToVo)
        .collect(Collectors.toList());
    Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
    Map<Long, List<User>> userIdUserListMap = userIdSet.isEmpty() ? new java.util.HashMap<>()
        : userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
    // 2. 填充信息
    pictureVOList.forEach(pictureVO -> {
      Long userId = pictureVO.getUserId();
      User user = null;
      if (userIdUserListMap.containsKey(userId)) {
        user = userIdUserListMap.get(userId).get(0);
      }
      pictureVO.setUser(userService.getUserVO(user));
    });
    pictureVOPage.setRecords(pictureVOList);
    return pictureVOPage;
  }

  @Override
  public void deletePicture(long pictureId, User loginUser) {
    ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
    ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
    Picture oldPicture = this.getById(pictureId);
    ThrowUtils.throwIf(oldPicture == null, ErrorCode.FORBIDDEN_ERROR);

    // 开启事务
    transactionTemplate.execute(status -> {
      // 操作数据库
      boolean result = this.removeById(pictureId);
      ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
      // 更新空间的使用额度，释放额度
      boolean update = spaceService.lambdaUpdate()
          .eq(Space::getId, oldPicture.getSpaceId())
          .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
          .setSql("totalCount = totalCount - 1")
          .update();
      ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
      return true;
    });
    vectorAsyncService.deleteVector(pictureId);
    // 异步清理文件
    this.clearPictureFile(oldPicture);
  }

  @Override
  public void clearPictureFile(Picture oldPicture) {
    String pictureUrl = oldPicture.getUrl();
    long count = this.lambdaQuery()
        .eq(Picture::getUrl, pictureUrl)
        .count();
    if (count > 1) {
      return;
    }
    // 删除图片
    cosManager.deleteObject(pictureUrl);
    String thumbnailUrl = oldPicture.getThumbnailUrl();
    // 删除缩略图
    if (StrUtil.isNotBlank(thumbnailUrl)) {
      cosManager.deleteObject(thumbnailUrl);
    }
  }

  @Override
  public void fillReviewParams(Picture picture, User loginUser) {
    if (userService.isAdmin(loginUser)) {
      picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
      picture.setReviewerId(loginUser.getId());
      picture.setReviewMessage("管理员自动过审");
      picture.setReviewTime(new Date());
    } else {
      picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
    }

  }

  @Override
  public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
    ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
    Long id = pictureReviewRequest.getId();
    Integer reviewStatus = pictureReviewRequest.getReviewStatus();
    PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
    String reviewMessage = pictureReviewRequest.getReviewMessage();
    if (id == null || reviewStatusEnum == null) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    Picture oldPicture = this.getById(id);
    ThrowUtils.throwIf(oldPicture == null, ErrorCode.FORBIDDEN_ERROR);
    if (oldPicture.getReviewStatus().equals(reviewStatus)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    Picture updatePicture = new Picture();
    BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
    updatePicture.setReviewerId(loginUser.getId());
    updatePicture.setReviewTime(new Date());
    boolean result = this.updateById(updatePicture);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
  }

  public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
    String searchText = pictureUploadByBatchRequest.getSearchText();
    Integer count = pictureUploadByBatchRequest.getCount();
    String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
    ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
    if (StrUtil.isBlank(searchText)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    if (StrUtil.isBlank(namePrefix)) {
      namePrefix = searchText;
    }
    String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
    Document document;
    try {
      document = Jsoup.connect(fetchUrl).get();
    } catch (IOException e) {
      log.error("获取图片失败", e);
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片失败");
    }
    Element div = document.getElementsByClass("dgControl").first();
    return 0;
  }

  @Override
  public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
    Picture picture = new Picture();
    BeanUtils.copyProperties(pictureEditRequest, picture);
    picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
    picture.setEditTime(new Date());
    this.validPicture(picture);
    long id = pictureEditRequest.getId();
    Picture oldPicture = this.getById(id);
    ThrowUtils.throwIf(oldPicture == null, ErrorCode.FORBIDDEN_ERROR);

    // 补充审核参数
    this.fillReviewParams(picture, loginUser);
    boolean result = this.updateById(picture);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
  }

  // @Override
  // public void checkPictureAuth(User loginUser, Picture picture) {
  // Long spaceId = picture.getId();
  // Long loginUserId = loginUser.getId();
  // // 公共图库，本人和管理员均可操作
  // if (spaceId == null) {
  // if (!picture.getUserId().equals(loginUserId) &&
  // !userService.isAdmin(loginUser)) {
  // throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
  // }
  // } else {
  // // 私有图库，仅管理员可以操作
  // if (!picture.getUserId().equals(loginUserId)) {
  // throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
  // }
  // }
  // }

  @Override
  public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
    ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
    ThrowUtils.throwIf(loginUser == null, ErrorCode.FORBIDDEN_ERROR);
    Space space = spaceService.getById(spaceId);
    ThrowUtils.throwIf(space == null, ErrorCode.FORBIDDEN_ERROR);
    if (!space.getUserId().equals(loginUser.getId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
    }
    List<Picture> pictureList = this.lambdaQuery()
        .eq(Picture::getSpaceId, spaceId)
        .like(Picture::getTags, picColor)
        .list();
    if (CollUtil.isEmpty(pictureList)) {
      return new ArrayList<>();
    }
    // 将颜色字符串转换为主色调
    Color targetColor = Color.decode(picColor);
    // 4. 计算相似度并排序
    List<Picture> sortedPictureList = pictureList.stream()
        .sorted(Comparator.comparingDouble(picture -> {
          String hexColor = picture.getPicColor();
          // 没有主色调的图片会默认排序到最后
          if (StrUtil.isBlank(hexColor)) {
            return Double.MAX_VALUE;
          }
          Color pictureColor = Color.decode(hexColor);
          // 计算相似度
          // 越大越相似
          return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
        }))
        .limit(12) // 取前 12 个
        .collect(Collectors.toList());
    return sortedPictureList.stream()
        .map(PictureVO::objToVo)
        .collect(Collectors.toList());
  }

  @Override
  public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
    List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
    Long spaceId = pictureEditByBatchRequest.getSpaceId();
    String category = pictureEditByBatchRequest.getCategory();
    List<String> tags = pictureEditByBatchRequest.getTags();
    ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
    ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
    ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
    Space space = spaceService.getById(spaceId);
    ThrowUtils.throwIf(space == null, ErrorCode.FORBIDDEN_ERROR);
    if (!space.getUserId().equals(loginUser.getId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
    }
    List<Picture> pictureList = this.lambdaQuery()
        .select(Picture::getId, Picture::getSpaceId)
        .eq(Picture::getSpaceId, spaceId)
        .in(Picture::getId, pictureIdList)
        .list();
    if (pictureList.isEmpty()) {
      return;
    }
    pictureList.forEach(picture -> {
      if (StrUtil.isNotBlank(category)) {
        picture.setCategory(category);
      }
      if (CollUtil.isNotEmpty(tags)) {
        picture.setTags(JSONUtil.toJsonStr(tags));
      }
    });
    // 批量重命名
    String nameRule = pictureEditByBatchRequest.getNameRule();
    fillPictureWithNameRule(pictureList, nameRule);
    // 5. 操作数据库进行批量更新
    boolean result = this.updateBatchById(pictureList);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量编辑失败");
  }

  /**
   * nameRule 格式：图片{序号}
   *
   * @param pictureList
   * @param nameRule
   */
  private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
    if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
      return;
    }
    long count = 1;
    try {
      for (Picture picture : pictureList) {
        String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
        picture.setName(pictureName);
      }
    } catch (Exception e) {
      log.error("名称解析错误", e);
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
    }
  }

  @Override
  public CreateOutPaintingTaskResponse createPictureOutPaintingTask(
      CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
    // 获取图片信息
    Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
    Picture picture = Optional.ofNullable(this.getById(pictureId))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
    // 校验权限，已经改为使用注解鉴权
    // checkPictureAuth(loginUser, picture);
    // 创建扩图任务
    CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
    CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
    input.setImageUrl(picture.getUrl());
    createOutPaintingTaskRequest.setInput(input);
    createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
    // 创建任务
    return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
  }
}
