package com.web.yunpicturebackend.agent.memory;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

@Configurable
public class AgentMemoryConfig {
  @Bean
  public ChatMemory chatMemory() {
    return MessageWindowChatMemory.withMaxMessages(10);
  }
}
