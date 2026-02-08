# 📚 Bookshelf API

A modern, secure book-sharing and management platform built with Spring Boot 3, featuring JWT authentication, Google
OAuth integration, and comprehensive rate limiting.

## 🌟 Features

### 📖 Core Functionality

- **Book Management**: Create, read, update, and delete books with detailed metadata
- **Book Sharing**: Share books with other users and manage lending/borrowing workflows
- **Advanced Search**: Find books by title, author, genre, or ISBN
- **Reading Progress**: Track reading status and bookmarks
- **Feedback System**: Rate and review books, plus application feedback

### 🔐 Security & Authentication

- **JWT Authentication**: Secure token-based authentication
- **Google OAuth Integration**: Sign in with Google account
- **Rate Limiting**: Configurable request throttling using Bucket4j
- **Password Encryption**: Secure password hashing with BCrypt
- **Account Verification**: Email-based account activation

### 🛡️ Enterprise Features

- **Property Encryption**: Sensitive configuration encryption with Jasypt
- **Audit Logging**: Track data changes with Spring Data JPA Auditing
- **CORS Configuration**: Configurable cross-origin resource sharing
- **API Documentation**: Interactive OpenAPI/Swagger documentation
- **Docker Support**: Containerized deployment with Docker Compose

## 🏗️ Technical Architecture

### Technology Stack

- **Backend**: Spring Boot 3.5.7, Java 21
- **Database**: PostgreSQL with Spring Data JPA
- **Security**: Spring Security, JWT, Google OAuth2
- **Documentation**: SpringDoc OpenAPI 3
- **Email**: Spring Boot Starter Mail with Thymeleaf templates
- **Rate Limiting**: Bucket4j for request throttling
- **Containerization**: Docker & Docker Compose

### Project Structure

```
src/main/java/com/arturmolla/bookshelf/
├── aspects/           # Rate limiting aspects and annotations
├── config/            # Security, CORS, encryption, and bean configuration
├── controller/        # REST API endpoints
├── model/             # DTOs, entities, and data models
├── repository/        # Data access layer
├── security/          # JWT, Google auth, and security services
└── service/           # Business logic layer
```

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 12+
- Docker & Docker Compose (optional)

### Environment Variables

Set the following environment variables before running the application:

```bash
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
JWT_SECRET_KEY=your-jwt-secret-key-minimum-32-chars
CORS_ALLOWED_ORIGINS=http://localhost:4200
```

### Running with Docker Compose

1. **Start the services:**
   ```bash
   docker-compose up -d
   ```

2. **Access the application:**
    - API Base URL: `http://localhost:8080/api/v1/`
    - API Documentation: `http://localhost:8080/api/v1/swagger-ui.html`
    - Database: `localhost:5433` (postgres/postgres)

### Running Locally

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd bookshelf/api
   ```

2. **Set up PostgreSQL database:**
   ```sql
   CREATE DATABASE bookshelf;
   ```

3. **Configure application properties:**
   ```bash
   cp config/application-example.yml config/application-dev.yml
   # Edit the configuration file with your database and security settings
   ```

4. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

## 📡 API Endpoints

### Authentication

- `POST /auth/register` - Register new user account
- `POST /auth/authenticate` - Login with email/password
- `POST /auth/google` - Google OAuth authentication
- `GET /auth/activate-account` - Activate account via email token

### Books Management

- `GET /books` - Get paginated books list
- `POST /books` - Create new book
- `GET /books/{book-id}` - Get book details
- `PUT /books/{book-id}` - Update book
- `DELETE /books/{book-id}` - Delete book
- `PATCH /books/shareable/{book-id}` - Toggle book sharing

### Book Transactions

- `POST /books/borrow/{book-id}` - Request to borrow book
- `PATCH /books/borrow/return/{book-id}` - Return borrowed book
- `PATCH /books/borrow/return/approve/{book-id}` - Approve book return
- `GET /books/owner` - Get user's owned books
- `GET /books/requested` - Get requested books
- `GET /books/returned` - Get returned books

### Feedback & Reviews

- `POST /feedbacks` - Submit book feedback
- `GET /feedbooks/book/{book-id}` - Get book feedbacks
- `POST /app-feedbacks` - Submit application feedback

### User Profile

- `GET /app-profile/profile` - Get user profile information

## ⚙️ Configuration

### Rate Limiting

Configure request throttling in `application.yml`:

```yaml
rate:
  limit:
    capacity: 30      # Maximum requests
    tokens: 30        # Token refill amount
    minutes: 2        # Refill interval
```

### Security Configuration

JWT and OAuth settings:

```yaml
security:
  jwt:
    token:
      secret-key: ${JWT_SECRET_KEY}
      expiration: 86400000  # 24 hours

google:
  client:
    id: ${GOOGLE_CLIENT_ID}
    secret: ${GOOGLE_CLIENT_SECRET}
```

### External Configuration

For production deployments, use external configuration:

1. **Create config directory:**
   ```bash
   mkdir config
   cp config/application-example.yml config/application-prod.yml
   ```

2. **Run with external config:**
   ```bash
   CONFIG_LOCATION=./config/ ./mvnw spring-boot:run
   ```

## 🔒 Security Features

### Property Encryption

Secure sensitive properties using Jasypt:

1. **Set encryption password:**
   ```bash
   export JASYPT_ENCRYPTOR_PASSWORD=your-secure-password
   ```

2. **Encrypt values:**
   ```bash
   ./mvnw jasypt:encrypt-value -Djasypt.encryptor.password=your-secure-password -Djasypt.plugin.value=valueToEncrypt
   ```

3. **Use encrypted values:**
   ```yaml
   database.password: ENC(encrypted-value-here)
   ```

### Rate Limiting

The application implements sophisticated rate limiting:

- **Global Filter**: Default limits for all endpoints
- **Method-Level**: Custom limits using `@RateLimit` annotation
- **IP-Based**: Per-client IP address throttling
- **Configurable**: Adjust capacity, refill rate, and duration

## 🐳 Docker Deployment

### Development

```bash
docker-compose up -d
```

### Production

```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 📊 Monitoring & Observability

- **Health Checks**: Spring Boot Actuator endpoints
- **API Documentation**: Interactive Swagger UI
- **Request Logging**: Comprehensive request/response logging
- **Error Handling**: Global exception handling with proper HTTP status codes

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔧 Production Best Practices

1. **Secret Management**: Use HashiCorp Vault or cloud provider secret managers
2. **CI/CD**: Inject secrets during deployment, never store in repositories
3. **Secret Rotation**: Implement regular credential rotation
4. **Monitoring**: Set up application and infrastructure monitoring
5. **Backup**: Regular database backups with point-in-time recovery
6. **SSL/TLS**: Use HTTPS in production with proper certificates
