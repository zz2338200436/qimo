package com._202510007517.platform.events;

import com._202510007517.platform.events.assignment.AssignmentSubmittedEvent;
import com._202510007517.platform.events.assignment.AssignmentSubmittedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainEventSerializationTest {

    @Test
    void event_records_round_trip_as_json_with_envelope_and_payload() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Instant occurredAt = Instant.parse("2026-05-13T00:00:00Z");
        AssignmentSubmittedEvent event = new AssignmentSubmittedEvent("evt-1", occurredAt,
                new EventAggregate("assignment", "assignment-42"),
                new AssignmentSubmittedPayload(42L, 7L, 1001L, 5L, 9L, false, occurredAt));

        String json = objectMapper.writeValueAsString(event);
        AssignmentSubmittedEvent restored = objectMapper.readValue(json, AssignmentSubmittedEvent.class);

        assertTrue(json.contains("\"eventId\""));
        assertTrue(json.contains("\"occurredAt\""));
        assertTrue(json.contains("\"aggregate\""));
        assertTrue(json.contains("\"payload\""));
        assertEquals(event, restored);
    }
}
