package com.web.yunpicturebackend.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.qcloud.cos.utils.IOUtils;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.web.yunpicturebackend.annotation.AuthCheck;
import com.web.yunpicturebackend.common.BaseResponse;
import com.web.yunpicturebackend.common.ResultUtils;
import com.web.yunpicturebackend.constant.UserConstant;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.manager.CosManager;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试文件上传的接口
 */
@Slf4j
@RestController
@RequestMapping("/file")
@Tag(name = "文件", description = "文件上传与下载接口")
public class FileController {
  @Resource
  private CosManager cosManager;

  // @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  @PostMapping(value = "/test/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "测试上传文件", description = "上传文件到 COS 并返回路径")
  public BaseResponse<String> testuploadFile(
      @Parameter(description = "要上传的文件", required = true) @RequestParam("file") MultipartFile multipartFile) {
    if (multipartFile == null || multipartFile.isEmpty()) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
    }
    File file = null;
    String filename = multipartFile.getOriginalFilename();
    if (filename == null || filename.trim().isEmpty()) {
      filename = "file-" + System.currentTimeMillis();
    }
    String filepath = String.format("test/%s", filename);
    try {
      String suffix = "";
      if (filename != null && filename.contains(".")) {
        suffix = filename.substring(filename.lastIndexOf('.'));
      }
      file = File.createTempFile("upload-", suffix);
      multipartFile.transferTo(file);
      cosManager.putObject(filepath, file);
      return ResultUtils.success(filepath);
    } catch (Exception e) {
      log.error("testuploadFile error", e);
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件失败");
    } finally {
      if (file != null) {
        boolean delete = file.delete();
        if (!delete) {
          log.error("delete file error, filepath: {}", filepath);
        }
      }
    }
  }

  /**
   * 测试文件下载
   *
   * @param filepath 文件路径
   * @param response 响应对象
   * @throws IOException
   */
  // @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  @GetMapping("/test/download")
  @Operation(summary = "测试下载文件", description = "从 COS 下载文件并返回二进制流")
  public void testDownloadFile(@Parameter(description = "文件路径", required = true) String filepath,
      HttpServletResponse response) throws IOException {
    COSObjectInputStream cosobjectInput = null;
    try {
      COSObject cosobject = cosManager.getObject(filepath);
      cosobjectInput = cosobject.getObjectContent();
      byte[] bytes = IOUtils.toByteArray(cosobjectInput);
      // 设置响应头
      response.setContentType("application/octet-stream");
      response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
      response.getOutputStream().write(bytes);
      response.getOutputStream().flush();
    } catch (Exception e) {
      log.error("testDownloadFile error, filepath: {}", filepath, e);
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载文件失败");
    } finally {
      // 释放流
      if (cosobjectInput != null) {
        cosobjectInput.close();
      }
    }
  }

}
