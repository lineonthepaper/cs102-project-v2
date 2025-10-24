package com.smartattendance.controller;

import com.smartattendance.dto.request.CreateStudentRequest;
import com.smartattendance.dto.response.StudentDTO;
import com.smartattendance.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @Test
    void getAllStudents_ShouldReturnOkStatus() throws Exception {
        // Arrange
        StudentDTO student = new StudentDTO();
        student.setId("S0000001");
        student.setEmail("test@example.com");
        student.setFirstName("John");
        student.setLastName("Doe");
        
        when(studentService.getAllStudents()).thenReturn(List.of(student));

        // Act & Assert
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("S0000001"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"));
    }

    @Test
    void getAllStudents_ShouldReturnEmptyList_WhenNoStudents() throws Exception {
        // Arrange
        when(studentService.getAllStudents()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createStudent_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
        // Arrange
        String invalidRequest = """
            {
                "email": "invalid-email",
                "firstName": "John",
                "lastName": "Doe"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStudent_ShouldReturnBadRequest_WhenFieldsAreMissing() throws Exception {
        // Arrange
        String invalidRequest = """
            {
                "email": "test@example.com"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
}

