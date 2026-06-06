package com.web.yunpicturebackend.common;

import lombok.Data;
import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {
  private Long id;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  private static final long serialVersionUID = 1L;
}
