package com._202510007517.major_assignment.client;

import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;

class UserServiceProfileClientTest {

    @Test
    void getStudentProfileReadsResponseResultData() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://user-service.test/api/users/students/42"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 200,
                          "message": "获取学生详情成功",
                          "data": {
                            "studentId": 42,
                            "username": "student42",
                            "realName": "学生四二",
                            "className": "未知班级",
                            "roles": ["STUDENT"]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        UserServiceProfileClient client = new UserServiceProfileClient(restTemplate, "http://user-service.test");

        Optional<StudentProfileDTO> profile = client.getStudentProfile(42L);

        assertThat(profile).isPresent();
        assertThat(profile.get().getStudentId()).isEqualTo(42L);
        assertThat(profile.get().getRealName()).isEqualTo("学生四二");
        assertThat(profile.get().getClassName()).isEqualTo("未知班级");
        assertThat(profile.get().getRoles()).containsExactly("STUDENT");
        server.verify();
    }

    @Test
    void getStudentProfileReturnsEmptyWhenUserServiceDoesNotFindStudent() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://user-service.test/api/users/students/404"))
                .andExpect(method(GET))
                .andRespond(withResourceNotFound());
        UserServiceProfileClient client = new UserServiceProfileClient(restTemplate, "http://user-service.test");

        Optional<StudentProfileDTO> profile = client.getStudentProfile(404L);

        assertThat(profile).isEmpty();
        server.verify();
    }

    @Test
    void getUserProfileReadsResponseResultData() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://user-service.test/api/users/7"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 200,
                          "message": "获取用户成功",
                          "data": {
                            "id": 7,
                            "username": "teacher7",
                            "name": "王老师",
                            "roles": ["TEACHER"]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        UserServiceProfileClient client = new UserServiceProfileClient(restTemplate, "http://user-service.test");

        Optional<UserProfileDTO> profile = client.getUserProfile(7L);

        assertThat(profile).isPresent();
        assertThat(profile.get().getId()).isEqualTo(7L);
        assertThat(profile.get().getName()).isEqualTo("王老师");
        server.verify();
    }

    @Test
    void updateStudentProfileSendsOnlyUserProfileFieldsAndReadsUpdatedProfile() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://user-service.test/api/users/students/42"))
                .andExpect(method(PUT))
                .andExpect(content().json("""
                        {"realName":"新名字","email":"new42@example.com"}
                        """))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 200,
                          "message": "学生信息更新成功",
                          "data": {
                            "studentId": 42,
                            "realName": "新名字",
                            "email": "new42@example.com",
                            "roles": ["STUDENT"]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        UserServiceProfileClient client = new UserServiceProfileClient(restTemplate, "http://user-service.test");
        UpdateStudentProfileDTO request = new UpdateStudentProfileDTO();
        request.setRealName("新名字");
        request.setEmail("new42@example.com");

        Optional<StudentProfileDTO> profile = client.updateStudentProfile(42L, request);

        assertThat(profile).isPresent();
        assertThat(profile.get().getRealName()).isEqualTo("新名字");
        assertThat(profile.get().getEmail()).isEqualTo("new42@example.com");
        server.verify();
    }

    @Test
    void updateUserProfileSendsProfileFieldsAndReadsUpdatedProfile() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://user-service.test/api/users/7"))
                .andExpect(method(PUT))
                .andExpect(content().json("""
                        {"name":"教师新名","phone":"13900000007"}
                        """))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "code": 200,
                          "message": "用户信息更新成功",
                          "data": {
                            "id": 7,
                            "username": "teacher7",
                            "name": "教师新名",
                            "phone": "13900000007",
                            "roles": ["TEACHER"]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        UserServiceProfileClient client = new UserServiceProfileClient(restTemplate, "http://user-service.test");
        UpdateUserProfileDTO request = new UpdateUserProfileDTO();
        request.setName("教师新名");
        request.setPhone("13900000007");

        Optional<UserProfileDTO> profile = client.updateUserProfile(7L, request);

        assertThat(profile).isPresent();
        assertThat(profile.get().getId()).isEqualTo(7L);
        assertThat(profile.get().getName()).isEqualTo("教师新名");
        assertThat(profile.get().getPhone()).isEqualTo("13900000007");
        server.verify();
    }
}
