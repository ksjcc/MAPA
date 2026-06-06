package com.web.yunpicturebackend.manager.vector;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureVector {

  private Long id;

  private List<Float> vector;
}
