package com._202510007517.major_assignment.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthServiceClientTest {

    @Test
    void changePasswordPostsToAuthServiceInternalEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://auth-service.test/internal/auth/users/42/change-password"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"currentPassword":"oldPass1","newPassword":"newPass2"}
                        """))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        AuthServiceClient client = new AuthServiceClient(restTemplate, "http://auth-service.test");

        boolean changed = client.changePassword(42L, "oldPass1", "newPass2");

        assertThat(changed).isTrue();
        server.verify();
    }
}
