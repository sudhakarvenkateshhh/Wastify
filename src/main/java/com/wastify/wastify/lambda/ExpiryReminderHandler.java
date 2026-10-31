package com.wastify.wastify.lambda;



import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ExpiryReminderHandler
        implements RequestHandler<Map<String, Object>, String> {

    private static final String TABLE_NAME =
            System.getenv().getOrDefault(
                    "TABLE_NAME",
                    "WastifyDonations"
            );

    private static final DynamoDbClient DYNAMO =
            DynamoDbClient.builder()
                    .region(Region.AP_SOUTHEAST_2)
                    .build();

    private static final Duration REMINDER_WINDOW =
            Duration.ofHours(24);

    @Override
    public String handleRequest(
            Map<String, Object> event,
            Context context
    ) {

        Instant now = Instant.now();
        Instant reminderLimit = now.plus(REMINDER_WINDOW);

        int remindersCreated = 0;
        int donationsExpired = 0;
        int invalidDates = 0;

        Map<String, AttributeValue> lastKey = null;

        context.getLogger().log(
                "Starting Wastify donation expiry check"
        );

        do {

            ScanRequest.Builder scanBuilder =
                    ScanRequest.builder()
                            .tableName(TABLE_NAME)
                            .filterExpression("#status = :available")
                            .expressionAttributeNames(
                                    Map.of("#status", "status")
                            )
                            .expressionAttributeValues(
                                    Map.of(
                                            ":available",
                                            AttributeValue.builder()
                                                    .s("AVAILABLE")
                                                    .build()
                                    )
                            );

            if (lastKey != null && !lastKey.isEmpty()) {
                scanBuilder.exclusiveStartKey(lastKey);
            }

            ScanResponse response =
                    DYNAMO.scan(scanBuilder.build());

            for (Map<String, AttributeValue> item :
                    response.items()) {

                AttributeValue idAttribute = item.get("donationId");
                AttributeValue expiryAttribute = item.get("expiryDate");

                if (idAttribute == null ||
                        idAttribute.s() == null ||
                        expiryAttribute == null ||
                        expiryAttribute.s() == null) {

                    invalidDates++;
                    continue;
                }

                String donationId = idAttribute.s();
                String expiryString = expiryAttribute.s();

                Instant expiry;

                try {
                    expiry = Instant.parse(expiryString);

                } catch (Exception e) {
                    invalidDates++;

                    context.getLogger().log(
                            "Invalid expiry date: " + donationId
                    );

                    continue;
                }

                if (!expiry.isAfter(now)) {

                    expireDonation(donationId, now);

                    donationsExpired++;

                    context.getLogger().log(
                            "Expired donation: " + donationId
                    );

                } else if (!expiry.isAfter(reminderLimit)) {

                    if (createReminder(donationId, now)) {

                        remindersCreated++;

                        context.getLogger().log(
                                "Reminder created: " + donationId
                        );
                    }
                }
            }

            lastKey = response.lastEvaluatedKey();

        } while (lastKey != null && !lastKey.isEmpty());

        String result =
                "Wastify expiry check completed. " +
                        "Reminders created: " + remindersCreated +
                        ", Donations expired: " + donationsExpired +
                        ", Invalid dates: " + invalidDates;

        context.getLogger().log(result);

        return result;
    }

    private void expireDonation(
            String donationId,
            Instant now
    ) {

        Map<String, AttributeValue> key =
                Map.of(
                        "donationId",
                        AttributeValue.builder()
                                .s(donationId)
                                .build()
                );

        Map<String, AttributeValue> values =
                new HashMap<>();

        values.put(
                ":expired",
                AttributeValue.builder()
                        .s("EXPIRED")
                        .build()
        );

        values.put(
                ":available",
                AttributeValue.builder()
                        .s("AVAILABLE")
                        .build()
        );

        values.put(
                ":now",
                AttributeValue.builder()
                        .s(now.toString())
                        .build()
        );

        UpdateItemRequest request =
                UpdateItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .key(key)
                        .updateExpression(
                                "SET #status = :expired, " +
                                        "expiryProcessedAt = :now"
                        )
                        .conditionExpression(
                                "#status = :available"
                        )
                        .expressionAttributeNames(
                                Map.of("#status", "status")
                        )
                        .expressionAttributeValues(values)
                        .build();

        try {
            DYNAMO.updateItem(request);

        } catch (ConditionalCheckFailedException e) {

            // Donation was changed by another process.
        }
    }

    private boolean createReminder(
            String donationId,
            Instant now
    ) {

        Map<String, AttributeValue> key =
                Map.of(
                        "donationId",
                        AttributeValue.builder()
                                .s(donationId)
                                .build()
                );

        Map<String, AttributeValue> values =
                new HashMap<>();

        values.put(
                ":true",
                AttributeValue.builder()
                        .bool(true)
                        .build()
        );

        values.put(
                ":now",
                AttributeValue.builder()
                        .s(now.toString())
                        .build()
        );

        values.put(
                ":available",
                AttributeValue.builder()
                        .s("AVAILABLE")
                        .build()
        );

        UpdateItemRequest request =
                UpdateItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .key(key)
                        .updateExpression(
                                "SET expiryReminderDue = :true, " +
                                        "expiryReminderAt = :now"
                        )
                        .conditionExpression(
                                "#status = :available AND " +
                                        "attribute_not_exists(expiryReminderDue)"
                        )
                        .expressionAttributeNames(
                                Map.of("#status", "status")
                        )
                        .expressionAttributeValues(values)
                        .build();

        try {
            DYNAMO.updateItem(request);
            return true;

        } catch (ConditionalCheckFailedException e) {

            // Reminder already exists or donation is no longer available.
            return false;
        }
    }
}
