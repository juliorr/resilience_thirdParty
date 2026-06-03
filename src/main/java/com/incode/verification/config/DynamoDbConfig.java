package com.incode.verification.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(AppProperties properties) {
        AppProperties.DynamoDb config = properties.dynamodb();
        DynamoDbClientBuilderCustomizer builder = new DynamoDbClientBuilderCustomizer(config);
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    private record DynamoDbClientBuilderCustomizer(AppProperties.DynamoDb config) {

        DynamoDbClient build() {
            var builder = DynamoDbClient.builder().region(Region.of(config.region()));
            if (StringUtils.hasText(config.endpoint())) {
                builder.endpointOverride(URI.create(config.endpoint()));
            }
            if (StringUtils.hasText(config.accessKey()) && StringUtils.hasText(config.secretKey())) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKey(), config.secretKey())));
            } else {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            }
            return builder.build();
        }
    }
}
