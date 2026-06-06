package com.web.yunpicturebackend.Service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.web.yunpicturebackend.Service.PictureRecommendService;
import com.web.yunpicturebackend.Service.PictureService;
import com.web.yunpicturebackend.agent.embedding.EmbeddingApi;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;
import com.web.yunpicturebackend.manager.vector.MilvusManager;
import com.web.yunpicturebackend.model.entity.Picture;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PictureRecommendServiceImpl implements PictureRecommendService {
  private static final int RECALL_TOP_K = 20;
  private static final int RECOMMEND_LIMIT = 10;
  private static final String QUERY_REWRITE_PROMPT = """
      你要把用户的找图需求改写成适合向量检索的搜索词。

      用户需求：
      %s

      请严格返回 JSON：
      {
        "searchText": "适合检索的中文短语，包含主题、场景、风格、色彩等关键信息"
      }

      要求：
      1. 只返回 JSON
      2. searchText 不超过 30 个字
      3. 不要输出解释
      """;
  private static final String RERANK_PROMPT = """
      你要从候选图片中挑选最符合用户需求的结果。

      用户需求：
      %s

      候选图片列表：
      %s

      请严格返回 JSON：
      {
        "recommendUrls": ["url1", "url2", "url3"]
      }

      要求：
      1. 只返回最符合需求的 5 到 10 个 URL
      2. 优先考虑主题、场景、风格、色彩和标签匹配度
      3. 只返回 JSON，不要解释
      """;

  private final ChatLanguageModel chatLanguageModel;
  private final EmbeddingApi embeddingApi;
  private final MilvusManager milvusManager;
  private final PictureService pictureService;

  @Override
  public List<String> recommend(String query) {
    if (StrUtil.isBlank(query)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "推荐条件不能为空");
    }
    try {
      String searchText = rewriteQuery(query);
      List<Float> queryVector = embeddingApi.textEmbedding(searchText);
      List<Long> pictureIds = milvusManager.searchIds(queryVector, RECALL_TOP_K);
      if (CollUtil.isEmpty(pictureIds)) {
        return Collections.emptyList();
      }

      List<Picture> pictures = pictureService.listByIds(pictureIds);
      if (CollUtil.isEmpty(pictures)) {
        return Collections.emptyList();
      }

      Map<Long, Picture> pictureMap = pictures.stream()
          .collect(Collectors.toMap(Picture::getId, Function.identity(), (a, b) -> a));
      List<Picture> orderedPictures = pictureIds.stream()
          .map(pictureMap::get)
          .filter(Objects::nonNull)
          .filter(picture -> StrUtil.isNotBlank(picture.getUrl()))
          .toList();
      if (CollUtil.isEmpty(orderedPictures)) {
        return Collections.emptyList();
      }

      String candidatePayload = buildCandidatePayload(orderedPictures);
      String response = chatLanguageModel.chat(
          UserMessage.from(List.of(
              TextContent.from(RERANK_PROMPT.formatted(query, candidatePayload)))))
          .aiMessage()
          .text();
      List<String> recommendUrls = parseRecommendUrls(response);
      if (CollUtil.isNotEmpty(recommendUrls)) {
        return recommendUrls.stream()
            .distinct()
            .limit(RECOMMEND_LIMIT)
            .toList();
      }

      return orderedPictures.stream()
          .map(Picture::getUrl)
          .filter(StrUtil::isNotBlank)
          .distinct()
          .limit(RECOMMEND_LIMIT)
          .toList();
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("图片推荐失败，query={}", query, e);
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片推荐失败");
    }
  }

  private String rewriteQuery(String query) {
    String response = chatLanguageModel.chat(
        UserMessage.from(List.of(
            TextContent.from(QUERY_REWRITE_PROMPT.formatted(query)))))
        .aiMessage()
        .text();
    if (StrUtil.isBlank(response)) {
      return query;
    }
    String jsonText = extractJson(response);
    JSONObject jsonObject = JSONUtil.parseObj(jsonText);
    String searchText = jsonObject.getStr("searchText");
    return StrUtil.blankToDefault(StrUtil.trim(searchText), query);
  }

  private String buildCandidatePayload(List<Picture> pictures) {
    List<Map<String, Object>> candidates = pictures.stream()
        .sorted(Comparator.comparing(Picture::getId))
        .limit(RECALL_TOP_K)
        .map(picture -> Map.<String, Object>ofEntries(
            Map.entry("pictureId", picture.getId()),
            Map.entry("name", StrUtil.blankToDefault(picture.getName(), "")),
            Map.entry("url", StrUtil.blankToDefault(picture.getUrl(), "")),
            Map.entry("category", StrUtil.blankToDefault(picture.getCategory(), "")),
            Map.entry("tags", StrUtil.blankToDefault(picture.getTags(), "")),
            Map.entry("aiTags", StrUtil.blankToDefault(picture.getAiTags(), "")),
            Map.entry("aiDescription", StrUtil.blankToDefault(picture.getAiDescription(), "")),
            Map.entry("aiScene", StrUtil.blankToDefault(picture.getAiScene(), "")),
            Map.entry("aiTheme", StrUtil.blankToDefault(picture.getAiTheme(), "")),
            Map.entry("aiColors", StrUtil.blankToDefault(picture.getAiColors(), "")),
            Map.entry("picColor", StrUtil.blankToDefault(picture.getPicColor(), ""))))
        .toList();
    return JSONUtil.toJsonStr(candidates);
  }

  private List<String> parseRecommendUrls(String response) {
    if (StrUtil.isBlank(response)) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 未返回推荐结果");
    }
    String jsonText = extractJson(response);
    JSONObject jsonObject = JSONUtil.parseObj(jsonText);
    Object recommendUrls = jsonObject.get("recommendUrls");
    if (recommendUrls instanceof JSONArray jsonArray) {
      Set<String> urlSet = new LinkedHashSet<>();
      for (Object item : jsonArray) {
        String url = StrUtil.trim(String.valueOf(item));
        if (StrUtil.isNotBlank(url)) {
          urlSet.add(url);
        }
      }
      return List.copyOf(urlSet);
    }
    return Collections.emptyList();
  }

  private String extractJson(String content) {
    if (StrUtil.isBlank(content)) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 返回推荐结果为空");
    }
    String trimmedContent = content.trim();
    int start = trimmedContent.indexOf('{');
    int end = trimmedContent.lastIndexOf('}');
    if (start < 0 || end <= start) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 返回推荐结果格式错误");
    }
    return trimmedContent.substring(start, end + 1);
  }
}
