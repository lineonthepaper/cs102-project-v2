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
import com.smartattendance.dto.response.user.StudentFaceProcessingResult;
import com.smartattendance.dto.response.user.StudentImportResultDTO;

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

            List<String> warnings = new ArrayList<>();
            List<CreateStudentRequest> students;
            if (dataFile.toString().toLowerCase().endsWith(".csv")) {
                students = parseCsvWithImages(dataFile, extractDir, warnings);
            } else if (dataFile.toString().toLowerCase().endsWith(".xlsx")) {
                students = parseXlsxWithImages(dataFile, extractDir, warnings);
            } else {
                throw new IllegalArgumentException("Unsupported file format");
            }

            List<StudentImportResultDTO> importResults = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (CreateStudentRequest studentRequest : students) {
                try {
                    StudentFaceProcessingResult result = studentService.createStudent(studentRequest);
                    String fullName = result.getStudent().getFirstName() + " " + result.getStudent().getLastName();
                    importResults.add(StudentImportResultDTO.success(
                            fullName,
                            result.getStudent().getEmail(),
                    result.getStudent(),
                    result.getFaceProcessingSummary()));
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
                    String fullName = studentRequest.getFirstName() + " " + studentRequest.getLastName();
                    importResults.add(StudentImportResultDTO.failure(
                            fullName,
                            studentRequest.getEmail(),
                            e.getMessage()));
                }
            }

            long importedCount = importResults.stream()
                    .filter(StudentImportResultDTO::isSuccess)
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Successfully imported %d out of %d students",
                    importedCount, students.size()));
            response.put("imported", importedCount);
            response.put("total", students.size());
            response.put("results", importResults);
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }
            if (!warnings.isEmpty()) {
                response.put("warnings", warnings);
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
                    String fileName = p.getFileName().toString();
                    return (pathStr.endsWith(".csv") || pathStr.endsWith(".xlsx"))
                            && !pathStr.contains("__macosx")
                            && !pathStr.contains("/.")
                            && !fileName.startsWith(".")
                            && !fileName.startsWith("~$");
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

    private List<CreateStudentRequest> parseCsvWithImages(Path csvFile, Path extractDir, List<String> warnings)
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

            List<String> imageBase64List = findAndProcessStudentImages(extractDir, folderName, warnings, i + 1);

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

    private List<CreateStudentRequest> parseXlsxWithImages(Path xlsxFile, Path extractDir, List<String> warnings)
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
                List<String> imageBase64List = findAndProcessStudentImages(extractDir, folderName, warnings, i + 1);

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

    private List<String> findAndProcessStudentImages(Path extractDir, String folderName, List<String> warnings,
            int rowNumber)
            throws IOException {

        List<String> imageBase64List = new ArrayList<>();

        Path studentFolder = findStudentFolder(extractDir, folderName);

        if (studentFolder != null && Files.exists(studentFolder) && Files.isDirectory(studentFolder)) {
            List<Path> imageFiles = Files.list(studentFolder)
                    .filter(Files::isRegularFile)
                    .filter(p -> isImageFile(p.getFileName().toString()))
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return !name.startsWith(".")
                                && !name.startsWith("~$")
                                && !name.startsWith("._");
                    })
                    .sorted()
                    .collect(Collectors.toList());

            // Validate: max 8 images
            if (imageFiles.size() > 8) {
                warnings.add(String.format(
                        "Row %d '%s': %d images found. Using first 8 images and skipping the rest.",
                        rowNumber, folderName, imageFiles.size()));
                imageFiles = imageFiles.subList(0, 8);
            }

            // Convert images to Base64
            for (Path imageFile : imageFiles) {
                try {
                    byte[] imageBytes = Files.readAllBytes(imageFile);

                    // Skip images smaller than 1 KB (likely invalid)
                    if (imageBytes.length < 1024) {
                        warnings.add(String.format(
                                "Row %d '%s': Image '%s' is too small and was skipped.",
                                rowNumber, folderName, imageFile.getFileName()));
                        continue;
                    }

                    String base64 = Base64.getEncoder().encodeToString(imageBytes);
                    imageBase64List.add(base64);
                } catch (IOException e) {
                    logger.error("Failed to read image file: {}", imageFile, e);
                    warnings.add(String.format(
                            "Row %d '%s': Error reading image '%s'.",
                            rowNumber, folderName, imageFile.getFileName()));
                }
            }

            if (imageBase64List.isEmpty()) {
                warnings.add(String.format(
                        "Row %d '%s': No valid images found. Student will be created without face data.",
                        rowNumber, folderName));
            }
        } else {
            warnings.add(String.format(
                    "Row %d '%s': No image folder found. Student will be created without face data.",
                    rowNumber, folderName));
        }

        return imageBase64List;
    }

    private Path findStudentFolder(Path extractDir, String folderName) throws IOException {
        logger.info("Searching for folder: '{}'", folderName);

        try (var stream = Files.walk(extractDir, 3)) {
            List<Path> matchingFolders = stream
                    .filter(Files::isDirectory)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return !name.startsWith(".")
                                && !name.startsWith("~$")
                                && name.equalsIgnoreCase(folderName);
                    })
                    .collect(Collectors.toList());

            if (matchingFolders.isEmpty()) {
                logger.warn("No folder found for: '{}'", folderName);
                return null;
            }

            List<Path> preferredFolders = matchingFolders.stream()
                    .filter(p -> !p.toString().toLowerCase().contains("__macosx"))
                    .collect(Collectors.toList());

            List<Path> candidates = preferredFolders.isEmpty() ? matchingFolders : preferredFolders;

            if (candidates.size() > 1) {
                logger.warn("Multiple folders found for '{}': {}", folderName, candidates);
            }

            Path found = candidates.get(0);
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

}

