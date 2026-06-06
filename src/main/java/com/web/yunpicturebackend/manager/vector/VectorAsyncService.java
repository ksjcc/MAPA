package com.web.yunpicturebackend.manager.vector;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.web.yunpicturebackend.Service.PictureService;
import com.web.yunpicturebackend.agent.embedding.EmbeddingApi;
import com.web.yunpicturebackend.model.entity.Picture;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorAsyncService {
  private final PictureService pictureService;

  private final EmbeddingApi embeddingApi;

  private final MilvusManager milvusManager;

  @Async
  public void embedding(Long pictureId) {
    Picture picture = pictureService.getById(pictureId);
    if (picture == null) {
      log.error(
          "图片不存在 pictureId={}",
          pictureId);
      return;
    }

    String imageUrl = picture.getUrl();
    List<Float> vector = embeddingApi.imageEmbedding(imageUrl);
    milvusManager.replace(pictureId, vector);
  }

  @Async
  public void deleteVector(Long pictureId) {
    if (pictureId == null || pictureId <= 0) {
      return;
    }
    milvusManager.deleteById(pictureId);
  }
}
