# CI/CD Setup Guide

This project includes two GitHub Actions workflows for CI/CD:

## Workflows

### 1. Basic CI (`basic-ci.yml`)

A simple workflow that runs on every push and pull request:

- ✅ Runs tests
- 🔨 Builds the application
- 📦 Uploads JAR artifacts

**Triggers:** Push to any branch, PRs to main/develop

### 2. Full CI/CD Pipeline (`ci-cd.yml`)

A comprehensive pipeline with testing, building, and deployment:

- 🧪 **Test Stage**: Runs unit tests with reporting
- 🔨 **Build Stage**: Compiles, packages, and creates Docker images
- 🚀 **Deploy Stages**: Separate staging and production deployments

**Triggers:**

- Push to `main` → Production deployment
- Push to `develop` → Staging deployment
- PRs to `main` → Test only

## Setup Instructions

### 1. Repository Secrets

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

```
DOCKER_USERNAME          # Your Docker Hub username
DOCKER_PASSWORD          # Your Docker Hub password/token
```

### 2. Environment Configuration

For deployment stages, configure environments in GitHub:

- Go to Settings → Environments
- Create `staging` and `production` environments
- Add protection rules as needed

### 3. Application Configuration

Ensure your Spring Boot application includes:

- Spring Boot Actuator for health checks
- Proper test coverage
- Environment-specific configuration files

### 4. Docker Deployment

The pipeline builds and pushes Docker images to Docker Hub. The Dockerfile:

- Uses multi-stage build for optimization
- Runs as non-root user for security
- Includes health checks
- Optimized for Spring Boot applications

## Usage

### Running Locally

```bash
# Build and test
./mvnw clean test

# Build Docker image
docker build -t bookshelf-api .

# Run with Docker Compose
docker-compose up
```

### Customizing Deployment

Update the deployment steps in `ci-cd.yml` based on your target platform:

- **Kubernetes**: Add kubectl commands
- **AWS**: Use AWS CLI or CodeDeploy
- **Heroku**: Use Heroku CLI
- **Digital Ocean**: Use doctl

### Branch Strategy

- `main` → Production deployments
- `develop` → Staging deployments
- `feature/*` → Testing only

## Files Added

- `.github/workflows/ci-cd.yml` - Full CI/CD pipeline
- `.github/workflows/basic-ci.yml` - Simple CI workflow
- `Dockerfile` - Container configuration
- `.dockerignore` - Docker build optimization

## Next Steps

1. Push to GitHub to trigger the workflows
2. Configure your deployment targets
3. Add integration tests
4. Set up monitoring and alerting
5. Configure database migrations
6. Add security scanning
