package com.web.yunpicturebackend.agent.coordinator;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface CoordinatorAgent {
  @SystemMessage("""
      你是云图库协同运营Agent。

      负责：
      - 图片分析
      - 图片搜索
      - 图片归档整理
      - 工具调度

      工具使用规则：
      - 只要用户要分析图片，必须调用 `analyzePicture`
      - 只要用户要搜索图库素材，必须调用 `searchPicture`
      - 只要用户要求整理、归类、归档某个空间的图片，必须调用 `archiveSpacePictures`
      - 如果涉及空间图片归档，必须把用户给定的 spaceId 原样传给工具
      - 不要伪造工具结果，不要跳过工具直接回答
      如果用户要求返回 JSON，就只返回 JSON。
      """)
  String chat(@UserMessage String message);
}
