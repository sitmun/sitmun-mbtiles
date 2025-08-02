package org.sitmun.mbtiles.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TileRequestDto Validation Tests")
class TileRequestDtoValidationTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    @SuppressWarnings("resource")
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Valid TileRequestDto with all required fields should pass validation")
  void validTileRequestDto_shouldPassValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(-3.0)
            .minY(40.0)
            .maxX(-2.0)
            .maxY(41.0)
            .srs("EPSG:4326")
            .build();

    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://wmts.example.com/wmts")
            .layers(List.of("layer1", "layer2"))
            .type("WMTS")
            .build();

    TileRequestDto tileRequest =
        TileRequestDto.builder()
            .mapServices(List.of(mapService))
            .bbox(bbox)
            .minZoom(10)
            .maxZoom(15)
            .build();

    // When
    Set<ConstraintViolation<TileRequestDto>> violations = validator.validate(tileRequest);

    // Then
    assertTrue(violations.isEmpty(), "Valid TileRequestDto should have no violations");
  }

  @Test
  @DisplayName("TileRequestDto with null map services should fail validation")
  void nullMapServices_shouldFailValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(-3.0)
            .minY(40.0)
            .maxX(-2.0)
            .maxY(41.0)
            .srs("EPSG:4326")
            .build();

    TileRequestDto tileRequest =
        TileRequestDto.builder().mapServices(null).bbox(bbox).minZoom(10).maxZoom(15).build();

    // When
    Set<ConstraintViolation<TileRequestDto>> violations = validator.validate(tileRequest);

    // Then
    assertFalse(violations.isEmpty(), "Null mapServices should cause validation failure");
    assertTrue(
        violations.stream()
            .anyMatch(v -> v.getMessage().contains("Map services list cannot be null")));
  }

  @Test
  @DisplayName("TileRequestDto with empty map services list should fail validation")
  void emptyMapServices_shouldFailValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(-3.0)
            .minY(40.0)
            .maxX(-2.0)
            .maxY(41.0)
            .srs("EPSG:4326")
            .build();

    TileRequestDto tileRequest =
        TileRequestDto.builder().mapServices(List.of()).bbox(bbox).minZoom(10).maxZoom(15).build();

    // When
    Set<ConstraintViolation<TileRequestDto>> violations = validator.validate(tileRequest);

    // Then
    assertFalse(violations.isEmpty(), "Empty mapServices should cause validation failure");
    assertTrue(
        violations.stream()
            .anyMatch(v -> v.getMessage().contains("At least one map service must be provided")));
  }

  @Test
  @DisplayName("TileRequestDto with null bounding box should fail validation")
  void nullBbox_shouldFailValidation() {
    // Given
    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://wmts.example.com/wmts")
            .layers(List.of("layer1"))
            .type("WMTS")
            .build();

    TileRequestDto tileRequest =
        TileRequestDto.builder()
            .mapServices(List.of(mapService))
            .bbox(null)
            .minZoom(10)
            .maxZoom(15)
            .build();

    // When
    Set<ConstraintViolation<TileRequestDto>> violations = validator.validate(tileRequest);

    // Then
    assertFalse(violations.isEmpty(), "Null bbox should cause validation failure");
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("Bounding box cannot be null")));
  }

  @Test
  @DisplayName("TileRequestDto with invalid zoom levels should fail validation")
  void invalidZoomLevels_shouldFailValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(-3.0)
            .minY(40.0)
            .maxX(-2.0)
            .maxY(41.0)
            .srs("EPSG:4326")
            .build();

    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://wmts.example.com/wmts")
            .layers(List.of("layer1"))
            .type("WMTS")
            .build();

    TileRequestDto tileRequest =
        TileRequestDto.builder()
            .mapServices(List.of(mapService))
            .bbox(bbox)
            .minZoom(-1) // Invalid: negative zoom
            .maxZoom(10) // Valid zoom
            .build();

    // When
    Set<ConstraintViolation<TileRequestDto>> violations = validator.validate(tileRequest);

    // Then
    assertFalse(violations.isEmpty(), "Invalid zoom levels should cause validation failure");
    assertTrue(
        violations.stream()
            .anyMatch(v -> v.getMessage().contains("Minimum zoom level must be at least 0")));
  }

  @Test
  @DisplayName("TileRequestDto with min zoom greater than max zoom should fail validation")
  void minZoomGreaterThanMaxZoom_shouldFailValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(-3.0)
            .minY(40.0)
            .maxX(-2.0)
            .maxY(41.0)
            .srs("EPSG:4326")
            .build();

    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://wmts.example.com/wmts")
            .layers(List.of("layer1"))
            .type("WMTS")
            .build();

    TileRequestDto tileRequest =
        TileRequestDto.builder()
            .mapServices(List.of(mapService))
            .bbox(bbox)
            .minZoom(15) // Invalid: greater than maxZoom
            .maxZoom(10) // Invalid: less than minZoom
            .build();

    // When
    Set<ConstraintViolation<TileRequestDto>> violations = validator.validate(tileRequest);

    // Then
    assertFalse(violations.isEmpty(), "minZoom > maxZoom should cause validation failure");
    assertTrue(
        violations.stream()
            .anyMatch(
                v ->
                    v.getMessage()
                        .contains(
                            "Minimum zoom level must be less than or equal to maximum zoom level")));
  }

  @Test
  @DisplayName("TileRequestDto with equal min and max zoom should pass validation")
  void minZoomEqualToMaxZoom_shouldPassValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(-3.0)
            .minY(40.0)
            .maxX(-2.0)
            .maxY(41.0)
            .srs("EPSG:4326")
            .build();

    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://wmts.example.com/wmts")
            .layers(List.of("layer1"))
            .type("WMTS")
            .build();

    TileRequestDto tileRequest =
        TileRequestDto.builder()
            .mapServices(List.of(mapService))
            .bbox(bbox)
            .minZoom(10) // Valid: equal to maxZoom
            .maxZoom(10) // Valid: equal to minZoom
            .build();

    // When
    Set<ConstraintViolation<TileRequestDto>> violations = validator.validate(tileRequest);
    for (var violation : violations) {
      System.out.println(violation.getMessage());
    }
    // Then
    assertTrue(violations.isEmpty(), "minZoom == maxZoom should pass validation");
  }
}
