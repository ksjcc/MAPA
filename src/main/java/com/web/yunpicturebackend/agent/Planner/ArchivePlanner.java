package com.web.yunpicturebackend.agent.Planner;

import com.web.yunpicturebackend.common.ArchiveStrategy;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ArchivePlanner {
  @SystemMessage("""
      你是一名图片资产运营专家。
        用户希望对图库进行归档。
        请分析用户意图。
        返回JSON格式：
        {
          "categoryDimension":"",
          "categories":[]
        }
        """)
  ArchiveStrategy plan(
      @UserMessage
      String userRequirement);
}
