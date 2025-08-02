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

@DisplayName("MapServiceDto Validation Tests")
class MapServiceDtoValidationTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    @SuppressWarnings("resource")
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Valid MapServiceDto with all required fields should pass validation")
  void validMapServiceDto_shouldPassValidation() {
    // Given
    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://wmts.example.com/wmts")
            .layers(List.of("layer1", "layer2"))
            .type("WMTS")
            .build();

    // When
    Set<ConstraintViolation<MapServiceDto>> violations = validator.validate(mapService);

    // Then
    assertTrue(violations.isEmpty(), "Valid MapServiceDto should have no violations");
  }

  @Test
  @DisplayName("MapServiceDto with null URL should fail validation")
  void nullUrl_shouldFailValidation() {
    // Given
    MapServiceDto mapService =
        MapServiceDto.builder().url(null).layers(List.of("layer1")).type("WMTS").build();

    // When
    Set<ConstraintViolation<MapServiceDto>> violations = validator.validate(mapService);

    // Then
    assertFalse(violations.isEmpty(), "Null URL should cause validation failure");
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("Service URL cannot be null")));
  }

  @Test
  @DisplayName("MapServiceDto with invalid URL format should fail validation")
  void invalidUrlFormat_shouldFailValidation() {
    // Given
    String[] invalidUrls = {
      "not-a-url", "ftp://example.com", // Only HTTP/HTTPS allowed
    };

    for (String invalidUrl : invalidUrls) {
      MapServiceDto mapService =
          MapServiceDto.builder().url(invalidUrl).layers(List.of("layer1")).type("WMTS").build();

      // When
      Set<ConstraintViolation<MapServiceDto>> violations = validator.validate(mapService);

      // Then
      assertFalse(
          violations.isEmpty(),
          "Invalid URL format '" + invalidUrl + "' should cause validation failure");
      assertTrue(
          violations.stream()
              .anyMatch(
                  v -> v.getMessage().contains("Service URL must be a valid HTTP/HTTPS URL")));
    }
  }

  @Test
  @DisplayName("MapServiceDto with valid URL formats should pass validation")
  void validUrlFormats_shouldPassValidation() {
    // Given
    String[] validUrls = {
      "http://example.com",
      "https://example.com",
      "http://example.com:8080",
      "https://example.com:8443",
      "http://example.com/path",
      "https://example.com/path/to/service",
      "http://subdomain.example.com",
      "https://subdomain.example.com",
      "http://api.example.com/v1/wmts",
      "https://api.example.com/v1/wmts",
      "http://localhost:8080/wmts",
      "https://localhost:8443/wmts",
      "http://wmts.example.com/wmts",
      "https://wmts.example.com/wmts",
    };

    for (String validUrl : validUrls) {
      MapServiceDto mapService =
          MapServiceDto.builder().url(validUrl).layers(List.of("layer1")).type("WMTS").build();

      // When
      Set<ConstraintViolation<MapServiceDto>> violations = validator.validate(mapService);

      // Then
      assertTrue(
          violations.isEmpty(), "Valid URL format '" + validUrl + "' should pass validation");
    }
  }

  @Test
  @DisplayName("MapServiceDto with null layers should fail validation")
  void nullLayers_shouldFailValidation() {
    // Given
    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://wmts.example.com/wmts")
            .layers(null)
            .type("WMTS")
            .build();

    // When
    Set<ConstraintViolation<MapServiceDto>> violations = validator.validate(mapService);

    // Then
    assertFalse(violations.isEmpty(), "Null layers should cause validation failure");
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("Layers list cannot be null")));
  }

  @Test
  @DisplayName("MapServiceDto with empty layers list should fail validation")
  void emptyLayers_shouldFailValidation() {
    // Given
    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://wmts.example.com/wmts")
            .layers(List.of())
            .type("WMTS")
            .build();

    // When
    Set<ConstraintViolation<MapServiceDto>> violations = validator.validate(mapService);

    // Then
    assertFalse(violations.isEmpty(), "Empty layers should cause validation failure");
    assertTrue(
        violations.stream()
            .anyMatch(v -> v.getMessage().contains("At least one layer must be specified")));
  }

  @Test
  @DisplayName("MapServiceDto with null type should fail validation")
  void nullType_shouldFailValidation() {
    // Given
    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://wmts.example.com/wmts")
            .layers(List.of("layer1"))
            .type(null)
            .build();

    // When
    Set<ConstraintViolation<MapServiceDto>> violations = validator.validate(mapService);

    // Then
    assertFalse(violations.isEmpty(), "Null type should cause validation failure");
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("Service type cannot be null")));
  }

  @Test
  @DisplayName("Debug URL validation with various formats")
  void debugUrlValidation() {
    // Debug test to see what URLs are actually accepted/rejected
    String[] testUrls = {
      "not-a-url",
      "ftp://example.com",
      "http://example.com",
      "https://example.com",
      "http://",
      "https://",
      "https://example",
      "https://example.",
      "https://example..com",
      "https://example-.com",
      "https://-example.com",
    };

    for (String testUrl : testUrls) {
      MapServiceDto mapService =
          MapServiceDto.builder().url(testUrl).layers(List.of("layer1")).type("WMTS").build();

      Set<ConstraintViolation<MapServiceDto>> violations = validator.validate(mapService);

      if (!violations.isEmpty()) {
        violations.forEach(v -> System.out.println("  Error: " + v.getMessage()));
      }
    }
  }
}
