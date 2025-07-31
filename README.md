# SITMUN MBTiles Service

A Spring Boot microservice for generating MBTiles (MapBox Tiles) from WMTS map services. This service is part of the [SITMUN](https://sitmun.github.io/) geospatial platform ecosystem.

## Overview

The SITMUN MBTiles Service provides REST API endpoints to:
- Generate MBTiles files from WMTS map services
- Estimate tile generation size and requirements
- Monitor job progress and status
- Download completed MBTiles files

This service integrates with the [SITMUN Backend Core](https://github.com/sitmun/sitmun-backend-core) to provide tile generation capabilities for the SITMUN platform.

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.5.4
- **Build Tool**: Gradle
- **Database**: H2 (in-memory)
- **MBTiles Support**: mbtiles4j 1.2.0
- **Coordinate Transformations**: proj4j 1.1.5
- **XML Parsing**: dom4j 2.1.4
- **Testing**: JUnit 5, Spring Boot Test
- **Code Quality**: Spotless, JaCoCo, OWASP Dependency Check

## Features

### Core Functionality
- **WMTS Tile Harvesting**: Download tiles from WMTS map services
- **MBTiles Generation**: Create standardized MBTiles format files
- **Coordinate System Support**: Handle various SRS/CRS transformations with EPSG:3857 output
- **Batch Processing**: Process large tile sets efficiently with Spring Batch
- **Progress Tracking**: Monitor job status and progress in real-time
- **Tile Merging**: Intelligently combine multiple layers into single MBTiles file

### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/mbtiles` | POST | Start MBTiles generation job |
| `/mbtiles/estimate` | POST | Estimate tile generation requirements |
| `/mbtiles/{jobId}` | GET | Get job status and progress |
| `/mbtiles/{jobId}/file` | GET | Download completed MBTiles file |

## Quick Start

### Prerequisites
- Java 17 or later
- Gradle (wrapper included)

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/sitmun/sitmun-mbtiles.git
   cd sitmun-mbtiles
   ```

2. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

3. **Verify the service is running**
   ```bash
   curl http://localhost:8080/mbtiles
   ```

### Building

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Create JAR file
./gradlew jar
```

## API Usage

### Start MBTiles Generation

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
    "minLat": 40.0,
    "minLon": -3.0,
    "maxLat": 41.0,
    "maxLon": -2.0,
    "minZoom": 10,
    "maxZoom": 15,
    "srs": "EPSG:3857"
  }'
```

Response: Job ID (e.g., `123`)

### Estimate Generation Requirements

```bash
curl -X POST http://localhost:8080/mbtiles/estimate \
  -H "Content-Type: application/json" \
  -d '{
    "mapServices": [...],
    "minLat": 40.0,
    "minLon": -3.0,
    "maxLat": 41.0,
    "maxLon": -2.0,
    "minZoom": 10,
    "maxZoom": 15,
    "srs": "EPSG:3857"
  }'
```

Response:
```json
{
  "estimatedTiles": 1500,
  "estimatedSizeMB": 45.2,
  "estimatedTimeMinutes": 15
}
```

### Check Job Status

```bash
curl http://localhost:8080/mbtiles/123
```

Response:
```json
{
  "jobId": 123,
  "status": "RUNNING",
  "progress": 65,
  "totalTiles": 1500,
  "processedTiles": 975
}
```

### Download MBTiles File

```bash
curl -O -J http://localhost:8080/mbtiles/123/file
```

## Data Models

### TileRequestDto
```java
{
  "mapServices": [MapServiceDto],  // List of map services
  "minLat": double,               // Minimum latitude (EPSG:4326)
  "minLon": double,               // Minimum longitude (EPSG:4326)
  "maxLat": double,               // Maximum latitude (EPSG:4326)
  "maxLon": double,               // Maximum longitude (EPSG:4326)
  "minZoom": int,                 // Minimum zoom level
  "maxZoom": int,                 // Maximum zoom level
  "srs": String                   // Spatial reference system
}
```

### MapServiceDto
```java
{
  "url": String,      // WMTS service URL
  "layers": [String], // Layer names to harvest
  "type": String      // Service type (WMTS only)
}
```

## Configuration

The service uses H2 in-memory database by default. Configuration can be customized in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  batch:
    job:
      enabled: false
    jdbc:
      initialize-schema: always
```

## Architecture

The service follows a layered architecture:

```
Controllers → Services → Jobs → Process → MBTiles Writer/Reader
     ↓           ↓        ↓       ↓              ↓
   REST API   Business  Batch   Tile        SQLite DB
              Logic     Jobs   Processing   (MBTiles)
```

### Key Components

- **Controllers**: REST API endpoints (`MBTilesController`)
- **Services**: Business logic (`MBTilesService`, `MBTilesProgressService`)
- **Jobs**: Spring Batch job management (`MBTilesTask`, `TaskContext`)
- **Process**: Tile processing logic (`WMTSHarvestProcess`, `WMTSEstimateProcess`)
- **MBTiles I/O**: Custom MBTiles reader/writer (`CustomMBTilesReader`, `CustomMBTilesWriter`)
- **DTOs**: Data transfer objects for API communication
- **WMTS Utils**: WMTS capabilities parsing and coordinate transformations

### Processing Flow
1. **WMTS Capabilities Parsing**: Extract layer information and tile matrix sets
2. **Coordinate Calculation**: Convert geographic bounds to tile coordinates
3. **Tile Download**: Fetch tiles from WMTS services
4. **Tile Processing**: Merge and optimize tiles for MBTiles format
5. **MBTiles Generation**: Create SQLite database with tiles and metadata
6. **File Output**: Generate compliant MBTiles file

## Integration with SITMUN

This service is designed to integrate with the [SITMUN Backend Core](https://github.com/sitmun/sitmun-backend-core) to provide tile generation capabilities for the SITMUN platform. It can be deployed as a microservice alongside other SITMUN components.

## Development

### Project Structure
```
src/
  main/
    java/org/sitmun/mbtiles/
      Application.java              # Main application class
      config/                       # Configuration classes
      dto/                         # Data transfer objects
      jobs/                        # Spring Batch jobs
      process/                     # Tile processing logic
      service/                     # Business logic services
      web/                         # REST controllers
      wmts/                        # WMTS utilities
    resources/
      application.yml              # Application configuration
  test/                           # Test classes
```

### Code Quality

The project includes several code quality tools:

- **Spotless**: Code formatting with Google Java Format
- **JaCoCo**: Code coverage reporting
- **OWASP Dependency Check**: Security vulnerability scanning
- **Axion Release**: Version management

### Running Quality Checks

```bash
# Format code
./gradlew spotlessApply

# Check code coverage
./gradlew jacocoTestReport

# Security scan
./gradlew dependencyCheck
```

### Git Hooks

The project includes Git hooks for automated quality checks:

```bash
# Install Git hooks (automatic with build)
./gradlew setupGitHooks

# Remove Git hooks
./gradlew removeGitHooks
```

**Pre-commit checks**:
- Code formatting validation
- Unit and integration tests
- Code coverage verification

**Commit message validation**:
- Conventional commit format enforcement
- SITMUN-specific scope support `(mbtiles)`

See [GIT_HOOKS.md](GIT_HOOKS.md) for detailed documentation.

## Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests MBTilesServiceTest

# Run integration tests
./gradlew test --tests *IntegrationTest
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project uses the following license: [European Union Public License V. 1.2](LICENSE).

## Support

For questions and support:
- Open an issue on GitHub
- Check the [SITMUN documentation](https://sitmun.github.io/)
- Join the SITMUN community discussions 