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

    @Value("${face.voting.weight.base:0.95}")
    private double voteWeightBase;

    @Value("${face.voting.weight.alpha:50}")
    private double voteWeightAlpha;

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
                    faceRecognitionService, votingWindowSize, votingRequiredVotes);
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
            
            if (student.getFaceImages() == null || student.getFaceImages().isEmpty()) {
                System.out.println("[DEBUG]   No face images for this student");
                continue;
            }
            
            System.out.println("[DEBUG]   Found " + student.getFaceImages().size() + " face images");

            AttendanceStatus status = existingStatuses.get(student.getId());
            if (status != null && status.isPresent()) {
                System.out.println("[DEBUG]   Student already marked as: " + status + ", skipping");
                // Already marked present/late – skip to avoid re-scanning
                continue;
            }

            List<float[]> embeddings = new ArrayList<>();
            for (int i = 0; i < student.getFaceImages().size(); i++) {
                String base64Image = student.getFaceImages().get(i);
                System.out.println("[DEBUG]   Computing embedding for image " + (i+1) + "...");
                byte[] bytes = decodeBase64(base64Image);
                if (bytes == null) {
                    System.out.println("[DEBUG]   Failed to decode image " + (i+1));
                    continue;
                }
                faceRecognitionService.computeEmbedding(bytes).ifPresent(emb -> {
                    embeddings.add(emb);
                    System.out.println("[DEBUG]   Embedding computed: " + emb.length + " dimensions, " +
                                     "first 3 values: [" + emb[0] + ", " + emb[1] + ", " + emb[2] + "]");
                });
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

        return new SessionRecognitionContext(session, profiles, existingStatuses, faceRecognitionService, votingWindowSize, votingRequiredVotes);
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
        private final Map<String, Double> voteCounts = new ConcurrentHashMap<>();
        private final int windowSize;
        private final int requiredVotes;
        private int scans = 0;

        private volatile Map<String, List<float[]>> embeddingIndex;

        private SessionRecognitionContext(AttendanceSession session,
                                          Map<String, StudentProfile> profiles,
                                          Map<String, AttendanceStatus> statuses,
                                          FaceRecognitionService recognitionService,
                                          int windowSize,
                                          int requiredVotes) {
            this.session = session;
            this.profiles = profiles;
            this.statuses = new ConcurrentHashMap<>(statuses);
            this.recognitionService = recognitionService;
            rebuildEmbeddingIndex();
            this.windowSize = Math.max(1, windowSize);
            this.requiredVotes = Math.max(1, requiredVotes);
        }

        private void rebuildEmbeddingIndex() {
            this.embeddingIndex = profiles.values().stream()
                    .collect(Collectors.toMap(StudentProfile::getUserId, StudentProfile::getEmbeddings));
        }

        FaceScanResponseDTO match(byte[] imageBytes) {
            if (embeddingIndex.isEmpty()) {
                System.out.println("[DEBUG] No enrolled students with face data available");
                return FaceScanResponseDTOBuilder.noMatch("No enrolled students with face data available");
            }

            System.out.println("[DEBUG] Matching face against " + embeddingIndex.size() + " students");
            // Vote on top candidate each scan (even if below acceptance gates)
            Optional<ComparisonResult> top = recognitionService.topCandidate(imageBytes, embeddingIndex);
            if (top.isPresent() && top.get().getFaceName() != null) {
                String candidateId = top.get().getFaceName();
                float sim = top.get().getSimilarity();
                double weight = Math.exp(voteWeightAlpha * (Math.max(0.0, sim - voteWeightBase)));
                if (Double.isInfinite(weight) || Double.isNaN(weight)) {
                    weight = 0.0;
                }
                double clamped = Math.min(Math.max(weight, 0.0), 1_000_000.0);
                voteCounts.merge(candidateId, clamped, Double::sum);
            }
            scans++;

            // Decide when enough votes gathered or window completed
            Map.Entry<String, Double> bestVote = voteCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (bestVote != null && scans >= windowSize) {
                String studentId = bestVote.getKey();
                StudentProfile profile = profiles.get(studentId);
                if (profile == null) {
                    // reset for a fresh window next call
                    voteCounts.clear();
                    scans = 0;
                    return FaceScanResponseDTOBuilder.noMatch("Scanning...");
                }
                // Optional: compute similarity for reporting
                Optional<FaceMatch> gated = recognitionService.matchFace(imageBytes, embeddingIndex);
                float topSim = top.map(ComparisonResult::getSimilarity).orElse(0.0f);
                double similarity = gated.map(FaceMatch::getSimilarity).orElse((double) topSim);
                AttendanceStatus currentStatus = statuses.get(studentId);
                if (currentStatus != null && currentStatus.isPresent()) {
                    // reset after decision so the next click re-scans fresh
                    voteCounts.clear();
                    scans = 0;
                    return FaceScanResponseDTOBuilder.alreadyMarked(profile, similarity, currentStatus, "Student already marked as present");
                }
                ZonedDateTime now = ZonedDateTime.now(DEFAULT_ZONE);
                AttendanceStatus recommendedStatus = session.isCheckinLate(now.toLocalDateTime())
                        ? AttendanceStatus.LATE
                        : AttendanceStatus.PRESENT;
                // reset after decision so the next click re-scans fresh
                FaceScanResponseDTO decided = FaceScanResponseDTOBuilder.match(profile, similarity, recommendedStatus, now.toOffsetDateTime());
                voteCounts.clear();
                scans = 0;
                return decided;
            }

            // Keep scanning until we can decide
            System.out.println("[DEBUG] Voting in progress: scans=" + scans + "/" + windowSize + ", weightedVotes=" + voteCounts);
            return FaceScanResponseDTOBuilder.noMatch("Scanning...");
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

