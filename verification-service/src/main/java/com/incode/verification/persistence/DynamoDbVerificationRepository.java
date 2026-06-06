package com.incode.verification.persistence;

import com.incode.verification.config.AppProperties;
import com.incode.verification.domain.Source;
import com.incode.verification.domain.VerificationRecord;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
public class DynamoDbVerificationRepository implements VerificationRepository {

    private final DynamoDbTable<VerificationItem> table;
    private final VerificationResultCodec codec;

    public DynamoDbVerificationRepository(
            DynamoDbEnhancedClient enhancedClient, AppProperties properties, VerificationResultCodec codec) {
        this.table = enhancedClient.table(
                properties.persistence().tableName(), TableSchema.fromBean(VerificationItem.class));
        this.codec = codec;
    }

    @Override
    public void save(VerificationRecord record) {
        VerificationItem item = toItem(record);
        Expression notExists = Expression.builder()
                .expression("attribute_not_exists(verificationId)")
                .build();
        try {
            table.putItem(PutItemEnhancedRequest.builder(VerificationItem.class)
                    .item(item)
                    .conditionExpression(notExists)
                    .build());
        } catch (ConditionalCheckFailedException exception) {
            throw new DuplicateVerificationException(record.verificationId());
        }
    }

    @Override
    public Optional<VerificationRecord> findById(String verificationId) {
        VerificationItem item =
                table.getItem(Key.builder().partitionValue(verificationId).build());
        return Optional.ofNullable(item).map(this::toRecord);
    }

    private VerificationItem toItem(VerificationRecord record) {
        VerificationItem item = new VerificationItem();
        item.setVerificationId(record.verificationId());
        item.setQueryText(record.queryText());
        item.setTimestamp(record.timestamp().toString());
        item.setSource(record.source() == null ? null : record.source().name());
        item.setResultType(codec.type(record.result()));
        item.setResultJson(codec.toJson(record.result()));
        return item;
    }

    private VerificationRecord toRecord(VerificationItem item) {
        return new VerificationRecord(
                item.getVerificationId(),
                item.getQueryText(),
                Instant.parse(item.getTimestamp()),
                item.getSource() == null ? null : Source.valueOf(item.getSource()),
                codec.fromJson(item.getResultType(), item.getResultJson()));
    }
}
