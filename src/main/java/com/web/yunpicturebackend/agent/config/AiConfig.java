package com.web.yunpicturebackend.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AiConfig {

  @Bean
  public ChatLanguageModel chatLanguageModel(
      @Value("${langchain4j.deepseek.api-key}") String apiKey,
      @Value("${langchain4j.deepseek.base-url}") String baseUrl,
      @Value("${langchain4j.deepseek.model-name}") String modelName,
      @Value("${langchain4j.deepseek.temperature:0.7}") Double temperature) {
    return OpenAiChatModel.builder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .modelName(modelName)
        .temperature(temperature)
        .build();
  }

  @Bean
  public StreamingChatLanguageModel streamingChatLanguageModel(
      @Value("${langchain4j.deepseek.api-key}") String apiKey,
      @Value("${langchain4j.deepseek.base-url}") String baseUrl,
      @Value("${langchain4j.deepseek.model-name}") String modelName,
      @Value("${langchain4j.deepseek.temperature:0.7}") Double temperature) {
    return OpenAiStreamingChatModel.builder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .modelName(modelName)
        .temperature(temperature)
        .build();
  }
}
