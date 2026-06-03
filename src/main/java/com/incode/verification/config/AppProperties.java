package com.incode.verification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(ThirdParty thirdParty, Failure failure, Persistence persistence, DynamoDb dynamodb) {

    public record ThirdParty(@DefaultValue("http://localhost:8080") String baseUrl) {}

    public record Failure(
            @DefaultValue("0.40") double freeProbability, @DefaultValue("0.10") double premiumProbability) {}

    public record Persistence(
            @DefaultValue("verification") String tableName, @DefaultValue("true") boolean createTableOnStartup) {}

    public record DynamoDb(
            String endpoint, @DefaultValue("us-east-1") String region, String accessKey, String secretKey) {}
}
