# Backend Project Structure

This Spring Boot backend follows industry-standard package organization patterns for maintainability and scalability.

## Package Structure

```
com.smartattendance/
├── SmartAttendanceApplication.java    # Main Spring Boot application entry point
│
├── config/                             # Configuration classes
│   └── (Future: CORS, Security, Database configurations)
│
├── controller/                         # REST API Controllers
│   ├── admin/                          # Admin-specific controllers
│   │   ├── InstructorAdminController.java
│   │   └── TeachingAssistantAdminController.java
│   ├── CourseController.java
│   ├── InstructorController.java
│   ├── SectionController.java
│   ├── StudentController.java
│   └── TAController.java
│
├── dto/                                # Data Transfer Objects
│   ├── request/                        # Request DTOs (incoming data)
│   │   ├── AddInstructorRequest.java
│   │   ├── AddTARequest.java
│   │   ├── CreateStudentRequest.java
│   │   ├── UpdateEnrollmentRequest.java
│   │   ├── UpdateInstructorAssignmentsRequest.java
│   │   └── UpdateTAAssignmentsRequest.java
│   │
│   └── response/                       # Response DTOs (outgoing data)
│       ├── AttendanceRecordDTO.java
│       ├── AttendanceSessionDTO.java
│       ├── CourseDTO.java
│       ├── EnrollmentDTO.java
│       ├── InstructorDTO.java
│       ├── SectionAssignmentDTO.java
│       ├── SectionDTO.java
│       ├── StudentDTO.java
│       ├── TAAssignmentDTO.java
│       └── TADTO.java
│
├── entity/                             # JPA Entity classes (database models)
│   ├── AttendanceRecord.java
│   ├── AttendanceSession.java
│   ├── Course.java
│   ├── Section.java
│   ├── SectionAssignment.java
│   ├── SectionEnrollment.java
│   ├── TAAssignment.java
│   └── User.java
│
├── repository/                         # Spring Data JPA Repositories
│   ├── AttendanceRecordRepository.java
│   ├── CourseRepository.java
│   ├── SectionAssignmentRepository.java
│   ├── SectionEnrollmentRepository.java
│   ├── SectionRepository.java
│   ├── TAAssignmentRepository.java
│   └── UserRepository.java
│
├── service/                            # Business logic layer
│   ├── CourseService.java
│   ├── InstructorService.java
│   ├── SectionService.java
│   ├── StudentService.java
│   ├── SupabaseAuthService.java
│   └── TAService.java
│
└── exception/                          # Custom exception classes
    └── (Future: Custom exceptions for error handling)
```

## Design Patterns & Best Practices

### 1. **Layered Architecture**
- **Controller Layer**: Handles HTTP requests/responses
- **Service Layer**: Contains business logic
- **Repository Layer**: Data access layer (Spring Data JPA)
- **Entity Layer**: Database models
- **DTO Layer**: Decoupled data transfer objects

### 2. **Separation of Concerns**
- **Request DTOs** (`dto/request`): Validate and structure incoming data
- **Response DTOs** (`dto/response`): Format outgoing data, hide sensitive fields
- **Admin Controllers** (`controller/admin`): Separate admin operations from regular API

### 3. **Package Organization Benefits**
- **Modularity**: Easy to locate and modify related code
- **Scalability**: Clear structure for adding new features
- **Maintainability**: Logical grouping reduces cognitive load
- **Testing**: Easy to mock and test individual layers

## API Endpoints

### Students
- `GET /api/students` - Fetch all students with attendance and enrollment data
- `POST /api/students` - Create new student
- `PUT /api/students/{id}/enrollments` - Update student enrollments

### Instructors
- `GET /api/instructors` - Fetch all instructors with assignments
- `POST /api/instructors` - Create new instructor
- `DELETE /api/instructors/{id}` - Remove instructor
- `PUT /api/instructors/{id}/assignments` - Update instructor section assignments

### Teaching Assistants
- `GET /api/teaching-assistants` - Fetch all TAs with assignments
- `POST /api/teaching-assistants` - Add TA
- `DELETE /api/teaching-assistants/{id}` - Remove TA
- `PUT /api/teaching-assistants/{id}/assignments` - Update TA section assignments

### Courses
- `GET /api/courses` - Fetch all courses
- `POST /api/courses` - Create course
- `PUT /api/courses/{id}` - Update course
- `DELETE /api/courses/{id}` - Delete course

### Sections
- `GET /api/sections` - Fetch all sections
- `POST /api/sections` - Create section
- `PUT /api/sections/{id}` - Update section
- `DELETE /api/sections/{id}` - Delete section

### Admin Operations
- `DELETE /api/admin/instructors/{authId}` - Remove instructor from Supabase Auth
- `DELETE /api/admin/teaching-assistants/{authId}` - Remove TA from Supabase Auth

## Technology Stack

- **Framework**: Spring Boot 3.3.4
- **Java**: 21
- **Database**: PostgreSQL (Supabase)
- **ORM**: Spring Data JPA (Hibernate)
- **Build Tool**: Gradle (Kotlin DSL)

## Database Strategy

- **Connection**: Direct connection to Supabase PostgreSQL
- **Schema Management**: Hibernate DDL auto-update
- **ID Generation**: Database triggers for custom IDs (S0000001, I0000001, T0000001)
- **Transactions**: `@Transactional` for data consistency

## Future Improvements

1. Add global exception handling (`@ControllerAdvice`)
2. Implement custom exception classes in `/exception`
3. Add validation annotations to DTOs (`@Valid`, `@NotNull`, etc.)
4. Implement pagination for large datasets
5. Add API versioning (`/api/v1/...`)
6. Add Spring Security configuration in `/config`
7. Implement audit logging
8. Add integration tests

