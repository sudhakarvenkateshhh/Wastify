# Wastify AWS Ready

Complete starter app: Spring Boot REST API plus responsive web UI served by the same app. AWS services: DynamoDB for donation storage, optional SNS notifications, EC2 for hosting, IAM for permissions, and CloudWatch for logs. No AWS keys are stored in source code.

## Open in IntelliJ
1. Extract the ZIP.
2. IntelliJ → File → Open → select the extracted `Wastify-AWS-Ready` folder (the folder containing `pom.xml`).
3. Load it as a Maven project. Set Project SDK and Maven importer JDK to Java 17. Use IntelliJ bundled Maven; keep Maven offline mode disabled.
4. Wait for Maven to download dependencies.
5. Run `src/main/java/com/wastify/wastify/WastifyApplication.java`.

The app starts at http://localhost:8080. The AWS SDK uses its standard credential provider chain. The DynamoDB table must be created before listing or saving donations.

## AWS connection (Mumbai)
Region default: `ap-south-1`. In AWS Console → DynamoDB → Create table:
- Table name: `WastifyDonations`
- Partition key: `donationId`
- Type: String
Choose an option eligible under your AWS account's current free usage. Free-tier eligibility and prices vary by account; check Billing first.

### IAM permissions
For EC2, attach an instance role with permissions on only this table. Example policy (replace account ID):
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["dynamodb:PutItem", "dynamodb:Scan", "dynamodb:UpdateItem"],
    "Resource": "arn:aws:dynamodb:ap-south-1:YOUR_ACCOUNT_ID:table/WastifyDonations"
  }]
}
```
Do not put long-lived AWS access keys in code or on EC2. For local use, configure a secure AWS CLI profile and optionally set `AWS_PROFILE` in IntelliJ's run environment.

### Optional SNS
Create an SNS Standard topic in the same region, subscribe your email and confirm it. Set environment variable `SNS_TOPIC_ARN` to the topic ARN and grant the current identity/EC2 role `sns:Publish` on that topic. Leave it blank to run without notifications.

### Configuration environment variables
- `AWS_REGION` default `ap-south-1`
- `DYNAMODB_TABLE` default `WastifyDonations`
- `SNS_TOPIC_ARN` optional, blank disables notifications
- `PORT` default `8080`

## API
- `GET /api/donations/health`
- `GET /api/donations`
- `GET /api/donations?status=AVAILABLE`
- `POST /api/donations`
- `PATCH /api/donations/{donationId}/claim`

Example POST JSON:
```json
{
  "foodName": "Vegetable Rice",
  "restaurantName": "Green Kitchen",
  "location": "Tirupati, Andhra Pradesh",
  "quantity": "10 meal boxes",
  "expiryDate": "2026-10-20T20:00:00Z"
}
```

## Deploy to EC2 after local test
1. Launch an EC2 instance eligible for your account's free usage; check billing and public IPv4 charges.
2. Attach an EC2 instance role with the DynamoDB policy above (and SNS permission if enabled).
3. For initial testing only, allow SSH from your IP and TCP 8080 from your IP in the security group. Don't expose SSH to everyone. For a public production app, add HTTPS/reverse proxy and authentication.
4. Build the JAR with IntelliJ Maven tool window → Lifecycle → `package`.
5. Copy `target/wastify-1.0.0.jar` to EC2 and run `java -jar wastify-1.0.0.jar`.
6. Open `http://EC2_PUBLIC_IP:8080` while the test security-group rule is enabled.

## Troubleshooting
- `mvn` not recognized in PowerShell: use IntelliJ's bundled Maven or install Maven and configure PATH.
- Red imports: open the folder containing `pom.xml`, reload Maven, use JDK 17 for Maven importer, and check that offline mode is off.
- AWS credentials error: configure a local AWS profile or use the EC2 instance role.
- Table not found: confirm table name and region.
- Access denied: correct IAM permissions.
- This is a learning demo, not production-ready. Add authentication, authorization, HTTPS, rate limiting, and stricter security before public deployment.
- AWS resources can incur charges. Set budget alerts and delete resources you no longer need.
