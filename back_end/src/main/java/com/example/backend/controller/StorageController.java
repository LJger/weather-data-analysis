package com.example.backend.controller;

import com.example.backend.service.GisWeatherHdfsArchiveService;
import com.example.backend.service.GisWeatherTableHdfsExportService;
import com.example.backend.service.HdfsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 存储管理控制器
 * 提供 HDFS 分布式存储的文件管理、集群状态查询等功能
 * 仅管理员可访问
 */
@RestController
@RequestMapping("/api/storage")
@PreAuthorize("hasRole('ADMIN')")
public class StorageController {

    private static final Logger log = LoggerFactory.getLogger(StorageController.class);

    private final HdfsService hdfsService;
    private final GisWeatherHdfsArchiveService gisWeatherHdfsArchiveService;
    private final GisWeatherTableHdfsExportService gisWeatherTableHdfsExportService;
    private final StringRedisTemplate redisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public StorageController(HdfsService hdfsService,
                              GisWeatherHdfsArchiveService gisWeatherHdfsArchiveService,
                              GisWeatherTableHdfsExportService gisWeatherTableHdfsExportService,
                              StringRedisTemplate redisTemplate,
                              RedisConnectionFactory redisConnectionFactory) {
        this.hdfsService = hdfsService;
        this.gisWeatherHdfsArchiveService = gisWeatherHdfsArchiveService;
        this.gisWeatherTableHdfsExportService = gisWeatherTableHdfsExportService;
        this.redisTemplate = redisTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    // ====================== 集群状态 ======================

    /**
     * 获取存储集群总览信息（HDFS + Redis）
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getStorageOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        // HDFS 集群状态
        try {
            Map<String, Object> hdfsStatus = hdfsService.getClusterStatus();
            overview.put("hdfs", hdfsStatus);
        } catch (Exception e) {
            log.warn("获取 HDFS 状态失败", e);
            overview.put("hdfs", Map.of(
                    "connected", false,
                    "basePath", hdfsService.getBasePath(),
                    "error", e.getMessage()
            ));
        }

        // Redis 状态
        try {
            overview.put("redis", buildRedisStatus());
        } catch (Exception e) {
            log.warn("获取 Redis 状态失败", e);
            overview.put("redis", Map.of("error", e.getMessage()));
        }

        // 平台数据目录统计
        try {
            Map<String, Object> dataDirs = new LinkedHashMap<>();
            dataDirs.put("rawData", hdfsService.getDirectorySummary("/raw-data"));
            dataDirs.put("exports", hdfsService.getDirectorySummary("/exports"));
            dataDirs.put("backups", hdfsService.getDirectorySummary("/backups"));
            dataDirs.put("temp", hdfsService.getDirectorySummary("/temp"));
            overview.put("basePath", hdfsService.getBasePath());
            overview.put("platformInitialized", hdfsService.existsBasePath());
            overview.put("directories", dataDirs);
        } catch (Exception e) {
            log.warn("获取平台目录统计失败", e);
            overview.put("basePath", hdfsService.getBasePath());
            overview.put("platformInitialized", false);
            overview.put("directories", Map.of("error", e.getMessage()));
        }

        return ResponseEntity.ok(overview);
    }

    // ====================== 文件管理 ======================

    /**
     * 浏览 HDFS 目录
     */
    @GetMapping("/files")
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam(defaultValue = "/") String path) {
        List<Map<String, Object>> files = hdfsService.listDirectory(path);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", path);
        result.put("files", files);
        result.put("total", files.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 上传文件到 HDFS
     */
    @PostMapping("/files/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "/uploads") String targetDir) throws IOException {
        String fileName = file.getOriginalFilename();
        String targetPath = targetDir + "/" + fileName;
        String hdfsPath = hdfsService.uploadFile(file.getInputStream(), targetPath);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "文件上传成功");
        result.put("path", hdfsPath);
        result.put("size", file.getSize());
        result.put("originalName", fileName);
        return ResponseEntity.ok(result);
    }

    /**
     * 从 HDFS 下载文件
     */
    @GetMapping("/files/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String path) {
        byte[] content = hdfsService.downloadFile(path);
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(content.length)
                .body(content);
    }

    /**
     * 删除 HDFS 文件 / 目录
     */
    @DeleteMapping("/files")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @RequestParam String path,
            @RequestParam(defaultValue = "false") boolean recursive) {
        boolean result = hdfsService.delete(path, recursive);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", result);
        body.put("message", result ? "删除成功" : "文件不存在或删除失败");
        body.put("path", path);
        return ResponseEntity.ok(body);
    }

    /**
     * 创建 HDFS 目录
     */
    @PostMapping("/files/mkdir")
    public ResponseEntity<Map<String, Object>> mkdir(@RequestParam String path) {
        boolean result = hdfsService.mkdir(path);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", result);
        body.put("message", result ? "目录创建成功" : "目录创建失败");
        body.put("path", path);
        return ResponseEntity.ok(body);
    }

    /**
     * 重命名 / 移动文件
     */
    @PostMapping("/files/rename")
    public ResponseEntity<Map<String, Object>> renameFile(
            @RequestParam String srcPath,
            @RequestParam String dstPath) {
        boolean result = hdfsService.rename(srcPath, dstPath);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", result);
        body.put("message", result ? "重命名成功" : "重命名失败");
        return ResponseEntity.ok(body);
    }

    /**
     * 获取指定目录的统计信息
     */
    @GetMapping("/files/summary")
    public ResponseEntity<Map<String, Object>> getDirectorySummary(
            @RequestParam(defaultValue = "/") String path) {
        return ResponseEntity.ok(hdfsService.getDirectorySummary(path));
    }

    /**
     * 将 GIS 页面当前使用的气象展示数据归档到 HDFS。
     */
    @PostMapping("/gis/archive")
    public ResponseEntity<Map<String, Object>> archiveGisWeatherData(
            @RequestParam(defaultValue = "temperature") String element,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime observationTime) {
        return ResponseEntity.ok(gisWeatherHdfsArchiveService.archiveSnapshot(element, observationTime));
    }

    /**
     * 将 GIS 分析查询依赖的数据库表全量导出到 HDFS。
     */
    @PostMapping("/gis/export-tables")
    public ResponseEntity<Map<String, Object>> exportGisWeatherTables() {
        return ResponseEntity.ok(gisWeatherTableHdfsExportService.exportAllTables());
    }

    // ====================== Redis 缓存管理 ======================

    /**
     * 获取 Redis 缓存状态，不触发 HDFS 查询
     */
    @GetMapping("/cache/overview")
    public ResponseEntity<Map<String, Object>> getRedisOverview() {
        return ResponseEntity.ok(buildRedisStatus());
    }

    /**
     * 获取 Redis 缓存 key 列表（按模式匹配）
     */
    @GetMapping("/cache/keys")
    public ResponseEntity<Map<String, Object>> getCacheKeys(
            @RequestParam(defaultValue = "*") String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        List<Map<String, Object>> items = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                Map<String, Object> item = new LinkedHashMap<>();
                Long ttlSeconds = redisTemplate.getExpire(key);
                DataType type = redisTemplate.type(key);
                item.put("key", key);
                item.put("ttlSeconds", ttlSeconds);
                item.put("type", type == null ? "none" : type.code());
                items.add(item);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pattern", pattern);
        result.put("keys", keys);
        result.put("items", items);
        result.put("count", keys != null ? keys.size() : 0);
        return ResponseEntity.ok(result);
    }

    /**
     * 清除指定模式的 Redis 缓存
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, Object>> clearCache(
            @RequestParam(defaultValue = "weather:*") String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        long deleted = 0;
        if (keys != null && !keys.isEmpty()) {
            deleted = redisTemplate.delete(keys);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("pattern", pattern);
        result.put("deletedCount", deleted);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> buildRedisStatus() {
        Properties redisInfo = redisConnectionFactory.getConnection().info();
        Map<String, Object> redisStatus = new LinkedHashMap<>();
        redisStatus.put("usedMemory", redisInfo.getProperty("used_memory_human", "N/A"));
        redisStatus.put("connectedClients", redisInfo.getProperty("connected_clients", "0"));
        redisStatus.put("totalKeys", redisTemplate.getConnectionFactory()
                .getConnection().dbSize());
        redisStatus.put("uptime", redisInfo.getProperty("uptime_in_seconds", "0"));
        redisStatus.put("version", redisInfo.getProperty("redis_version", "unknown"));
        return redisStatus;
    }

    // ====================== 初始化 ======================

    /**
     * 初始化 HDFS 平台目录
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initPlatform() {
        hdfsService.initPlatformDirectories();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "HDFS 平台目录初始化完成");
        result.put("basePath", hdfsService.getBasePath());
        return ResponseEntity.ok(result);
    }
}
