package com.web.yunpicturebackend.manager;

import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.web.yunpicturebackend.config.CosClientConfig;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.exception.ThrowUtils;
import com.web.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.web.yunpicturebackend.model.vo.PictureVO;
import java.io.File;

@Slf4j
@Service
public class FileManager {
  @Resource
  private CosClientConfig cosClientConfig;
  @Resource
  private COSClient cosClient;
  @Resource
  private CosManager cosManager;

  public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
    validPicture(multipartFile);
    // 图片上传地址
    String uuid = RandomUtil.randomString(16);
    String originalfilename = uuid + "." + FileUtil.getSuffix(multipartFile.getOriginalFilename());
    // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
    String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
        FileUtil.getSuffix(originalfilename));
    String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
    File file = null;
    try {
      // 上传文件
      file = File.createTempFile(uploadPath, null);
      multipartFile.transferTo(file);
      PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
      // 获取图片信息对象
      ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
      // 计算宽高
      int picWidth = imageInfo.getWidth();
      int picHeight = imageInfo.getHeight();
      double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
      // 封装返回结果
      UploadPictureResult uploadPictureResult = new UploadPictureResult();
      uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
      uploadPictureResult.setPicName(FileUtil.mainName(originalfilename));
      uploadPictureResult.setPicSize(FileUtil.size(file));
      uploadPictureResult.setPicWidth(picWidth);
      uploadPictureResult.setPicHeight(picHeight);
      uploadPictureResult.setPicScale(picScale);
      uploadPictureResult.setPicFormat(imageInfo.getFormat());
      // 返回可访问的地址
      return uploadPictureResult;
    } catch (Exception e) {
      log.error("图片上传到对象存储失败", e);
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
    } finally {
      // 临时文件清理
      this.deleteTempFile(file);
    }
  }

  private void validPicture(MultipartFile multipartFile) {
    ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
    long filesize = multipartFile.getSize();
    final long ONE_M = 1024 * 1024;
    ThrowUtils.throwIf(filesize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
    String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
    final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
    ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件格式不支持");
  }

  /**
   * 清理临时文件
   *
   * @param file
   */
  public void deleteTempFile(File file) {
    if (file == null) {
      return;
    }
    // 删除临时文件
    boolean deleteResult = file.delete();
    if (!deleteResult) {
      log.error("file delete error, filepath = {}", file.getAbsolutePath());
    }
  }
}
