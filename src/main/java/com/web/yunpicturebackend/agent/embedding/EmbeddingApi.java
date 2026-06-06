package com.web.yunpicturebackend.agent.embedding;

import java.util.List;

public interface EmbeddingApi {

  List<Float> imageEmbedding(
      String imageUrl);

  List<Float> textEmbedding(
      String text);
}
