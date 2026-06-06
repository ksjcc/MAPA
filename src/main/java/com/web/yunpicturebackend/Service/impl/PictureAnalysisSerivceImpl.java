package com.web.yunpicturebackend.Service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.web.yunpicturebackend.Service.PictureAnalysisService;
import com.web.yunpicturebackend.agent.coordinator.CoordinatorAgent;
import com.web.yunpicturebackend.agent.dto.AnalysisResult;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.model.entity.Picture;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PictureAnalysisSerivceImpl implements PictureAnalysisService {
  private static final String ANALYSIS_PROMPT = """
      请分析这张图片，并严格返回 JSON 对象，不要输出 Markdown，不要补充解释。
      JSON 字段要求：
      {
        "description": "对图片内容的一句话描述",
        "tags": ["标签1", "标签2", "标签3"],
        "scene": "场景",
        "theme": "主题",
        "colors": ["主色1", "主色2", "主色3"],
        "ocrText": "图片中识别出的文字"
      }
      要求：
      1. description 简洁准确
      2. tags 和 colors 返回数组
      3. ocrText 返回图片中的主要文字，没有则返回空字符串
      4. 如果无法判断，返回空字符串或空数组
      """;

  private static final String ANALYSIS_AGENT_PROMPT = """
      请分析这张图片：%s

      要求：
      1. 必须调用图片分析工具，不要直接臆测结果
      2. 只返回 JSON 对象，不要输出 Markdown，不要补充解释
      3. JSON 字段必须包含 description、tags、scene、theme、colors、ocrText
      """;

  private final CoordinatorAgent coordinatorAgent;
  private final ChatLanguageModel chatLanguageModel;
  private final com.web.yunpicturebackend.Service.PictureService pictureService;

  @Override
  public AnalysisResult analysis(String imageUrl) {
    if (StrUtil.isBlank(imageUrl)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片地址不能为空");
    }
    try {
      String content = coordinatorAgent.chat(ANALYSIS_AGENT_PROMPT.formatted(imageUrl));
      return buildAnalysisResult(content);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("图片分析失败，imageUrl={}", imageUrl, e);
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片分析失败");
    }
  }

  public AnalysisResult analyzePictureDirect(String imageUrl) {
    if (StrUtil.isBlank(imageUrl)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片地址不能为空");
    }
    try {
      ChatResponse response = chatLanguageModel.chat(
          UserMessage.from(List.of(
              TextContent.from(ANALYSIS_PROMPT),
              ImageContent.from(imageUrl))));
      return buildAnalysisResult(response.aiMessage().text());
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("图片底层分析失败，imageUrl={}", imageUrl, e);
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片分析失败");
    }
  }

  @Override
  public void analyzeAndSave(Long pictureId) {
    if (pictureId == null || pictureId <= 0) {
      return;
    }
    Picture picture = pictureService.getById(pictureId);
    if (picture == null || StrUtil.isBlank(picture.getUrl())) {
      return;
    }
    AnalysisResult result = analyzePictureDirect(picture.getUrl());
    Picture update = new Picture();
    update.setId(pictureId);
    update.setAiDescription(StrUtil.blankToDefault(result.getDescription(), ""));
    update.setAiScene(StrUtil.blankToDefault(result.getScene(), ""));
    update.setAiTheme(StrUtil.blankToDefault(result.getTheme(), ""));
    update.setAiTags(JSONUtil.toJsonStr(result.getTags() == null ? Collections.emptyList() : result.getTags()));
    update.setAiColors(JSONUtil.toJsonStr(result.getColors() == null ? Collections.emptyList() : result.getColors()));
    update.setAiOcrText(StrUtil.blankToDefault(result.getOcrText(), ""));
    pictureService.updateById(update);
  }

  private AnalysisResult buildAnalysisResult(String content) {
    String jsonText = extractJson(content);
    JSONObject jsonObject = JSONUtil.parseObj(jsonText);
    return AnalysisResult.builder()
        .description(jsonObject.getStr("description", ""))
        .tags(toStringList(jsonObject.get("tags")))
        .scene(jsonObject.getStr("scene", ""))
        .theme(jsonObject.getStr("theme", ""))
        .colors(toStringList(jsonObject.get("colors")))
        .ocrText(jsonObject.getStr("ocrText", ""))
        .build();
  }

  private String extractJson(String content) {
    if (StrUtil.isBlank(content)) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 未返回分析结果");
    }
    String trimmedContent = content.trim();
    int start = trimmedContent.indexOf('{');
    int end = trimmedContent.lastIndexOf('}');
    if (start < 0 || end <= start) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 返回结果格式错误");
    }
    return trimmedContent.substring(start, end + 1);
  }

  private List<String> toStringList(Object value) {
    if (value == null) {
      return Collections.emptyList();
    }
    if (value instanceof JSONArray jsonArray) {
      return jsonArray.toList(String.class);
    }
    if (value instanceof List<?> list) {
      return list.stream()
          .map(String::valueOf)
          .toList();
    }
    String text = String.valueOf(value);
    if (StrUtil.isBlank(text)) {
      return Collections.emptyList();
    }
    return StrUtil.splitTrim(text, ',');
  }
}
