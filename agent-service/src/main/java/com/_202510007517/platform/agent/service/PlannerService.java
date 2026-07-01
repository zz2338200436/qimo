package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.PlannerDecision;

public interface PlannerService {
    PlannerDecision plan(PlannerContext context);
}
