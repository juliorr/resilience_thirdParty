package com.incode.verification.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(ThirdParty thirdParty, Persistence persistence, DynamoDb dynamodb, Security security) {

    public record ThirdParty(@DefaultValue("http://localhost:8081") String baseUrl) {}

    public record Security(List<String> openPaths) {}

    public record Persistence(
            @DefaultValue("verification") String tableName, @DefaultValue("true") boolean createTableOnStartup) {}

    public record DynamoDb(
            String endpoint, @DefaultValue("us-east-1") String region, String accessKey, String secretKey) {}
}
