package com.smartattendance.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.smartattendance.entity.ComparisonResult;

import com.smartattendance.dto.response.attendance.BoundingBoxDTO;
import com.smartattendance.dto.response.attendance.FaceScanResponseDTO;
import com.smartattendance.dto.response.attendance.FaceDetectionDTO;
import com.smartattendance.dto.response.attendance.RecognizedStudentDTO;
import com.smartattendance.entity.AttendanceSession;
import com.smartattendance.entity.AttendanceStatus;
import com.smartattendance.entity.AttendanceRecord;
import com.smartattendance.entity.SectionEnrollment;
import com.smartattendance.entity.User;
import com.smartattendance.repository.AttendanceRecordRepository;
import com.smartattendance.repository.AttendanceSessionRepository;
import com.smartattendance.repository.SectionEnrollmentRepository;
import com.smartattendance.repository.UserRepository;

@Component
public class SessionRecognitionManager {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Singapore");

    private final AttendanceSessionRepository sessionRepository;
    private final SectionEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AttendanceRecordRepository recordRepository;
    private final FaceRecognitionService faceRecognitionService;
    @Value("${face.voting.windowSize:10}")
    private int votingWindowSize;

    @Value("${face.voting.requiredVotes:6}")
    private int votingRequiredVotes;

    @Value("${face.voting.minSimilarity:0.92}")
    private double votingMinSimilarity;

    private final Map<Long, SessionRecognitionContext> sessionCache = new ConcurrentHashMap<>();

    public SessionRecognitionManager(AttendanceSessionRepository sessionRepository,
            SectionEnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            AttendanceRecordRepository recordRepository,
            FaceRecognitionService faceRecognitionService) {
        this.sessionRepository = sessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.faceRecognitionService = faceRecognitionService;
    }

    @Transactional
    public FaceScanResponseDTO scanFace(Long sessionId, byte[] imageBytes) {
        SessionRecognitionContext context = sessionCache.computeIfAbsent(sessionId, this::loadContext);
        return context.match(imageBytes);
    }

    public void registerAttendance(Long sessionId, String userId, AttendanceStatus status) {
        sessionCache.computeIfPresent(sessionId, (id, context) -> {
            context.updateStatus(userId, status);
            return context;
        });
    }

    public void invalidateSession(Long sessionId) {
        sessionCache.remove(sessionId);
    }

    private SessionRecognitionContext loadContext(Long sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance session not found: " + sessionId));

        List<SectionEnrollment> enrollments = enrollmentRepository
                .findBySectionIdAndIsActive(session.getSectionId(), Boolean.TRUE);

        double minCosSimilarity = Math.max(-1.0, Math.min(1.0, votingMinSimilarity * 2.0 - 1.0));

        if (enrollments.isEmpty()) {
            return new SessionRecognitionContext(session, Collections.emptyMap(), Collections.emptyMap(),
                    faceRecognitionService, votingWindowSize, votingRequiredVotes,
                    minCosSimilarity);
        }

        List<String> studentIds = enrollments.stream()
                .map(SectionEnrollment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        List<User> students = userRepository.findAllById(studentIds);

        Map<String, AttendanceStatus> existingStatuses = recordRepository.findBySessionId(session.getId()).stream()
                .collect(Collectors.toConcurrentMap(AttendanceRecord::getUserId,
                        record -> record.getStatus() != null ? record.getStatus() : AttendanceStatus.ABSENT,
                        (a, b) -> b));

        Map<String, StudentProfile> profiles = new ConcurrentHashMap<>();

        System.out.println("[DEBUG] ===== Loading Session Context =====");
        System.out.println("[DEBUG] Session ID: " + session.getId());
        System.out.println("[DEBUG] Total students enrolled: " + students.size());

        for (User student : students) {
            System.out.println("[DEBUG] Processing student: " + student.getId() + " - " +
                    student.getFirstName() + " " + student.getLastName());

            AttendanceStatus status = existingStatuses.get(student.getId());
            if (status != null && status.isPresent()) {
                System.out.println("[DEBUG]   Student already marked as: " + status + ", skipping");
            }

            List<float[]> embeddings = new ArrayList<>();
            // Prefer stored face_profiles embeddings if available
            try {
                java.util.List<com.smartattendance.entity.FaceProfile> profilesList = student.getFaceProfiles();
                if (profilesList != null && !profilesList.isEmpty()) {
                    System.out.println("[DEBUG]   Using stored face_profiles embeddings: " + profilesList.size());
                    for (com.smartattendance.entity.FaceProfile fp : profilesList) {
                        if (fp != null && fp.getEmbedding() != null && fp.getEmbedding().length > 0) {
                            embeddings.add(fp.getEmbedding());
                        }
                    }
                }
            } catch (Exception ignore) {
            }

            // Fallback: compute from legacy face_images if no profiles found
            if (embeddings.isEmpty()) {
                try {
                    java.util.List<String> images = student.getFaceImages();
                    if (images != null && !images.isEmpty()) {
                        System.out.println("[DEBUG]   Computing embeddings from legacy face_images: " + images.size());
                        for (int i = 0; i < images.size(); i++) {
                            String b64 = images.get(i);
                            byte[] bytes = decodeBase64(b64);
                            if (bytes == null)
                                continue;
                            faceRecognitionService.computeEmbedding(bytes).ifPresent(embeddings::add);
                        }
                    }
                } catch (Exception ignore) {
                }
            }

            if (!embeddings.isEmpty()) {
                System.out.println(
                        "[DEBUG]   Storing " + embeddings.size() + " embeddings for student: " + student.getId());
                profiles.put(student.getId(), new StudentProfile(student, embeddings));
            } else {
                System.out.println("[DEBUG]   WARNING: No valid embeddings computed for this student!");
            }
        }

        System.out.println("[DEBUG] Total profiles loaded: " + profiles.size());
        System.out.println("[DEBUG] =====================================");

        // Remove statuses for students we didn't keep (to keep maps aligned)
        Set<String> retainedIds = profiles.keySet();
        existingStatuses.keySet().retainAll(retainedIds);

        return new SessionRecognitionContext(session, profiles, existingStatuses, faceRecognitionService,
                votingWindowSize, votingRequiredVotes, minCosSimilarity);
    }

    private byte[] decodeBase64(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String sanitized = value.contains(",") ? value.substring(value.indexOf(',') + 1) : value;
        try {
            return Base64.getDecoder().decode(sanitized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static final class SimilarityCalibrator {

        private final float impostorMean;
        private final float logisticCenter;
        private final float logisticScale;
        private final float marginScale;

        private SimilarityCalibrator(float impostorMean,
                float logisticCenter,
                float logisticScale,
                float marginScale) {
            this.impostorMean = impostorMean;
            this.logisticCenter = logisticCenter;
            this.logisticScale = logisticScale;
            this.marginScale = marginScale;
        }

        static SimilarityCalibrator defaultCalibrator() {
            // Calibrated to keep displayed confidence closer to observed accuracy (~70%)
            return new SimilarityCalibrator(0.0f, 0.91f, 0.07f, 0.12f);
        }

        static SimilarityCalibrator compute(Map<String, List<float[]>> embeddingIndex) {
            if (embeddingIndex == null || embeddingIndex.isEmpty()) {
                return defaultCalibrator();
            }

            List<Float> positives = new ArrayList<>();
            List<Float> negatives = new ArrayList<>();
            List<Map.Entry<String, List<float[]>>> entries = new ArrayList<>(embeddingIndex.entrySet());

            for (Map.Entry<String, List<float[]>> entry : entries) {
                List<float[]> vectors = entry.getValue();
                if (vectors == null || vectors.size() < 2) {
                    continue;
                }
                for (int i = 0; i < vectors.size(); i++) {
                    float[] vi = vectors.get(i);
                    if (vi == null)
                        continue;
                    for (int j = i + 1; j < vectors.size(); j++) {
                        float[] vj = vectors.get(j);
                        if (vj == null)
                            continue;
                        positives.add(com.smartattendance.util.opencv.FaceRecognitionUtils.cosine(vi, vj));
                    }
                }
            }

            for (int i = 0; i < entries.size(); i++) {
                List<float[]> left = entries.get(i).getValue();
                if (left == null) {
                    continue;
                }
                for (int j = i + 1; j < entries.size(); j++) {
                    List<float[]> right = entries.get(j).getValue();
                    if (right == null) {
                        continue;
                    }
                    for (float[] a : left) {
                        if (a == null)
                            continue;
                        for (float[] b : right) {
                            if (b == null)
                                continue;
                            negatives.add(com.smartattendance.util.opencv.FaceRecognitionUtils.cosine(a, b));
                        }
                    }
                }
            }

            if (positives.isEmpty() || negatives.isEmpty()) {
                return defaultCalibrator();
            }

            float posMean = mean(positives);
            float negMean = mean(negatives);
            float posStd = std(positives, posMean);
            float negStd = std(negatives, negMean);

            float center = Math.max(0.88f, Math.min(0.97f, (posMean + negMean) / 2.0f));
            float spread = Math.max(0.06f, posMean - negMean);
            float scale = Math.max(0.07f, spread / 3.5f);
            float marginScale = Math.max(0.12f,
                    Math.min(0.25f, scale * 1.5f + 0.5f * (posStd + negStd)));

            System.out.println(String.format("[CALIBRATION] impostor mean=%.3f (±%.3f), genuine mean=%.3f (±%.3f)",
                    negMean, negStd, posMean, posStd));
            System.out.println(String.format("[CALIBRATION] logistic center=%.3f, scale=%.3f, marginScale=%.3f",
                    center, scale, marginScale));

            return new SimilarityCalibrator(negMean, center, scale, marginScale);
        }

        float confidence(float cosine, float runnerUpCosine) {
            float scale = logisticScale < 0.02f ? 0.02f : logisticScale;
            float logistic = clamp01((float) (1.0 / (1.0 + Math.exp(-(cosine - logisticCenter) / scale))));
            if (Float.isNaN(logistic)) {
                logistic = 0.5f;
            }

            float marginConfidence = 1.0f;
            if (!Float.isNaN(runnerUpCosine) && runnerUpCosine > -1.0f) {
                float margin = Math.max(0.0f, cosine - runnerUpCosine);
                marginConfidence = clamp01(margin / marginScale);
            }

            float blended = 0.75f * logistic + 0.25f * marginConfidence;
            return clamp01(blended);
        }

        float getImpostorMean() {
            return impostorMean;
        }

        private static float mean(List<Float> values) {
            float sum = 0.0f;
            for (float v : values) {
                sum += v;
            }
            return sum / values.size();
        }

        private static float std(List<Float> values, float mean) {
            if (values.size() <= 1) {
                return 0.0f;
            }
            float accum = 0.0f;
            for (float v : values) {
                float diff = v - mean;
                accum += diff * diff;
            }
            return (float) Math.sqrt(accum / (values.size() - 1));
        }

        private float clamp01(float value) {
            if (Float.isNaN(value)) {
                return 0.0f;
            }
            return Math.max(0.0f, Math.min(1.0f, value));
        }
    }

    private static final class SessionRecognitionContext {

        private final AttendanceSession session;
        private final Map<String, StudentProfile> profiles;
        private final Map<String, AttendanceStatus> statuses;
        private final FaceRecognitionService recognitionService;
        private final Map<String, Integer> voteCounts = new ConcurrentHashMap<>();
        private final Map<String, Float> maxSimilarityById = new ConcurrentHashMap<>();
        private final Map<String, org.opencv.core.Rect> boundingBoxByCandidate = new ConcurrentHashMap<>();
        private final Map<String, Integer> originalWidthByCandidate = new ConcurrentHashMap<>();
        private final Map<String, Integer> originalHeightByCandidate = new ConcurrentHashMap<>();
        private final int windowSize;
        private final int requiredVotes;
        private final double minSimilarity;
        private int scans = 0;

        private volatile Map<String, List<float[]>> embeddingIndex;
        private SimilarityCalibrator calibrator;

        private SessionRecognitionContext(AttendanceSession session,
                Map<String, StudentProfile> profiles,
                Map<String, AttendanceStatus> statuses,
                FaceRecognitionService recognitionService,
                int windowSize,
                int requiredVotes,
                double minSimilarity) {
            this.session = session;
            this.profiles = profiles;
            this.statuses = new ConcurrentHashMap<>(statuses);
            this.recognitionService = recognitionService;
            this.calibrator = SimilarityCalibrator.defaultCalibrator();
            rebuildEmbeddingIndex();
            this.windowSize = Math.max(1, windowSize);
            this.requiredVotes = Math.max(1, requiredVotes);
            this.minSimilarity = minSimilarity;
        }

        private void rebuildEmbeddingIndex() {
            this.embeddingIndex = profiles.values().stream()
                    .collect(Collectors.toMap(StudentProfile::getUserId, StudentProfile::getEmbeddings));
            this.calibrator = SimilarityCalibrator.compute(this.embeddingIndex);
        }

        FaceScanResponseDTO match(byte[] imageBytes) {
            if (embeddingIndex.isEmpty()) {
                System.out.println("\n[VOTING] No enrolled students with face data available");
                return FaceScanResponseDTOBuilder.noMatch("No enrolled students with face data available");
            }

            System.out.println("\n[VOTING] ============================================");
            System.out.println("[VOTING] Frame " + (scans + 1) + "/" + windowSize);
            System.out.println("[VOTING] Candidates: " + embeddingIndex.size() + " students");

            System.out.println("[VOTING] --- Comparison Pool ---");
            for (Map.Entry<String, List<float[]>> entry : embeddingIndex.entrySet()) {
                String studentId = entry.getKey();
                List<float[]> embeddings = entry.getValue();
                StudentProfile profile = profiles.get(studentId);
                String name = profile != null ? profile.getEmail() : studentId;
                System.out.println("[VOTING]   " + name + " (" + studentId + "): " +
                        embeddings.size() + " embeddings, " +
                        "dimensions: " + (embeddings.isEmpty() ? "N/A" : embeddings.get(0).length));
            }
            System.out.println("[VOTING] --------------------------");

            // Get all detected faces with embeddings
            List<FaceRecognitionService.EmbeddingWithBbox> allEmbeddings = recognitionService
                    .computeAllEmbeddingsWithBbox(imageBytes);

            if (!allEmbeddings.isEmpty()) {
                System.out.println("[VOTING] Detected " + allEmbeddings.size() + " face(s) in this frame");

                // Process each detected face
                for (FaceRecognitionService.EmbeddingWithBbox emb : allEmbeddings) {
                    // Get best match from computed embedding
                    ComparisonResult result = com.smartattendance.util.opencv.FaceRecognitionUtils
                            .findTopCandidate(emb.getEmbedding(), embeddingIndex);

                    if (result != null && result.getFaceName() != null) {
                        String candidateId = result.getFaceName();
                        float topSim = result.getSimilarity();

                        StudentProfile profile = profiles.get(candidateId);
                        String candidateName = profile != null ? profile.getEmail() : candidateId;

                        System.out.println("[VOTING] Top match: " + candidateName + " cosine=" +
                                String.format("%.3f", topSim));

                        // Only count as vote if above minimum threshold
                        if (topSim >= (float) this.minSimilarity) {
                            voteCounts.merge(candidateId, 1, Integer::sum);
                            maxSimilarityById.merge(candidateId, topSim, (a, b) -> a != null && a > b ? a : b);

                            // Store bounding box only for votes that count
                            boundingBoxByCandidate.put(candidateId, emb.getBoundingBox());
                            originalWidthByCandidate.put(candidateId, emb.getOriginalWidth());
                            originalHeightByCandidate.put(candidateId, emb.getOriginalHeight());

                            System.out.println("[VOTING] Vote counted! Total votes for " + candidateName + ": " +
                                    voteCounts.get(candidateId) + " (threshold=" +
                                    String.format("%.3f", this.minSimilarity) + ")");
                        } else {
                            System.out.println("[VOTING] Vote NOT counted (cosine " +
                                    String.format("%.3f", topSim) + " below threshold " +
                                    String.format("%.3f", this.minSimilarity) + ")");
                        }
                    }
                }
                scans++;
            } else {
                System.out.println("[VOTING] No face detected in this frame");
                scans++;
            }

            // Show current vote standings
            if (!voteCounts.isEmpty()) {
                System.out.println("[VOTING] Current standings:");
                voteCounts.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                        .forEach(e -> {
                            StudentProfile p = profiles.get(e.getKey());
                            String name = p != null ? p.getEmail() : e.getKey();
                            float sim = maxSimilarityById.getOrDefault(e.getKey(), 0.0f);
                            System.out.println("[VOTING]   " + name + ": " + e.getValue() +
                                    " votes (best cosine=" + String.format("%.3f", sim) + ")");
                        });
            }

            // Check if window is complete
            if (scans >= windowSize) {
                System.out.println("\n[VOTING] Window complete! Making decision...");

                if (voteCounts.isEmpty()) {
                    System.out.println("[VOTING] DECISION: No valid votes collected");
                    System.out.println("[VOTING] ============================================\n");
                    resetVoting();
                    return FaceScanResponseDTOBuilder.noMatch("No valid frames detected; please try again");
                }

                // Find ALL candidates with enough votes
                List<Map.Entry<String, Integer>> winners = voteCounts.entrySet().stream()
                        .filter(e -> e.getValue() >= requiredVotes)
                        .sorted((a, b) -> {
                            int cmp = Integer.compare(b.getValue(), a.getValue());
                            if (cmp != 0)
                                return cmp;
                            float sa = maxSimilarityById.getOrDefault(a.getKey(), 0.0f);
                            float sb = maxSimilarityById.getOrDefault(b.getKey(), 0.0f);
                            return Float.compare(sb, sa);
                        })
                        .collect(Collectors.toList());

                if (winners.isEmpty()) {
                    System.out.println("[VOTING] DECISION: No candidates met threshold (" + requiredVotes + " votes)");
                    System.out.println("[VOTING] ============================================\n");
                    resetVoting();
                    return FaceScanResponseDTOBuilder.noMatch("No clear match; please try again");
                }

                ZonedDateTime now = ZonedDateTime.now(DEFAULT_ZONE);
                AttendanceStatus recommendedStatus = determineAttendanceStatus(now);

                Map.Entry<String, Integer> best = winners.get(0);
                String studentId = best.getKey();
                StudentProfile profile = profiles.get(studentId);

                if (profile == null) {
                    System.out.println("[VOTING] WARNING: Profile not found for " + studentId);
                    resetVoting();
                    return FaceScanResponseDTOBuilder.noMatch("Unable to identify student profile");
                }

                float cosine = maxSimilarityById.getOrDefault(studentId, 0.0f);
                float runnerUp = getRunnerUpSimilarity(studentId);
                // SimilarityCalibrator activeCalibrator = (calibrator != null)
                // ? calibrator
                // : SimilarityCalibrator.defaultCalibrator();
                // float confidence = activeCalibrator.confidence(cosine, runnerUp);
                float confidence = cosine;

                System.out
                        .println(String.format("[VOTING] Winner %s: votes=%d cosine=%.3f runnerUp=%.3f confidence=%.3f",
                                profile.getEmail(), best.getValue(), cosine, runnerUp, confidence));

                org.opencv.core.Rect bbox = boundingBoxByCandidate.get(studentId);
                int originalWidth = originalWidthByCandidate.getOrDefault(studentId, 0);
                int originalHeight = originalHeightByCandidate.getOrDefault(studentId, 0);

                AttendanceStatus currentStatus = statuses.get(studentId);
                AttendanceStatus statusForDto = (currentStatus != null && currentStatus.isPresent())
                        ? currentStatus
                        : recommendedStatus;

                FaceScanResponseDTO dto = FaceScanResponseDTOBuilder.match(
                        profile,
                        confidence,
                        statusForDto,
                        now.toOffsetDateTime(),
                        bbox,
                        originalWidth,
                        originalHeight);
                dto.setRawSimilarity(cosine);

                float marginValue = (!Float.isNaN(runnerUp) && runnerUp > -1.0f)
                        ? (cosine - runnerUp)
                        : cosine;

                if (currentStatus != null && currentStatus.isPresent()) {
                    dto.setMatched(false);
                    dto.setAlreadyMarked(true);
                    dto.setRecommendedStatus(currentStatus.getCode());
                    dto.setMessage(String.format("Student already marked as %s (cos=%.3f).",
                            currentStatus.getCode(), cosine));
                } else {
                    dto.setMatched(true);
                    dto.setAlreadyMarked(false);
                    dto.setRecommendedStatus(recommendedStatus != null ? recommendedStatus.getCode() : null);
                    dto.setMessage(String.format("Match found (cos=%.3f, margin=%.3f). Please confirm.",
                            cosine, marginValue));
                }
                dto.setMargin(marginValue);

                System.out.println("[VOTING] ============================================\n");
                resetVoting();
                return dto;
            }

            System.out.println("[VOTING] Continue scanning... (" + scans + "/" + windowSize + " frames)");
            System.out.println("[VOTING] ============================================\n");

            if (!voteCounts.isEmpty() && !maxSimilarityById.isEmpty()) {
                return FaceScanResponseDTOBuilder.scanningWithAllDetections(
                        null,
                        profiles, maxSimilarityById, boundingBoxByCandidate,
                        originalWidthByCandidate, originalHeightByCandidate);
            }

            return FaceScanResponseDTOBuilder.noMatch("Scanning...");
        }

        private void resetVoting() {
            voteCounts.clear();
            maxSimilarityById.clear();
            boundingBoxByCandidate.clear();
            originalWidthByCandidate.clear();
            originalHeightByCandidate.clear();
            scans = 0;
        }

        private float getRunnerUpSimilarity(String excludeId) {
            float runnerUp = Float.NEGATIVE_INFINITY;
            for (Map.Entry<String, Float> entry : maxSimilarityById.entrySet()) {
                if (!entry.getKey().equals(excludeId)) {
                    if (entry.getValue() > runnerUp) {
                        runnerUp = entry.getValue();
                    }
                }
            }
            return runnerUp == Float.NEGATIVE_INFINITY ? Float.NaN : runnerUp;
        }

        private AttendanceStatus determineAttendanceStatus(ZonedDateTime now) {
            try {
                java.time.LocalDate scanDate = now.toLocalDate();
                java.time.LocalDate sessDate = session.getSessionDate();
                java.time.LocalTime sessStart = session.getScheduledStartTime();

                if (sessDate == null || sessStart == null) {
                    return AttendanceStatus.PRESENT;
                }

                if (scanDate.isBefore(sessDate)) {
                    return AttendanceStatus.PRESENT;
                } else if (scanDate.isAfter(sessDate)) {
                    return AttendanceStatus.LATE;
                } else {
                    // Same date: check if within 15 minutes of start time
                    java.time.LocalDateTime cutoff = java.time.LocalDateTime.of(sessDate, sessStart).plusMinutes(15);
                    return now.toLocalDateTime().isAfter(cutoff) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
                }
            } catch (Exception e) {
                return AttendanceStatus.PRESENT;
            }
        }

        void updateStatus(String userId, AttendanceStatus status) {
            if (userId == null) {
                return;
            }
            if (status != null) {
                statuses.put(userId, status);
            } else {
                statuses.remove(userId);
            }
        }
    }

    private static final class StudentProfile {

        private final String userId;
        private final String displayId;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final List<float[]> embeddings;

        StudentProfile(User user, List<float[]> embeddings) {
            this.userId = user.getId();
            this.displayId = user.getNormalizedStudentId();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.email = user.getEmail();
            this.embeddings = Collections.unmodifiableList(new ArrayList<>(embeddings));
        }

        String getUserId() {
            return userId;
        }

        String getDisplayId() {
            return displayId;
        }

        String getFirstName() {
            return firstName;
        }

        String getLastName() {
            return lastName;
        }

        String getEmail() {
            return email;
        }

        List<float[]> getEmbeddings() {
            return embeddings;
        }
    }

    private static final class FaceScanResponseDTOBuilder {

        static FaceScanResponseDTO noMatch(String message) {
            FaceScanResponseDTO dto = new FaceScanResponseDTO();
            dto.setMatched(false);
            dto.setMessage(message);
            dto.setAlreadyMarked(false);
            dto.setSimilarity(0.0);
            dto.setRawSimilarity(0.0);
            dto.setMargin(0.0);
            return dto;
        }

        static FaceScanResponseDTO scanningWithAllDetections(SimilarityCalibrator calibrator,
                Map<String, StudentProfile> profiles,
                Map<String, Float> maxSimilarityById,
                Map<String, org.opencv.core.Rect> boundingBoxByCandidate,
                Map<String, Integer> originalWidthByCandidate,
                Map<String, Integer> originalHeightByCandidate) {
            List<FaceDetectionDTO> detections = new ArrayList<>();

            // Create detection entry for each candidate that has votes
            for (Map.Entry<String, Float> entry : maxSimilarityById.entrySet()) {
                String candidateId = entry.getKey();
                double rawSimilarity = entry.getValue();

                org.opencv.core.Rect bbox = boundingBoxByCandidate.get(candidateId);
                if (bbox == null)
                    continue;

                int width = originalWidthByCandidate.getOrDefault(candidateId, 0);
                int height = originalHeightByCandidate.getOrDefault(candidateId, 0);

                BoundingBoxDTO bboxDTO = new BoundingBoxDTO(
                        bbox.x, bbox.y, bbox.width, bbox.height, width, height);

                StudentProfile profile = profiles.get(candidateId);
                if (profile == null)
                    continue;

                RecognizedStudentDTO studentDTO = new RecognizedStudentDTO();
                studentDTO.setId(profile.getUserId());
                studentDTO.setDisplayId(profile.getDisplayId());
                studentDTO.setFirstName(profile.getFirstName());
                studentDTO.setLastName(profile.getLastName());
                studentDTO.setEmail(profile.getEmail());

                detections.add(new FaceDetectionDTO(bboxDTO, studentDTO, rawSimilarity, rawSimilarity));
            }

            FaceScanResponseDTO dto = new FaceScanResponseDTO();
            dto.setMatched(false);
            dto.setAlreadyMarked(false);
            dto.setMessage("Scanning...");
            dto.setAllDetections(detections);
            dto.setMargin(0.0);

            // Still set primary bounding box and student (the leader) for backwards
            // compatibility
            if (!detections.isEmpty()) {
                FaceDetectionDTO leader = detections.stream()
                        .max((a, b) -> Double.compare(a.getRawSimilarity(), b.getRawSimilarity()))
                        .orElse(detections.get(0));
                dto.setBoundingBox(leader.getBoundingBox());
                dto.setStudent(leader.getStudent());
                dto.setSimilarity(leader.getSimilarity());
                dto.setRawSimilarity(leader.getRawSimilarity());
                dto.setMargin(0.0);
            }

            return dto;
        }

        static FaceScanResponseDTO match(StudentProfile profile,
                double similarity,
                AttendanceStatus recommendedStatus,
                OffsetDateTime checkInTime) {
            return match(profile, similarity, recommendedStatus, checkInTime, null, 0, 0);
        }

        static FaceScanResponseDTO match(StudentProfile profile,
                double similarity,
                AttendanceStatus recommendedStatus,
                OffsetDateTime checkInTime,
                org.opencv.core.Rect boundingBox,
                int originalWidth,
                int originalHeight) {
            FaceScanResponseDTO dto = new FaceScanResponseDTO();
            dto.setMatched(true);
            dto.setAlreadyMarked(false);
            dto.setSimilarity(similarity);
            dto.setRawSimilarity(similarity);
            dto.setMargin(0.0);
            dto.setRecommendedStatus(recommendedStatus != null ? recommendedStatus.getCode() : null);
            dto.setRecommendedCheckInTime(checkInTime);

            if (boundingBox != null) {
                BoundingBoxDTO bboxDTO = new BoundingBoxDTO(
                        boundingBox.x,
                        boundingBox.y,
                        boundingBox.width,
                        boundingBox.height,
                        originalWidth,
                        originalHeight);
                dto.setBoundingBox(bboxDTO);
            }

            RecognizedStudentDTO studentDTO = new RecognizedStudentDTO();
            studentDTO.setId(profile.getUserId());
            studentDTO.setDisplayId(profile.getDisplayId());
            studentDTO.setFirstName(profile.getFirstName());
            studentDTO.setLastName(profile.getLastName());
            studentDTO.setEmail(profile.getEmail());
            dto.setStudent(studentDTO);

            return dto;
        }
    }

    public void invalidateSessionsForUser(String userId) {
        // Simple approach: clear entire cache
        int sizeBefore = sessionCache.size();
        sessionCache.clear();
        System.out.println(
                "[CACHE] Cleared " + sizeBefore + " session cache(s) after face data change for user: " + userId);
    }

    public void warmCache(Long sessionId) {
        // Force cache load without scanning
        sessionCache.computeIfAbsent(sessionId, this::loadContext);
        System.out.println("[CACHE] Pre-warmed cache for session: " + sessionId);
    }
}
