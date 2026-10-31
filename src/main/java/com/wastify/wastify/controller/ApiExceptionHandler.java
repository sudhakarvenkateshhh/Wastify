package com.wastify.wastify.controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import java.util.Map;
import java.util.stream.Collectors;
@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException e){
    String msg=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(Map.of("error",msg));
  }
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String,String>> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason()==null?"Request failed":e.getReason()));}
  @ExceptionHandler(DynamoDbException.class)
  public ResponseEntity<Map<String,String>> dynamo(DynamoDbException e){System.err.println("DynamoDB error: "+e.getMessage());return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error","DynamoDB request failed. Check table, region and IAM permissions."));}
  @ExceptionHandler(SdkClientException.class)
  public ResponseEntity<Map<String,String>> credentials(SdkClientException e){System.err.println("AWS client error: "+e.getMessage());return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error","AWS credentials/connection unavailable. Check AWS setup."));}
}
