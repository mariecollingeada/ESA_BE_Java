# Pet Management API

A Spring Boot REST API for managing pets with user authentication, built with Java 21.

## Features

- **Authentication & Authorization**
  - User registration and login with JWT tokens
  - Password reset via email (SendGrid)
  - Role-based access control (USER, ADMIN)

- **Pet Management**
  - Create, read, update, and delete pets
  - Image uploads via Cloudinary
  - Owner-based access control

## Tech Stack

- **Framework:** Spring Boot 4.0.3
- **Language:** Java 21
- **Database:** PostgreSQL 15
- **Authentication:** Spring Security + JWT
- **Migrations:** Flyway
- **Image Storage:** Cloudinary
- **Email:** SendGrid
- **Testing:** JUnit 5, Testcontainers
- **Containerization:** Docker & Docker Compose

## Prerequisites

- Java 21
- Maven
- Docker & Docker Compose
- PostgreSQL (or use Docker)

## Getting Started

### Environment Variables

Create a `.env` file in the project root:

```env
APP_JWT_SECRET=your-secret-key-at-least-32-characters
SENDGRID_API_KEY=your-sendgrid-api-key
SENDGRID_FROM_EMAIL=noreply@yourdomain.com
CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name
```

### Running with Docker Compose

> **Note:** Due to SSL certificate issues with Maven in Docker, you must build the JAR locally before running Docker Compose.

```bash
# Build the JAR locally first
./mvnw clean package -DskipTests

# Then start all services
docker compose up --build
```

The API will be available at `http://localhost:8080`.

### Running Locally (Development)

1. Start PostgreSQL:
   ```bash
   docker-compose up db
   ```

2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

## API Endpoints

### Authentication (`/auth`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Login and get JWT token |
| GET | `/auth/profile` | Get current user profile |
| POST | `/auth/forgot-password` | Request password reset email |
| POST | `/auth/reset-password` | Reset password with token |

### Pets (`/api/pets`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/pets` | Get all pets (preview) |
| GET | `/api/pets/{id}` | Get pet by ID |
| GET | `/api/pets/my-pets` | Get current user's pets |
| POST | `/api/pets/create` | Create a new pet |
| PUT | `/api/pets/{id}` | Update a pet |
| DELETE | `/api/pets/{id}` | Delete a pet |
| POST | `/api/pets/{id}/image` | Upload pet image |

## Testing

### Run All Tests
```bash
./mvnw test
```

### Test Coverage

Generate coverage report:
```bash
./mvnw test jacoco:report
```

View the coverage report at `target/site/jacoco/index.html`.

Tests use Testcontainers to spin up a PostgreSQL instance automatically.

### Test Cases

#### Unit Tests

| Test Class | Test Cases |
|------------|------------|
| **JwtUtilTest** | Generates valid JWT tokens, extracts username from token, validates correct tokens, rejects invalid/null/empty/malformed/tampered/expired tokens |
| **JwtAuthenticationFilterTest** | Authenticates requests with valid tokens, skips authentication when no token provided, rejects invalid/expired/malformed tokens |
| **AuthServiceTest** | Registers users with encoded passwords, rejects duplicate emails/usernames, validates password matching and length requirements, normalizes email and username, initiates password reset emails, handles missing emails gracefully, resets passwords with valid tokens, rejects invalid/expired/used tokens, validates password policy on reset |
| **AuthControllerTest** | Returns JWT on successful login, returns 401 for invalid credentials, creates users on valid registration, rejects duplicate emails, returns profile for authenticated users |
| **EmailServiceTest** | Sends password reset emails via SendGrid, logs errors when SendGrid fails |
| **PetServiceTest** | Returns pet previews, retrieves pets by ID, throws error for non-existent pets, saves new pets, allows owners to update their pets, prevents non-owners from updating, allows owners to delete their pets, prevents non-owners from deleting, uploads and updates pet images |
| **PetControllerTest** | Returns list of all pets, returns pet by ID, creates pets with 201 status, updates pets, deletes pets with 204 status |

#### Integration Tests

| Test Class | Test Cases |
|------------|------------|
| **AuthIntegrationTest** | Persists registered users to database, returns valid JWT on login, rejects wrong passwords, returns user info for authenticated requests, creates reset tokens on forgot password, updates password with valid reset token, rejects expired reset tokens, rejects already-used reset tokens |
| **PetIntegrationTest** | Persists created pets to database, retrieves pets by ID, returns only the requesting user's pets, allows owners to update pets, prevents non-owners from updating, allows owners to delete pets, prevents non-owners from deleting, persists multiple pets for same user, returns pets in descending order by creation date |
| **SecurityIntegrationTest** | Hashes passwords correctly, verifies correct passwords, rejects incorrect passwords, sets 1-hour expiry on reset tokens, generates unique reset tokens, creates non-expired tokens, creates unused tokens, authenticates valid JWT tokens, loads enabled users by username, returns correct user roles |

## Project Structure

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── auth/           # Authentication & user management
│   │   ├── pets/           # Pet CRUD operations
│   │   ├── security/       # JWT & Spring Security config
│   │   └── config/         # Application configuration
│   └── resources/
│       ├── db/migration/   # Flyway migrations
│       └── application.properties
└── test/                   # Unit & integration tests
```

## License

This project is for demonstration purposes.
