package com.incode.verification.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.domain.VerificationResult;
import com.incode.verification.domain.VerificationResult.Match;
import com.incode.verification.domain.VerificationResult.NoResults;
import com.incode.verification.domain.VerificationResult.ThirdPartiesDown;
import org.springframework.stereotype.Component;

@Component
public class VerificationResultCodec {

    static final String TYPE_MATCH = "MATCH";
    static final String TYPE_NO_RESULTS = "NO_RESULTS";
    static final String TYPE_THIRD_PARTIES_DOWN = "THIRD_PARTIES_DOWN";

    private final ObjectMapper objectMapper;

    public VerificationResultCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String type(VerificationResult result) {
        return switch (result) {
            case Match ignored -> TYPE_MATCH;
            case NoResults ignored -> TYPE_NO_RESULTS;
            case ThirdPartiesDown ignored -> TYPE_THIRD_PARTIES_DOWN;
        };
    }

    public String toJson(VerificationResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize verification result", exception);
        }
    }

    public VerificationResult fromJson(String type, String json) {
        try {
            return switch (type) {
                case TYPE_MATCH -> objectMapper.readValue(json, Match.class);
                case TYPE_NO_RESULTS -> new NoResults();
                case TYPE_THIRD_PARTIES_DOWN -> new ThirdPartiesDown();
                default -> throw new IllegalStateException("Unknown verification result type: " + type);
            };
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize verification result", exception);
        }
    }
}
