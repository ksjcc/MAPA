package com.web.yunpicturebackend.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.web.yunpicturebackend.agent.Planner.ArchivePlanner;
import com.web.yunpicturebackend.agent.coordinator.CoordinatorAgent;
import com.web.yunpicturebackend.agent.prompt.CoordinatorPrompt;
import com.web.yunpicturebackend.agent.tool.AnalysisTool;
import com.web.yunpicturebackend.agent.tool.ArchiveTool;
import com.web.yunpicturebackend.agent.tool.RecommendTool;
import com.web.yunpicturebackend.agent.tool.SearchTool;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;

@Configuration
public class AgentConfig {

  @Bean
  public CoordinatorAgent coordinatorAgent(
      ChatLanguageModel chatLanguageModel,
      ChatMemory chatMemory,
      SearchTool searchTool,
      RecommendTool recommendTool,
      AnalysisTool analysisTool,
      ArchiveTool archiveTool) {

    return AiServices.builder(CoordinatorAgent.class)
        .chatLanguageModel(chatLanguageModel)
        .chatMemory(chatMemory)
        .tools(searchTool, analysisTool, archiveTool, recommendTool)
        .systemMessageProvider(memoryId -> CoordinatorPrompt.SYSTEM_PROMPT)
        .build();
  }

  @Bean
  public ArchivePlanner archivePlanner(
      ChatLanguageModel chatLanguageModel) {
    return AiServices.builder(ArchivePlanner.class)
        .chatLanguageModel(chatLanguageModel)
        .build();
  }
}
