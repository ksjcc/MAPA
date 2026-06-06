package com.web.yunpicturebackend.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.web.yunpicturebackend.Service.PictureService;
import com.web.yunpicturebackend.Service.SpaceAnalyzeService;
import com.web.yunpicturebackend.Service.SpaceService;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.exception.ThrowUtils;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceRankAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceSizeAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.web.yunpicturebackend.model.dto.space.analyze.SpaceUserAnalyzeRequest;
import com.web.yunpicturebackend.model.entity.Picture;
import com.web.yunpicturebackend.model.entity.Space;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceSizeAnalyzeResponse;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.web.yunpicturebackend.model.vo.space.analyze.SpaceUserAnalyzeResponse;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;

import com.web.yunpicturebackend.Service.UserService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {
  @Resource
  private UserService userService;
  @Resource
  private SpaceService spaceService;
  @Resource
  private PictureService pictureService;

  public SpaceAnalyzeServiceImpl(UserService userService) {
    this.userService = userService;
  }

  private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
    if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
      ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
    }
    Long spaceId = spaceAnalyzeRequest.getSpaceId();
    ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
    Space space = spaceService.getById(spaceId);
    ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
    spaceService.checkSpaceAuth(loginUser, space);
  }

  private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest,
      QueryWrapper<Picture> queryWrapper) {
    if (spaceAnalyzeRequest.isQueryAll()) {
      return;
    }
    if (spaceAnalyzeRequest.isQueryPublic()) {
      queryWrapper.isNull("spaceId");
      return;
    }
    Long spaceId = spaceAnalyzeRequest.getSpaceId();
    if (spaceId != null) {
      queryWrapper.eq("spaceId", spaceId);
      return;
    }
    throw new BusinessException(ErrorCode.PARAMS_ERROR);

  }

  @Override
  public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
      User loginUser) {
    ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
      boolean isAdmin = userService.isAdmin(loginUser);
      ThrowUtils.throwIf(!isAdmin, ErrorCode.NO_AUTH_ERROR);
      QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
      queryWrapper.select("picSize");
      if (!spaceUsageAnalyzeRequest.isQueryAll()) {
        fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
      }
      List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
      long usedsize = pictureObjList.stream().mapToLong(obj -> (Long) obj).sum();
      long usedCount = pictureObjList.size();
      // 封装返回结果
      SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
      spaceUsageAnalyzeResponse.setUsedSize(usedsize);
      spaceUsageAnalyzeResponse.setUsedCount(usedCount);
      // 公共图库（或者全部空间）无数量和容量限制、也没有比例
      spaceUsageAnalyzeResponse.setMaxSize(null);
      spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
      spaceUsageAnalyzeResponse.setMaxCount(null);
      spaceUsageAnalyzeResponse.setCountUsageRatio(null);
      return spaceUsageAnalyzeResponse;
    } else {
      Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
      ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
      Space space = spaceService.getById(spaceId);
      ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
      checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
      // 封装返回结果
      SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
      spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
      spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
      spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
      spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
      // 计算比例
      double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
      double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
      spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
      spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
      return spaceUsageAnalyzeResponse;
    }
  }

  @Override
  public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(
      SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
    ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
    QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
    fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
    queryWrapper.select("category AS category",
        "COUNT(*) AS count",
        "SUM(picSize) AS totalSize").groupBy("category");
    // 查询并转换结果
    return pictureService.getBaseMapper().selectMaps(queryWrapper)
        .stream()
        .map(result -> {
          String category = (String) result.get("category");
          Long count = ((Number) result.get("count")).longValue();
          Long totalSize = ((Number) result.get("totalSize")).longValue();
          return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
        })
        .collect(Collectors.toList());
  }

  @Override
  public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRquest,
      User loginUser) {
    ThrowUtils.throwIf(spaceTagAnalyzeRquest == null, ErrorCode.PARAMS_ERROR);
    checkSpaceAnalyzeAuth(spaceTagAnalyzeRquest, loginUser);
    QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
    fillAnalyzeQueryWrapper(spaceTagAnalyzeRquest, queryWrapper);
    queryWrapper.select("tag AS tag");
    List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
        .stream()
        .filter(ObjUtil::isNotEmpty)
        .map(Object::toString)
        .collect(Collectors.toList());
    // 解析标签并统计
    Map<String, Long> tagCountMap = tagsJsonList.stream()
        // ["Java", "Python"], ["Java", "PHP"] => "Java", "Python", "Java", "PHP"
        .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
        .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
    return tagCountMap.entrySet().stream()
        .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())) // 降序排序
        .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
      User loginUser) {
    ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

    // 构造查询条件
    QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
    fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

    // 查询所有符合条件的图片大小
    queryWrapper.select("picSize");
    // 100、120、1000
    List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper)
        .stream()
        .filter(ObjUtil::isNotNull)
        .map(size -> (Long) size)
        .collect(Collectors.toList());
    // 定义分段范围，注意使用有序的 Map
    Map<String, Long> sizeRanges = new LinkedHashMap<>();
    sizeRanges.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());
    sizeRanges.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
    sizeRanges.put("500KB-1MB",
        picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
    sizeRanges.put(">1MB", picSizeList.stream().filter(size -> size >= 1 * 1024 * 1024).count());

    // 转换为响应对象
    return sizeRanges.entrySet().stream()
        .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
      User loginUser) {
    ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    // 检查权限
    checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

    // 构造查询条件
    QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
    fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
    // 补充用户 id 查询
    Long userId = spaceUserAnalyzeRequest.getUserId();
    queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
    // 补充分析维度：每日、每周、每月
    String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
    switch (timeDimension) {
      case "day":
        queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period", "count(*) as count");
        break;
      case "week":
        queryWrapper.select("YEARWEEK(createTime) as period", "count(*) as count");
        break;
      case "month":
        queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as period", "count(*) as count");
        break;
      default:
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
    }

    // 分组排序
    queryWrapper.groupBy("period").orderByAsc("period");

    // 查询并封装结果
    List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
    return queryResult
        .stream()
        .map(result -> {
          String period = result.get("period").toString();
          Long count = ((Number) result.get("count")).longValue();
          return new SpaceUserAnalyzeResponse(period, count);
        })
        .collect(Collectors.toList());
  }

  @Override
  public List<Space> getSpaceRankAnalyzeRequests(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
      User loginUser) {
    ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    // 检查权限
    ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
    QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
    queryWrapper.select("id", "spaceName", "userId", "totalSize")
        .orderByDesc("totalSize")
        .last("limit " + spaceRankAnalyzeRequest.getTopN()); // 取前 N 名
    // 查询并封装结果
    return spaceService.list(queryWrapper);
  }
}
