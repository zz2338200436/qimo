package com._202510007517.platform.course.service;

import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentRequestDTO;
import com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.MajorDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.domain.CourseRecord;
import com._202510007517.platform.course.repository.CourseRepository;
import com._202510007517.platform.course.repository.TeacherKnowledgePointRepository;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.feign.UserFeignClient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseApplicationServiceTest {

    @Test
    void teacherKnowledgePointServiceCreatesAndReadsPoint() {
        FakeTeacherKnowledgePointRepository repository = new FakeTeacherKnowledgePointRepository();
        TeacherKnowledgePointService service = new TeacherKnowledgePointService(repository, new FakeCourseRepository());

        Map<String, Object> created = service.createKnowledgePoint(7L, Map.of(
                "pointName", "面向对象基础",
                "description", "封装、继承、多态",
                "difficulty", "中等",
                "orderIndex", 1,
                "courseId", 101L
        ));

        assertThat(created)
                .containsEntry("id", 1L)
                .containsEntry("pointName", "面向对象基础")
                .containsEntry("name", "面向对象基础")
                .containsEntry("courseId", 101L);
        assertThat(service.listKnowledgePointsByCourse(7L, 101L)).hasSize(1);
    }

    @Test
    void teacherKnowledgePointServiceRejectsForeignCourse() {
        TeacherKnowledgePointService service = new TeacherKnowledgePointService(
                new FakeTeacherKnowledgePointRepository(),
                new FakeCourseRepository() {
                    @Override
                    public boolean teacherOwnsCourse(Long teacherId, Long courseId) {
                        return false;
                    }
                });

        assertThatThrownBy(() -> service.createKnowledgePoint(7L, Map.of(
                "pointName", "面向对象基础",
                "courseId", 101L
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权访问该课程");
    }

    @Test
    void listTeacherCoursesAddsStudentCounts() {
        CourseApplicationService service = new CourseApplicationService(new FakeCourseRepository(), new FakeUserFeignClient());

        List<CourseDTO> courses = service.listTeacherCourses(7L, null, null, null, null);

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getId()).isEqualTo(101L);
        assertThat(courses.get(0).getCourseName()).isEqualTo("Distributed Systems");
        assertThat(courses.get(0).getStudentCount()).isEqualTo(36);
    }

    @Test
    void createCourseRejectsStartDateAfterEndDate() {
        CourseApplicationService service = new CourseApplicationService(new FakeCourseRepository(), new FakeUserFeignClient());
        CourseUpsertRequestDTO request = new CourseUpsertRequestDTO();
        request.setCourseName("Distributed Systems");
        request.setCourseCode("DS101");
        request.setCredit(3);
        request.setTotalHours(48);
        request.setStartDate("2026-09-10");
        request.setEndDate("2026-09-01");

        assertThatThrownBy(() -> service.createCourse(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("开始日期不能晚于结束日期");
    }

    @Test
    void listTeacherClassesGroupsCoursesByClass() {
        CourseApplicationService service = new CourseApplicationService(new FakeCourseRepository(), new FakeUserFeignClient());

        List<TeacherClassDTO> classes = service.listTeacherClasses(7L, null, null, null, null, null);

        assertThat(classes).hasSize(1);
        assertThat(classes.get(0).getId()).isEqualTo(501L);
        assertThat(classes.get(0).getClassName()).isEqualTo("软件 2301");
        assertThat(classes.get(0).getCourseName()).isEqualTo("Distributed Systems、Algorithms");
        assertThat(classes.get(0).getCourseCount()).isEqualTo(2);
        assertThat(classes.get(0).getCourses()).hasSize(2);
    }

    @Test
    void assignCourseRejectsDuplicateAssignment() {
        CourseApplicationService service = new CourseApplicationService(new FakeCourseRepository() {
            @Override
            public boolean classCourseExists(Long classId, Long courseId) {
                return true;
            }
        }, new FakeUserFeignClient());
        CourseAssignmentRequestDTO request = new CourseAssignmentRequestDTO();
        request.setClassId(501L);
        request.setCourseId(101L);

        assertThatThrownBy(() -> service.assignCourse(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无需重复分配");
    }

    @Test
    void listMajorsMapsRepositoryRows() {
        CourseApplicationService service = new CourseApplicationService(new FakeCourseRepository() {
            @Override
            public List<Map<String, Object>> findMajors() {
                return List.of(
                        Map.of("id", 2L, "majorName", "软件工程"),
                        Map.of("id", 3L, "majorName", "网络工程")
                );
            }
        }, new FakeUserFeignClient());

        List<MajorDTO> majors = service.listMajors();

        assertThat(majors).hasSize(2);
        assertThat(majors.get(0).getId()).isEqualTo(2L);
        assertThat(majors.get(0).getMajorName()).isEqualTo("软件工程");
        assertThat(majors.get(1).getId()).isEqualTo(3L);
        assertThat(majors.get(1).getMajorName()).isEqualTo("网络工程");
    }

    @Test
    void listStudentClassIdsReturnsDistinctClassIds() {
        CourseApplicationService service = new CourseApplicationService(new FakeCourseRepository() {
            @Override
            public List<Long> findClassIdsByStudentId(Long studentId) {
                return List.of(501L, 502L, 501L);
            }
        }, new FakeUserFeignClient());

        List<Long> classIds = service.listStudentClassIds(42L);

        assertThat(classIds).containsExactly(501L, 502L);
    }

    @Test
    void listStudentCoursesReturnsStudentScopedCourses() {
        CourseApplicationService service = new CourseApplicationService(new FakeCourseRepository() {
            @Override
            public List<CourseRecord> findByStudentIdWithSearch(Long studentId, String searchQuery, String category, String status) {
                CourseRecord record = new CourseRecord();
                record.setId(201L);
                record.setCourseName("Operating Systems");
                record.setCourseCode("OS201");
                record.setDescription("Kernel and process management");
                record.setCredit(4);
                record.setCourseCategory("专业课");
                record.setTotalHours(64);
                record.setTeacherId(7L);
                record.setCourseStatus("进行中");
                record.setSemester("2026春");
                return List.of(record);
            }
        }, new FakeUserFeignClient());

        List<CourseDTO> courses = service.listStudentCourses(42L, null, null, null);

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getId()).isEqualTo(201L);
        assertThat(courses.get(0).getCourseName()).isEqualTo("Operating Systems");
        assertThat(courses.get(0).getTeacherName()).isEqualTo("Teacher Seven");
    }

    @Test
    void getStudentCourseRejectsCourseOutsideStudentScope() {
        CourseApplicationService service = new CourseApplicationService(new FakeCourseRepository() {
            @Override
            public List<CourseRecord> findByStudentIdWithSearch(Long studentId, String searchQuery, String category, String status) {
                return List.of();
            }

            @Override
            public Optional<CourseRecord> findById(Long id) {
                CourseRecord record = new CourseRecord();
                record.setId(id);
                record.setCourseName("Operating Systems");
                return Optional.of(record);
            }
        }, new FakeUserFeignClient());

        assertThatThrownBy(() -> service.getStudentCourse(42L, 201L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权访问该课程");
    }

    @Test
    void listClassStudentsEnrichesStudentProfiles() {
        CourseApplicationService service = new CourseApplicationService(
                new FakeCourseRepository() {
                    @Override
                    public List<Map<String, Object>> findStudentsByClassId(Long classId) {
                        return List.of(
                                Map.of("id", 42L),
                                Map.of("id", 43L)
                        );
                    }
                },
                new FakeUserFeignClient()
        );

        List<Map<String, Object>> students = service.listClassStudents(7L, 501L);

        assertThat(students).hasSize(2);
        assertThat(students.get(0))
                .containsEntry("id", 42L)
                .containsEntry("username", "student42")
                .containsEntry("name", "Student Forty Two")
                .containsEntry("email", "student42@example.com")
                .containsEntry("phone", "13800000042");
        assertThat(students.get(1))
                .containsEntry("id", 43L)
                .containsEntry("username", "student43");
    }

    @Test
    void classNameExistsDelegatesToRepository() {
        CourseApplicationService service = new CourseApplicationService(new FakeCourseRepository() {
            @Override
            public boolean classNameExists(String className, Long excludedClassId) {
                return "软件 2301".equals(className) && Long.valueOf(501L).equals(excludedClassId);
            }
        }, new FakeUserFeignClient());

        assertThat(service.classNameExists("软件 2301", 501L)).isTrue();
    }

    @Test
    void getTeacherStudentCombinesProfileAndClassScope() {
        CourseApplicationService service = new CourseApplicationService(new FakeCourseRepository() {
            @Override
            public List<Long> findStudentIdsByTeacherId(Long teacherId) {
                return List.of(42L);
            }
        }, new FakeUserFeignClient());

        Map<String, Object> student = service.getTeacherStudent(7L, 42L);

        assertThat(student)
                .containsEntry("studentId", 42L)
                .containsEntry("realName", "Student Forty Two")
                .containsEntry("username", "student42")
                .containsEntry("className", "软件 2301");
    }

    @Test
    void updateTeacherStudentUpdatesProfileAndClass() {
        FakeCourseRepository repository = new FakeCourseRepository() {
            @Override
            public List<Long> findStudentIdsByTeacherId(Long teacherId) {
                return List.of(42L);
            }
        };
        FakeUserFeignClient userClient = new FakeUserFeignClient();
        CourseApplicationService service = new CourseApplicationService(repository, userClient);

        Map<String, Object> updated = service.updateTeacherStudent(7L, 42L, Map.of(
                "realName", "Student Renamed",
                "email", "renamed@example.com",
                "classId", 501
        ));

        assertThat(updated)
                .containsEntry("studentId", 42L)
                .containsEntry("realName", "Student Renamed")
                .containsEntry("email", "renamed@example.com")
                .containsEntry("className", "班级 501");
        assertThat(repository.deletedStudentClassId).isEqualTo(42L);
        assertThat(repository.insertedClassId).isEqualTo(501L);
        assertThat(repository.insertedStudentId).isEqualTo(42L);
        assertThat(userClient.lastUpdateRequest.getRealName()).isEqualTo("Student Renamed");
    }

    @Test
    void updateTeacherStudentIgnoresAllClassSentinel() {
        FakeCourseRepository repository = new FakeCourseRepository() {
            @Override
            public List<Long> findStudentIdsByTeacherId(Long teacherId) {
                return List.of(42L);
            }
        };
        CourseApplicationService service = new CourseApplicationService(repository, new FakeUserFeignClient());

        Map<String, Object> updated = service.updateTeacherStudent(7L, 42L, Map.of(
                "realName", "Student Renamed",
                "classId", "all"
        ));

        assertThat(updated)
                .containsEntry("studentId", 42L)
                .containsEntry("realName", "Student Renamed")
                .containsEntry("className", "软件 2301");
        assertThat(repository.deletedStudentClassId).isNull();
        assertThat(repository.insertedClassId).isNull();
        assertThat(repository.insertedStudentId).isNull();
    }

    private static class FakeCourseRepository implements CourseRepository {
        Long deletedStudentClassId;
        Long insertedClassId;
        Long insertedStudentId;

        @Override
        public List<CourseRecord> findByTeacherIdWithSearch(Long teacherId, String name, String courseCode, String category, String status) {
            CourseRecord record = new CourseRecord();
            record.setId(101L);
            record.setCourseName("Distributed Systems");
            record.setCourseCode("DS101");
            record.setCredit(3);
            record.setTotalHours(48);
            record.setTeacherId(teacherId);
            return List.of(record);
        }

        @Override
        public Optional<CourseRecord> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public List<CourseRecord> findByStudentIdWithSearch(Long studentId, String searchQuery, String category, String status) {
            return List.of();
        }

        @Override
        public List<CourseRecord> findAllCourses() {
            return List.of();
        }

        @Override
        public CourseRecord insert(CourseRecord course) {
            return course;
        }

        @Override
        public void update(CourseRecord course) {
        }

        @Override
        public void delete(Long id) {
        }

        @Override
        public Map<Long, Integer> countStudentsByCourseIds(List<Long> courseIds) {
            return Map.of(101L, 36);
        }

        @Override
        public List<Map<String, Object>> findStudentsByCourseId(Long courseId) {
            return List.of();
        }

        @Override
        public List<Map<String, Object>> findClassesByTeacherId(Long teacherId, String className, String grade, String majorName, Long majorId, Long courseId) {
            return List.of(
                    row(501L, "软件 2301", "2023", 40, 36, 7L, "教师七", 2L, "软件工程", 101L, "Distributed Systems", "周一 1-2", "A101"),
                    row(501L, "软件 2301", "2023", 40, 36, 7L, "教师七", 2L, "软件工程", 102L, "Algorithms", "周三 3-4", "A102")
            );
        }

        @Override
        public Map<String, Object> findClassById(Long classId) {
            return Map.of();
        }

        @Override
        public Long insertClass(Map<String, Object> classData) {
            return 1L;
        }

        @Override
        public void updateClass(Map<String, Object> classData) {
        }

        @Override
        public void deleteClass(Long classId) {
        }

        @Override
        public List<Map<String, Object>> findClassAssignments(Long teacherId, Long courseId, Long classId) {
            return List.of();
        }

        @Override
        public Long insertClassCourse(Map<String, Object> assignData) {
            return 1L;
        }

        @Override
        public void deleteClassCourse(Long assignmentId) {
        }

        @Override
        public void deleteClassCourse(Long classId, Long courseId) {
        }

        @Override
        public boolean teacherOwnsCourse(Long teacherId, Long courseId) {
            return true;
        }

        @Override
        public boolean teacherCanAccessClass(Long teacherId, Long classId) {
            return true;
        }

        @Override
        public boolean classCourseExists(Long classId, Long courseId) {
            return false;
        }

        @Override
        public List<Map<String, Object>> findMajors() {
            return List.of();
        }

        @Override
        public List<Long> findClassIdsByStudentId(Long studentId) {
            return List.of();
        }

        @Override
        public List<Map<String, Object>> findStudentsByClassId(Long classId) {
            return List.of();
        }

        @Override
        public List<Long> findStudentIdsByTeacherId(Long teacherId) {
            return List.of();
        }

        @Override
        public boolean classNameExists(String className, Long excludedClassId) {
            return false;
        }

        @Override
        public void replaceStudentClass(Long studentId, Long classId) {
            this.deletedStudentClassId = studentId;
            this.insertedClassId = classId;
            this.insertedStudentId = studentId;
        }

        private Map<String, Object> row(Long id, String className, String year, Integer capacity,
                                        Integer studentCount, Long teacherId, String teacherName,
                                        Long majorId, String majorName, Long courseId,
                                        String courseName, String classTime, String classLocation) {
            java.util.Map<String, Object> row = new java.util.HashMap<>();
            row.put("id", id);
            row.put("className", className);
            row.put("year", year);
            row.put("capacity", capacity);
            row.put("studentCount", studentCount);
            row.put("teacherId", teacherId);
            row.put("teacherName", teacherName);
            row.put("majorId", majorId);
            row.put("majorName", majorName);
            row.put("courseId", courseId);
            row.put("courseName", courseName);
            row.put("classTime", classTime);
            row.put("classLocation", classLocation);
            return row;
        }
    }

    private static class FakeTeacherKnowledgePointRepository implements TeacherKnowledgePointRepository {
        private final List<Map<String, Object>> rows = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public List<Map<String, Object>> findByTeacherId(Long teacherId) {
            return List.copyOf(rows);
        }

        @Override
        public List<Map<String, Object>> findByTeacherIdAndCourseId(Long teacherId, Long courseId) {
            return rows.stream()
                    .filter(row -> java.util.Objects.equals(row.get("courseId"), courseId))
                    .toList();
        }

        @Override
        public Optional<Map<String, Object>> findByTeacherIdAndKnowledgePointId(Long teacherId, Long knowledgePointId) {
            return rows.stream()
                    .filter(row -> java.util.Objects.equals(row.get("id"), knowledgePointId))
                    .findFirst();
        }

        @Override
        public Long insert(Map<String, Object> payload) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", nextId++);
            row.put("pointName", payload.get("pointName"));
            row.put("name", payload.get("pointName"));
            row.put("description", payload.get("description"));
            row.put("difficulty", payload.get("difficulty"));
            row.put("orderIndex", payload.get("orderIndex"));
            row.put("courseId", payload.get("courseId"));
            row.put("courseName", "Distributed Systems");
            rows.add(row);
            return (Long) row.get("id");
        }

        @Override
        public void update(Map<String, Object> payload) {
            Map<String, Object> row = findByTeacherIdAndKnowledgePointId(7L, ((Number) payload.get("id")).longValue())
                    .orElseThrow();
            row.put("pointName", payload.get("pointName"));
            row.put("name", payload.get("pointName"));
            row.put("description", payload.get("description"));
            row.put("difficulty", payload.get("difficulty"));
            row.put("orderIndex", payload.get("orderIndex"));
            row.put("courseId", payload.get("courseId"));
        }

        @Override
        public void delete(Long knowledgePointId) {
            rows.removeIf(row -> java.util.Objects.equals(row.get("id"), knowledgePointId));
        }
    }

    private static class FakeUserFeignClient implements UserFeignClient {
        UpdateStudentProfileDTO lastUpdateRequest;

        @Override
        public UserProfileDTO getProfile(Long userId) {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setId(userId);
            dto.setUsername("teacher" + userId);
            dto.setName(userId == 7L ? "Teacher Seven" : "User " + userId);
            return dto;
        }

        @Override
        public UserProfileDTO updateProfile(Long userId, com._202510007517.platform.user.api.dto.UpdateUserProfileDTO request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserProfileDTO getProfileByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getRoles(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com._202510007517.platform.user.api.dto.UserRolesDTO getRolesDetail(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com._202510007517.platform.user.api.dto.StudentProfileDTO getStudentProfile(Long studentId) {
            StudentProfileDTO dto = new StudentProfileDTO();
            dto.setStudentId(studentId);
            dto.setUsername("student" + studentId);
            dto.setRealName(studentId == 42L ? "Student Forty Two" : "Student " + studentId);
            dto.setEmail("student" + studentId + "@example.com");
            dto.setPhone("138000000" + studentId);
            dto.setClassName("软件 2301");
            dto.setRoles(List.of("STUDENT"));
            return dto;
        }

        @Override
        public com._202510007517.platform.user.api.dto.StudentProfileDTO updateStudentProfile(Long studentId, com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO request) {
            this.lastUpdateRequest = request;
            StudentProfileDTO dto = getStudentProfile(studentId);
            if (request.getRealName() != null) {
                dto.setRealName(request.getRealName());
            }
            if (request.getEmail() != null) {
                dto.setEmail(request.getEmail());
            }
            if (request.getPhone() != null) {
                dto.setPhone(request.getPhone());
            }
            if (request.getAvatar() != null) {
                dto.setAvatar(request.getAvatar());
            }
            return dto;
        }

        @Override
        public List<UserProfileDTO> listByIds(List<Long> ids) {
            return ids.stream().map(id -> {
                UserProfileDTO dto = new UserProfileDTO();
                dto.setId(id);
                if (id == 7L) {
                    dto.setUsername("teacher7");
                    dto.setName("Teacher Seven");
                    dto.setEmail("teacher7@example.com");
                    dto.setPhone("13800000007");
                } else {
                    dto.setUsername("student" + id);
                    dto.setName(id == 42L ? "Student Forty Two" : "Student Forty Three");
                    dto.setEmail("student" + id + "@example.com");
                    dto.setPhone("138000000" + id);
                }
                return dto;
            }).toList();
        }
    }
}
