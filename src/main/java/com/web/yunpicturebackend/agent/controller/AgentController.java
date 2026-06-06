package com.web.yunpicturebackend.agent.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.web.yunpicturebackend.agent.dto.AgentRequest;
import com.web.yunpicturebackend.agent.gateway.AgentGateway;
import com.web.yunpicturebackend.common.BaseResponse;
import com.web.yunpicturebackend.common.ResultUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {
  private final AgentGateway agentGateway;

  @PostMapping("/chat")
  public BaseResponse<String> chat(
      @RequestBody AgentRequest request) {

    return ResultUtils.success(
        agentGateway.execute(
            request.getMessage(),
            request.getSpaceId()));
  }
}
