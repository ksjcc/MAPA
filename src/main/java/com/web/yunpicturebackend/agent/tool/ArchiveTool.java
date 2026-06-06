package com.web.yunpicturebackend.agent.tool;

import org.springframework.stereotype.Component;

import com.web.yunpicturebackend.Service.ArchiveService;

import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ArchiveTool {
  private final ArchiveService archiveService;

  @Tool("""
      对指定空间的图片进行整理、归类和归档。
      适用于：
      - 按颜色整理空间图片
      - 按场景归类空间图片
      - 按活动主题整理空间图片
      输入：
      - userRequirement: 用户的自然语言归档要求
      - spaceId: 需要整理的空间 id
      输出：
      - 归档结果 JSON
      """)
  public String archiveSpacePictures(String userRequirement, Long spaceId) {
    return JSONUtil.toJsonStr(archiveService.archiveSpacePictures(spaceId, userRequirement));
  }
}
