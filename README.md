# SITMUN MBTiles Service

A Spring Boot microservice for generating MBTiles (MapBox Tiles) from WMTS map services. This service is part of the [SITMUN](https://sitmun.github.io/) geospatial platform ecosystem.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
  - [Prerequisites](#prerequisites)
  - [Local Development](#local-development)
  - [Troubleshooting bootRun](#troubleshooting-bootrun)
  - [Building](#building)
- [Features](#features)
  - [Core Functionality](#core-functionality)
  - [Performance Optimizations](#performance-optimizations)
  - [Development Features](#development-features)
- [API Reference](#api-reference)
  - [Endpoints](#endpoints)
  - [Usage Examples](#usage-examples)
  - [Data Models](#data-models)
- [Configuration](#configuration)
  - [Profiles](#profiles)
  - [Configuration Files](#configuration-files)
  - [Base Configuration](#base-configuration)
  - [Configuration Options](#configuration-options)
- [Architecture](#architecture)
  - [Technology Stack](#technology-stack)
  - [System Architecture](#system-architecture)
  - [Key Components](#key-components)
  - [Processing Flow](#processing-flow)
  - [Strategy Pattern](#strategy-pattern)
  - [Extensibility](#extensibility)
  - [Error Handling](#error-handling)
- [Development](#development)
  - [Profiles Explained](#profiles-explained)
  - [Deployment](#deployment)
  - [Project Structure](#project-structure)
  - [Build System](#build-system)
  - [Code Quality](#code-quality)
  - [Running Quality Checks](#running-quality-checks)
  - [Version Management](#version-management)
  - [Testing](#testing)
  - [Development Workflow](#development-workflow)
- [Advanced Features](#advanced-features)
  - [Performance Optimization](#performance-optimization)
  - [Monitoring and Observability](#monitoring-and-observability)
  - [Temporary File Management](#temporary-file-management)
- [Contributing](#contributing)
  - [Development Guidelines](#development-guidelines)
- [Integration with SITMUN](#integration-with-sitmun)
- [Support](#support)
- [License](#license)

## Overview

The SITMUN MBTiles Service provides REST API endpoints to:

- Generate MBTiles files from WMTS map services
- Estimate tile generation size and requirements
- Monitor job progress and status
- Download completed MBTiles files

This service integrates with the [SITMUN Backend Core](https://github.com/sitmun/sitmun-backend-core) to provide tile generation capabilities for the SITMUN platform.

## Quick Start

### Prerequisites

- Java 17 or later

### Local Development

1. **Clone the repository**

   ```bash
   git clone https://github.com/sitmun/sitmun-mbtiles.git
   cd sitmun-mbtiles
   ```

2. **Build the application**

   ```bash
   ./gradlew build -x test
   ```

3. **Run the application**

   ```bash
   # Run with Java directly (recommended)
   java -jar build/libs/sitmun-mbtiles.jar --spring.profiles.active=prod
   
   # Or use Gradle bootRun directly
   ./gradlew bootRun --args='--spring.profiles.active=prod'
  
   ```

4. **Verify the service is running**

   ```bash
   # Check health status
   curl http://localhost:8080/actuator/health
   
   # Test the MBTiles endpoint (will return 400 for invalid request, but confirms service is running)
   curl -X POST http://localhost:8080/mbtiles/estimate \
     -H "Content-Type: application/json" \
     -d '{"bbox": {"minX": 0, "minY": 0, "maxX": 1, "maxY": 1, "srs": "EPSG:4326"}, "minZoom": 10, "maxZoom": 15}'
   ```

### Troubleshooting bootRun

#### Port Already in Use

```bash
# Use different port
./gradlew bootRun --args='--spring.profiles.active=prod --server.port=8081'
```

#### Memory Issues

```bash
# Increase heap size
./gradlew bootRun --args='--spring.profiles.active=prod -Xmx4g -Xms2g'
```

### Building

```bash
# Build the project (includes Git hooks setup)
./gradlew build

# Build without tests (faster for development)
./gradlew build -x test

# Run tests
./gradlew test

# Create JAR file
./gradlew jar

# Format code
./gradlew spotlessApply

# Check code coverage
./gradlew jacocoTestReport
```

> **💡 Tip**: For development, use `./gradlew build -x test` for faster builds, then run the JAR directly with `java -jar build/libs/sitmun-mbtiles.jar --spring.profiles.active=dev`

## Features

### Core Functionality

- **WMTS Tile Harvesting**: Download tiles from WMTS map services
- **MBTiles Generation**: Create standardized MBTiles format files (SQLite-based)
- **Batch Processing**: Process large tile sets efficiently with Spring Batch
- **Progress Tracking**: Monitor job status and progress in real-time
- **Tile Merging**: Intelligently combine multiple layers into single MBTiles file
- **Custom MBTiles I/O**: Enhanced reader/writer with tile combination capabilities
- **Temporary File Management**: Centralized temporary file creation and cleanup with configurable scheduling

### Performance Optimizations

- **Batch Processing**: Efficient tile processing with Spring Batch
- **Memory Management**: Proper resource cleanup and memory optimization
- **Concurrent Processing**: Multi-threaded tile downloading
- **Tile Combination**: Efficient merging of multiple layers
- **Automatic Cleanup**: Scheduled cleanup of temporary files to prevent disk space issues

### Development Features

- **Spring Boot DevTools**: Auto-restart and live reload with intelligent exclusions (automatically excluded from production builds via `developmentOnly` dependency)
- **Profile-based Configuration**: Separate dev and prod configurations
- **H2 Console**: Database management interface (dev profile only)
- **Debug Logging**: Detailed logging for development (dev profile only)
- **Automated Quality Checks**: Git hooks for pre-commit validation
- **Conventional Commits**: Enforced commit message format
- **Version Management**: Automated versioning with Axion Release
- **Code Formatting**: Automated code formatting with Spotless
- **Coverage Reporting**: JaCoCo integration for code coverage
- **Comprehensive Testing**: Unit and integration tests with comprehensive coverage

## API Reference

### Endpoints

| Endpoint                | Method | Description                           |
|-------------------------|--------|---------------------------------------|
| `/mbtiles`              | POST   | Start MBTiles generation job          |
| `/mbtiles/estimate`     | POST   | Estimate tile generation requirements |
| `/mbtiles/{jobId}`      | GET    | Get job status and progress           |
| `/mbtiles/{jobId}/file` | GET    | Download completed MBTiles file       |

### Usage Examples

#### Start MBTiles Generation

```bash
curl -X POST http://localhost:8080/mbtiles \
  -H "Content-Type: application/json" \
  -d '{
    "mapServices": [
      {
        "url": "https://wmts.example.com/wmts",
        "layers": ["layer1", "layer2"],
        "type": "WMTS"
      }
    ],
    "bbox": {
      "minX": -3.0,
      "minY": 40.0,
      "maxX": -2.0,
      "maxY": 41.0,
      "srs": "EPSG:4326"
    },
    "minZoom": 10,
    "maxZoom": 15
  }'
```

Response: Job ID (e.g., `123`)

#### Estimate Generation Requirements

```bash
curl -X POST http://localhost:8080/mbtiles/estimate \
  -H "Content-Type: application/json" \
  -d '{
    "mapServices": [
      {
        "url": "https://wmts.example.com/wmts",
        "layers": ["layer1"],
        "type": "WMTS"
      }
    ],
    "bbox": {
      "minX": -3.0,
      "minY": 40.0,
      "maxX": -2.0,
      "maxY": 41.0,
      "srs": "EPSG:4326"
    },
    "minZoom": 10,
    "maxZoom": 15
  }'
```

Response:
```json
{
  "tileCount": 1500,
  "estimatedTileSizeKb": 45.2,
  "estimatedMbtilesSizeMb": 15.3
}
```

#### Check Job Status

```bash
curl http://localhost:8080/mbtiles/123
```

Response:
```json
{
  "status": "RUNNING",
  "processedTiles": 975,
  "totalTiles": 1500,
  "errorMessage": null
}
```

#### Download MBTiles File

```bash
curl -O -J http://localhost:8080/mbtiles/123/file
```

### Data Models

#### TileRequestDto

```
{
  "mapServices": [MapServiceDto],
  "bbox": BoundingBoxDto,
  "minZoom": int,
  "maxZoom": int
}
```

#### BoundingBoxDto

```
{
  "minX": double,    // Required, must be ≤ maxX
  "minY": double,    // Required, must be ≤ maxY
  "maxX": double,    // Required, must be ≥ minX
  "maxY": double,    // Required, must be ≥ minY
  "srs": String      // Required, must be in EPSG format (e.g., "EPSG:4326")
}
```

#### MapServiceDto

```
{
  "url": String,      // Required, valid HTTP/HTTPS URL
  "layers": [String], // Required, non-empty list
  "type": String      // Required
}
```

#### MBTilesEstimateDto

```
{
  "tileCount": int,              // Total number of tiles
  "estimatedTileSizeKb": double, // Average tile size in KB
  "estimatedMbtilesSizeMb": double // Estimated MBTiles file size in MB
}
```

#### MBTilesJobStatusDto

```
{
  "status": String,        // Job status (STARTED, RUNNING, COMPLETED, FAILED)
  "processedTiles": long,  // Number of processed tiles
  "totalTiles": long,      // Total number of tiles
  "errorMessage": String   // Error message if job failed (optional)
}
```

## Configuration

The service uses profile-based configuration to separate development and production settings:

### Profiles

- **Default**: Basic configuration without DevTools
- **Dev**: Development tools, H2 console, debug logging
- **Prod**: Production-optimized, no DevTools, minimal logging

### Configuration Files

- `application.yml`: Base configuration
- `application-dev.yml`: Development profile settings
- `application-prod.yml`: Production profile settings

### Base Configuration

The service uses H2 in-memory database for Spring Batch metadata. Configuration can be customized in the respective profile files:

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
  
  # H2 Database Configuration
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  # H2 Console Configuration
  h2:
    console:
      enabled: false

  # Spring Batch Configuration
  batch:
    job:
      enabled: false
    jdbc:
      initialize-schema: always
      platform: h2

# MBTiles Service Configuration
mbtiles:
  # Job Processing Configuration
  job:
    corePoolSize: 2      # Core thread pool size
    maxPoolSize: 4       # Maximum thread pool size
    queueCapacity: 10    # Queue capacity for tasks
  
  # Temporary File Configuration
  temp:
    directory: ${java.io.tmpdir}  # Temporary directory path
    cleanup:
      enabled: true               # Enable automatic cleanup
      cron: "0 0 2 * * *"        # Cron expression for cleanup (every day at 2 AM)
```

### Configuration Options

The service provides comprehensive configuration options for different aspects:

#### Job Processing Configuration

Configurable thread pools for job processing:

```yaml
mbtiles:
  job:
    corePoolSize: 2      # Core thread pool size
    maxPoolSize: 4       # Maximum thread pool size
    queueCapacity: 10    # Queue capacity for tasks
```

#### Temporary File Configuration

Configurable temporary file management with automatic cleanup:

```yaml
mbtiles:
  temp:
    directory: ${java.io.tmpdir}  # Temporary directory path
    cleanup:
      enabled: true               # Enable automatic cleanup
      cron: "0 0 2 * * *"        # Cron expression for cleanup (every day at 2 AM)
```

**Cron Expression Examples:**
- `"0 0 2 * * *"` - Every day at 2 AM (default)
- `"0 0 0 * * *"` - Every day at midnight
- `"0 0 3 * * *"` - Every day at 3 AM
- `"0 0 2 * * 0"` - Every Sunday at 2 AM
- `"0 */5 * * * *"` - Every 5 minutes
- `"0 0 * * * *"` - Every hour

## Architecture

### Technology Stack

- **Java**: 17
- **Spring Boot**: 3.5.4
- **Spring Batch**: 5.2.2 (for job processing)
- **Build Tool**: Gradle with Version Catalogs
- **Database**: H2 (in-memory) for Spring Batch metadata
- **MBTiles Support**: mbtiles4j 1.2.0
- **Coordinate Transformations**: proj4j 1.1.5
- **XML Parsing**: dom4j 2.1.4
- **Testing**: JUnit 5, Spring Boot Test, Mockito
- **Code Quality**: Spotless (Google Java Format), JaCoCo, Axion Release
- **Development Tools**: Git Hooks, Conventional Commits

### System Architecture

The service follows a layered architecture with Spring Batch for job processing:

```
Controllers → Services → Jobs → Process → MBTiles Writer/Reader
     ↓           ↓        ↓       ↓              ↓
   REST API   Business  Batch   Tile        SQLite DB
              Logic     Jobs   Processing   (MBTiles)
```

### Key Components

- **Controllers**: REST API endpoints (`MBTilesController`)
- **Services**: Business logic (`MBTilesEstimateService`, `MBTilesProgressService`, `TemporaryFileService`)
- **Batch**: Spring Batch job management (`MBTilesTask`, `MBTilesTaskContext`)
- **Tile Sources**: WMTS tile source processing (`WMTSProcess`, `MBTilesEstimateStrategy`)
- **MBTiles I/O**: Custom MBTiles reader/writer (`CustomMBTilesReader`, `CustomMBTilesWriter`)
- **DTOs**: Data transfer objects for API communication
- **WMTS Tile Source**: WMTS capabilities parsing and coordinate transformations
- **Utils**: Utility classes for coordinate transformations (`Proj4CoordinateUtils`)
- **Configuration**: Application configuration (`TemporaryFileConfiguration`)

### Processing Flow

1. **Tile Source Capabilities Parsing**: Extract layer information and tile matrix sets
2. **Coordinate Calculation**: Convert geographic bounds to tile coordinates
3. **Tile Download**: Fetch tiles from WMTS tile sources
4. **Tile Processing**: Merge and optimize tiles for MBTiles format
5. **MBTiles Generation**: Create SQLite database with tiles and metadata
6. **File Output**: Generate compliant MBTiles file

### Strategy Pattern

The service uses a strategy pattern for extensible tile source processing. This allows different tile sources (e.g. WMTS) to be implemented without changing the core application logic.

#### Strategy Interfaces:
- **MBTilesTaskStrategy**: Interface for tile processing strategies
- **MBTilesEstimateStrategy**: Interface for size estimation strategies

#### Current Strategy Implementations:
- **WMTSProcess**: WMTS tile source strategy for harvesting and processing

#### Strategy Consumers:
- **MBTilesEstimateService**: Main service that uses estimation strategies to calculate tile generation requirements
- **MBTilesTask**: Main batch task that uses processing strategies to harvest tiles

#### Supporting Components:
- **CustomMBTilesWriter**: Enhanced MBTiles writing with tile combination
- **CustomMBTilesReader**: Enhanced MBTiles reading capabilities

### Extensibility

The strategy pattern allows integration of new tile sources without code changes when they are implemented.

#### How to Extend to New Tile Sources:

To add support for a new tile source (e.g., WMS, OSM, TMS), follow these steps:

1. **Create a new strategy implementation** in the `tilesources/` package:
   ```java
   package org.sitmun.mbtiles.tilesources.wms;

   @Component
   public class WMSProcess implements MBTilesTaskStrategy, MBTilesEstimateStrategy {
       @Override
       public boolean accept(MBTilesTaskContext context) {
           return Constants.WMS_TYPE.equals(context.getService().getType());
       }
       
       @Override
       public void process(StepContext stepContext, MBTilesTaskContext context) {
           // WMS-specific tile harvesting logic
       }
       
       @Override
       public MBTilesEstimateDto estimate(MBTilesTaskContext context) {
           // WMS-specific size estimation logic
       }
   }
   ```

2. **Add the new service type** to `Constants.java`:
   ```java
   public static final String WMS_TYPE = "WMS";
   ```

3. **The application automatically discovers and uses** the new strategy through Spring's dependency injection.

#### Benefits:
- ✅ **No code changes required** in existing services
- ✅ **Automatic discovery** of new strategies
- ✅ **Consistent interface** across all tile sources
- ✅ **Easy testing** with mock strategies
- ✅ **Runtime selection** based on service type

### Error Handling

The service includes comprehensive error handling with RFC 7807 ProblemDetail format.

#### HTTP Status Codes

- **200 OK**: Successful operation
- **400 Bad Request**: Validation errors or invalid request parameters
- **404 Not Found**: Resource not found (job or file)
- **500 Internal Server Error**: Unexpected server errors

#### Global Exception Handler

The application uses a `GlobalExceptionHandler` that provides standardized error responses following RFC 7807 (Problem Details for HTTP APIs):

- **Validation Errors**: Handles `@Valid` annotation validation failures
- **Business Logic Errors**: Custom exceptions for different error scenarios
- **Resource Not Found**: File and job not found scenarios
- **Internal Server Errors**: Unexpected exceptions with proper logging

#### Error Response Format

All error responses follow the RFC 7807 ProblemDetail format:

```json
{
  "type": "urn:sitmun-mbtiles:problem:validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Request validation failed",
  "errors": ["mapServices: Map services list cannot be null"]
}
```

#### Problem Type URIs

The service uses the following URN-based problem type identifiers:

| Problem Type URI | Title | Status | Description |
|------------------|-------|--------|-------------|
| `urn:sitmun-mbtiles:problem:validation-error` | Validation Error | 400 | Request validation failures (missing fields, invalid formats, etc.) |
| `urn:sitmun-mbtiles:problem:invalid-request` | Invalid Request | 400 | Invalid request parameters or business logic errors |
| `urn:sitmun-mbtiles:problem:resource-not-found` | Resource Not Found | 404 | Job or file not found |
| `urn:sitmun-mbtiles:problem:internal-error` | Internal Server Error | 500 | Internal processing errors |
| `urn:sitmun-mbtiles:problem:unexpected-error` | Unexpected Error | 500 | Unexpected exceptions |

#### Common Error Scenarios

**Validation Errors (400):**
- Invalid request parameters
- Missing required fields
- Invalid coordinate bounds
- Invalid zoom levels
- Invalid URL formats

**Resource Not Found (404):**
- Job ID doesn't exist
- MBTiles file not found
- Job completed but file was deleted

**Internal Server Error (500):**
- WMTS service unavailable
- Processing errors
- Unexpected exceptions

#### Custom Exceptions

The service includes comprehensive custom exceptions:

- **MBTilesNoStrategyException**: When no suitable strategy is found
- **MBTilesFileNotFoundException**: When requested file doesn't exist
- **MBTilesUnexpectedRequestException**: Invalid request parameters
- **MBTilesUnexpectedInternalException**: Internal processing errors

Each tile source has their specific exceptions:

- **WMTSHarvestException**: WMTS tile harvesting and processing errors
- **WMTSCapabilitiesException**: WMTS capabilities parsing errors

#### Input Validation

The service includes comprehensive input validation using Jakarta Validation (Bean Validation):

#### Error Handling Benefits

- **RFC 7807 Compliance**: Standard HTTP API error response format
- **No Stack Traces**: Clean error responses without exposing internal details
- **Structured Errors**: Consistent error format across all endpoints

## Development

### Profiles Explained

#### Development Profile (`dev`)
- **DevTools**: Auto-restart enabled with intelligent exclusions for batch jobs and core packages
- **H2 Console**: Available at `http://localhost:8080/h2-console`
- **Debug Logging**: Detailed logs for troubleshooting
- **SQL Logging**: Shows database queries
- **Auto-restart**: Excludes batch, tilesources, io, config, service, utils, dto, controllers packages
- **Livereload**: Enabled on port 35729 for browser auto-refresh

#### Production Profile (`prod`)
- **DevTools**: Automatically excluded (Spring Boot handles this)
- **H2 Console**: Disabled for security
- **Minimal Logging**: Optimized for performance
- **Extended Timeout**: 60s graceful shutdown for batch jobs
- **Production Settings**: Optimized for deployment

#### Default Profile (no profile)
- **Basic Configuration**: Uses `application.yml` only
- **No DevTools**: Safe for any environment
- **Standard Settings**: Balanced for development and production

### Deployment

#### Production Deployment

For production deployment, use the production profile:

```bash
# Build JAR (DevTools automatically excluded in production)
./gradlew build

# Run with production profile (recommended)
java -jar build/libs/sitmun-mbtiles.jar --spring.profiles.active=prod

# Or use Gradle bootRun directly
./gradlew bootRun --args='--spring.profiles.active=prod'
```

#### Development Deployment

For development, use the development profile:

```bash
# Build JAR (DevTools included for development)
./gradlew build -x test

# Run with development profile (recommended)
java -jar build/libs/sitmun-mbtiles.jar --spring.profiles.active=dev

# Or use Gradle bootRun directly
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Project Structure

```
src/
  main/
    java/org/sitmun/mbtiles/
      Application.java              # Main application class
      Constants.java                # Application constants
      config/                       # Configuration classes
      dto/                         # Data transfer objects
      batch/                       # Spring Batch jobs
      io/                          # MBTiles I/O operations
      service/                     # Business logic services
      tilesources/                  # Tile source processing
        wmts/                       # WMTS tile source processing
      utils/                       # Utility classes
      controllers/                  # REST controllers
    resources/
      application.yml               # Application configuration
      application-dev.yml           # Development profile
      application-prod.yml          # Production profile
  test/
    java/org/sitmun/mbtiles/
      service/                     # Service unit tests
      controllers/                 # Controller tests
      dto/                        # Validation tests
```

#### Package Descriptions

- **`config/`**: Spring Boot configuration classes for batch jobs and temporary file management
- **`dto/`**: Data transfer objects for API communication and validation
- **`batch/`**: Spring Batch job processing and task execution
- **`io/`**: Custom MBTiles reader/writer implementations
- **`service/`**: Business logic services and exception handling
- **`tilesources/`**: Tile source processing strategies (WMTS, WMS, etc.)
- **`utils/`**: Utility classes for coordinate transformations and common operations
- **`controllers/`**: REST API controllers and global exception handling

### Build System

The project uses Gradle with Version Catalogs for dependency management:

- **Version Catalog**: `gradle/libs.versions.toml` - Centralized dependency versions
- **Plugins**: Spring Boot, Lombok, Spotless, Axion Release
- **Quality Tools**: JaCoCo for coverage, Spotless for formatting

### Code Quality

The project includes several code quality tools:

- **Spotless**: Code formatting with Google Java Format
- **JaCoCo**: Code coverage reporting
- **Axion Release**: Version management with semantic versioning
- **Git Hooks**: Automated quality checks and commit validation

### Running Quality Checks

```bash
# Format code
./gradlew spotlessApply

# Check formatting without applying
./gradlew spotlessCheck

# Check code coverage
./gradlew jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Version Management

The project uses Axion Release for automated version management:

```bash
# Check current version
./gradlew currentVersion

# Create a new release
./gradlew release

# Create a new patch version
./gradlew patch
```

#### Creating a Release

**Prerequisites:**
1. **Clean Git State**: Ensure all changes are committed
2. **Working Directory**: No uncommitted changes
3. **Git Repository**: Must be a valid Git repository

**Step-by-Step Release Process:**

```bash
# 1. Check current Git status
git status

# 2. Add and commit any pending changes
git add .
git commit -m "docs: update documentation for release"

# 3. Verify the repository is clean
git status

# 4. Check current version
./gradlew currentVersion

# 5. Create a new release
./gradlew release

# 6. Push the release tag
git push --tags
```

**Release Types:**

- **`./gradlew release`**: Creates a new patch version (e.g., 1.0.0 → 1.0.1)
- **`./gradlew release -Prelease.scope=minor`**: Creates a new minor version (e.g., 1.0.0 → 1.1.0)
- **`./gradlew release -Prelease.scope=major`**: Creates a new major version (e.g., 1.0.0 → 2.0.0)

**Troubleshooting:**

If the release fails with "No such property: commit", ensure:
1. All changes are committed to Git
2. You're on a valid branch (not detached HEAD)
3. Git repository is properly initialized

### Testing

The project includes comprehensive testing:

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests MBTilesServiceTest

# Run integration tests
./gradlew test --tests *IntegrationTest

# Run validation tests
./gradlew test --tests *ValidationTest

# Run error handling tests
./gradlew test --tests GlobalExceptionHandlerTest

# Run tests with coverage
./gradlew test jacocoTestReport
```

#### Test Coverage

- **Unit Tests**: Service layer, controller layer, utility classes
- **Integration Tests**: End-to-end API testing
- **Validation Tests**: Comprehensive DTO validation testing
- **Error Handling Tests**: Global exception handler and error scenarios
- **Exception Testing**: Comprehensive error scenario coverage
- **Edge Cases**: Boundary conditions and error handling

#### Validation Test Coverage

The project includes extensive validation testing:

- **TileRequestDtoValidationTest**: Tests all validation constraints including cross-field validation
- **BoundingBoxDtoValidationTest**: Tests coordinate bounds and SRS format validation
- **MapServiceDtoValidationTest**: Tests URL format and service type validation
- **GlobalExceptionHandlerTest**: Tests error response format and RFC 7807 compliance

### Development Workflow

#### Git Hooks

The project includes automated Git hooks that run on every commit:

**Pre-commit checks**:
- Code formatting validation (Spotless)
- Unit and integration tests
- Code coverage verification

**Commit message validation**:
- Conventional commit format enforcement
- SITMUN-specific scope support `(mbtiles)`

#### Commit Message Format

Follow the conventional commit format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Test changes
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `ci`: CI/CD changes
- `build`: Build system changes

**Examples**:
```bash
git commit -m "feat(mbtiles): add tile merging functionality"
git commit -m "fix(mbtiles): resolve memory leak in tile processing"
git commit -m "docs: update README with MBTiles compliance info"
git commit -m "test: add integration tests for WMTS harvesting"
git commit -m "style: format code with Google Java Format"
```

#### Managing Git Hooks

```bash
# Install Git hooks (automatic with build)
./gradlew setupGitHooks

# Remove Git hooks
./gradlew removeGitHooks
```

## Advanced Features

### Performance Optimization

The service includes several performance optimizations:

- **Batch Processing**: Efficient tile processing with Spring Batch
- **Tile Combination**: Efficient merging of multiple layers
- **Automatic Cleanup**: Scheduled cleanup prevents disk space issues

### Monitoring and Observability

- **Spring Boot Actuator**: Health checks, metrics, and application monitoring
- **Custom Health Indicators**: MBTiles service health monitoring
- **Progress Tracking**: Real-time job progress monitoring
- **Error Handling**: Comprehensive error handling and logging
- **Scheduled Cleanup**: Automatic temporary file cleanup with configurable scheduling

#### Actuator Endpoints

| Endpoint | Description | Access |
|----------|-------------|--------|
| `/actuator/health` | Application health status | Public |

**Health Check Response:**
```json
{
  "status": "UP"
}
```

### Temporary File Management

The service includes a comprehensive temporary file management system:

- **Centralized Service**: `TemporaryFileService` provides unified temporary file operations
- **Configurable Directory**: Set custom temporary directory location via configuration
- **Automatic Cleanup**: Scheduled cleanup of old temporary files (24-hour retention)
- **Safe Operations**: Exception-safe file creation and deletion
- **Unique File Creation**: UUID-based unique file names for collision prevention

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes following the conventional commit format
4. Add tests for new functionality
5. Ensure all tests pass and code is formatted
6. Submit a pull request

### Development Guidelines

- Follow the conventional commit format
- Write tests for new functionality
- Ensure code coverage remains high
- Run quality checks before committing
- Update documentation as needed

## Integration with SITMUN

This service is designed to provide tile generation capabilities for the [SITMUN](https://github.com/sitmun/) platform. It can be deployed as a microservice alongside other SITMUN components.

## Support

For questions and support:

- Open an issue on GitHub
- Check the [SITMUN documentation](https://sitmun.github.io/)
- Join the SITMUN community discussions

## License

This project uses the following license: [European Union Public License V. 1.2](LICENSE).