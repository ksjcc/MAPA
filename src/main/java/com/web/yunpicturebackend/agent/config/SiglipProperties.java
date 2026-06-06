package com.web.yunpicturebackend.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "siglip")
public class SiglipProperties {

  private String host;

  private Integer port;
}
