package com.incode.thirdparty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Failure failure) {

    public record Failure(
            @DefaultValue("0.40") double freeProbability, @DefaultValue("0.10") double premiumProbability) {}
}
