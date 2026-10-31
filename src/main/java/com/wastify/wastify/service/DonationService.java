package com.wastify.wastify.service;
import com.wastify.wastify.model.Donation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import java.time.Instant;
import java.util.*;
@Service
public class DonationService {
  private final DynamoDbClient db; private final String table;
  public DonationService(DynamoDbClient db, @Value("${aws.dynamodb.table:WastifyDonations}") String table){this.db=db;this.table=table;}
  public Donation create(Donation d){
    d.setDonationId(UUID.randomUUID().toString()); d.setStatus("AVAILABLE"); d.setCreatedAt(Instant.now().toString());
    Map<String,AttributeValue> item=new HashMap<>();
    item.put("donationId",s(d.getDonationId())); item.put("foodName",s(d.getFoodName()));
    item.put("restaurantName",s(d.getRestaurantName())); item.put("location",s(d.getLocation()));
    item.put("quantity",s(d.getQuantity())); item.put("expiryDate",s(d.getExpiryDate()));
    item.put("status",s(d.getStatus())); item.put("createdAt",s(d.getCreatedAt()));
    db.putItem(PutItemRequest.builder().tableName(table).item(item).build()); return d;
  }
  public List<Donation> getAll(String status){
    List<Donation> out=new ArrayList<>(); Map<String,AttributeValue> start=null;
    do { ScanRequest.Builder b=ScanRequest.builder().tableName(table); if(start!=null&&!start.isEmpty())b.exclusiveStartKey(start);
      ScanResponse r=db.scan(b.build()); for(Map<String,AttributeValue> item:r.items()){Donation d=convert(item);if(status==null||status.isBlank()||status.equalsIgnoreCase(d.getStatus()))out.add(d);}
      start=r.lastEvaluatedKey();
    } while(start!=null&&!start.isEmpty());
    out.sort(Comparator.comparing(Donation::getCreatedAt,Comparator.nullsLast(Comparator.reverseOrder()))); return out;
  }
  public Donation claim(String id){
    UpdateItemResponse r=db.updateItem(UpdateItemRequest.builder().tableName(table).key(Map.of("donationId",s(id)))
      .updateExpression("SET #st = :claimed").conditionExpression("attribute_exists(donationId) AND #st = :available")
      .expressionAttributeNames(Map.of("#st","status")).expressionAttributeValues(Map.of(":claimed",s("CLAIMED"),":available",s("AVAILABLE")))
      .returnValues(ReturnValue.ALL_NEW).build()); return convert(r.attributes());
  }
  private Donation convert(Map<String,AttributeValue> m){Donation d=new Donation();d.setDonationId(v(m,"donationId"));d.setFoodName(v(m,"foodName"));d.setRestaurantName(v(m,"restaurantName"));d.setLocation(v(m,"location"));d.setQuantity(v(m,"quantity"));d.setExpiryDate(v(m,"expiryDate"));d.setStatus(v(m,"status"));d.setCreatedAt(v(m,"createdAt"));return d;}
  private AttributeValue s(String v){return AttributeValue.builder().s(v==null?"":v).build();}
  private String v(Map<String,AttributeValue> m,String k){AttributeValue a=m.get(k);return a==null?"":a.s();}
}
