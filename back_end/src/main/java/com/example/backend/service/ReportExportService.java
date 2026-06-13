package com.example.backend.service;

import com.example.backend.dto.AnalysisReportExportRequest;
import com.example.backend.dto.MeteorDataCorrelationMatrixCell;
import com.example.backend.dto.MeteorDataCorrelationRequest;
import com.example.backend.dto.MeteorDataCorrelationResponse;
import com.example.backend.dto.MeteorDataFilterOptionsResponse;
import com.example.backend.dto.MeteorDataSpatialProvinceStat;
import com.example.backend.dto.MeteorDataSpatialResponse;
import com.example.backend.dto.MeteorDataSpatialStationPoint;
import com.example.backend.dto.MeteorDataTableRecordResponse;
import com.example.backend.dto.MeteorDataTimeSeriesPoint;
import com.example.backend.dto.MeteorRecordExportRequest;
import com.example.backend.dto.ReportFilterSummary;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReportExportService {

    private static final int MAX_EXPORT_ROWS = 100_000;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Map<String, String> ELEMENT_NAME_MAP = Map.ofEntries(
            Map.entry("PRS", "气压 (PRS)"),
            Map.entry("PRS_Sea", "海平面气压 (PRS_Sea)"),
            Map.entry("PRS_Max", "最高气压 (PRS_Max)"),
            Map.entry("PRS_Min", "最低气压 (PRS_Min)"),
            Map.entry("TEM", "气温 (TEM)"),
            Map.entry("TEM_MAX", "最高气温 (TEM_MAX)"),
            Map.entry("TEM_MIN", "最低气温 (TEM_MIN)"),
            Map.entry("RHU", "相对湿度 (RHU)"),
            Map.entry("RHU_Min", "最小相对湿度 (RHU_Min)"),
            Map.entry("VAP", "水汽压 (VAP)"),
            Map.entry("PRE_3h", "3小时降水量 (PRE_3h)"),
            Map.entry("WIN_D_INST_Max", "极大风速风向 (WIN_D_INST_Max)"),
            Map.entry("WIN_S_MAX", "最大风速 (WIN_S_MAX)"),
            Map.entry("WIN_D_S_Max", "最大风速风向 (WIN_D_S_Max)"),
            Map.entry("WIN_S_Avg_2mi", "2分钟平均风速 (WIN_S_Avg_2mi)"),
            Map.entry("WIN_D_Avg_2mi", "2分钟平均风向 (WIN_D_Avg_2mi)"),
            Map.entry("WEP_Now", "现在天气 (WEP_Now)"),
            Map.entry("WIN_S_Inst_Max", "极大风速 (WIN_S_Inst_Max)"),
            Map.entry("VIS", "水平能见度 (VIS)"),
            Map.entry("CLO_Cov", "总云量 (CLO_Cov)"),
            Map.entry("CLO_Cov_Low", "低云量 (CLO_Cov_Low)"),
            Map.entry("CLO_COV_LM", "云量 (CLO_COV_LM)")
    );

    private final MeteorDataService meteorDataService;
    private final HdfsService hdfsService;

    public ReportExportService(MeteorDataService meteorDataService, HdfsService hdfsService) {
        this.meteorDataService = meteorDataService;
        this.hdfsService = hdfsService;
    }

    @Transactional(readOnly = true)
    public byte[] exportAnalysisReport(Integer userId, AnalysisReportExportRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("导出请求不能为空");
        }
        String analysisType = normalizeAnalysisType(request.analysisType());
        ReportFilterSummary filters = Optional.ofNullable(request.filters())
                .orElse(new ReportFilterSummary(null, null, null, null, null, null, null, null));

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            addTitle(document, analysisTypeName(analysisType) + "报告");
            addParagraph(document, "导出时间：" + formatTime(OffsetDateTime.now()));
            addFilterTable(document, userId, analysisType, filters);
            addHeading(document, "可视化图表");
            addChartImage(document, request.chartImageBase64());
            addHeading(document, "分析结论");
            buildAnalysisLines(userId, analysisType, filters).forEach(line -> addBullet(document, line));
            document.write(outputStream);
            byte[] content = outputStream.toByteArray();
            archiveGeneratedReport(content, reportPath(userId, "analysis", "weather-analysis-report", "docx"));
            return content;
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportRecordsExcel(Integer userId, MeteorRecordExportRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("导出请求不能为空");
        }
        long total = meteorDataService.countRecordsByFilters(
                userId,
                request.taskIds(),
                request.stationIds(),
                request.elementCodes(),
                request.startTime(),
                request.endTime()
        );
        if (total > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException("当前筛选结果共 " + total + " 条，超过 " + MAX_EXPORT_ROWS + " 条导出上限，请缩小筛选范围");
        }

        List<MeteorDataTableRecordResponse> records = meteorDataService.queryRecordsForExport(
                userId,
                request.taskIds(),
                request.stationIds(),
                request.elementCodes(),
                request.startTime(),
                request.endTime(),
                MAX_EXPORT_ROWS
        );

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            createFilterSheet(workbook, headerStyle, userId, request, total);
            createRecordSheet(workbook, headerStyle, records);
            workbook.write(outputStream);
            byte[] content = outputStream.toByteArray();
            archiveGeneratedReport(content, reportPath(userId, "records", "weather-data-records", "xlsx"));
            return content;
        }
    }

    private void archiveGeneratedReport(byte[] content, String targetPath) {
        hdfsService.uploadFile(new ByteArrayInputStream(content), targetPath);
    }

    private String reportPath(Integer userId, String category, String filenamePrefix, String extension) {
        String timestamp = OffsetDateTime.now().format(FILE_TIME_FORMATTER);
        return "/exports/reports/user-" + userId + "/" + category + "/" + filenamePrefix + "-" + timestamp + "." + extension;
    }

    private void createFilterSheet(Workbook workbook,
                                   CellStyle headerStyle,
                                   Integer userId,
                                   MeteorRecordExportRequest request,
                                   long total) {
        Sheet sheet = workbook.createSheet("筛选条件");
        MeteorDataFilterOptionsResponse options = meteorDataService.loadFilterOptions(userId);
        List<String[]> rows = List.of(
                new String[]{"导出时间", formatTime(OffsetDateTime.now())},
                new String[]{"任务", taskLabels(request.taskIds(), options)},
                new String[]{"站点", stationLabels(request.stationIds(), options)},
                new String[]{"要素", elementLabels(request.elementCodes())},
                new String[]{"开始时间", formatTime(request.startTime())},
                new String[]{"结束时间", formatTime(request.endTime())},
                new String[]{"导出记录数", String.valueOf(total)}
        );
        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(rows.get(i)[0]);
            row.getCell(0).setCellStyle(headerStyle);
            row.createCell(1).setCellValue(rows.get(i)[1]);
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createRecordSheet(Workbook workbook,
                                   CellStyle headerStyle,
                                   List<MeteorDataTableRecordResponse> records) {
        Sheet sheet = workbook.createSheet("气象数据明细");
        sheet.createFreezePane(0, 1);
        String[] headers = {"时间", "任务ID", "任务名称", "站点ID", "站点名称", "要素代码", "要素名称", "数值"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
            header.getCell(i).setCellStyle(headerStyle);
        }
        for (int i = 0; i < records.size(); i++) {
            MeteorDataTableRecordResponse record = records.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(formatTime(record.datetime()));
            row.createCell(1).setCellValue(record.taskId() == null ? "" : String.valueOf(record.taskId()));
            row.createCell(2).setCellValue(nullToDash(record.taskName()));
            row.createCell(3).setCellValue(record.stationId() == null ? "" : String.valueOf(record.stationId()));
            row.createCell(4).setCellValue(nullToDash(record.stationName()));
            row.createCell(5).setCellValue(nullToDash(record.elementCode()));
            row.createCell(6).setCellValue(elementLabel(record.elementCode()));
            if (record.value() != null) {
                row.createCell(7).setCellValue(record.value().doubleValue());
            } else {
                row.createCell(7).setCellValue("");
            }
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private List<String> buildAnalysisLines(Integer userId, String analysisType, ReportFilterSummary filters) {
        return switch (analysisType) {
            case "timeSeries" -> buildTimeSeriesAnalysis(userId, filters);
            case "correlation" -> buildCorrelationAnalysis(userId, filters);
            case "spatial" -> buildSpatialAnalysis(userId, filters);
            default -> throw new IllegalArgumentException("不支持的分析类型：" + analysisType);
        };
    }

    private List<String> buildTimeSeriesAnalysis(Integer userId, ReportFilterSummary filters) {
        String elementCode = Optional.ofNullable(filters.elementCode())
                .orElseGet(() -> firstOrNull(filters.elementCodes()));
        if (elementCode == null || elementCode.isBlank()) {
            throw new IllegalArgumentException("时间序列报告需要选择气象要素");
        }
        List<MeteorDataTimeSeriesPoint> points = meteorDataService.loadTimeSeries(
                userId,
                elementCode,
                filters.startTime(),
                filters.endTime(),
                filters.granularity(),
                filters.stationIds(),
                filters.taskIds()
        );
        List<MeteorDataTimeSeriesPoint> valid = points.stream()
                .filter(point -> point.average() != null)
                .toList();
        if (valid.isEmpty()) {
            return List.of("当前筛选条件下无可分析数据。");
        }
        BigDecimal avg = valid.stream()
                .map(MeteorDataTimeSeriesPoint::average)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valid.size()), 4, java.math.RoundingMode.HALF_UP);
        MeteorDataTimeSeriesPoint max = valid.stream()
                .max(Comparator.comparing(point -> Optional.ofNullable(point.maximum()).orElse(point.average())))
                .orElse(valid.get(0));
        MeteorDataTimeSeriesPoint min = valid.stream()
                .min(Comparator.comparing(point -> Optional.ofNullable(point.minimum()).orElse(point.average())))
                .orElse(valid.get(0));
        BigDecimal first = valid.get(0).average();
        BigDecimal last = valid.get(valid.size() - 1).average();
        String trend = last.compareTo(first) > 0 ? "整体上升" : last.compareTo(first) < 0 ? "整体下降" : "整体持平";
        return List.of(
                "共分析 " + valid.size() + " 个时间桶，平均值为 " + formatNumber(avg) + "。",
                "最高值为 " + formatNumber(Optional.ofNullable(max.maximum()).orElse(max.average())) + "，出现在 " + formatTime(max.timestamp()) + "。",
                "最低值为 " + formatNumber(Optional.ofNullable(min.minimum()).orElse(min.average())) + "，出现在 " + formatTime(min.timestamp()) + "。",
                "首末时间桶均值由 " + formatNumber(first) + " 变化至 " + formatNumber(last) + "，趋势判断为" + trend + "。"
        );
    }

    private List<String> buildCorrelationAnalysis(Integer userId, ReportFilterSummary filters) {
        List<String> elementCodes = filters.elementCodes();
        if (elementCodes == null || elementCodes.size() < 2) {
            throw new IllegalArgumentException("相关性报告需要至少选择两个气象要素");
        }
        MeteorDataCorrelationResponse response = meteorDataService.calculateCorrelation(
                new MeteorDataCorrelationRequest(
                        userId,
                        elementCodes,
                        filters.taskIds(),
                        filters.stationIds(),
                        filters.startTime(),
                        filters.endTime(),
                        filters.granularity()
                )
        );
        List<MeteorDataCorrelationMatrixCell> cells = response.matrix().stream()
                .filter(cell -> !Objects.equals(cell.xElement(), cell.yElement()))
                .filter(cell -> Double.isFinite(cell.coefficient()))
                .toList();
        if (cells.isEmpty()) {
            return List.of("当前筛选条件下无可分析数据。");
        }
        MeteorDataCorrelationMatrixCell strongestPositive = cells.stream()
                .max(Comparator.comparingDouble(MeteorDataCorrelationMatrixCell::coefficient))
                .orElse(cells.get(0));
        MeteorDataCorrelationMatrixCell strongestNegative = cells.stream()
                .min(Comparator.comparingDouble(MeteorDataCorrelationMatrixCell::coefficient))
                .orElse(cells.get(0));
        int maxSamples = cells.stream().mapToInt(MeteorDataCorrelationMatrixCell::effectiveSamples).max().orElse(0);
        return List.of(
                "本次相关性分析覆盖 " + response.elementCodes().size() + " 个气象要素，统计粒度为 " + granularityLabel(response.granularity()) + "。",
                "最强正相关为 " + elementLabel(strongestPositive.xElement()) + " 与 " + elementLabel(strongestPositive.yElement()) + "，相关系数 " + formatNumber(strongestPositive.coefficient()) + "，" + correlationLevel(strongestPositive.coefficient()) + "。",
                "最强负相关为 " + elementLabel(strongestNegative.xElement()) + " 与 " + elementLabel(strongestNegative.yElement()) + "，相关系数 " + formatNumber(strongestNegative.coefficient()) + "，" + correlationLevel(strongestNegative.coefficient()) + "。",
                "最大有效样本量为 " + maxSamples + "，样本量越高的系数可信度越高。"
        );
    }

    private List<String> buildSpatialAnalysis(Integer userId, ReportFilterSummary filters) {
        String elementCode = Optional.ofNullable(filters.elementCode())
                .orElseGet(() -> firstOrNull(filters.elementCodes()));
        if (elementCode == null || elementCode.isBlank()) {
            throw new IllegalArgumentException("空间分析报告需要选择气象要素");
        }
        MeteorDataSpatialResponse response = meteorDataService.loadSpatialMap(
                userId,
                elementCode,
                filters.startTime(),
                filters.endTime(),
                filters.province()
        );
        if (filters.province() != null && !filters.province().isBlank()) {
            List<MeteorDataSpatialStationPoint> stations = response.stations().stream()
                    .filter(station -> station.value() != null)
                    .toList();
            if (stations.isEmpty()) {
                return List.of("当前筛选条件下无可分析数据。");
            }
            MeteorDataSpatialStationPoint max = stations.stream()
                    .max(Comparator.comparing(MeteorDataSpatialStationPoint::value))
                    .orElse(stations.get(0));
            MeteorDataSpatialStationPoint min = stations.stream()
                    .min(Comparator.comparing(MeteorDataSpatialStationPoint::value))
                    .orElse(stations.get(0));
            return List.of(
                    "空间分析范围为 " + filters.province() + "，覆盖 " + stations.size() + " 个有效站点。",
                    "站点最高值为 " + max.stationName() + "，数值 " + formatNumber(max.value()) + "。",
                    "站点最低值为 " + min.stationName() + "，数值 " + formatNumber(min.value()) + "。",
                    "空间差异可用于识别区域内气象要素的高值区与低值区。"
            );
        }
        List<MeteorDataSpatialProvinceStat> provinces = response.provinces().stream()
                .filter(province -> province.averageValue() != null)
                .toList();
        if (provinces.isEmpty()) {
            return List.of("当前筛选条件下无可分析数据。");
        }
        MeteorDataSpatialProvinceStat max = provinces.stream()
                .max(Comparator.comparing(MeteorDataSpatialProvinceStat::averageValue))
                .orElse(provinces.get(0));
        MeteorDataSpatialProvinceStat min = provinces.stream()
                .min(Comparator.comparing(MeteorDataSpatialProvinceStat::averageValue))
                .orElse(provinces.get(0));
        long stationCount = provinces.stream().map(MeteorDataSpatialProvinceStat::stationCount).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
        return List.of(
                "空间分析覆盖 " + provinces.size() + " 个省级区域，累计站点数 " + stationCount + "。",
                "省级最高平均值为 " + max.province() + "，数值 " + formatNumber(max.averageValue()) + "。",
                "省级最低平均值为 " + min.province() + "，数值 " + formatNumber(min.averageValue()) + "。",
                "整体空间分布反映了不同区域之间的气象要素差异。"
        );
    }

    private void addTitle(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(20);
        run.setText(title);
    }

    private void addHeading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setText(text);
    }

    private void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.createRun().setText(text);
    }

    private void addBullet(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setIndentationLeft(360);
        paragraph.createRun().setText("• " + text);
    }

    private void addFilterTable(XWPFDocument document, Integer userId, String analysisType, ReportFilterSummary filters) {
        addHeading(document, "筛选条件");
        MeteorDataFilterOptionsResponse options = meteorDataService.loadFilterOptions(userId);
        XWPFTable table = document.createTable();
        addTableRow(table.getRow(0), "分析类型", analysisTypeName(analysisType));
        addTableRow(table.createRow(), "任务", taskLabels(filters.taskIds(), options));
        addTableRow(table.createRow(), "站点", stationLabels(filters.stationIds(), options));
        addTableRow(table.createRow(), "气象要素", filterElementLabel(filters));
        addTableRow(table.createRow(), "时间范围", formatTime(filters.startTime()) + " 至 " + formatTime(filters.endTime()));
        addTableRow(table.createRow(), "统计粒度", granularityLabel(filters.granularity()));
        if (filters.province() != null && !filters.province().isBlank()) {
            addTableRow(table.createRow(), "省份", filters.province());
        }
    }

    private void addTableRow(XWPFTableRow row, String name, String value) {
        row.getCell(0).setText(name);
        if (row.getTableCells().size() < 2) {
            row.addNewTableCell();
        }
        row.getCell(1).setText(value);
    }

    private void addChartImage(XWPFDocument document, String chartImageBase64) {
        if (chartImageBase64 == null || chartImageBase64.isBlank()) {
            addParagraph(document, "未提供图表截图。");
            return;
        }
        String encoded = chartImageBase64.contains(",")
                ? chartImageBase64.substring(chartImageBase64.indexOf(',') + 1)
                : chartImageBase64;
        try (ByteArrayInputStream imageInput = new ByteArrayInputStream(Base64.getDecoder().decode(encoded))) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = paragraph.createRun();
            run.addPicture(imageInput, XWPFDocument.PICTURE_TYPE_PNG, "chart.png", Units.toEMU(520), Units.toEMU(300));
        } catch (Exception ignored) {
            addParagraph(document, "图表截图解析失败，报告仅保留文字分析。");
        }
    }

    private String taskLabels(List<Long> taskIds, MeteorDataFilterOptionsResponse options) {
        if (taskIds == null || taskIds.isEmpty()) {
            return "全部任务";
        }
        Map<Long, String> taskMap = options.tasks().stream()
                .collect(Collectors.toMap(task -> task.taskId(), task -> task.taskName(), (left, right) -> left));
        return taskIds.stream()
                .map(taskId -> taskMap.getOrDefault(taskId, String.valueOf(taskId)))
                .collect(Collectors.joining("、"));
    }

    private String stationLabels(List<Integer> stationIds, MeteorDataFilterOptionsResponse options) {
        if (stationIds == null || stationIds.isEmpty()) {
            return "全部站点";
        }
        Map<Integer, String> stationMap = options.stations().stream()
                .collect(Collectors.toMap(station -> station.stationId(), station -> station.stationName(), (left, right) -> left));
        return stationIds.stream()
                .map(stationId -> stationMap.getOrDefault(stationId, "站点" + stationId) + " (" + stationId + ")")
                .collect(Collectors.joining("、"));
    }

    private String elementLabels(List<String> elementCodes) {
        if (elementCodes == null || elementCodes.isEmpty()) {
            return "全部要素";
        }
        return elementCodes.stream().map(this::elementLabel).collect(Collectors.joining("、"));
    }

    private String filterElementLabel(ReportFilterSummary filters) {
        if (filters.elementCode() != null && !filters.elementCode().isBlank()) {
            return elementLabel(filters.elementCode());
        }
        return elementLabels(filters.elementCodes());
    }

    private String elementLabel(String code) {
        if (code == null || code.isBlank()) {
            return "-";
        }
        return ELEMENT_NAME_MAP.getOrDefault(code, code);
    }

    private String normalizeAnalysisType(String analysisType) {
        if (analysisType == null || analysisType.isBlank()) {
            throw new IllegalArgumentException("分析类型不能为空");
        }
        String normalized = analysisType.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "timeseries", "time-series", "time_series" -> "timeSeries";
            case "correlation" -> "correlation";
            case "spatial" -> "spatial";
            default -> throw new IllegalArgumentException("不支持的分析类型：" + analysisType);
        };
    }

    private String analysisTypeName(String analysisType) {
        return switch (analysisType) {
            case "timeSeries" -> "时间序列分析";
            case "correlation" -> "要素相关性分析";
            case "spatial" -> "空间分析";
            default -> analysisType;
        };
    }

    private String granularityLabel(String granularity) {
        if (granularity == null || granularity.isBlank()) {
            return "小时";
        }
        return switch (granularity.toLowerCase(Locale.ROOT)) {
            case "day" -> "天";
            case "week" -> "周";
            case "month" -> "月";
            default -> "小时";
        };
    }

    private String correlationLevel(double coefficient) {
        double abs = Math.abs(coefficient);
        if (abs >= 0.8) {
            return "呈强相关";
        }
        if (abs >= 0.5) {
            return "呈中等相关";
        }
        if (abs >= 0.3) {
            return "呈弱相关";
        }
        return "相关性较弱";
    }

    private String formatTime(OffsetDateTime time) {
        return time == null ? "-" : time.format(TIME_FORMATTER);
    }

    private String formatNumber(BigDecimal value) {
        return value == null ? "-" : value.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatNumber(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String firstOrNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
