package com.web.yunpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.web.yunpicturebackend")
@MapperScan("com.web.yunpicturebackend.mapper")
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true) // 代理功能，可以增强目标类的方法
public class YunpictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YunpictureBackendApplication.class, args);
    }

}
