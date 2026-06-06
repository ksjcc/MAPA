package com.web.yunpicturebackend.agent.tool;

import org.springframework.stereotype.Component;

import com.web.yunpicturebackend.Service.impl.PictureAnalysisSerivceImpl;

import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnalysisTool {
  private final PictureAnalysisSerivceImpl pictureAnalysisService;

  @Tool("""
      分析图片内容并提取结构化信息。
      适用于：
      图片内容理解
      标签提取
      场景识别
      主题归纳
      色彩分析
      输入：
      图片 URL
      输出：
      图片分析结果 JSON
      """)
  public String analyzePicture(String imageUrl) {
    return JSONUtil.toJsonStr(pictureAnalysisService.analysis(imageUrl));
  }
}
