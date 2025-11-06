package com.smartattendance.mapper;

import com.smartattendance.dto.response.attendance.AttendanceRecordDTO;
import com.smartattendance.dto.response.attendance.AttendanceSessionDTO;
import com.smartattendance.dto.response.auth.UserDTO;
import com.smartattendance.dto.response.course.CourseDTO;
import com.smartattendance.dto.response.course.EnrollmentDTO;
import com.smartattendance.dto.response.course.SectionDTO;
import com.smartattendance.dto.response.user.SectionAssignmentDTO;
import com.smartattendance.dto.response.user.TAAssignmentDTO;
import com.smartattendance.entity.AttendanceRecord;
import com.smartattendance.entity.AttendanceSession;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Section;
import com.smartattendance.entity.SectionAssignment;
import com.smartattendance.entity.SectionEnrollment;
import com.smartattendance.entity.TAAssignment;
import com.smartattendance.entity.User;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-06T13:53:35+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251023-0518, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class EntityMapperImpl implements EntityMapper {

    @Override
    public CourseDTO toCourseDTO(Course course) {
        if ( course == null ) {
            return null;
        }

        CourseDTO courseDTO = new CourseDTO();

        courseDTO.setCode( course.getCode() );
        if ( course.hasDescription() ) {
            courseDTO.setDescription( course.getDescription() );
        }
        courseDTO.setId( course.getId() );
        courseDTO.setTitle( course.getTitle() );

        return courseDTO;
    }

    @Override
    public List<CourseDTO> toCourseDTOs(List<Course> courses) {
        if ( courses == null ) {
            return null;
        }

        List<CourseDTO> list = new ArrayList<CourseDTO>( courses.size() );
        for ( Course course : courses ) {
            list.add( toCourseDTO( course ) );
        }

        return list;
    }

    @Override
    public SectionDTO toSectionDTO(Section section) {
        if ( section == null ) {
            return null;
        }

        SectionDTO sectionDTO = new SectionDTO();

        sectionDTO.setCourse( toCourseDTO( section.getCourse() ) );
        sectionDTO.setCourseId( section.getCourseId() );
        sectionDTO.setEndTime( section.getEndTime() );
        sectionDTO.setId( section.getId() );
        if ( section.hasLocation() ) {
            sectionDTO.setLocation( section.getLocation() );
        }
        sectionDTO.setSectionCode( section.getSectionCode() );
        sectionDTO.setStartTime( section.getStartTime() );
        sectionDTO.setYear( section.getYear() );

        sectionDTO.setMeetingDay( com.smartattendance.util.helper.DateTimeUtils.dayNumberToName(section.getMeetingDay()) );
        sectionDTO.setSemester( section.getSemester() != null ? section.getSemester().getValue() : null );

        return sectionDTO;
    }

    @Override
    public List<SectionDTO> toSectionDTOs(List<Section> sections) {
        if ( sections == null ) {
            return null;
        }

        List<SectionDTO> list = new ArrayList<SectionDTO>( sections.size() );
        for ( Section section : sections ) {
            list.add( toSectionDTO( section ) );
        }

        return list;
    }

    @Override
    public EnrollmentDTO toEnrollmentDTO(SectionEnrollment enrollment) {
        if ( enrollment == null ) {
            return null;
        }

        EnrollmentDTO enrollmentDTO = new EnrollmentDTO();

        enrollmentDTO.setSection( toSectionDTO( enrollment.getSection() ) );
        enrollmentDTO.setEnrolledAt( enrollment.getEnrolledAt() );
        enrollmentDTO.setId( enrollment.getId() );
        enrollmentDTO.setIsActive( enrollment.getIsActive() );
        enrollmentDTO.setSectionId( enrollment.getSectionId() );
        enrollmentDTO.setUserId( enrollment.getUserId() );

        return enrollmentDTO;
    }

    @Override
    public List<EnrollmentDTO> toEnrollmentDTOs(List<SectionEnrollment> enrollments) {
        if ( enrollments == null ) {
            return null;
        }

        List<EnrollmentDTO> list = new ArrayList<EnrollmentDTO>( enrollments.size() );
        for ( SectionEnrollment sectionEnrollment : enrollments ) {
            list.add( toEnrollmentDTO( sectionEnrollment ) );
        }

        return list;
    }

    @Override
    public AttendanceRecordDTO toAttendanceRecordDTO(AttendanceRecord record) {
        if ( record == null ) {
            return null;
        }

        AttendanceRecordDTO attendanceRecordDTO = new AttendanceRecordDTO();

        attendanceRecordDTO.setAttendanceSession( toAttendanceSessionDTO( record.getAttendanceSession() ) );
        attendanceRecordDTO.setCheckinTime( record.getCheckinTime() );
        attendanceRecordDTO.setCheckoutTime( record.getCheckoutTime() );
        attendanceRecordDTO.setId( record.getId() );
        attendanceRecordDTO.setSessionId( record.getSessionId() );
        if ( record.getStatus() != null ) {
            attendanceRecordDTO.setStatus( record.getStatus().name() );
        }
        attendanceRecordDTO.setUserId( record.getUserId() );

        return attendanceRecordDTO;
    }

    @Override
    public List<AttendanceRecordDTO> toAttendanceRecordDTOs(List<AttendanceRecord> records) {
        if ( records == null ) {
            return null;
        }

        List<AttendanceRecordDTO> list = new ArrayList<AttendanceRecordDTO>( records.size() );
        for ( AttendanceRecord attendanceRecord : records ) {
            list.add( toAttendanceRecordDTO( attendanceRecord ) );
        }

        return list;
    }

    @Override
    public AttendanceSessionDTO toAttendanceSessionDTO(AttendanceSession session) {
        if ( session == null ) {
            return null;
        }

        AttendanceSessionDTO attendanceSessionDTO = new AttendanceSessionDTO();

        attendanceSessionDTO.setSessionDate( session.getSessionDate() );
        attendanceSessionDTO.setScheduledStartTime( session.getScheduledStartTime() );
        attendanceSessionDTO.setScheduledEndTime( session.getScheduledEndTime() );
        attendanceSessionDTO.setId( session.getId() );
        attendanceSessionDTO.setNotes( session.getNotes() );
        attendanceSessionDTO.setSection( toSectionDTO( session.getSection() ) );
        attendanceSessionDTO.setSectionId( session.getSectionId() );
        attendanceSessionDTO.setStatus( session.getStatus() );

        return attendanceSessionDTO;
    }

    @Override
    public List<AttendanceSessionDTO> toAttendanceSessionDTOs(List<AttendanceSession> sessions) {
        if ( sessions == null ) {
            return null;
        }

        List<AttendanceSessionDTO> list = new ArrayList<AttendanceSessionDTO>( sessions.size() );
        for ( AttendanceSession attendanceSession : sessions ) {
            list.add( toAttendanceSessionDTO( attendanceSession ) );
        }

        return list;
    }

    @Override
    public TAAssignmentDTO toTAAssignmentDTO(TAAssignment assignment) {
        if ( assignment == null ) {
            return null;
        }

        TAAssignmentDTO tAAssignmentDTO = new TAAssignmentDTO();

        tAAssignmentDTO.setSection( toSectionDTO( assignment.getSection() ) );
        tAAssignmentDTO.setId( assignment.getId() );
        tAAssignmentDTO.setSectionId( assignment.getSectionId() );
        tAAssignmentDTO.setUserId( assignment.getUserId() );

        return tAAssignmentDTO;
    }

    @Override
    public List<TAAssignmentDTO> toTAAssignmentDTOs(List<TAAssignment> assignments) {
        if ( assignments == null ) {
            return null;
        }

        List<TAAssignmentDTO> list = new ArrayList<TAAssignmentDTO>( assignments.size() );
        for ( TAAssignment tAAssignment : assignments ) {
            list.add( toTAAssignmentDTO( tAAssignment ) );
        }

        return list;
    }

    @Override
    public SectionAssignmentDTO toSectionAssignmentDTO(SectionAssignment assignment) {
        if ( assignment == null ) {
            return null;
        }

        SectionAssignmentDTO sectionAssignmentDTO = new SectionAssignmentDTO();

        sectionAssignmentDTO.setSection( toSectionDTO( assignment.getSection() ) );
        sectionAssignmentDTO.setId( assignment.getId() );
        sectionAssignmentDTO.setIsActive( assignment.getIsActive() );
        sectionAssignmentDTO.setRole( assignment.getRole() );
        sectionAssignmentDTO.setSectionId( assignment.getSectionId() );
        sectionAssignmentDTO.setUserId( assignment.getUserId() );

        return sectionAssignmentDTO;
    }

    @Override
    public List<SectionAssignmentDTO> toSectionAssignmentDTOs(List<SectionAssignment> assignments) {
        if ( assignments == null ) {
            return null;
        }

        List<SectionAssignmentDTO> list = new ArrayList<SectionAssignmentDTO>( assignments.size() );
        for ( SectionAssignment sectionAssignment : assignments ) {
            list.add( toSectionAssignmentDTO( sectionAssignment ) );
        }

        return list;
    }

    @Override
    public UserDTO toUserDTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserDTO userDTO = new UserDTO();

        userDTO.setAuthId( user.getAuthId() );
        userDTO.setEmail( user.getEmail() );
        userDTO.setEnabled( user.getEnabled() );
        userDTO.setFirstName( user.getFirstName() );
        userDTO.setId( user.getId() );
        userDTO.setIsInstructor( user.getIsInstructor() );
        userDTO.setIsStudent( user.getIsStudent() );
        userDTO.setIsTA( user.getIsTA() );
        userDTO.setLastName( user.getLastName() );

        return userDTO;
    }
}
