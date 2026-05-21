package com._202510007517.platform.assignment.controller;

import com._202510007517.platform.assignment.service.AssignmentOutboxAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssignmentInternalOutboxControllerTest {

    @Test
    void statusReturnsRelayAndCountSnapshot() throws Exception {
        AssignmentOutboxAdminService service = mock(AssignmentOutboxAdminService.class);
        when(service.status()).thenReturn(Map.of(
                "relayJobPresent", true,
                "pendingCount", 1,
                "publishedCount", 2,
                "failedCount", 0
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentInternalOutboxController(service)).build();

        mockMvc.perform(get("/internal/assignments/outbox/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relayJobPresent").value(true))
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.publishedCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(0));
    }

    @Test
    void relayOnceReturnsPublishedCount() throws Exception {
        AssignmentOutboxAdminService service = mock(AssignmentOutboxAdminService.class);
        when(service.relayOnce()).thenReturn(1);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentInternalOutboxController(service)).build();

        mockMvc.perform(post("/internal/assignments/outbox/relay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedCount").value(1));

        verify(service).relayOnce();
    }
}
