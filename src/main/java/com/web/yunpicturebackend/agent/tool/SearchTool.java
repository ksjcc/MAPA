package com.web.yunpicturebackend.agent.tool;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.web.yunpicturebackend.Service.PictureService;
import com.web.yunpicturebackend.agent.embedding.EmbeddingApi;
import com.web.yunpicturebackend.agent.dto.PictureSearchResult;
import com.web.yunpicturebackend.manager.vector.MilvusManager;
import com.web.yunpicturebackend.model.entity.Picture;

import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SearchTool {
  private final PictureService pictureService;
  private final EmbeddingApi embeddingApi;
  private final MilvusManager milvusManager;

  @Tool("""
      搜索图库中的图片素材。
      适用于：
      活动素材搜索
      海报搜索
      Banner搜索
      商品图搜索
      输入：
      关键词
      输出：
      图片列表
      """)
  public String searchPicture(String keyword, Long spaceId) {
    List<Float> queryVector = embeddingApi.textEmbedding(keyword);
    List<Long> pictureIds = milvusManager.searchIds(queryVector, 50);
    List<Picture> pictures = pictureService.listByIds(pictureIds);
    Map<Long, Picture> pictureMap = pictures.stream()
        .collect(Collectors.toMap(Picture::getId, Function.identity(), (a, b) -> a));
    return JSONUtil.toJsonStr(
        pictureIds.stream()
            .map(pictureMap::get)
            .filter(java.util.Objects::nonNull)
            .map(this::convert)
            .toList());
  }

  private PictureSearchResult convert(
      Picture picture) {

    return PictureSearchResult.builder()
        .pictureId(picture.getId())
        .name(picture.getName())
        .url(picture.getUrl())
        .tags(picture.getTags())
        .build();
  }
}
