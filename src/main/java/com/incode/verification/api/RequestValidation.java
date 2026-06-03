package com.incode.verification.api;

import java.util.UUID;
import org.springframework.util.StringUtils;

final class RequestValidation {

    private RequestValidation() {}

    static String requireNonBlank(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidRequestException(field + " must not be blank");
        }
        return value;
    }

    static String requireUuid(String value, String field) {
        requireNonBlank(value, field);
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(field + " must be a valid GUID");
        }
    }
}
