package com.web.yunpicturebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

  private String host = "127.0.0.1";

  private Integer port = 19530;

  private String database;

  private String username;

  private String password;

  private String token;

  private Boolean secure = false;

  private Long connectTimeoutMs = 10000L;

  private Long keepAliveTimeMs = 30000L;

  private Long keepAliveTimeoutMs = 10000L;

  private Long idleTimeoutMs = 60000L;

  private Long rpcDeadlineMs = 10000L;

  private String collectionName = "picture_vector";

  private String primaryFieldName = "id";

  private String vectorFieldName = "embedding";

  private Integer dimension = 768;

  private String indexName = "picture_vector_idx";

  private String indexType = "IVF_FLAT";

  private String metricType = "COSINE";

  private Integer shardsNum = 2;

  private Integer nlist = 1024;

  private Integer nprobe = 20;

  private Integer hnswM = 16;

  private Integer hnswEfConstruction = 200;

  private Integer hnswEfSearch = 64;

  private Boolean autoCreateCollection = false;

  private Boolean autoLoadCollection = false;

  private Long syncLoadTimeoutMs = 30000L;

  private Long syncLoadIntervalMs = 500L;

  private Long syncFlushTimeoutMs = 30000L;

  private Long syncFlushIntervalMs = 500L;
}
