package com.web.yunpicturebackend.agent.gateway;

import org.springframework.stereotype.Service;

import com.web.yunpicturebackend.agent.coordinator.CoordinatorAgent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentGateway {
  private final CoordinatorAgent coordinatorAgent;

  public String execute(String prompt, Long spaceId) {
    String agentMessage = """
        用户原始需求：%s
        spaceId：%s
        如果这是空间图片整理/归档任务，请基于这个 spaceId 调用对应工具。
        """.formatted(prompt, spaceId == null ? "" : spaceId);
    return coordinatorAgent.chat(agentMessage);
  }
}
