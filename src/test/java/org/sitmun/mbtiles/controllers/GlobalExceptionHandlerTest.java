package org.sitmun.mbtiles.controllers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.mbtiles.dto.BoundingBoxDto;
import org.sitmun.mbtiles.dto.MapServiceDto;
import org.sitmun.mbtiles.dto.TileRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should handle validation errors without stack traces")
  void shouldHandleValidationErrorsWithoutStackTraces() throws Exception {
    // Given - Invalid request with null map services
    TileRequestDto invalidRequest =
        TileRequestDto.builder()
            .mapServices(null) // This will cause a validation error
            .bbox(
                BoundingBoxDto.builder()
                    .minX(-3.0)
                    .minY(40.0)
                    .maxX(-2.0)
                    .maxY(41.0)
                    .srs("EPSG:4326")
                    .build())
            .minZoom(10)
            .maxZoom(15)
            .build();

    // When & Then
    mockMvc
        .perform(
            post("/mbtiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:sitmun-mbtiles:problem:validation-error"))
        .andExpect(jsonPath("$.title").value("Validation Error"))
        .andExpect(jsonPath("$.detail").value("Request validation failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors[0]").value("mapServices: Map services list cannot be null"));
  }

  @Test
  @DisplayName("Should handle multiple validation errors")
  void shouldHandleMultipleValidationErrors() throws Exception {
    // Given - Invalid request with multiple validation issues
    TileRequestDto invalidRequest =
        TileRequestDto.builder()
            .mapServices(List.of()) // Empty list
            .bbox(
                BoundingBoxDto.builder()
                    .minX(10.0) // minX > maxX
                    .minY(20.0) // minY > maxY
                    .maxX(5.0)
                    .maxY(10.0)
                    .srs("invalid-srs") // Invalid SRS format
                    .build())
            .minZoom(25) // Invalid zoom level
            .maxZoom(10) // minZoom > maxZoom
            .build();

    // When & Then
    mockMvc
        .perform(
            post("/mbtiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Validation Error"))
        .andExpect(jsonPath("$.detail").value("Request validation failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors").value(hasSize(Matchers.greaterThan(1))));
  }

  @Test
  @DisplayName("Should handle invalid URL format in map service")
  void shouldHandleInvalidUrlFormat() throws Exception {
    // Given - Invalid request with invalid URL
    MapServiceDto invalidMapService =
        MapServiceDto.builder()
            .url("not-a-valid-url")
            .layers(List.of("layer1"))
            .type("WMTS")
            .build();

    TileRequestDto invalidRequest =
        TileRequestDto.builder()
            .mapServices(List.of(invalidMapService))
            .bbox(
                BoundingBoxDto.builder()
                    .minX(-3.0)
                    .minY(40.0)
                    .maxX(-2.0)
                    .maxY(41.0)
                    .srs("EPSG:4326")
                    .build())
            .minZoom(10)
            .maxZoom(15)
            .build();

    // When & Then
    mockMvc
        .perform(
            post("/mbtiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Validation Error"))
        .andExpect(jsonPath("$.detail").value("Request validation failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(
            jsonPath("$.errors[0]")
                .value("mapServices[0].url: Service URL must be a valid HTTP/HTTPS URL"));
  }

  @Test
  @DisplayName("Should handle null layers in map service")
  void shouldHandleNullLayers() throws Exception {
    // Given - Invalid request with null layers
    MapServiceDto invalidMapService =
        MapServiceDto.builder().url("https://example.com/wmts").layers(null).type("WMTS").build();

    TileRequestDto invalidRequest =
        TileRequestDto.builder()
            .mapServices(List.of(invalidMapService))
            .bbox(
                BoundingBoxDto.builder()
                    .minX(-3.0)
                    .minY(40.0)
                    .maxX(-2.0)
                    .maxY(41.0)
                    .srs("EPSG:4326")
                    .build())
            .minZoom(10)
            .maxZoom(15)
            .build();

    // When & Then
    mockMvc
        .perform(
            post("/mbtiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Validation Error"))
        .andExpect(jsonPath("$.detail").value("Request validation failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(
            jsonPath("$.errors")
                .value(
                    containsInAnyOrder(
                        "mapServices[0].layers: Layers list cannot be null",
                        "mapServices[0].layers: At least one layer must be specified")));
  }
}
