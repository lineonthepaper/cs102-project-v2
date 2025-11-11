package com.smartattendance.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.smartattendance.dto.request.export.ExportAsCSVRequest;
import com.smartattendance.dto.request.export.ExportAsXLSXRequest;
import com.smartattendance.dto.request.export.ExportRequest;
import com.smartattendance.entity.XLSXSheetEntry;

@Service
public class ExportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Singapore");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public byte[] exportSectionAsXLSX(ExportAsXLSXRequest request) {

        List<Map<String, Object>> data = getCourseDataFromDatabase(request);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Workbook workbook = new XSSFWorkbook();
        Map<String, XLSXSheetEntry> sheets = new HashMap<>();

        try {

            if (data == null || data.isEmpty()) {
                Sheet sheet = createNewSheet(workbook, "Attendance Data");

                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("No data found for the specified criteria.");

                workbook.write(baos);
                workbook.close();

                return baos.toByteArray();
            }

            for (Map<String, Object> dataRow : data) {
                insertIntoWorkbookBySectionCode(workbook, sheets, dataRow);
            }

            workbook.write(baos);
            workbook.close();

            return baos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    public String exportSectionAsCSV(ExportAsCSVRequest request) {
        List<Map<String, Object>> data = getCourseDataFromDatabase(request);

        if (data == null || data.isEmpty()) {
            return "No data found for the specified criteria.";
        }

        StringBuilder csvContent = new StringBuilder();

        String[] headers = {
                "Course Section",
                "Year",
                "Semester",
                "Session Date",
                "Student ID",
                "Name",
                "Status",
                "Check-in Date",
                "Check-in Time",
                "Notes",
                "Marking Method",
                "Confidence Level"
        };

        csvContent.append(String.join(",", headers)).append("\n");

        for (Map<String, Object> dataRow : data) {
            if (dataRow.get("session_id") == null && data.size() == 1) {
                csvContent.append("No data found for the specified criteria.");
                break;
            } else if (dataRow.get("session_id") == null) {
                continue;
            }

            String[] csvRow = {
                    dataRow.get("section_code").toString(),
                    dataRow.get("year").toString(),
                    dataRow.get("semester").toString(),
                    formatSessionDate(dataRow.get("session_date")),
                    dataRow.get("user_id").toString(),
                    dataRow.get("first_name") + " " + dataRow.get("last_name"),
                    dataRow.get("status").toString(),
                    formatCheckinDate(dataRow.get("checkin_time")),
                    formatCheckinTime(dataRow.get("checkin_time")),
                    dataRow.get("notes") != null ? dataRow.get("notes").toString() : "",
                    Boolean.parseBoolean(dataRow.get("is_automatic").toString()) ? "Automatic" : "Manual",
                    Double.parseDouble(dataRow.get("confidence_level").toString()) >= 0
                            ? dataRow.get("confidence_level").toString()
                            : "N/A"
            };

            csvContent.append(String.join(",", csvRow)).append("\n");
        }

        return csvContent.toString();
    }

    private List<Map<String, Object>> getCourseDataFromDatabase(ExportRequest request) {
        // sql params here
        String userIdSql;

        if (request.getRole().equals("admin")) {
            userIdSql = "";
        } else if (request.getRole().equals("instructor")) {
            userIdSql = "AND sa.user_id = '" + request.getUserId().toString() + "'";
        } else if (request.getRole().equals("teaching assistant")) {
            userIdSql = "AND ta.user_id = '" + request.getUserId().toString() + "'";
        } else {
            return null;
        }

        String sectionCodeSql = request.getSectionCode().equals("all") ? " like '%'"
                : "= '" + request.getSectionCode() + "'";
        String yearSql = request.getYear().equals("all") ? "> 0" : "=" + request.getYear();
        String semesterSql = request.getSemester().equals("all") ? "> 0" : "=" + request.getSemester();

        // fetch from db
        List<Map<String, Object>> data = null;
        if (request.getRole().equals("admin") || request.getRole().equals("instructor")) {
            data = jdbcTemplate.queryForList("SELECT\r\n" + //
                    "   ar.*, \r\n" + //
                    "   u.first_name, \r\n" + //
                    "   u.last_name, \r\n" + //
                    "   c.title, \r\n" + //
                    "   c.code, \r\n" + //
                    "   asess.session_date, \r\n" + //
                    "   s.section_code, \r\n" + //
                    "   s.year, \r\n" + //
                    "   s.semester \r\n" + //
                    "FROM \r\n" + //
                    "   section_assignments sa \r\n" + //
                    "JOIN \r\n" + //
                    "   sections s ON sa.section_id = s.id \r\n" + //
                    "JOIN \r\n" + //
                    "   courses c ON s.course_id = c.id \r\n" + //
                    "LEFT JOIN \r\n" + //
                    "   attendance_sessions asess ON s.id = asess.section_id \r\n" + //
                    "LEFT JOIN \r\n" + //
                    "   attendance_records ar ON asess.id = ar.session_id \r\n" + //
                    "LEFT JOIN \r\n" + //
                    "   users u ON ar.user_id = u.id \r\n" + //
                    "WHERE \r\n" + //
                    "   s.year " + yearSql + " \r\n" + //
                    userIdSql + " \r\n" + //
                    "   AND s.semester " + semesterSql + " \r\n" + //
                    "   AND s.section_code " + sectionCodeSql + " \r\n" + //
                    "ORDER BY \r\n" + //
                    "   ar.id ASC nulls last,\r\n" + //
                    "   s.section_code ASC, \r\n" + //
                    "   s.year DESC, \r\n" + //
                    "   s.semester ASC\r\n");

        } else {
            data = jdbcTemplate.queryForList("SELECT\r\n" + //
                    "   ar.*,\r\n" + //
                    "   u.first_name,\r\n" + //
                    "   u.last_name,\r\n" + //
                    "   c.title,\r\n" + //
                    "   c.code,\r\n" + //
                    "   asess.session_date,\r\n" + //
                    "   s.section_code,\r\n" + //
                    "   s.year,\r\n" + //
                    "   s.semester\r\n" + //
                    "FROM\r\n" + //
                    "   ta_assignments ta\r\n" + //
                    "JOIN\r\n" + //
                    "   sections s ON ta.section_id = s.id\r\n" + //
                    "JOIN\r\n" + //
                    "   courses c ON s.course_id = c.id\r\n" + //
                    "LEFT JOIN\r\n" + //
                    "   attendance_sessions asess ON s.id = asess.section_id\r\n" + //
                    "LEFT JOIN\r\n" + //
                    "   attendance_records ar ON asess.id = ar.session_id\r\n" + //
                    "LEFT JOIN\r\n" + //
                    "   users u ON ar.user_id = u.id\r\n" + //
                    "WHERE\r\n" + //
                    "   s.year " + yearSql + " \r\n" + //
                    userIdSql + " \r\n" + //
                    "   AND s.semester " + semesterSql + " \r\n" + //
                    "   AND s.section_code " + sectionCodeSql + " \r\n" + //
                    "ORDER BY\r\n" + //
                    "   ar.id ASC nulls last,\r\n" + //
                    "   s.section_code ASC,\r\n" + //
                    "   s.year DESC,\r\n" + //
                    "   s.semester ASC");

        }
        return data;
    }

    private void insertIntoWorkbookBySectionCode(Workbook workbook, Map<String, XLSXSheetEntry> sheets,
            Map<String, Object> dataRow) {
        String entryKey = dataRow.get("section_code").toString() + " Semester " + dataRow.get("semester").toString()
                + " " + dataRow.get("year").toString();

        // create sheet for section if doesn't exist
        if (sheets.get(entryKey) == null) {
            Sheet sheet = createNewSheet(workbook, entryKey);
            sheets.put(entryKey, new XLSXSheetEntry(sheet, 1));
        }

        // insert data into sheet
        XLSXSheetEntry sheetEntry = sheets.get(entryKey);
        insertDataIntoSheet(sheetEntry, dataRow);
    }

    private void insertDataIntoSheet(XLSXSheetEntry sheetEntry, Map<String, Object> dataRow) {
        if (dataRow.get("session_id") == null) {
            if (sheetEntry.getCurrentRowIndex() == 1) {
                Sheet sheet = sheetEntry.getSheet();
                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("No data found for the specified criteria.");
            }
            return;
        }

        Sheet sheet = sheetEntry.getSheet();

        int currentRowIndex = sheetEntry.getCurrentRowIndex();
        Row row = sheet.createRow(currentRowIndex);

        // Write data to cells in the order of headers
        row.createCell(0).setCellValue(dataRow.get("section_code").toString());
        row.createCell(1).setCellValue(dataRow.get("year").toString());
        row.createCell(2).setCellValue(dataRow.get("semester").toString());
        row.createCell(3).setCellValue(formatSessionDate(dataRow.get("session_date")));
        row.createCell(4).setCellValue(dataRow.get("user_id").toString());
        row.createCell(5).setCellValue(dataRow.get("first_name") + " " + dataRow.get("last_name"));
        row.createCell(6).setCellValue(dataRow.get("status").toString());
        row.createCell(7).setCellValue(formatCheckinDate(dataRow.get("checkin_time")));
        row.createCell(8).setCellValue(formatCheckinTime(dataRow.get("checkin_time")));
        row.createCell(9).setCellValue(dataRow.get("notes") != null ? dataRow.get("notes").toString() : "");
        row.createCell(10).setCellValue(
                Boolean.parseBoolean(dataRow.get("is_automatic").toString()) ? "Automatic" : "Manual");
        row.createCell(11)
                .setCellValue(Double.parseDouble(dataRow.get("confidence_level").toString()) >= 0
                        ? dataRow.get("confidence_level").toString()
                        : "N/A");

        sheetEntry.setCurrentRowIndex(currentRowIndex + 1);
    }

    private String formatCheckinDate(Object value) {
        LocalDateTime dateTime = toLocalDateTime(value);
        if (dateTime == null) {
            return "";
        }
        LocalDateTime adjusted = dateTime.plusHours(8);
        return DATE_FORMAT.format(adjusted.toLocalDate());
    }

    private String formatCheckinTime(Object value) {
        LocalDateTime dateTime = toLocalDateTime(value);
        if (dateTime == null) {
            return "";
        }
        LocalDateTime adjusted = dateTime.plusHours(8);
        return TIME_FORMAT.format(adjusted.toLocalTime());
    }

    private String formatSessionDate(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof java.sql.Date sqlDate) {
            return DATE_FORMAT.format(sqlDate.toLocalDate());
        }
        if (value instanceof LocalDate localDate) {
            return DATE_FORMAT.format(localDate);
        }
        if (value instanceof Timestamp timestamp) {
            return DATE_FORMAT.format(timestamp.toLocalDateTime().toLocalDate());
        }

        String raw = value.toString();
        if (raw.contains("T")) {
            return raw.substring(0, raw.indexOf('T'));
        }
        if (raw.contains(" ")) {
            return raw.substring(0, raw.indexOf(' '));
        }
        return raw;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.atZoneSameInstant(DEFAULT_ZONE).toLocalDateTime();
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atZone(DEFAULT_ZONE).toLocalDateTime();
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().atStartOfDay();
        }

        String raw = value.toString().trim();
        if (raw.isEmpty()) {
            return null;
        }

        try {
            OffsetDateTime parsedOffset = OffsetDateTime.parse(raw);
            return parsedOffset.atZoneSameInstant(DEFAULT_ZONE).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        try {
            // Handle formats like "2025-10-07 08:55:00"
            String sanitized = raw.contains("T") ? raw : raw.replace(' ', 'T');
            return LocalDateTime.parse(sanitized);
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private Sheet createNewSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.createSheet(sheetName);

        String[] headers = {
                "Course Section",
                "Year",
                "Semester",
                "Session",
                "Student ID",
                "Name",
                "Status",
                "Check-in Time",
                "Notes",
                "Marking Method",
                "Confidence Level"
        };

        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        return sheet;
    }
}