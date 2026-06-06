package com.web.yunpicturebackend.agent.tool;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.web.yunpicturebackend.Service.PictureRecommendService;

import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecommendTool {
  private final PictureRecommendService pictureRecommendService;

  @Tool("""
      你现在要为用户做图片推荐。

      用户提出了图片要求

      任务要求：
      1. 必须先分析用户对风格、主题、场景、色彩或标签的要求
      2. 再使用搜索工具到图库中搜索最相关的图片
      3. 返回 5 到 10 个最值得推荐的图片 URL
      4. 只返回 JSON，不要输出 Markdown，不要解释

      返回格式：
      {
        "recommendUrls": ["url1", "url2", "url3"]
      }
      """)
  public String recommendPicture(String query) {
    List<String> recommendUrls = pictureRecommendService.recommend(query);
    return JSONUtil.toJsonStr(Map.of("recommendUrls", recommendUrls));
  }
}
