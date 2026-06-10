Document Management System (Backend)
This project is forked from a private repositories: https://github.com/Manifest-cnpmnc/DM_FE and https://github.com/Manifest-cnpmnc/DM_BE
# Document Management API (Spring Boot)

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL
- (Optional) Docker 24+ and Docker Compose

## Environment Variables

Required for local and production runs. Copy `.env.example` to `.env` and fill values accordingly.

```ini
DB_URL=jdbc:postgresql://localhost:5432/your_db?sslmode=require
DB_USERNAME=your_username
DB_PASSWORD=your_password
PORT=8080
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:*,http://127.0.0.1:*
JPA_DDL_AUTO=update
JPA_SHOW_SQL=false
JWT_SECRET=your_64_char_hex_secret_here
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
ADMIN_SEED_ENABLED=false
ADMIN_SEED_EMAIL=admin@example.com
ADMIN_SEED_PASSWORD=change_this_to_a_strong_password
ADMIN_SEED_FULL_NAME=System Administrator
```

## Run Locally

```bash
mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Run with Docker

```bash
docker build -t document-management-api .
docker run -p 8080:8080 --env-file .env document-management-api
```

## Docker Image

The Dockerfile uses a multi-stage build:
1. `build` stage: Maven Temurin 21 image for compiling and packaging.
2. `runtime` stage: Eclipse Temurin 21 JRE Alpine for the final container.

Non-root user `suser` runs the app for improved security.

### Security Notes

- Do not expose `ADMIN_SEED_ENABLED=true` in production.
- Set `JPA_DDL_AUTO=validate` or `none` in production.
- Restrict `/swagger-ui/**` outside dev environments.

## CI/CD (GitHub Actions)

The workflow at `.github/workflows/ci.yml` does the following:

1. **Test** branch: runs `mvn verify`.
2. **Push to main**: builds and pushes a container image to GitHub Container Registry (`ghcr.io`) with tags:
   - `sha-<short commit>`
   - `latest`

Image name is always: `ghcr.io/<OWNER>/<REPO>`.

### Permissions

The workflow uses `GITHUB_TOKEN` for package write access to GitHub Container Registry. No extra secrets are required for push-to-registry.

### Manual Deploy

After CI pushes the image, deploy it to your platform using:

```bash
docker run -p 8080:8080 --env-file .env ghcr.io/<OWNER>/<REPO>:latest
```

Or use your hosting platform's integration with GHCR (Render, Fly.io, AWS ECS, etc.).

## Build & Test

```bash
mvn clean package -DskipTests
mvn test
```
