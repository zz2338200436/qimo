package com._202510007517.platform.ai.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class GenerateQuestionsRequestDTO {

    @NotBlank
    private String topic;

    @Min(1)
    @Max(50)
    private Integer count;

    @NotBlank
    private String difficulty;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
