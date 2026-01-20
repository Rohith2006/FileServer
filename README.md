# Mini File Server - Production-Grade CI/CD Pipeline

A production-ready file server with secure CI/CD automation, demonstrating modern DevOps practices including shift-left security, containerization, and Kubernetes deployment.

## 🚀 Application Overview

Mini File Server is a lightweight Java HTTP server that provides file upload/download capabilities with health and version endpoints. Built with production-grade standards including comprehensive testing, security scanning, and automated deployment.

### Features

- **File Upload**: POST `/upload` with `X-Filename` header
- **File Download**: GET `/download?name=<filename>`
- **Health Check**: GET `/health` - Returns HTTP 200 + "OK"
- **Version Info**: GET `/version` - Returns application version

### Technology Stack

- **Language**: Java 17
- **Build Tool**: Maven
- **Testing**: JUnit 5
- **Code Quality**: Checkstyle (Google Java Style)
- **Container**: Docker (multi-stage builds)
- **Orchestration**: Kubernetes
- **CI/CD**: GitHub Actions

---

## 🏗️ CI/CD Architecture

### Pipeline Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      CI PIPELINE (Automated)                 │
├─────────────────────────────────────────────────────────────┤
│  Push to main / Manual Trigger                               │
│    ↓                                                          │
│  1. Checkstyle (Linting) ──→ Prevents technical debt        │
│    ↓                                                          │
│  2. Unit Tests ──→ Prevents regressions                     │
│    ↓                                                          │
│  3. CodeQL (SAST) ──→ Detects OWASP Top 10 issues          │
│    ↓                                                          │
│  4. OWASP Dependency Check ──→ Supply-chain risks           │
│    ↓                                                          │
│  5. Docker Build ──→ Creates container image                │
│    ↓                                                          │
│  6. Trivy Scan ──→ Prevents vulnerable images shipping      │
│    ↓                                                          │
│  7. Smoke Test ──→ Ensures image is runnable                │
│    ↓                                                          │
│  8. Push to DockerHub ──→ Enables CD deployment             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   CD PIPELINE (Manual Trigger)               │
├─────────────────────────────────────────────────────────────┤
│  Workflow Dispatch with image tag                            │
│    ↓                                                          │
│  1. Create Kind Cluster ──→ Local K8s environment           │
│    ↓                                                          │
│  2. Deploy to Kubernetes ──→ Apply manifests                │
│    ↓                                                          │
│  3. Verify Rollout ──→ Wait for healthy pods                │
│    ↓                                                          │
│  4. Health Check ──→ Confirm service availability           │
│    ↓                                                          │
│  5. OWASP ZAP Baseline (DAST) ──→ Runtime security scan     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔒 Security Integration

### Why Each CI Stage Exists

| **Stage** | **Purpose** | **Security Risk Mitigated** | **Fail Fast?** |
|-----------|-------------|----------------------------|----------------|
| **Checkstyle** | Enforces Google Java Style coding standards | Prevents technical debt, improves maintainability | ✅ Yes |
| **Unit Tests** | Validates business logic and endpoint behavior | Prevents regressions, ensures functionality | ✅ Yes |
| **CodeQL SAST** | Static analysis scanning for vulnerabilities | Detects SQL injection, XSS, command injection (OWASP Top 10) | ✅ Yes |
| **Dependency Check** | Scans for known CVEs in dependencies | Identifies supply-chain vulnerabilities | ✅ Yes (CVSS ≥7) |
| **Docker Build** | Creates reproducible container artifacts | Ensures consistent deployments | ✅ Yes |
| **Trivy Scan** | Container image vulnerability scanning | Prevents shipping images with CRITICAL vulnerabilities | ✅ Yes |
| **Smoke Test** | Validates container runs and responds | Ensures image is functional before deployment | ✅ Yes |
| **Push to Registry** | Publishes trusted images to DockerHub | Enables downstream CD pipeline | ✅ Yes |

### CD Security Practices

- **OWASP ZAP**: Dynamic Application Security Testing (DAST) scans the running application for runtime vulnerabilities
- **Non-blocking**: ZAP scan doesn't fail deployment but provides visibility into security posture
- **Health Verification**: Ensures deployed pods are responsive before declaring success

---

## 🔐 Secrets Management

### Required GitHub Secrets

Configure these secrets in your GitHub repository (`Settings > Secrets > Actions`):

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `DOCKERHUB_USERNAME` | DockerHub account username | `myusername` |
| `DOCKERHUB_TOKEN` | DockerHub access token (not password) | Generated from DockerHub Security settings |

### Why Secrets Are Externalized

1. **Security**: Credentials never appear in code or logs
2. **Rotation**: Secrets can be updated without code changes
3. **Compliance**: Meets security compliance requirements (SOC 2, ISO 27001)
4. **Auditability**: GitHub tracks secret access and usage

---

## 💻 Local Development

### Prerequisites

- Java 17
- Maven 3.6+
- Docker (optional, for containerization)

### Build and Run

```bash
# Build the project
mvn clean package

# Run unit tests
mvn test

# Run Checkstyle
mvn checkstyle:check

# Run the application
java -jar target/mini-file-server.jar
```

### Test Endpoints

```bash
# Health check
curl http://localhost:8080/health

# Version check
curl http://localhost:8080/version

# Upload a file
curl -X POST -H "X-Filename: test.txt" --data "Hello World" http://localhost:8080/upload

# Download a file
curl -O http://localhost:8080/download?name=test.txt
```

### Docker Build (Local)

```bash
# Build Docker image
docker build -t mini-file-server:local .

# Run container
docker run -p 8080:8080 mini-file-server:local

# Test health check
curl http://localhost:8080/health
```

---

## 🚢 CI/CD Execution

### Running CI Pipeline

The CI pipeline runs automatically on:
- Every push to `main` branch
- Manual trigger via GitHub Actions UI

```bash
# Trigger manually
# Go to: Actions > CI Pipeline > Run workflow
```

### Running CD Pipeline

The CD pipeline must be triggered manually:

1. Navigate to **Actions > CD Pipeline**
2. Click **Run workflow**
3. Enter the Docker image tag (e.g., `latest` or commit SHA)
4. Click **Run workflow**

The pipeline will:
- Create a Kind Kubernetes cluster
- Deploy the application
- Verify health
- Run OWASP ZAP baseline scan
- Display logs and deployment status

---

## 📊 Pipeline Stages Deep Dive

### CI Pipeline Stages

#### 1. Checkstyle (Linting)
- **Tool**: Maven Checkstyle Plugin with Google Java Style
- **Purpose**: Enforces consistent code style, prevents technical debt
- **Outcome**: Build fails on style violations

#### 2. Unit Tests
- **Tool**: JUnit 5 + Maven Surefire
- **Purpose**: Validates business logic, prevents regressions
- **Coverage**: Health, Version, Upload, Download endpoints
- **Outcome**: Build fails on test failures

#### 3. CodeQL (SAST)
- **Tool**: GitHub CodeQL
- **Purpose**: Static analysis for security vulnerabilities
- **Detects**: SQL injection, XSS, command injection, path traversal
- **Outcome**: Fails on HIGH/CRITICAL findings

#### 4. OWASP Dependency Check
- **Tool**: Dependency Check Action
- **Purpose**: Identifies vulnerable dependencies (supply-chain security)
- **Threshold**: Fails on CVSS score ≥ 7 (HIGH or CRITICAL)
- **Report**: HTML report uploaded as artifact

#### 5. Docker Build
- **Tool**: Docker Buildx
- **Purpose**: Multi-stage build with layer caching
- **Stages**: Maven build → Eclipse Temurin JRE runtime
- **Optimization**: Uses GitHub Actions cache for faster builds

#### 6. Trivy Scan
- **Tool**: Trivy vulnerability scanner
- **Purpose**: Scans Docker images for OS and library vulnerabilities
- **Threshold**: Fails on CRITICAL vulnerabilities
- **Report**: SARIF report uploaded to GitHub Security tab

#### 7. Container Smoke Test
- **Tool**: Docker + curl
- **Purpose**: Validates container starts and responds correctly
- **Tests**: 
  - Health endpoint returns HTTP 200
  - Version endpoint returns valid version string
- **Outcome**: Fails if container is not functional

#### 8. Push to DockerHub
- **Tool**: Docker CLI
- **Purpose**: Publishes trusted images to registry
- **Tags**: 
  - `latest` - always points to most recent build
  - `<commit-sha>` - immutable tag for specific commit
- **Authentication**: Uses GitHub Secrets for secure login

### CD Pipeline Stages

#### 1. Kind Cluster Setup
- **Tool**: kind (Kubernetes in Docker)
- **Purpose**: Creates local Kubernetes cluster for testing
- **Benefit**: No external dependencies, fully reproducible

#### 2. Deploy to Kubernetes
- **Manifests**: Deployment + Service YAML
- **Security**: Non-root user, resource limits, liveness/readiness probes
- **Strategy**: RollingUpdate with 2 replicas

#### 3. Verify Rollout
- **Tool**: `kubectl rollout status`
- **Purpose**: Waits for pods to be healthy before proceeding
- **Timeout**: 300 seconds

#### 4. Health Check
- **Tool**: Port-forward + curl
- **Purpose**: Confirms service is responding correctly
- **Outcome**: Fails if health endpoint unreachable

#### 5. OWASP ZAP Baseline (DAST)
- **Tool**: OWASP ZAP baseline scanner
- **Purpose**: Dynamic security testing on running application
- **Mode**: Non-blocking (reports findings but doesn't fail)
- **Report**: HTML report uploaded as artifact

---

## 📁 Project Structure

```
FileServer/
├── .github/
│   └── workflows/
│       ├── ci.yml              # CI pipeline definition
│       └── cd.yml              # CD pipeline definition
├── k8s/
│   ├── deployment.yaml         # Kubernetes Deployment manifest
│   └── service.yaml            # Kubernetes Service manifest
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/fileserver/
│   │           └── MiniFileServer.java
│   └── test/
│       └── java/
│           └── com/fileserver/
│               └── MiniFileServerTest.java
├── storage/                    # File storage directory (gitignored)
├── Dockerfile                  # Multi-stage Docker build
├── pom.xml                     # Maven project configuration
├── README.md                   # This file
└── LICENSE                     # Project license
```

---

## 🎯 Known Limitations

1. **Storage**: Files stored in ephemeral storage (not persisted across container restarts)
   - **Mitigation**: Use PersistentVolumes in production Kubernetes

2. **Authentication**: No authentication/authorization on endpoints
   - **Mitigation**: Add OAuth2/JWT in production

3. **Rate Limiting**: No protection against abuse
   - **Mitigation**: Implement rate limiting middleware

4. **File Size Limits**: No upload size restrictions
   - **Mitigation**: Add request size limits in configuration

5. **Horizontal Scaling**: Upload/download inconsistent across replicas
   - **Mitigation**: Use shared storage (S3, NFS) for multi-pod deployments

---

## 🔮 Future Improvements

### Security
- [ ] Add mTLS between services
- [ ] Implement API authentication (JWT)
- [ ] Add rate limiting and DDoS protection
- [ ] Enable network policies in Kubernetes

### Observability
- [ ] Add structured logging (JSON logs)
- [ ] Integrate Prometheus metrics
- [ ] Add distributed tracing (OpenTelemetry)
- [ ] Set up Grafana dashboards

### Infrastructure
- [ ] Deploy to production Kubernetes (EKS/GKE/AKS)
- [ ] Add Helm charts for easier deployment
- [ ] Implement GitOps with ArgoCD
- [ ] Add blue-green or canary deployments

### Testing
- [ ] Add integration tests
- [ ] Implement contract testing
- [ ] Add performance/load testing
- [ ] Increase code coverage to 90%+

---

## 📝 License

This project is licensed under the terms specified in the LICENSE file.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

All PRs must pass CI checks (Checkstyle, tests, security scans) before merging.

---

## 📞 Support

For issues or questions:
- Open a GitHub Issue
- Review CI/CD logs in Actions tab
- Check container logs: `kubectl logs -l app=mini-file-server`

---

**Built with ❤️ for Production DevOps**