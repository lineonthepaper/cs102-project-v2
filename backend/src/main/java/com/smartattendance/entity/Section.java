package com.smartattendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalTime;
import java.time.Duration;
import com.smartattendance.util.helper.DateTimeUtils;
import com.smartattendance.util.constants.AttendanceConstants;

@Getter  // Only generate getters
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sections")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_code", nullable = false)
    private String sectionCode;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", insertable = false, updatable = false)
    private Course course;

    @Column(name = "year")
    private Integer year;

    @Column(name = "semester")
    private Semester semester;  // Changed from Integer to Semester enum

    @Column(name = "meeting_day")
    private Integer meetingDay;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "location")
    private String location;

    // ===== VALIDATED SETTERS (ISP Fix) =====
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setSectionCode(String sectionCode) {
        if (sectionCode == null || sectionCode.isBlank()) {
            throw new IllegalArgumentException("Section code cannot be null or empty");
        }
        this.sectionCode = sectionCode.trim();
    }
    
    public void setCourseId(Long courseId) {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Course ID must be a positive number");
        }
        this.courseId = courseId;
    }
    
    public void setCourse(Course course) {
        this.course = course;
    }
    
    public void setYear(Integer year) {
        if (year != null && (year < 2000 || year > 2100)) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100");
        }
        this.year = year;
    }
    
    public void setSemester(Semester semester) {
        this.semester = semester;
    }
    
    public void setMeetingDay(Integer meetingDay) {
        if (meetingDay != null && (meetingDay < 1 || meetingDay > 7)) {
            throw new IllegalArgumentException("Meeting day must be between 1 (Monday) and 7 (Sunday)");
        }
        this.meetingDay = meetingDay;
    }
    
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
    
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
    
    public void setLocation(String location) {
        this.location = location != null ? location.trim() : null;
    }

    // ===== BUSINESS LOGIC METHODS (Rich Domain Model) =====
    
    /**
     * Validates if the time range is valid (end time is after start time).
     * 
     * @return true if time range is valid
     */
    public boolean isValidTimeRange() {
        if (startTime == null || endTime == null) {
            return false;
        }
        return endTime.isAfter(startTime);
    }
    
    /**
     * Calculate the duration of the session in minutes.
     * 
     * @return duration in minutes, or null if times are not set
     */
    public Long getSessionDurationMinutes() {
        if (startTime == null || endTime == null) {
            return null;
        }
        return Duration.between(startTime, endTime).toMinutes();
    }
    
    /**
     * Get the duration of the session as a Duration object.
     * 
     * @return duration, or null if times are not set
     */
    public Duration getSessionDuration() {
        if (startTime == null || endTime == null) {
            return null;
        }
        return Duration.between(startTime, endTime);
    }
    
    /**
     * Check if this section meets on the given day number (1=Monday, 7=Sunday).
     * 
     * @param dayNumber the day number to check
     * @return true if section meets on this day
     */
    public boolean meetsOnDay(Integer dayNumber) {
        return meetingDay != null && meetingDay.equals(dayNumber);
    }
    
    /**
     * Get the formatted schedule string.
     * Example: "Monday 10:00 - 11:30"
     * 
     * @return formatted schedule
     */
    public String getFormattedSchedule() {
        if (meetingDay == null || startTime == null || endTime == null) {
            return AttendanceConstants.SCHEDULE_NOT_SET;
        }
        String dayName = DateTimeUtils.dayNumberToName(meetingDay);
        return String.format("%s %s - %s", dayName, startTime, endTime);
    }
    
    /**
     * Get the full section identifier.
     * Example: "CS102-001"
     * 
     * @return full section identifier
     */
    public String getFullSectionCode() {
        if (course != null && course.getCode() != null && sectionCode != null) {
            return course.getCode() + "-" + sectionCode;
        }
        return sectionCode != null ? sectionCode : AttendanceConstants.UNKNOWN;
    }
    
    /**
     * Get the semester name.
     * Now using type-safe enum instead of integer values.
     * 
     * @return semester name
     */
    public String getSemesterName() {
        if (semester == null) {
            return AttendanceConstants.UNKNOWN;
        }
        return semester.getDisplayName();  // Much cleaner with enum!
    }
    
    /**
     * Get the full term description.
     * Example: "Fall 2024"
     * 
     * @return full term description
     */
    public String getTermDescription() {
        if (year == null || semester == null) {
            return AttendanceConstants.UNKNOWN_TERM;
        }
        return getSemesterName() + " " + year;
    }
    
    /**
     * Check if this section has a location assigned.
     * 
     * @return true if location is set
     */
    public boolean hasLocation() {
        return location != null && !location.isBlank();
    }
    
    /**
     * Check if this section is currently active (current year and semester).
     * Note: This is a simple check - you might want to make it more sophisticated.
     * 
     * @param currentYear the current year
     * @param currentSemester the current semester
     * @return true if section is active
     */
    public boolean isActive(Integer currentYear, Integer currentSemester) {
        if (year == null || semester == null) {
            return false;
        }
        return year.equals(currentYear) && semester.equals(currentSemester);
    }
    
    /**
     * Validates all required fields are present.
     * 
     * @return true if all required fields are set
     */
    public boolean isComplete() {
        return sectionCode != null && !sectionCode.isBlank()
            && courseId != null
            && year != null
            && semester != null
            && meetingDay != null
            && startTime != null
            && endTime != null
            && isValidTimeRange();
    }
}

