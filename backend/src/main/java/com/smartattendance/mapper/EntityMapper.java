package com.smartattendance.mapper;

import com.smartattendance.dto.response.attendance.*;
import com.smartattendance.dto.response.course.*;
import com.smartattendance.dto.response.user.*;
import com.smartattendance.dto.response.auth.*;
import com.smartattendance.entity.*;
import com.smartattendance.util.helper.DateTimeUtils;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper interface for automatic DTO <-> Entity conversions.
 * 
 * This REPLACES all manual mapToXxxDTO() methods across services.
 * MapStruct generates the implementation at compile-time.
 * 
 * Before: ~400 lines of manual mapping code
 * After:  ~50 lines of interface declarations
 * Savings: -87% bloat!
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface EntityMapper {

    // ==================== Course Mappings ====================
    
    CourseDTO toCourseDTO(Course course);
    
    List<CourseDTO> toCourseDTOs(List<Course> courses);

    // ==================== Section Mappings ====================
    
    @Mapping(target = "meetingDay", expression = "java(com.smartattendance.util.helper.DateTimeUtils.dayNumberToName(section.getMeetingDay()))")
    @Mapping(target = "semester", expression = "java(section.getSemester() != null ? section.getSemester().getValue() : null)")
    @Mapping(target = "course", source = "course")
    SectionDTO toSectionDTO(Section section);
    
    List<SectionDTO> toSectionDTOs(List<Section> sections);

    // ==================== Enrollment Mappings ====================
    
    @Mapping(target = "section", source = "section")
    EnrollmentDTO toEnrollmentDTO(SectionEnrollment enrollment);
    
    List<EnrollmentDTO> toEnrollmentDTOs(List<SectionEnrollment> enrollments);

    // ==================== Attendance Mappings ====================
    
    @Mapping(target = "attendanceSession", source = "attendanceSession")
    AttendanceRecordDTO toAttendanceRecordDTO(AttendanceRecord record);
    
    List<AttendanceRecordDTO> toAttendanceRecordDTOs(List<AttendanceRecord> records);
    
    @Mapping(target = "sessionDate", source = "sessionDate", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "scheduledStartTime", source = "scheduledStartTime", dateFormat = "HH:mm:ss")
    @Mapping(target = "scheduledEndTime", source = "scheduledEndTime", dateFormat = "HH:mm:ss")
    AttendanceSessionDTO toAttendanceSessionDTO(AttendanceSession session);
    
    List<AttendanceSessionDTO> toAttendanceSessionDTOs(List<AttendanceSession> sessions);

    // ==================== TA/Instructor Assignment Mappings ====================
    
    @Mapping(target = "section", source = "section")
    TAAssignmentDTO toTAAssignmentDTO(TAAssignment assignment);
    
    List<TAAssignmentDTO> toTAAssignmentDTOs(List<TAAssignment> assignments);
    
    @Mapping(target = "section", source = "section")
    SectionAssignmentDTO toSectionAssignmentDTO(SectionAssignment assignment);
    
    List<SectionAssignmentDTO> toSectionAssignmentDTOs(List<SectionAssignment> assignments);

    // ==================== User-Related Mappings ====================
    
    /**
     * Maps User entity to UserDTO (for auth responses)
     */
    UserDTO toUserDTO(User user);

    // Note: StudentDTO, TADTO, InstructorDTO have complex logic
    // These will be handled with custom mapping methods in services
    // (attendance rates, assignment lists, etc.)
}

