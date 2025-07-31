package org.sitmun.mbtiles.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.mbtiles.dto.MBTilesEstimateDto;
import org.sitmun.mbtiles.dto.MBTilesJobStatusDto;
import org.sitmun.mbtiles.dto.MapServiceDto;
import org.sitmun.mbtiles.dto.TileRequestDto;
import org.sitmun.mbtiles.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("MBTiles Controller Tests")
class MBTilesControllerTest {

  @MockitoBean private MBTilesService mbTilesService;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should start MBTiles job successfully")
  void startMBTilesJobSuccessfully() throws Exception {
    // Given - replicate the Postman request body
    TileRequestDto tileRequest = createTileRequest();
    Long expectedJobId = 123L;

    when(mbTilesService.startJob(any(TileRequestDto.class))).thenReturn(expectedJobId);

    // When & Then
    mockMvc
        .perform(
            post("/mbtiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tileRequest)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string(expectedJobId.toString()));
  }

  @Test
  @DisplayName("Should get job status successfully")
  void getJobStatusSuccessfully() throws Exception {
    // Given
    long jobId = 123L;
    MBTilesJobStatusDto statusDto = new MBTilesJobStatusDto("STARTED", 250L, 1000L, null);
    when(mbTilesService.getJobStatus(jobId)).thenReturn(statusDto);

    // When & Then
    mockMvc
        .perform(get("/mbtiles/{jobId}", jobId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(jsonPath("$.status").value("STARTED"))
        .andExpect(jsonPath("$.processedTiles").value(250))
        .andExpect(jsonPath("$.totalTiles").value(1000));
  }

  @Test
  @DisplayName("Should get MBTiles file successfully")
  void getMBTilesFileSuccessfully() throws Exception {
    // Given
    long jobId = 123L;
    byte[] fileContent = "mock mbtiles content".getBytes();
    ResourceDto expectedResponse =
        ResourceDto.builder().fileBytes(fileContent).fileName("test.mbtiles").build();

    when(mbTilesService.getMBTilesFile(jobId)).thenReturn(expectedResponse);

    // When & Then
    mockMvc
        .perform(get("/mbtiles/{jobId}/file", jobId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
        .andExpect(header().string("Content-Disposition", "attachment; filename=test.mbtiles"))
        .andExpect(header().longValue("Content-Length", fileContent.length));
  }

  @Test
  @DisplayName("Should estimate MBTiles size successfully")
  void estimateMBTilesSizeSuccessfully() throws Exception {
    // Given
    TileRequestDto tileRequest = createTileRequest();
    MBTilesEstimateDto estimateDto =
        MBTilesEstimateDto.builder()
            .tileCount(1000)
            .estimatedTileSizeKb(500.0)
            .estimatedMbtilesSizeMb(1500.0)
            .build();

    when(mbTilesService.estimateSize(any(TileRequestDto.class))).thenReturn(estimateDto);

    // When & Then
    mockMvc
        .perform(
            post("/mbtiles/estimate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tileRequest)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(jsonPath("$.tileCount").value(1000))
        .andExpect(jsonPath("$.estimatedTileSizeKb").value(500.0))
        .andExpect(jsonPath("$.estimatedMbtilesSizeMb").value(1500.0));
  }

  @Test
  @DisplayName("Should handle job not found")
  void handleJobNotFound() throws Exception {
    // Given
    long jobId = 999L;
    when(mbTilesService.getJobStatus(jobId)).thenThrow(new MBTilesFileNotFoundException());

    // When & Then
    mockMvc.perform(get("/mbtiles/{jobId}", jobId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should handle file not found")
  void handleFileNotFound() throws Exception {
    // Given
    long jobId = 999L;
    when(mbTilesService.getMBTilesFile(jobId)).thenThrow(new MBTilesFileNotFoundException());

    // When & Then
    mockMvc.perform(get("/mbtiles/{jobId}/file", jobId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should handle invalid tile request")
  void handleInvalidTileRequest() throws Exception {
    // Given
    TileRequestDto invalidRequest = new TileRequestDto(); // Empty request
    when(mbTilesService.startJob(any(TileRequestDto.class)))
        .thenThrow(new MBTilesUnexpectedRequestException());

    // When & Then
    mockMvc
        .perform(
            post("/mbtiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should handle job execution exception")
  void handleJobExecutionException() throws Exception {
    // Given
    TileRequestDto tileRequest = createTileRequest();
    when(mbTilesService.startJob(any(TileRequestDto.class)))
        .thenThrow(new MBTilesUnexpectedInternalException());

    // When & Then
    mockMvc
        .perform(
            post("/mbtiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tileRequest)))
        .andExpect(status().isInternalServerError());
  }

  @Test
  @DisplayName("Should handle file download exception")
  void handleFileDownloadException() throws Exception {
    // Given
    long jobId = 123L;
    when(mbTilesService.getMBTilesFile(jobId)).thenThrow(new MBTilesFileNotFoundException());

    // When & Then
    mockMvc.perform(get("/mbtiles/{jobId}/file", jobId)).andExpect(status().isNotFound());
  }

  private TileRequestDto createTileRequest() {
    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://ide.cime.es/geoserver2/gwc/service/wmts")
            .layers(List.of("base_referencia:base_referencia"))
            .type("WMTS")
            .build();

    return TileRequestDto.builder()
        .mapServices(List.of(mapService))
        .minLon(-33.03050653847904)
        .minLat(24.414264413170205)
        .maxLon(7.484046934682446)
        .maxLat(45.670757480670375)
        .minZoom(1)
        .maxZoom(8)
        .srs("EPSG:4326")
        .build();
  }
}
