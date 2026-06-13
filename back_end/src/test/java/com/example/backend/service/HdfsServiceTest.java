package com.example.backend.service;

import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HdfsServiceTest {

    @Test
    void listDirectoryUsesRecursiveContentSummaryForDirectorySize() throws Exception {
        FileSystem fileSystem = mock(FileSystem.class);
        HdfsService hdfsService = new HdfsService(fileSystem);
        ReflectionTestUtils.setField(hdfsService, "basePath", "/weather-platform");

        Path parent = new Path("/weather-platform/raw-data");
        Path gisDirectory = new Path("/weather-platform/raw-data/gis");
        FileStatus directoryStatus = new FileStatus(0, true, 0, 0, 1_700_000_000_000L, gisDirectory);
        ContentSummary directorySummary = new ContentSummary.Builder()
                .length(2048)
                .spaceConsumed(4096)
                .fileCount(1)
                .directoryCount(1)
                .build();

        when(fileSystem.exists(parent)).thenReturn(true);
        when(fileSystem.listStatus(parent)).thenReturn(new FileStatus[]{directoryStatus});
        when(fileSystem.getContentSummary(gisDirectory)).thenReturn(directorySummary);

        List<Map<String, Object>> result = hdfsService.listDirectory("/raw-data");

        assertThat(result).hasSize(1);
        assertThat(result.get(0))
                .containsEntry("name", "gis")
                .containsEntry("isDirectory", true)
                .containsEntry("size", 2048L)
                .containsEntry("sizeFormatted", "2.00 KB")
                .containsEntry("spaceConsumed", 4096L)
                .containsEntry("spaceConsumedFormatted", "4.00 KB");
    }

    @Test
    void listDirectoryIncludesHdfsBlockAndReplicationMetadataForFiles() throws Exception {
        FileSystem fileSystem = mock(FileSystem.class);
        HdfsService hdfsService = new HdfsService(fileSystem);
        ReflectionTestUtils.setField(hdfsService, "basePath", "/weather-platform");

        Path parent = new Path("/weather-platform/raw-data/gis");
        Path file = new Path("/weather-platform/raw-data/gis/weather-gis-temperature.json");
        FileStatus fileStatus = new FileStatus(
                3_145_728L,
                false,
                3,
                1_048_576L,
                1_700_000_000_000L,
                file
        );

        when(fileSystem.exists(parent)).thenReturn(true);
        when(fileSystem.listStatus(parent)).thenReturn(new FileStatus[]{fileStatus});

        List<Map<String, Object>> result = hdfsService.listDirectory("/raw-data/gis");

        assertThat(result).hasSize(1);
        assertThat(result.get(0))
                .containsEntry("name", "weather-gis-temperature.json")
                .containsEntry("isDirectory", false)
                .containsEntry("replication", (short) 3)
                .containsEntry("blockSize", 1_048_576L)
                .containsEntry("blockSizeFormatted", "1.00 MB")
                .containsEntry("blockCount", 3L)
                .containsEntry("spaceConsumed", 9_437_184L)
                .containsEntry("spaceConsumedFormatted", "9.00 MB");
    }
}
