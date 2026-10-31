package com.wastify.wastify.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
@Service
public class NotificationService {
  private final SnsClient sns; private final String topicArn;
  public NotificationService(SnsClient sns,@Value("${aws.sns.topic-arn:}") String topicArn){this.sns=sns;this.topicArn=topicArn==null?"":topicArn.trim();}
  public void notifyDonation(String subject,String message){
    if(topicArn.isBlank())return;
    sns.publish(PublishRequest.builder().topicArn(topicArn).subject(subject.length()>100?subject.substring(0,100):subject).message(message).build());
  }
}
