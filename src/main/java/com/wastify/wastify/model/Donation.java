package com.wastify.wastify.model;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public class Donation {
  private String donationId;
  @NotBlank(message="Food name is required") @Size(max=100) private String foodName;
  @NotBlank(message="Donor name is required") @Size(max=100) private String restaurantName;
  @Size(max=160) private String location;
  @Size(max=80) private String quantity;
  @Size(max=80) private String expiryDate;
  private String status;
  private String createdAt;
  public Donation() {}
  public String getDonationId(){return donationId;} public void setDonationId(String v){donationId=v;}
  public String getFoodName(){return foodName;} public void setFoodName(String v){foodName=v;}
  public String getRestaurantName(){return restaurantName;} public void setRestaurantName(String v){restaurantName=v;}
  public String getLocation(){return location;} public void setLocation(String v){location=v;}
  public String getQuantity(){return quantity;} public void setQuantity(String v){quantity=v;}
  public String getExpiryDate(){return expiryDate;} public void setExpiryDate(String v){expiryDate=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public String getCreatedAt(){return createdAt;} public void setCreatedAt(String v){createdAt=v;}
}
