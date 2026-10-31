package com.wastify.wastify.controller;
import com.wastify.wastify.model.Donation;
import com.wastify.wastify.service.DonationService;
import com.wastify.wastify.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.core.exception.SdkException;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/donations")
public class DonationController {
  private final DonationService service; private final NotificationService notifications;
  public DonationController(DonationService service,NotificationService notifications){this.service=service;this.notifications=notifications;}
  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public Donation create(@Valid @RequestBody Donation d){
    Donation saved=service.create(d);
    try{notifications.notifyDonation("New food donation available","Food: "+saved.getFoodName()+"\nDonor: "+saved.getRestaurantName()+"\nLocation: "+safe(saved.getLocation())+"\nQuantity: "+safe(saved.getQuantity())+"\nID: "+saved.getDonationId());}
    catch(SdkException e){System.err.println("SNS notification failed: "+e.getMessage());}
    return saved;
  }
  @GetMapping public List<Donation> all(@RequestParam(required=false) String status){return service.getAll(status);}
  @PatchMapping("/{id}/claim") public Donation claim(@PathVariable String id){
    try{return service.claim(id);}
    catch(ConditionalCheckFailedException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"Donation does not exist or was already claimed");}
    catch(ResourceNotFoundException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"DynamoDB table not found. Check table name and region.");}
  }
  @GetMapping("/health") public Map<String,String> health(){return Map.of("status","UP","application","Wastify");}
  private String safe(String s){return s==null||s.isBlank()?"Not provided":s;}
}
