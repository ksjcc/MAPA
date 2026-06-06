package com.web.yunpicturebackend.Service;

import java.util.List;

public interface PictureRecommendService {
  List<String> recommend(String query);
}
