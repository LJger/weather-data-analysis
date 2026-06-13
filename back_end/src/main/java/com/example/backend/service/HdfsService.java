package com.example.backend.service;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.fs.permission.FsPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HDFS 分布式文件系统服务
 * 提供文件上传、下载、目录管理、状态查询等功能
 */
@Service
public class HdfsService {

    private static final Logger log = LoggerFactory.getLogger(HdfsService.class);

    private final FileSystem fileSystem;

    @Value("${hdfs.base-path}")
    private String basePath;

    public HdfsService(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    // ====================== 目录操作 ======================

    /**
     * 创建目录（递归）
     */
    public boolean mkdir(String path) {
        try {
            Path hdfsPath = resolvePath(path);
            boolean result = fileSystem.mkdirs(hdfsPath, FsPermission.getDirDefault());
            log.info("创建 HDFS 目录: {} -> {}", hdfsPath, result);
            return result;
        } catch (IOException e) {
            log.error("创建 HDFS 目录失败: {}", path, e);
            throw new RuntimeException("创建 HDFS 目录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列出目录内容
     */
    public List<Map<String, Object>> listDirectory(String path) {
        try {
            Path hdfsPath = resolvePath(path);
            if (!fileSystem.exists(hdfsPath)) {
                return Collections.emptyList();
            }

            FileStatus[] statuses = fileSystem.listStatus(hdfsPath);
            List<Map<String, Object>> result = new ArrayList<>();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Shanghai"));

            for (FileStatus status : statuses) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", status.getPath().getName());
                item.put("path", status.getPath().toUri().getPath());
                item.put("isDirectory", status.isDirectory());
                item.put("type", status.isDirectory() ? "DIRECTORY" : "FILE");
                long displaySize = status.getLen();
                if (status.isDirectory()) {
                    ContentSummary summary = fileSystem.getContentSummary(status.getPath());
                    displaySize = summary.getLength();
                    item.put("spaceConsumed", summary.getSpaceConsumed());
                    item.put("spaceConsumedFormatted", formatFileSize(summary.getSpaceConsumed()));
                    item.put("fileCount", summary.getFileCount());
                    item.put("directoryCount", summary.getDirectoryCount());
                } else {
                    long blockSize = status.getBlockSize();
                    int replication = Math.max(status.getReplication(), (short) 1);
                    long blockCount = blockSize > 0 && displaySize > 0
                            ? (displaySize + blockSize - 1) / blockSize
                            : 0L;
                    long spaceConsumed = displaySize * replication;
                    item.put("blockSize", blockSize);
                    item.put("blockSizeFormatted", formatFileSize(blockSize));
                    item.put("blockCount", blockCount);
                    item.put("spaceConsumed", spaceConsumed);
                    item.put("spaceConsumedFormatted", formatFileSize(spaceConsumed));
                }
                item.put("size", displaySize);
                item.put("sizeFormatted", formatFileSize(displaySize));
                item.put("replication", status.getReplication());
                item.put("modificationTime", dtf.format(Instant.ofEpochMilli(status.getModificationTime())));
                item.put("owner", status.getOwner());
                item.put("group", status.getGroup());
                item.put("permission", status.getPermission().toString());
                result.add(item);
            }
            result.sort(Comparator
                    .comparing((Map<String, Object> item) -> !Boolean.TRUE.equals(item.get("isDirectory")))
                    .thenComparing(item -> String.valueOf(item.get("name")).toLowerCase(Locale.ROOT)));
            return result;
        } catch (IOException e) {
            log.error("列出 HDFS 目录失败: {}", path, e);
            throw new RuntimeException("列出 HDFS 目录失败: " + e.getMessage(), e);
        }
    }

    // ====================== 文件操作 ======================

    /**
     * 上传文件到 HDFS
     */
    public String uploadFile(InputStream inputStream, String targetPath) {
        try {
            Path hdfsPath = resolvePath(targetPath);
            // 确保父目录存在
            fileSystem.mkdirs(hdfsPath.getParent());

            try (FSDataOutputStream outputStream = fileSystem.create(hdfsPath, true)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            log.info("文件上传至 HDFS 成功: {}", hdfsPath);
            return hdfsPath.toUri().getPath();
        } catch (IOException e) {
            log.error("上传文件至 HDFS 失败: {}", targetPath, e);
            throw new RuntimeException("上传文件至 HDFS 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将文本内容写入 HDFS（如 CSV 数据导出）
     */
    public String writeText(String content, String targetPath) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(
                content.getBytes(StandardCharsets.UTF_8))) {
            return uploadFile(bais, targetPath);
        } catch (IOException e) {
            throw new RuntimeException("写入文本至 HDFS 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 HDFS 下载文件
     */
    public byte[] downloadFile(String path) {
        try {
            Path hdfsPath = resolvePath(path);
            if (!fileSystem.exists(hdfsPath)) {
                throw new FileNotFoundException("HDFS 文件不存在: " + path);
            }

            try (FSDataInputStream inputStream = fileSystem.open(hdfsPath);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }
        } catch (IOException e) {
            log.error("从 HDFS 下载文件失败: {}", path, e);
            throw new RuntimeException("从 HDFS 下载文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件或目录
     */
    public boolean delete(String path, boolean recursive) {
        try {
            Path hdfsPath = resolvePath(path);
            if (!fileSystem.exists(hdfsPath)) {
                return false;
            }
            boolean result = fileSystem.delete(hdfsPath, recursive);
            log.info("删除 HDFS 路径: {} (recursive={}) -> {}", hdfsPath, recursive, result);
            return result;
        } catch (IOException e) {
            log.error("删除 HDFS 路径失败: {}", path, e);
            throw new RuntimeException("删除 HDFS 路径失败: " + e.getMessage(), e);
        }
    }

    /**
     * 重命名 / 移动文件
     */
    public boolean rename(String srcPath, String dstPath) {
        try {
            Path src = resolvePath(srcPath);
            Path dst = resolvePath(dstPath);
            boolean result = fileSystem.rename(src, dst);
            log.info("HDFS 重命名: {} -> {} = {}", src, dst, result);
            return result;
        } catch (IOException e) {
            log.error("HDFS 重命名失败: {} -> {}", srcPath, dstPath, e);
            throw new RuntimeException("HDFS 重命名失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断文件是否存在
     */
    public boolean exists(String path) {
        try {
            return fileSystem.exists(resolvePath(path));
        } catch (IOException e) {
            return false;
        }
    }

    // ====================== 存储统计 ======================

    /**
     * 获取 HDFS 集群存储状态
     */
    public Map<String, Object> getClusterStatus() {
        try {
            FsStatus status = fileSystem.getStatus();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("connected", true);
            result.put("uri", fileSystem.getUri().toString());
            result.put("basePath", basePath);
            result.put("safeMode", isInSafeMode());
            result.put("basePathExists", existsBasePath());
            result.put("totalStorage", status.getCapacity());
            result.put("usedStorage", status.getUsed());
            result.put("availableStorage", status.getRemaining());
            result.put("totalFormatted", formatFileSize(status.getCapacity()));
            result.put("usedFormatted", formatFileSize(status.getUsed()));
            result.put("availableFormatted", formatFileSize(status.getRemaining()));
            result.put("usagePercent", status.getCapacity() > 0
                    ? Math.round(status.getUsed() * 10000.0 / status.getCapacity()) / 100.0
                    : 0);
            return result;
        } catch (IOException e) {
            log.error("获取 HDFS 集群状态失败", e);
            throw new RuntimeException("获取 HDFS 集群状态失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取指定目录的大小（递归）
     */
    public long getDirectorySize(String path) {
        try {
            Path hdfsPath = resolvePath(path);
            if (!fileSystem.exists(hdfsPath)) {
                return 0;
            }
            ContentSummary summary = fileSystem.getContentSummary(hdfsPath);
            return summary.getLength();
        } catch (IOException e) {
            log.error("获取 HDFS 目录大小失败: {}", path, e);
            return 0;
        }
    }

    /**
     * 获取指定目录的详细统计信息
     */
    public Map<String, Object> getDirectorySummary(String path) {
        try {
            Path hdfsPath = resolvePath(path);
            if (!fileSystem.exists(hdfsPath)) {
                return Collections.emptyMap();
            }
            ContentSummary summary = fileSystem.getContentSummary(hdfsPath);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("path", hdfsPath.toUri().getPath());
            result.put("length", summary.getLength());
            result.put("lengthFormatted", formatFileSize(summary.getLength()));
            result.put("fileCount", summary.getFileCount());
            result.put("directoryCount", summary.getDirectoryCount());
            result.put("spaceConsumed", summary.getSpaceConsumed());
            result.put("spaceConsumedFormatted", formatFileSize(summary.getSpaceConsumed()));
            return result;
        } catch (IOException e) {
            log.error("获取 HDFS 目录统计失败: {}", path, e);
            throw new RuntimeException("获取 HDFS 目录统计失败: " + e.getMessage(), e);
        }
    }

    // ====================== 平台业务初始化 ======================

    /**
     * 初始化平台所需的 HDFS 目录结构
     */
    public void initPlatformDirectories() {
        String[] dirs = {
                "/",
                "/raw-data",           // 原始采集数据
                "/raw-data/meteor",    // 气象观测数据
                "/raw-data/history",   // 历史气象数据
                "/exports",            // 数据导出
                "/backups",            // 数据备份
                "/temp"                // 临时文件
        };
        for (String dir : dirs) {
            mkdir(dir);
        }
        log.info("HDFS 平台目录初始化完成");
    }

    public boolean existsBasePath() {
        try {
            return fileSystem.exists(new Path(basePath));
        } catch (IOException e) {
            log.warn("检查 HDFS 平台目录是否存在失败: {}", basePath, e);
            return false;
        }
    }

    public String getBasePath() {
        return basePath;
    }

    private Boolean isInSafeMode() {
        if (fileSystem instanceof DistributedFileSystem distributedFileSystem) {
            try {
                return distributedFileSystem.setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_GET);
            } catch (IOException e) {
                log.warn("获取 HDFS SafeMode 状态失败", e);
            }
        }
        return null;
    }

    // ====================== 工具方法 ======================

    /**
     * 解析路径（相对路径自动加上 basePath 前缀）
     */
    private Path resolvePath(String path) {
        if (path.startsWith("/")) {
            // 绝对路径但可能不包含 basePath
            if (!path.startsWith(basePath)) {
                return new Path(basePath + path);
            }
            return new Path(path);
        }
        return new Path(basePath + "/" + path);
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int idx = (int) (Math.log(bytes) / Math.log(1024));
        idx = Math.min(idx, units.length - 1);
        double value = bytes / Math.pow(1024, idx);
        return String.format("%.2f %s", value, units[idx]);
    }
}
