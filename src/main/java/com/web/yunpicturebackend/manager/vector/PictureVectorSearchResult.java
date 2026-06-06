package com.web.yunpicturebackend.manager.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureVectorSearchResult {

  private Long id;

  private Float score;
}
