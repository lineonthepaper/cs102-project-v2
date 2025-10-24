package com.smartattendance.service;

import com.smartattendance.dto.response.CourseDTO;
import com.smartattendance.dto.response.SectionDTO;
import com.smartattendance.entity.Section;
import com.smartattendance.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public List<SectionDTO> getAllSections() {
        return sectionRepository.findAll().stream()
                .<SectionDTO>map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SectionDTO createSection(SectionDTO sectionDTO) {
        Section section = new Section();
        section.setCourseId(sectionDTO.getCourseId());
        section.setSectionCode(sectionDTO.getSectionCode());
        section.setYear(sectionDTO.getYear());
        section.setSemester(sectionDTO.getSemester());
        section.setMeetingDay(dayNameToNumber(sectionDTO.getMeetingDay()));
        section.setStartTime(sectionDTO.getStartTime());
        section.setEndTime(sectionDTO.getEndTime());
        section.setLocation(sectionDTO.getLocation());
        
        Section savedSection = sectionRepository.save(section);
        return mapToDTO(savedSection);
    }

    @Transactional
    public SectionDTO updateSection(Long id, SectionDTO sectionDTO) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found with ID: " + id));
        
        section.setCourseId(sectionDTO.getCourseId());
        section.setSectionCode(sectionDTO.getSectionCode());
        section.setYear(sectionDTO.getYear());
        section.setSemester(sectionDTO.getSemester());
        section.setMeetingDay(dayNameToNumber(sectionDTO.getMeetingDay()));
        section.setStartTime(sectionDTO.getStartTime());
        section.setEndTime(sectionDTO.getEndTime());
        section.setLocation(sectionDTO.getLocation());
        
        Section updatedSection = sectionRepository.save(section);
        return mapToDTO(updatedSection);
    }

    @Transactional
    public void deleteSection(Long id) {
        sectionRepository.deleteById(id);
    }

    private SectionDTO mapToDTO(Section section) {
        CourseDTO courseDTO = null;
        if (section.getCourse() != null) {
            courseDTO = new CourseDTO(
                    section.getCourse().getId(),
                    section.getCourse().getCode(),
                    section.getCourse().getTitle(),
                    section.getCourse().getDescription()
            );
        }
        
        return new SectionDTO(
                section.getId(),
                section.getSectionCode(),
                section.getCourseId(),
                section.getYear(),
                section.getSemester(),
                dayNumberToName(section.getMeetingDay()),
                section.getStartTime(),
                section.getEndTime(),
                section.getLocation(),
                courseDTO
        );
    }
    
    private String dayNumberToName(Integer dayNumber) {
        if (dayNumber == null) return null;
        return switch (dayNumber) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> null;
        };
    }
    
    private Integer dayNameToNumber(String dayName) {
        if (dayName == null) return null;
        return switch (dayName.toLowerCase()) {
            case "monday" -> 1;
            case "tuesday" -> 2;
            case "wednesday" -> 3;
            case "thursday" -> 4;
            case "friday" -> 5;
            case "saturday" -> 6;
            case "sunday" -> 7;
            default -> null;
        };
    }
}

