
package com.wastify.wastify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AwsConfig {

  @Bean
  public Region awsRegion(
          @Value("${aws.region:ap-southeast-2}") String region) {
    return Region.of(region);
  }

  @Bean
  public DynamoDbClient dynamoDbClient(Region region) {
    return DynamoDbClient.builder()
            .region(region)
            .build();
  }

  @Bean
  public SnsClient snsClient(Region region) {
    return SnsClient.builder()
            .region(region)
            .build();
  }
}
