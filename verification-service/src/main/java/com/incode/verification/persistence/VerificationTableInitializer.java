package com.incode.verification.persistence;

import com.incode.verification.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Component
public class VerificationTableInitializer {

    private static final Logger log = LoggerFactory.getLogger(VerificationTableInitializer.class);
    private static final String PARTITION_KEY = "verificationId";

    private final DynamoDbClient dynamoDbClient;
    private final AppProperties properties;

    public VerificationTableInitializer(DynamoDbClient dynamoDbClient, AppProperties properties) {
        this.dynamoDbClient = dynamoDbClient;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createTableIfMissing() {
        if (!properties.persistence().createTableOnStartup()) {
            return;
        }
        String tableName = properties.persistence().tableName();
        if (tableExists(tableName)) {
            log.info("DynamoDB table '{}' already exists", tableName);
            return;
        }
        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName(tableName)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(PARTITION_KEY)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName(PARTITION_KEY)
                        .keyType(KeyType.HASH)
                        .build())
                .build());
        dynamoDbClient.waiter().waitUntilTableExists(builder -> builder.tableName(tableName));
        log.info("Created DynamoDB table '{}'", tableName);
    }

    private boolean tableExists(String tableName) {
        try {
            dynamoDbClient.describeTable(
                    DescribeTableRequest.builder().tableName(tableName).build());
            return true;
        } catch (ResourceNotFoundException exception) {
            return false;
        }
    }
}
