package com.smartattendance.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartattendance.dto.request.user.CreateStudentRequest;
import com.smartattendance.dto.response.user.StudentDTO;
import com.smartattendance.dto.response.user.StudentFaceProcessingResult;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class ImportService {
    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final StudentService studentService;

    public ImportService(StudentService studentService) {
        this.studentService = studentService;
    }

    public Map<String, Object> importStudentsFromZip(MultipartFile zipFile) throws IOException {
        Path tempDir = Files.createTempDirectory("student_import_");

        try {
            Path extractDir = tempDir.resolve("extracted");
            Files.createDirectories(extractDir);
            extractZipFile(zipFile.getInputStream(), extractDir);

            Path dataFile = findSingleDataFile(extractDir);

            List<CreateStudentRequest> students;
            if (dataFile.toString().toLowerCase().endsWith(".csv")) {
                students = parseCsvWithImages(dataFile, extractDir);
            } else if (dataFile.toString().toLowerCase().endsWith(".xlsx")) {
                students = parseXlsxWithImages(dataFile, extractDir);
            } else {
                throw new IllegalArgumentException("Unsupported file format");
            }

            List<StudentDTO> importedStudents = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (CreateStudentRequest studentRequest : students) {
                try {
                    StudentFaceProcessingResult result = studentService.createStudent(studentRequest);
                    importedStudents.add(result.getStudent());
                    if (result.getFaceProcessingSummary() != null && result.getFaceProcessingSummary().hasFailures()) {
                        logger.warn("Imported student {} {} with {} rejected face image(s)",
                                studentRequest.getFirstName(),
                                studentRequest.getLastName(),
                                result.getFaceProcessingSummary().getRejectedCount());
                    }
                } catch (Exception e) {
                    String errorMsg = String.format("Failed to import %s %s: %s",
                            studentRequest.getFirstName(),
                            studentRequest.getLastName(),
                            e.getMessage());
                    logger.error(errorMsg);
                    errors.add(errorMsg);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Successfully imported %d out of %d students",
                    importedStudents.size(), students.size()));
            response.put("imported", importedStudents.size());
            response.put("total", students.size());
            response.put("students", importedStudents);
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }

            return response;

        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void extractZipFile(InputStream zipInputStream, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = destDir.resolve(entry.getName()).normalize();

                if (!filePath.startsWith(destDir)) {
                    throw new IOException("Invalid ZIP entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());

                    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private Path findSingleDataFile(Path extractDir) throws IOException {
        List<Path> csvFiles = Files.walk(extractDir, 3)
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String pathStr = p.toString().toLowerCase();
                    return (pathStr.endsWith(".csv") || pathStr.endsWith(".xlsx"))
                            && !pathStr.contains("__MACOSX")
                            && !pathStr.contains("/.")
                            && !p.getFileName().toString().startsWith(".");
                })
                .collect(Collectors.toList());

        if (csvFiles.isEmpty()) {
            throw new IllegalArgumentException("ZIP must contain at least one CSV or XLSX file");
        }

        if (csvFiles.size() > 1) {
            logger.error("Found {} CSV files:", csvFiles.size());
            csvFiles.forEach(f -> logger.error("  - {}", f));

            throw new IllegalArgumentException(
                    String.format("ZIP must contain exactly 1 CSV or XLSX file, found %d", csvFiles.size()));
        }

        return csvFiles.get(0);
    }

    private List<CreateStudentRequest> parseCsvWithImages(Path csvFile, Path extractDir)
            throws IOException {

        List<CreateStudentRequest> students = new ArrayList<>();
        List<String> lines = Files.readAllLines(csvFile);

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty())
                continue;

            String[] columns = parseCSVLine(line);
            if (columns.length < 3) {
                logger.warn("Skipping invalid CSV line {}: {}", i + 1, line);
                continue;
            }

            String firstName = columns[0].trim();
            String lastName = columns[1].trim();
            String email = columns[2].trim();

            logger.info("Parsed CSV - Name: {} {}, Email from CSV: {}", firstName, lastName, email);

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                logger.warn("Skipping line {} with empty fields", i + 1);
                continue;
            }

            String folderName = firstName + " " + lastName;

            validateStudentFolder(extractDir, folderName, i + 1);

            List<String> imageBase64List = findAndProcessStudentImages(extractDir, folderName);

            CreateStudentRequest request = new CreateStudentRequest();
            request.setFirstName(firstName);
            request.setLastName(lastName);
            request.setEmail(email);
            request.setFaceImages(imageBase64List);

            students.add(request);
        }

        if (students.isEmpty()) {
            throw new IllegalArgumentException("No valid student records found in CSV");
        }

        return students;
    }

    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());

        return fields.toArray(new String[0]);
    }

    private List<CreateStudentRequest> parseXlsxWithImages(Path xlsxFile, Path extractDir)
            throws IOException {

        List<CreateStudentRequest> students = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(xlsxFile.toFile());
                Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                Cell firstNameCell = row.getCell(0);
                Cell lastNameCell = row.getCell(1);
                Cell emailCell = row.getCell(2);

                if (firstNameCell == null || lastNameCell == null || emailCell == null) {
                    logger.warn("Skipping row {} with empty cells", i + 1);
                    continue;
                }

                String firstName = getCellValueAsString(firstNameCell).trim();
                String lastName = getCellValueAsString(lastNameCell).trim();
                String email = getCellValueAsString(emailCell).trim();

                if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                    logger.warn("Skipping row {} with empty fields", i + 1);
                    continue;
                }

                String folderName = firstName + " " + lastName;
                validateStudentFolder(extractDir, folderName, i + 1);
                List<String> imageBase64List = findAndProcessStudentImages(extractDir, folderName);

                CreateStudentRequest request = new CreateStudentRequest();
                request.setFirstName(firstName);
                request.setLastName(lastName);
                request.setEmail(email);
                request.setFaceImages(imageBase64List);

                students.add(request);
            }
        }

        if (students.isEmpty()) {
            throw new IllegalArgumentException("No valid student records found in XLSX");
        }

        return students;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case CellType.STRING:
                return cell.getStringCellValue();
            case CellType.NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return "";
        }
    }

    private List<String> findAndProcessStudentImages(Path extractDir, String folderName)
            throws IOException {

        List<String> imageBase64List = new ArrayList<>();

        Path studentFolder = findStudentFolder(extractDir, folderName);

        if (studentFolder != null && Files.exists(studentFolder) && Files.isDirectory(studentFolder)) {
            List<Path> imageFiles = Files.list(studentFolder)
                    .filter(Files::isRegularFile)
                    .filter(p -> isImageFile(p.getFileName().toString()))
                    .sorted()
                    .collect(Collectors.toList());

            // Validate: max 8 images
            if (imageFiles.size() > 8) {
                throw new IllegalArgumentException(
                        String.format("Student '%s' has %d images (max 8 allowed)",
                                folderName, imageFiles.size()));
            }

            // Convert images to Base64
            for (Path imageFile : imageFiles) {
                try {
                    byte[] imageBytes = Files.readAllBytes(imageFile);
                    String base64 = Base64.getEncoder().encodeToString(imageBytes);
                    imageBase64List.add(base64);
                } catch (IOException e) {
                    logger.error("Failed to read image file: {}", imageFile, e);
                }
            }
        }

        return imageBase64List;
    }

    private Path findStudentFolder(Path extractDir, String folderName) throws IOException {
        logger.info("Searching for folder: '{}'", folderName);

        try (var stream = Files.walk(extractDir, 3)) {
            List<Path> matchingFolders = stream
                    .filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().startsWith("__MACOSX"))
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(folderName))
                    .collect(Collectors.toList());

            if (matchingFolders.isEmpty()) {
                logger.warn("No folder found for: '{}'", folderName);
                return null;
            }

            if (matchingFolders.size() > 1) {
                logger.warn("Multiple folders found for '{}': {}", folderName, matchingFolders);
            }

            Path found = matchingFolders.get(0);
            logger.info("Found folder: {}", found);
            return found;
        }
    }

    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".png") ||
                lower.endsWith(".gif") ||
                lower.endsWith(".bmp") ||
                lower.endsWith(".webp");
    }

    private void deleteDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                Files.walk(directory)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logger.warn("Failed to delete: {}", path);
                            }
                        });
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup directory: {}", directory, e);
        }
    }

    private void validateStudentFolder(Path extractDir, String folderName, int lineNumber)
            throws IOException {

        Path studentFolder = findStudentFolder(extractDir, folderName);

        if (studentFolder == null || !Files.exists(studentFolder)) {
            throw new IllegalArgumentException(
                    String.format("CSV line %d: No folder found for student '%s'", lineNumber, folderName));
        }

        if (!Files.isDirectory(studentFolder)) {
            throw new IllegalArgumentException(
                    String.format("CSV line %d: '%s' is not a folder", lineNumber, folderName));
        }

        List<Path> imageFiles = Files.list(studentFolder)
                .filter(Files::isRegularFile)
                .filter(p -> isImageFile(p.getFileName().toString()))
                .collect(Collectors.toList());

        if (imageFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("CSV line %d: Folder '%s' contains no valid images", lineNumber, folderName));
        }

        if (imageFiles.size() > 8) {
            throw new IllegalArgumentException(
                    String.format("CSV line %d: Folder '%s' has %d images (max 8 allowed)",
                            lineNumber, folderName, imageFiles.size()));
        }
    }
}
