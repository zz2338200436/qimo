package com._202510007517.platform.events;

import java.util.Objects;

public record EventAggregate(String type, String id) {

    public EventAggregate {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(id, "id must not be null");
    }
}
