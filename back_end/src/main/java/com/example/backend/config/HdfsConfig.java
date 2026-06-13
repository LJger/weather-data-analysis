package com.example.backend.config;

import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;

/**
 * HDFS 分布式文件系统配置
 */
@Configuration
public class HdfsConfig {

    private static final Logger log = LoggerFactory.getLogger(HdfsConfig.class);

    @Value("${hdfs.uri}")
    private String hdfsUri;

    @Value("${hdfs.user}")
    private String hdfsUser;

    @Bean
    public org.apache.hadoop.conf.Configuration hadoopConfiguration() {
        org.apache.hadoop.conf.Configuration configuration = new org.apache.hadoop.conf.Configuration();
        configuration.set("fs.defaultFS", hdfsUri);
        // 禁用安全认证（开发环境）
        configuration.set("hadoop.security.authentication", "simple");
        // 设置副本数
        configuration.set("dfs.replication", "1");
        // 客户端使用 hostname 而非 IP（可根据集群网络配置调整）
        configuration.set("dfs.client.use.datanode.hostname", "true");
        return configuration;
    }

    @Bean
    public FileSystem fileSystem(org.apache.hadoop.conf.Configuration hadoopConfiguration) throws IOException {
        try {
            FileSystem fs = FileSystem.get(URI.create(hdfsUri), hadoopConfiguration, hdfsUser);
            log.info("HDFS 文件系统连接成功: {}", hdfsUri);
            return fs;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HDFS 连接被中断", e);
        }
    }
}
