package org.sitmun.mbtiles.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BoundingBoxDto Validation Tests")
class BoundingBoxDtoValidationTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    @SuppressWarnings("resource")
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Valid BoundingBoxDto with all required fields should pass validation")
  void validBoundingBoxDto_shouldPassValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(-3.0)
            .minY(40.0)
            .maxX(-2.0)
            .maxY(41.0)
            .srs("EPSG:4326")
            .build();

    // When
    Set<ConstraintViolation<BoundingBoxDto>> violations = validator.validate(bbox);

    // Then
    assertTrue(violations.isEmpty(), "Valid BoundingBoxDto should have no violations");
  }

  @Test
  @DisplayName("BoundingBoxDto with null SRS should fail validation")
  void nullSrs_shouldFailValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder().minX(-3.0).minY(40.0).maxX(-2.0).maxY(41.0).srs(null).build();

    // When
    Set<ConstraintViolation<BoundingBoxDto>> violations = validator.validate(bbox);

    // Then
    assertFalse(violations.isEmpty(), "Null SRS should cause validation failure");
    assertTrue(
        violations.stream()
            .anyMatch(
                v -> v.getMessage().contains("SRS (Spatial Reference System) cannot be null")));
  }

  @Test
  @DisplayName("BoundingBoxDto with invalid SRS format should fail validation")
  void invalidSrsFormat_shouldFailValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(-3.0)
            .minY(40.0)
            .maxX(-2.0)
            .maxY(41.0)
            .srs("4326") // Invalid: missing EPSG: prefix
            .build();

    // When
    Set<ConstraintViolation<BoundingBoxDto>> violations = validator.validate(bbox);

    // Then
    assertFalse(violations.isEmpty(), "Invalid SRS format should cause validation failure");
    assertTrue(
        violations.stream().anyMatch(v -> v.getMessage().contains("SRS must be in EPSG format")));
  }

  @Test
  @DisplayName("BoundingBoxDto with valid EPSG formats should pass validation")
  void validEpsgFormats_shouldPassValidation() {
    // Given
    String[] validSrs = {"EPSG:4326", "EPSG:3857", "EPSG:25830", "EPSG:32632"};

    for (String srs : validSrs) {
      BoundingBoxDto bbox =
          BoundingBoxDto.builder().minX(-3.0).minY(40.0).maxX(-2.0).maxY(41.0).srs(srs).build();

      // When
      Set<ConstraintViolation<BoundingBoxDto>> violations = validator.validate(bbox);

      // Then
      assertTrue(violations.isEmpty(), "Valid EPSG format " + srs + " should pass validation");
    }
  }

  @Test
  @DisplayName("BoundingBoxDto with minX greater than maxX should fail validation")
  void minXGreaterThanMaxX_shouldFailValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(5.0) // Invalid: greater than maxX
            .minY(40.0)
            .maxX(3.0) // Invalid: less than minX
            .maxY(41.0)
            .srs("EPSG:4326")
            .build();

    // When
    Set<ConstraintViolation<BoundingBoxDto>> violations = validator.validate(bbox);

    // Then
    assertFalse(violations.isEmpty(), "minX > maxX should cause validation failure");
    assertTrue(
        violations.stream()
            .anyMatch(
                v ->
                    v.getMessage()
                        .contains(
                            "Minimum X coordinate must be less than or equal to maximum X coordinate")));
  }

  @Test
  @DisplayName("BoundingBoxDto with minY greater than maxY should fail validation")
  void minYGreaterThanMaxY_shouldFailValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(-3.0)
            .minY(45.0) // Invalid: greater than maxY
            .maxX(-2.0)
            .maxY(40.0) // Invalid: less than minY
            .srs("EPSG:4326")
            .build();

    // When
    Set<ConstraintViolation<BoundingBoxDto>> violations = validator.validate(bbox);

    // Then
    assertFalse(violations.isEmpty(), "minY > maxY should cause validation failure");
    assertTrue(
        violations.stream()
            .anyMatch(
                v ->
                    v.getMessage()
                        .contains(
                            "Minimum Y coordinate must be less than or equal to maximum Y coordinate")));
  }

  @Test
  @DisplayName("BoundingBoxDto with equal bounds should pass validation")
  void equalBounds_shouldPassValidation() {
    // Given
    BoundingBoxDto bbox =
        BoundingBoxDto.builder()
            .minX(-3.0) // Valid: equal to maxX
            .minY(40.0) // Valid: equal to maxY
            .maxX(-3.0) // Valid: equal to minX
            .maxY(40.0) // Valid: equal to minY
            .srs("EPSG:4326")
            .build();

    // When
    Set<ConstraintViolation<BoundingBoxDto>> violations = validator.validate(bbox);

    // Then
    assertTrue(violations.isEmpty(), "Equal bounds should pass validation");
  }
}
