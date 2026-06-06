package com.incode.verification.persistence;

import com.incode.verification.config.AppProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;

@Component("dynamoDb")
public class DynamoDbHealthIndicator implements HealthIndicator {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public DynamoDbHealthIndicator(DynamoDbClient dynamoDbClient, AppProperties properties) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = properties.persistence().tableName();
    }

    @Override
    public Health health() {
        try {
            String status = dynamoDbClient
                    .describeTable(
                            DescribeTableRequest.builder().tableName(tableName).build())
                    .table()
                    .tableStatusAsString();
            return Health.up()
                    .withDetail("table", tableName)
                    .withDetail("status", status)
                    .build();
        } catch (RuntimeException exception) {
            return Health.down(exception).withDetail("table", tableName).build();
        }
    }
}
