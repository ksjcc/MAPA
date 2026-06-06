package com.web.yunpicturebackend.config;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.hutool.core.util.StrUtil;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;

@Configuration
@EnableConfigurationProperties(MilvusProperties.class)
public class MilvusConfig {

  @Bean
  public MilvusServiceClient milvusClient(MilvusProperties milvusProperties) {
    ConnectParam.Builder builder = ConnectParam.newBuilder()
        .withHost(milvusProperties.getHost())
        .withPort(milvusProperties.getPort())
        .withConnectTimeout(milvusProperties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
        .withKeepAliveTime(milvusProperties.getKeepAliveTimeMs(), TimeUnit.MILLISECONDS)
        .withKeepAliveTimeout(milvusProperties.getKeepAliveTimeoutMs(), TimeUnit.MILLISECONDS)
        .withIdleTimeout(milvusProperties.getIdleTimeoutMs(), TimeUnit.MILLISECONDS)
        .withRpcDeadline(milvusProperties.getRpcDeadlineMs(), TimeUnit.MILLISECONDS)
        .withSecure(Boolean.TRUE.equals(milvusProperties.getSecure()));

    if (StrUtil.isNotBlank(milvusProperties.getDatabase())) {
      builder.withDatabaseName(milvusProperties.getDatabase());
    }
    if (StrUtil.isNotBlank(milvusProperties.getToken())) {
      builder.withToken(milvusProperties.getToken());
    } else if (StrUtil.isNotBlank(milvusProperties.getUsername())
        && StrUtil.isNotBlank(milvusProperties.getPassword())) {
      builder.withAuthorization(milvusProperties.getUsername(), milvusProperties.getPassword());
    }

    return new MilvusServiceClient(builder.build());
  }
}
