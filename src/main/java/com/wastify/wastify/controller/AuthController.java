
package com.wastify.wastify.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String TABLE = "WastifyUsers";

    private final DynamoDbClient dynamoDb;
    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    public AuthController(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    @PostMapping("/signup")
    public Map<String, Object> signup(
            @RequestBody Map<String, String> body) {

        String name = body.get("name");
        String email = normalize(body.get("email"));
        String password = body.get("password");

        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.length() < 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter your name, email, and a password of at least 8 characters");
        }

        Map<String, AttributeValue> item = Map.of(
                "email", AttributeValue.builder().s(email).build(),
                "name", AttributeValue.builder().s(name.trim()).build(),
                "passwordHash",
                AttributeValue.builder().s(encoder.encode(password)).build()
        );

        try {
            dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(TABLE)
                    .item(item)
                    .conditionExpression("attribute_not_exists(email)")
                    .build());

            return Map.of("success", true,
                    "message", "Account created! You can now log in.");

        } catch (ConditionalCheckFailedException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This email is already registered");
        }
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody Map<String, String> body) {

        String email = normalize(body.get("email"));
        String password = body.get("password");

        if (email == null || password == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        }

        Map<String, AttributeValue> item =
                dynamoDb.getItem(GetItemRequest.builder()
                                .tableName(TABLE)
                                .key(Map.of("email",
                                        AttributeValue.builder().s(email).build()))
                                .consistentRead(true)
                                .build())
                        .item();

        if (item == null || !item.containsKey("passwordHash")
                || !encoder.matches(password, item.get("passwordHash").s())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Incorrect email or password");
        }

        return Map.of("success", true, "message", "Login successful");
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
