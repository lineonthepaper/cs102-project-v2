package com.smartattendance.service;

import com.smartattendance.dto.response.course.SectionDTO;
import com.smartattendance.entity.Section;
import com.smartattendance.entity.Semester;
import com.smartattendance.mapper.EntityMapper;
import com.smartattendance.repository.SectionRepository;
import com.smartattendance.util.helper.DateTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final EntityMapper mapper;

    public SectionService(SectionRepository sectionRepository, EntityMapper mapper) {
        this.sectionRepository = sectionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<SectionDTO> getAllSections() {
        return mapper.toSectionDTOs(sectionRepository.findAll());
    }

    @Transactional
    public SectionDTO createSection(SectionDTO sectionDTO) {
        Section section = new Section();
        section.setCourseId(sectionDTO.getCourseId());
        section.setSectionCode(sectionDTO.getSectionCode());
        section.setYear(sectionDTO.getYear());
        // Convert Integer to Semester enum
        section.setSemester(sectionDTO.getSemester() != null ? Semester.fromValue(sectionDTO.getSemester()) : null);
        section.setMeetingDay(DateTimeUtils.dayNameToNumber(sectionDTO.getMeetingDay()));
        section.setStartTime(sectionDTO.getStartTime());
        section.setEndTime(sectionDTO.getEndTime());
        section.setLocation(sectionDTO.getLocation());
        
        return mapper.toSectionDTO(sectionRepository.save(section));
    }

    @Transactional
    public SectionDTO updateSection(Long id, SectionDTO sectionDTO) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new com.smartattendance.exception.ResourceNotFoundException("Section", id.toString()));
        
        section.setCourseId(sectionDTO.getCourseId());
        section.setSectionCode(sectionDTO.getSectionCode());
        section.setYear(sectionDTO.getYear());
        // Convert Integer to Semester enum
        section.setSemester(sectionDTO.getSemester() != null ? Semester.fromValue(sectionDTO.getSemester()) : null);
        section.setMeetingDay(DateTimeUtils.dayNameToNumber(sectionDTO.getMeetingDay()));
        section.setStartTime(sectionDTO.getStartTime());
        section.setEndTime(sectionDTO.getEndTime());
        section.setLocation(sectionDTO.getLocation());
        
        return mapper.toSectionDTO(sectionRepository.save(section));
    }

    @Transactional
    public void deleteSection(Long id) {
        sectionRepository.deleteById(id);
    }
}

