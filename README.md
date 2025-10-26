# Spring Boot S3 JSON Reader

A Spring Boot application that reads JSON data from Amazon S3 buckets and exposes it through a REST API.

## Features

- 🚀 Spring Boot 3.2.0 application
- ☁️ AWS S3 integration using AWS SDK v2
- 🔐 Secure credential management using AWS Default Credential Provider Chain
- 🌐 RESTful API endpoint to retrieve JSON data
- ⏰ **Background scheduled loader using ScheduledExecutorService**
- 💾 **In-memory caching of S3 data with thread-safe access**
- 📊 **Monitoring endpoints for scheduler status and cache statistics**
- 📦 Maven-based project structure

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- AWS CLI configured with valid credentials
- Access to an S3 bucket with JSON data

## Configuration

The application uses the AWS Default Credential Provider Chain, which automatically checks for credentials in the following order:

1. Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`)
2. System properties
3. Web Identity Token from AWS STS
4. Credentials file at `~/.aws/credentials`
5. EC2 Instance profile credentials

### Application Properties

Configure the following properties in `src/main/resources/application.yml`:

```yaml
aws:
  s3:
    bucket-name: ${AWS_S3_BUCKET_NAME:your-bucket-name}
    region: ${AWS_S3_REGION:us-east-1}
    json-file-key: ${AWS_S3_JSON_FILE_KEY:data.json}

scheduler:
  s3:
    enabled: ${SCHEDULER_ENABLED:true}
    initial-delay: ${SCHEDULER_INITIAL_DELAY:0}  # milliseconds
    fixed-delay: ${SCHEDULER_FIXED_DELAY:5000}   # milliseconds (5 seconds default)
    thread-pool-size: ${SCHEDULER_THREAD_POOL_SIZE:2}
```

You can override these using environment variables:
- `AWS_S3_BUCKET_NAME` - Your S3 bucket name
- `AWS_S3_REGION` - AWS region (default: us-east-1)
- `AWS_S3_JSON_FILE_KEY` - Path to your JSON file in the bucket
- `SCHEDULER_ENABLED` - Enable/disable the background scheduler (default: true)
- `SCHEDULER_INITIAL_DELAY` - Initial delay before first execution in ms (default: 0)
- `SCHEDULER_FIXED_DELAY` - Delay between executions in ms (default: 5000)
- `SCHEDULER_THREAD_POOL_SIZE` - Thread pool size for scheduler (default: 2)

## Building the Application

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package as JAR
mvn package
```

## Running the Application

### Using Maven
```bash
mvn spring-boot:run
```

### Using JAR
```bash
java -jar target/s3-json-reader-1.0.0.jar
```

The application will start on port 8080 by default.

## API Endpoints

### Original Endpoints

#### Get JSON Data
Retrieves JSON data directly from S3 (not from cache).

```
GET http://localhost:8080/api/json
```

**Response:**
- `200 OK` - Returns the JSON content from S3
- `500 Internal Server Error` - If there's an error accessing S3

### Scheduler Endpoints

#### Get Scheduler Status
```
GET http://localhost:8080/api/scheduler/status
```

Returns the current status of the scheduler and cache statistics.

#### Get Cached Data
```
GET http://localhost:8080/api/scheduler/cached-data
```

Returns the currently cached JSON data (loaded by the background scheduler).

#### Trigger Manual Load
```
POST http://localhost:8080/api/scheduler/trigger-load
```

Manually triggers a data load from S3 to refresh the cache.

#### Clear Cache
```
DELETE http://localhost:8080/api/scheduler/cache
```

Clears the cached data.

#### Health Check
```
GET http://localhost:8080/api/scheduler/health
```

Returns the health status of the scheduler.

**Example Usage:**
```bash
# Get scheduler status
curl -X GET http://localhost:8080/api/scheduler/status

# Get cached data
curl -X GET http://localhost:8080/api/scheduler/cached-data

# Trigger manual load
curl -X POST http://localhost:8080/api/scheduler/trigger-load

# Check health
curl -X GET http://localhost:8080/api/scheduler/health
```

## Project Structure

```
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/s3jsonreader/
│       │       ├── S3JsonReaderApplication.java    # Main application class
│       │       ├── config/
│       │       │   └── AwsS3Config.java           # AWS S3 configuration
│       │       ├── controller/
│       │       │   └── JsonController.java        # REST controller
│       │       └── service/
│       │           └── S3Service.java             # S3 service layer
│       └── resources/
│           └── application.yml                    # Application configuration
├── pom.xml                                        # Maven configuration
└── README.md                                      # This file
```

## Security Considerations

- Never commit AWS credentials to version control
- Use IAM roles with minimal required permissions
- Consider implementing request authentication for production use
- Enable S3 bucket versioning and encryption
- Implement proper error handling to avoid exposing sensitive information

## Troubleshooting

### AWS Credentials Issues
If you encounter authentication errors:
1. Ensure AWS CLI is configured: `aws configure`
2. Verify credentials: `aws s3 ls`
3. Check if using temporary credentials (session token required)

### Connection Issues
- Verify the bucket name and region are correct
- Ensure your AWS credentials have the necessary S3 permissions
- Check network connectivity to AWS

## Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Test
- AWS SDK for Java v2 (S3)
- Jackson for JSON processing

## License

This project is open source and available under the MIT License.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Contact

For questions or support, please open an issue in the GitHub repository.
