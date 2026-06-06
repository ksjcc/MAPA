package com.web.yunpicturebackend.Service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.web.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.web.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.web.yunpicturebackend.common.BaseResponse;
import com.web.yunpicturebackend.model.dto.picture.CreatePictureOutPaintingTaskRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureEditByBatchRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureEditRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureReviewRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureUpdateRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.web.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.web.yunpicturebackend.model.entity.Picture;
import com.web.yunpicturebackend.model.entity.User;
import com.web.yunpicturebackend.model.vo.PictureVO;

public interface PictureService extends IService<Picture> {
  List<Picture> searchByKeyWord(String keyword, Long spaceId);

  /**
   * 校验图片
   *
   * @param picture
   */
  void validPicture(Picture picture);

  /**
   * 上传图片
   *
   * @param inputSource          文件输入源
   * @param pictureUploadRequest
   * @param loginUser
   * @return
   */
  PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

  QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRquest);

  /**
   * 删除图片
   * 
   * @param pictureId
   * @param loginUser
   */
  void deletePicture(long pictureId, User loginUser);

  void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

  void clearPictureFile(Picture oldPicture);

  void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

  void fillReviewParams(Picture picture, User loginUser);

  /**
   * 获取图片包装类（单条）
   *
   * @param picture
   * @param request
   * @return
   */
  PictureVO getPictureVO(Picture picture, HttpServletRequest request);

  /**
   * 获取图片包装类（分页）
   *
   * @param picturePage
   * @param request
   * @return
   */
  Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

  Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

  // void checkPictureAuth(User loginUser, Picture picture);

  List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

  void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

  CreateOutPaintingTaskResponse createPictureOutPaintingTask(
      CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
      User loginUser);
}
