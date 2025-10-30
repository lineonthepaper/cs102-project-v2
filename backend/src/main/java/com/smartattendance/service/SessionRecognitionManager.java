package com.smartattendance.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.smartattendance.entity.ComparisonResult;

import com.smartattendance.dto.response.attendance.FaceScanResponseDTO;
import com.smartattendance.dto.response.attendance.RecognizedStudentDTO;
import com.smartattendance.entity.AttendanceRecord;
import com.smartattendance.entity.AttendanceSession;
import com.smartattendance.entity.AttendanceStatus;
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

    @Value("${face.voting.minMargin:0.01}")
    private double votingMinMargin;

    @Value("${face.voting.mode:best}")
    private String votingMode;


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

    @Transactional(readOnly = true)
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

        if (enrollments.isEmpty()) {
            return new SessionRecognitionContext(session, Collections.emptyMap(), Collections.emptyMap(),
                    faceRecognitionService, votingWindowSize, votingRequiredVotes, votingMinSimilarity, votingMinMargin,
                    "best".equalsIgnoreCase((votingMode != null ? votingMode : "").trim()));
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
                // Already marked present/late – skip to avoid re-scanning
                continue;
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
            } catch (Exception ignore) {}

            // Fallback: compute from legacy face_images if no profiles found
            if (embeddings.isEmpty()) {
                try {
                    java.util.List<String> images = student.getFaceImages();
                    if (images != null && !images.isEmpty()) {
                        System.out.println("[DEBUG]   Computing embeddings from legacy face_images: " + images.size());
                        for (int i = 0; i < images.size(); i++) {
                            String b64 = images.get(i);
                            byte[] bytes = decodeBase64(b64);
                            if (bytes == null) continue;
                            faceRecognitionService.computeEmbedding(bytes).ifPresent(embeddings::add);
                        }
                    }
                } catch (Exception ignore) {}
            }

            if (!embeddings.isEmpty()) {
                System.out.println("[DEBUG]   Storing " + embeddings.size() + " embeddings for student: " + student.getId());
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
                votingWindowSize, votingRequiredVotes, votingMinSimilarity, votingMinMargin,
                "best".equalsIgnoreCase((votingMode != null ? votingMode : "").trim()));
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

    private static final class SessionRecognitionContext {

        private final AttendanceSession session;
        private final Map<String, StudentProfile> profiles;
        private final Map<String, AttendanceStatus> statuses;
        private final FaceRecognitionService recognitionService;
        private final Map<String, Integer> voteCounts = new ConcurrentHashMap<>();
        private final Map<String, Float> maxSimilarityById = new ConcurrentHashMap<>();
        private final int windowSize;
        private final int requiredVotes;
        private final double minSimilarity;
        private int scans = 0;

        private volatile Map<String, List<float[]>> embeddingIndex;

        private SessionRecognitionContext(AttendanceSession session,
                                          Map<String, StudentProfile> profiles,
                                          Map<String, AttendanceStatus> statuses,
                                          FaceRecognitionService recognitionService,
                                          int windowSize,
                                          int requiredVotes,
                                          double minSimilarity,
                                          double minMargin,
                                          boolean bestMode) {
            this.session = session;
            this.profiles = profiles;
            this.statuses = new ConcurrentHashMap<>(statuses);
            this.recognitionService = recognitionService;
            rebuildEmbeddingIndex();
            this.windowSize = Math.max(1, windowSize);
            this.requiredVotes = Math.max(1, requiredVotes);
            this.minSimilarity = minSimilarity;
            // minMargin and bestMode are not used in simplified voting logic
        }

        private void rebuildEmbeddingIndex() {
            this.embeddingIndex = profiles.values().stream()
                    .collect(Collectors.toMap(StudentProfile::getUserId, StudentProfile::getEmbeddings));
        }

        FaceScanResponseDTO match(byte[] imageBytes) {
            if (embeddingIndex.isEmpty()) {
                System.out.println("\n[VOTING] No enrolled students with face data available");
                return FaceScanResponseDTOBuilder.noMatch("No enrolled students with face data available");
            }

            System.out.println("\n[VOTING] ============================================");
            System.out.println("[VOTING] Frame " + (scans + 1) + "/" + windowSize);
            System.out.println("[VOTING] Candidates: " + embeddingIndex.size() + " students");
            
            // Get top candidate for this frame (no threshold applied yet)
            Optional<ComparisonResult> top = recognitionService.topCandidate(imageBytes, embeddingIndex);
            
            if (top.isPresent() && top.get().getFaceName() != null) {
                String candidateId = top.get().getFaceName();
                float topSim = top.get().getSimilarity();
                StudentProfile profile = profiles.get(candidateId);
                String candidateName = profile != null ? profile.getEmail() : candidateId;
                
                System.out.println("[VOTING] Top match: " + candidateName + " with " + 
                                 String.format("%.2f%%", topSim * 100) + " similarity");
                
                // Only count as vote if above minimum threshold
                if (topSim >= (float) this.minSimilarity) {
                    voteCounts.merge(candidateId, 1, Integer::sum);
                    maxSimilarityById.merge(candidateId, topSim, (a, b) -> a != null && a > b ? a : b);
                    System.out.println("[VOTING] Vote counted! Total votes for " + candidateName + ": " + 
                                     voteCounts.get(candidateId));
                } else {
                    System.out.println("[VOTING] Vote NOT counted (below " + 
                                     String.format("%.2f%%", this.minSimilarity * 100) + " threshold)");
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
                                         " votes (best: " + String.format("%.2f%%", sim * 100) + ")");
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

                // Find candidate with most votes
                Map.Entry<String, Integer> bestVote = voteCounts.entrySet().stream()
                        .max((a, b) -> {
                            int cmp = Integer.compare(a.getValue(), b.getValue());
                            if (cmp != 0) return cmp;
                            // Tie-breaker: higher similarity wins
                            float sa = maxSimilarityById.getOrDefault(a.getKey(), 0.0f);
                            float sb = maxSimilarityById.getOrDefault(b.getKey(), 0.0f);
                            return Float.compare(sa, sb);
                        })
                        .orElse(null);

                if (bestVote == null) {
                    System.out.println("[VOTING] DECISION: No candidate found");
                    System.out.println("[VOTING] ============================================\n");
                    resetVoting();
                    return FaceScanResponseDTOBuilder.noMatch("Scanning...");
                }

                String studentId = bestVote.getKey();
                int votes = bestVote.getValue();
                StudentProfile profile = profiles.get(studentId);
                String candidateName = profile != null ? profile.getEmail() : studentId;
                
                System.out.println("[VOTING] Winner: " + candidateName + " with " + votes + " votes");
                
                // Check minimum votes requirement
                if (votes < requiredVotes) {
                    System.out.println("[VOTING] DECISION: REJECTED - insufficient votes (" + 
                                     votes + " < " + requiredVotes + " required)");
                    System.out.println("[VOTING] ============================================\n");
                    resetVoting();
                    return FaceScanResponseDTOBuilder.noMatch("No clear match; please try again");
                }
                
                if (profile == null) {
                    System.out.println("[VOTING] DECISION: ERROR - profile not found");
                    System.out.println("[VOTING] ============================================\n");
                    resetVoting();
                    return FaceScanResponseDTOBuilder.noMatch("Error processing match");
                }
                
                // Check if already marked
                AttendanceStatus currentStatus = statuses.get(studentId);
                if (currentStatus != null && currentStatus.isPresent()) {
                    double similarity = (double) maxSimilarityById.getOrDefault(studentId, 0.0f);
                    System.out.println("[VOTING] DECISION: Already marked as " + currentStatus);
                    System.out.println("[VOTING] ============================================\n");
                    resetVoting();
                    return FaceScanResponseDTOBuilder.alreadyMarked(profile, similarity, currentStatus, 
                                                                    "Student already marked as " + currentStatus);
                }
                
                // Determine attendance status based on time
                ZonedDateTime now = ZonedDateTime.now(DEFAULT_ZONE);
                AttendanceStatus recommendedStatus = determineAttendanceStatus(now);
                double similarity = (double) maxSimilarityById.getOrDefault(studentId, 0.0f);
                
                System.out.println("[VOTING] DECISION: ACCEPTED ✓");
                System.out.println("[VOTING]   Student: " + candidateName);
                System.out.println("[VOTING]   Similarity: " + String.format("%.2f%%", similarity * 100));
                System.out.println("[VOTING]   Status: " + recommendedStatus);
                System.out.println("[VOTING] ============================================\n");
                
                resetVoting();
                return FaceScanResponseDTOBuilder.match(profile, similarity, recommendedStatus, now.toOffsetDateTime());
            }

            System.out.println("[VOTING] Continue scanning... (" + scans + "/" + windowSize + " frames)");
            System.out.println("[VOTING] ============================================\n");
            return FaceScanResponseDTOBuilder.noMatch("Scanning...");
        }
        
        private void resetVoting() {
            voteCounts.clear();
            maxSimilarityById.clear();
            scans = 0;
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

            if (status != null && status.isPresent()) {
                profiles.remove(userId);
                rebuildEmbeddingIndex();
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
            return dto;
        }

        static FaceScanResponseDTO alreadyMarked(StudentProfile profile,
                                                 double similarity,
                                                 AttendanceStatus status,
                                                 String message) {
            FaceScanResponseDTO dto = match(profile, similarity, status, null);
            dto.setAlreadyMarked(true);
            dto.setMessage(message);
            dto.setMatched(false);
            return dto;
        }

        static FaceScanResponseDTO match(StudentProfile profile,
                                         double similarity,
                                         AttendanceStatus recommendedStatus,
                                         OffsetDateTime checkInTime) {
            FaceScanResponseDTO dto = new FaceScanResponseDTO();
            dto.setMatched(true);
            dto.setAlreadyMarked(false);
            dto.setSimilarity(similarity);
            dto.setRecommendedStatus(recommendedStatus != null ? recommendedStatus.getCode() : null);
            dto.setRecommendedCheckInTime(checkInTime);

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
}

