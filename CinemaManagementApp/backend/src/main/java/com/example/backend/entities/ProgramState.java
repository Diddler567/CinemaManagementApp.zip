package com.example.backend.entities;

import java.util.Set;

public enum ProgramState {
    CREATED,
    SUBMISSION,
    ASSIGNMENT,
    REVIEW,
    SCHEDULING,
    FINAL_PUBLICATION,
    DECISION,
    ANNOUNCED;

    //ACCEPT BOTH SPEC NAME + YOUR OLD NAME
    public static ProgramState fromString(String raw) {
        if (raw == null) throw new IllegalArgumentException("ProgramState is required");
        String v = raw.trim().toUpperCase();

        //BACKWARD COMPATIBILITY: OLD NAME -> NEW NAME
        if ("FINAL_SUBMISSION".equals(v)) v = "FINAL_PUBLICATION";

        return ProgramState.valueOf(v);
    }

    public boolean canTransitionTo(ProgramState next) {
        return switch (this) {
            case CREATED -> Set.of(SUBMISSION).contains(next);
            case SUBMISSION -> Set.of(ASSIGNMENT).contains(next);
            case ASSIGNMENT -> Set.of(REVIEW).contains(next);
            case REVIEW -> Set.of(SCHEDULING).contains(next);
            case SCHEDULING -> Set.of(FINAL_PUBLICATION).contains(next);
            case FINAL_PUBLICATION -> Set.of(DECISION).contains(next);
            case DECISION -> Set.of(ANNOUNCED).contains(next);
            case ANNOUNCED -> Set.of().contains(next);
        };
    }
}
