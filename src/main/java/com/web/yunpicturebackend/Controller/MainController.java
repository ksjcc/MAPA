package com.web.yunpicturebackend.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.web.yunpicturebackend.common.BaseResponse;
import com.web.yunpicturebackend.common.ResultUtils;

@RestController
@RequestMapping("/")
@Tag(name = "系统", description = "系统与健康检查接口")
public class MainController {
  /**
   * 健康检查接口
   * 
   * @return
   */
  @GetMapping("/health")
  @Operation(summary = "健康检查", description = "用于检测服务是否正常")
  public BaseResponse<String> health() {
    return ResultUtils.success("ok");
  }
}
