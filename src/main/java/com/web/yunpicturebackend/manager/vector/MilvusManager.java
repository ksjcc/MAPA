package com.web.yunpicturebackend.manager.vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.web.yunpicturebackend.config.MilvusProperties;
import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.exception.IllegalResponseException;
import io.milvus.exception.ParamException;
import io.milvus.grpc.DataType;
import io.milvus.grpc.DescribeIndexResponse;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.DescribeIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class MilvusManager {

  private final MilvusServiceClient client;

  private final MilvusProperties milvusProperties;

  @PostConstruct
  public void init() {
    if (Boolean.TRUE.equals(milvusProperties.getAutoCreateCollection())) {
      createCollectionIfNotExists();
    }
    if (Boolean.TRUE.equals(milvusProperties.getAutoLoadCollection()) && hasCollection()) {
      loadCollection();
    }
  }

  @PreDestroy
  public void destroy() {
    try {
      client.close(5L);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Close Milvus client interrupted", e);
    }
  }

  public boolean hasCollection() {
    HasCollectionParam.Builder builder = HasCollectionParam.newBuilder()
        .withCollectionName(milvusProperties.getCollectionName());
    fillDatabase(builder);
    R<Boolean> response = client.hasCollection(builder.build());
    return getDataOrThrow(response, "检查 Milvus Collection 失败");
  }

  public void createCollectionIfNotExists() {
    if (hasCollection()) {
      createIndexIfNotExists();
      return;
    }
    createCollection();
  }

  public void createCollection() {
    validDimension(milvusProperties.getDimension());

    List<FieldType> fieldTypes = new ArrayList<>();
    fieldTypes.add(FieldType.newBuilder()
        .withName(milvusProperties.getPrimaryFieldName())
        .withDataType(DataType.Int64)
        .withPrimaryKey(true)
        .withAutoID(false)
        .build());
    fieldTypes.add(FieldType.newBuilder()
        .withName(milvusProperties.getVectorFieldName())
        .withDataType(DataType.FloatVector)
        .withDimension(milvusProperties.getDimension())
        .build());

    CreateCollectionParam.Builder builder = CreateCollectionParam.newBuilder()
        .withCollectionName(milvusProperties.getCollectionName())
        .withDescription("picture vector collection")
        .withFieldTypes(fieldTypes)
        .withShardsNum(milvusProperties.getShardsNum())
        .withConsistencyLevel(ConsistencyLevelEnum.BOUNDED);
    fillDatabase(builder);

    executeOrThrow(client.createCollection(builder.build()), "创建 Milvus Collection 失败");
    createIndexIfNotExists();
    log.info("Milvus collection created: {}", milvusProperties.getCollectionName());
  }

  public List<Long> insert(List<PictureVector> pictureVectors) {
    if (CollUtil.isEmpty(pictureVectors)) {
      return Collections.emptyList();
    }
    List<Long> ids = new ArrayList<>(pictureVectors.size());
    List<List<Float>> vectors = new ArrayList<>(pictureVectors.size());
    for (PictureVector pictureVector : pictureVectors) {
      if (pictureVector == null || pictureVector.getId() == null) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片向量 id 不能为空");
      }
      validVector(pictureVector.getVector());
      ids.add(pictureVector.getId());
      vectors.add(pictureVector.getVector());
    }

    InsertParam.Builder builder = InsertParam.newBuilder()
        .withCollectionName(milvusProperties.getCollectionName())
        .withFields(List.of(
            new InsertParam.Field(milvusProperties.getPrimaryFieldName(), ids),
            new InsertParam.Field(milvusProperties.getVectorFieldName(), vectors)));
    fillDatabase(builder);

    MutationResult mutationResult = executeOrThrow(client.insert(builder.build()), "Milvus 向量插入失败");
    flushCollection();
    log.info("Inserted {} vectors into Milvus collection {}", mutationResult.getInsertCnt(),
        milvusProperties.getCollectionName());
    return ids;
  }

  public Long insertVector(Long pictureId, List<Float> vector) {
    if (pictureId == null) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片向量 id 不能为空");
    }
    insert(Collections.singletonList(new PictureVector(pictureId, vector)));
    return pictureId;
  }

  public Long replace(Long pictureId, List<Float> vector) {
    deleteById(pictureId);
    return insertVector(pictureId, vector);
  }

  public List<PictureVectorSearchResult> search(List<Float> vector, int topK) {
    return search(vector, topK, null);
  }

  public List<PictureVectorSearchResult> search(List<Float> vector, int topK, String expr) {
    validVector(vector);
    if (topK <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "topK 必须大于 0");
    }

    loadCollection();

    SearchParam.Builder builder = SearchParam.newBuilder()
        .withCollectionName(milvusProperties.getCollectionName())
        .withVectorFieldName(milvusProperties.getVectorFieldName())
        .withVectors(Collections.singletonList(vector))
        .withTopK(topK)
        .withMetricType(resolveMetricType())
        .withParams(buildSearchExtraParam())
        .withOutFields(Collections.singletonList(milvusProperties.getPrimaryFieldName()));
    fillDatabase(builder);
    if (StrUtil.isNotBlank(expr)) {
      builder.withExpr(expr);
    }

    SearchResults searchResults = executeOrThrow(client.search(builder.build()), "Milvus 向量检索失败");
    return toSearchResults(searchResults);
  }

  public long delete(Collection<Long> pictureIds) {
    if (CollUtil.isEmpty(pictureIds)) {
      return 0L;
    }
    String expr = milvusProperties.getPrimaryFieldName() + " in [" + pictureIds.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(",")) + "]";

    DeleteParam.Builder builder = DeleteParam.newBuilder()
        .withCollectionName(milvusProperties.getCollectionName())
        .withExpr(expr);
    fillDatabase(builder);

    MutationResult mutationResult = executeOrThrow(client.delete(builder.build()), "Milvus 向量删除失败");
    flushCollection();
    log.info("Deleted {} vectors from Milvus collection {}", mutationResult.getDeleteCnt(),
        milvusProperties.getCollectionName());
    return mutationResult.getDeleteCnt();
  }

  public long deleteById(Long pictureId) {
    if (pictureId == null || pictureId <= 0) {
      return 0L;
    }
    return delete(Collections.singletonList(pictureId));
  }

  public List<Long> searchIds(List<Float> vector, int topK) {
    return search(vector, topK).stream()
        .map(PictureVectorSearchResult::getId)
        .collect(Collectors.toList());
  }

  public List<Long> searchIds(List<Float> vector, int topK, String expr) {
    return search(vector, topK, expr).stream()
        .map(PictureVectorSearchResult::getId)
        .collect(Collectors.toList());
  }

  public void loadCollection() {
    LoadCollectionParam.Builder builder = LoadCollectionParam.newBuilder()
        .withCollectionName(milvusProperties.getCollectionName())
        .withSyncLoad(true)
        .withSyncLoadWaitingInterval(milvusProperties.getSyncLoadIntervalMs())
        .withSyncLoadWaitingTimeout(milvusProperties.getSyncLoadTimeoutMs());
    fillDatabase(builder);
    executeOrThrow(client.loadCollection(builder.build()), "加载 Milvus Collection 失败");
  }

  private void createIndexIfNotExists() {
    if (hasIndex()) {
      return;
    }
    CreateIndexParam.Builder builder = CreateIndexParam.newBuilder()
        .withCollectionName(milvusProperties.getCollectionName())
        .withFieldName(milvusProperties.getVectorFieldName())
        .withIndexName(milvusProperties.getIndexName())
        .withIndexType(resolveIndexType())
        .withMetricType(resolveMetricType())
        .withExtraParam(buildIndexExtraParam());
    fillDatabase(builder);
    executeOrThrow(client.createIndex(builder.build()), "创建 Milvus 索引失败");
  }

  private boolean hasIndex() {
    DescribeIndexParam.Builder builder = DescribeIndexParam.newBuilder()
        .withCollectionName(milvusProperties.getCollectionName())
        .withFieldName(milvusProperties.getVectorFieldName())
        .withIndexName(milvusProperties.getIndexName());
    fillDatabase(builder);
    R<DescribeIndexResponse> response = client.describeIndex(builder.build());
    if (isSuccess(response)) {
      DescribeIndexResponse describeIndexResponse = response.getData();
      return describeIndexResponse != null && describeIndexResponse.getIndexDescriptionsCount() > 0;
    }
    return false;
  }

  private void flushCollection() {
    FlushParam.Builder builder = FlushParam.newBuilder()
        .addCollectionName(milvusProperties.getCollectionName())
        .withSyncFlush(true)
        .withSyncFlushWaitingInterval(milvusProperties.getSyncFlushIntervalMs())
        .withSyncFlushWaitingTimeout(milvusProperties.getSyncFlushTimeoutMs());
    fillDatabase(builder);
    executeOrThrow(client.flush(builder.build()), "刷新 Milvus Collection 失败");
  }

  private List<PictureVectorSearchResult> toSearchResults(SearchResults searchResults) {
    SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResults.getResults());
    List<SearchResultsWrapper.IDScore> idScores;
    try {
      idScores = wrapper.getIDScore(0);
    } catch (ParamException | IllegalResponseException e) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "解析 Milvus 检索结果失败: " + e.getMessage());
    }
    return idScores.stream()
        .map(item -> new PictureVectorSearchResult(item.getLongID(), item.getScore()))
        .collect(Collectors.toList());
  }

  private void validVector(List<Float> vector) {
    if (CollUtil.isEmpty(vector)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "向量不能为空");
    }
    validDimension(vector.size());
  }

  private void validDimension(int vectorDimension) {
    if (milvusProperties.getDimension() == null || milvusProperties.getDimension() <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "Milvus 向量维度配置非法");
    }
    if (vectorDimension != milvusProperties.getDimension()) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR,
          "向量维度不匹配，期望 " + milvusProperties.getDimension() + "，实际 " + vectorDimension);
    }
  }

  private MetricType resolveMetricType() {
    try {
      return MetricType.valueOf(milvusProperties.getMetricType().toUpperCase());
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "Milvus metricType 配置非法");
    }
  }

  private IndexType resolveIndexType() {
    try {
      return IndexType.valueOf(milvusProperties.getIndexType().toUpperCase());
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "Milvus indexType 配置非法");
    }
  }

  private String buildIndexExtraParam() {
    switch (resolveIndexType()) {
      case IVF_FLAT:
      case IVF_SQ8:
      case IVF_PQ:
      case GPU_IVF_FLAT:
      case GPU_IVF_PQ:
      case BIN_IVF_FLAT:
        return "{\"nlist\":" + milvusProperties.getNlist() + "}";
      case HNSW:
        return "{\"M\":" + milvusProperties.getHnswM() + ",\"efConstruction\":"
            + milvusProperties.getHnswEfConstruction() + "}";
      default:
        return "{}";
    }
  }

  private String buildSearchExtraParam() {
    switch (resolveIndexType()) {
      case IVF_FLAT:
      case IVF_SQ8:
      case IVF_PQ:
      case GPU_IVF_FLAT:
      case GPU_IVF_PQ:
      case BIN_IVF_FLAT:
        return "{\"nprobe\":" + milvusProperties.getNprobe() + "}";
      case HNSW:
        return "{\"ef\":" + milvusProperties.getHnswEfSearch() + "}";
      default:
        return "{}";
    }
  }

  private boolean isSuccess(R<?> response) {
    return response != null && response.getStatus() != null
        && response.getStatus().equals(R.Status.Success.getCode());
  }

  private <T> T getDataOrThrow(R<T> response, String errorMessage) {
    if (isSuccess(response)) {
      return response.getData();
    }
    throw buildMilvusException(response, errorMessage);
  }

  private <T> T executeOrThrow(R<T> response, String errorMessage) {
    return getDataOrThrow(response, errorMessage);
  }

  private BusinessException buildMilvusException(R<?> response, String errorMessage) {
    String responseMessage = response == null ? null : response.getMessage();
    Exception exception = response == null ? null : response.getException();
    String detail = StrUtil.blankToDefault(responseMessage, exception == null ? null : exception.getMessage());
    String fullMessage = StrUtil.isBlank(detail) ? errorMessage : errorMessage + ": " + detail;
    return new BusinessException(ErrorCode.OPERATION_ERROR, fullMessage);
  }

  private void fillDatabase(HasCollectionParam.Builder builder) {
    if (StrUtil.isNotBlank(milvusProperties.getDatabase())) {
      builder.withDatabaseName(milvusProperties.getDatabase());
    }
  }

  private void fillDatabase(CreateCollectionParam.Builder builder) {
    if (StrUtil.isNotBlank(milvusProperties.getDatabase())) {
      builder.withDatabaseName(milvusProperties.getDatabase());
    }
  }

  private void fillDatabase(CreateIndexParam.Builder builder) {
    if (StrUtil.isNotBlank(milvusProperties.getDatabase())) {
      builder.withDatabaseName(milvusProperties.getDatabase());
    }
  }

  private void fillDatabase(DescribeIndexParam.Builder builder) {
    if (StrUtil.isNotBlank(milvusProperties.getDatabase())) {
      builder.withDatabaseName(milvusProperties.getDatabase());
    }
  }

  private void fillDatabase(InsertParam.Builder builder) {
    if (StrUtil.isNotBlank(milvusProperties.getDatabase())) {
      builder.withDatabaseName(milvusProperties.getDatabase());
    }
  }

  private void fillDatabase(SearchParam.Builder builder) {
    if (StrUtil.isNotBlank(milvusProperties.getDatabase())) {
      builder.withDatabaseName(milvusProperties.getDatabase());
    }
  }

  private void fillDatabase(DeleteParam.Builder builder) {
    if (StrUtil.isNotBlank(milvusProperties.getDatabase())) {
      builder.withDatabaseName(milvusProperties.getDatabase());
    }
  }

  private void fillDatabase(LoadCollectionParam.Builder builder) {
    if (StrUtil.isNotBlank(milvusProperties.getDatabase())) {
      builder.withDatabaseName(milvusProperties.getDatabase());
    }
  }

  private void fillDatabase(FlushParam.Builder builder) {
    if (StrUtil.isNotBlank(milvusProperties.getDatabase())) {
      builder.withDatabaseName(milvusProperties.getDatabase());
    }
  }
}
