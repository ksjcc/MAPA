package com.web.yunpicturebackend.agent.embedding;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.web.yunpicturebackend.model.dto.embedding.EmbeddingRequest;
import com.web.yunpicturebackend.model.dto.embedding.EmbeddingResponse;

@Service
public class SiglipEmbeddingApi implements EmbeddingApi {
  private final RestTemplate restTemplate = new RestTemplate();

  private static final String BASE_URL = "http://localhost:8000";

  @Override
  public List<Float> imageEmbedding(String imageUrl) {
    EmbeddingRequest request = new EmbeddingRequest();
    request.setImageUrl(imageUrl);
    EmbeddingResponse response = restTemplate.postForObject(BASE_URL +
        "/embedding/image",
        request,
        EmbeddingResponse.class);
    return response.getVector();
  }

  @Override
  public List<Float> textEmbedding(String text) {
    EmbeddingRequest request = new EmbeddingRequest();
    request.setText(text);
    EmbeddingResponse response = restTemplate.postForObject(
        BASE_URL +
            "/embedding/text",
        request,
        EmbeddingResponse.class);
    return response.getVector();
  }
}
