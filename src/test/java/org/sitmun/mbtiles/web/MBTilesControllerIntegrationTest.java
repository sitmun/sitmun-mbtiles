package org.sitmun.mbtiles.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.mbtiles.dto.MapServiceDto;
import org.sitmun.mbtiles.dto.TileRequestDto;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("MBTiles Controller Integration Tests")
@Slf4j
class MBTilesControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private JobExplorer jobExplorer;

  @Autowired private JobOperator jobOperator;

  @Test
  @DisplayName("Should start MBTiles job with valid request")
  void startMBTilesJobWithValidRequest() throws Exception {
    // Given
    TileRequestDto tileRequest = createTileRequest();

    // When - Start the job via HTTP request (this should now be asynchronous)
    String jobIdResponse =
        mockMvc
            .perform(
                post("/mbtiles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(tileRequest)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn()
            .getResponse()
            .getContentAsString();

    long jobId = Long.parseLong(jobIdResponse);

    // Then - Verify the job was created in Spring Batch
    final JobExecution jobExecution = jobExplorer.getJobExecution(jobId);
    assertNotNull(jobExecution, "Job should be created in Spring Batch");
    assertEquals(
        "mbtilesJob",
        jobExecution.getJobInstance().getJobName(),
        "Job name should be 'mbtilesJob'");

    stopExecution(jobId);
  }

  @Test
  @DisplayName("Should get job status with valid job ID")
  void getJobStatusWithValidJobId() throws Exception {
    // Given - First create a job
    TileRequestDto tileRequest = createTileRequest();

    // Create the job and get the job ID
    String jobIdResponse =
        mockMvc
            .perform(
                post("/mbtiles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(tileRequest)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long jobId = Long.parseLong(jobIdResponse);

    // When & Then - Now get the status of the created job
    mockMvc
        .perform(get("/mbtiles/{jobId}", jobId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

    stopExecution(jobId);
  }

  @Test
  @DisplayName("Should get MBTiles file with valid job ID")
  void getMBTilesFileWithValidJobId() throws Exception {
    // Given - First create a job
    TileRequestDto tileRequest = createTileRequest();

    // Create the job and get the job ID
    String jobIdResponse =
        mockMvc
            .perform(
                post("/mbtiles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(tileRequest)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn()
            .getResponse()
            .getContentAsString();

    long jobId = Long.parseLong(jobIdResponse);
    awaitCompleted(jobId);

    // Job completed, the file should be available
    mockMvc
        .perform(get("/mbtiles/{jobId}/file", jobId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE));
  }

  @Test
  @DisplayName("Should estimate MBTiles size with valid request")
  void estimateMBTilesSizeWithValidRequest() throws Exception {
    // Given
    TileRequestDto tileRequest = createTileRequest();

    // When & Then
    mockMvc
        .perform(
            post("/mbtiles/estimate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tileRequest)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tileCount").value(12))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
  }

  @Test
  @DisplayName("Should handle invalid job ID for status")
  void handleInvalidJobIdForStatus() throws Exception {
    // Given
    Long invalidJobId = 999999L;

    // When & Then
    mockMvc.perform(get("/mbtiles/{jobId}", invalidJobId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should handle invalid job ID for file download")
  void handleInvalidJobIdForFileDownload() throws Exception {
    // Given
    Long invalidJobId = 999999L;

    // When & Then
    mockMvc.perform(get("/mbtiles/{jobId}/file", invalidJobId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should handle invalid tile request")
  void handleInvalidTileRequest() throws Exception {
    // Given
    TileRequestDto invalidRequest = new TileRequestDto(); // Empty request

    // When & Then
    // The controller accepts the request and creates a job, but the job fails during execution
    // This is correct behavior - validation happens during job execution, not at request time
    String jobIdResponse =
        mockMvc
            .perform(
                post("/mbtiles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isOk()) // Controller accepts the request
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
            .andReturn()
            .getResponse()
            .getContentAsString();

    long jobId = Long.parseLong(jobIdResponse);
    awaitFailed(jobId);

    mockMvc
        .perform(get("/mbtiles/{jobId}", jobId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"));
  }

  @Test
  @DisplayName("Should demonstrate complete MBTiles workflow - create, status, download")
  void demonstrateCompleteMBTilesWorkflow() throws Exception {
    // Given - Create a job
    TileRequestDto tileRequest = createTileRequest();

    // Step 1: Create the job
    String jobIdResponse =
        mockMvc
            .perform(
                post("/mbtiles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(tileRequest)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
            .andReturn()
            .getResponse()
            .getContentAsString();

    long jobId = Long.parseLong(jobIdResponse);

    // Step 2: Check job status (should be available immediately after creation)
    awaitStarted(jobId);
    mockMvc
        .perform(get("/mbtiles/{jobId}", jobId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

    // Step 3: Wait for job completion
    awaitCompleted(jobId);

    mockMvc
        .perform(get("/mbtiles/{jobId}/file", jobId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE));
  }

  private TileRequestDto createTileRequest() {

    // Map services from Postman request
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
        .minZoom(2)
        .maxZoom(4)
        .srs("EPSG:4326")
        .build();
  }

  private void awaitCondition(long jobId, Function<BatchStatus, Boolean> condition) {
    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until(
            () -> {
              final JobExecution jobExecution = jobExplorer.getJobExecution(jobId);
              assertNotNull(jobExecution, "Job should be created in Spring Batch");
              return condition.apply(jobExecution.getStatus());
            });
  }

  private void awaitStarted(long jobId) {
    awaitCondition(jobId, status -> status == BatchStatus.STARTED);
  }

  private void awaitFailed(long jobId) {
    awaitCondition(jobId, status -> status == BatchStatus.FAILED);
  }

  private void awaitCompleted(long jobId) {
    awaitCondition(jobId, status -> status == BatchStatus.COMPLETED);
  }

  private void awaitStoppingStoppedCompletedFailed(long jobId) {
    awaitCondition(
        jobId,
        status ->
            status == BatchStatus.STOPPED
                || status == BatchStatus.STOPPING
                || status == BatchStatus.COMPLETED
                || status == BatchStatus.FAILED);
  }

  private void stopExecution(Long jobId) {
    try {
      log.info("Attempting to stop job {}", jobId);
      assertTrue(jobOperator.stop(jobId));
      awaitStoppingStoppedCompletedFailed(jobId);
    } catch (Exception e) {
      log.error("Error stopping job {}: {}", jobId, e.getMessage());
      fail("Failed to stop job " + jobId + ": " + e.getMessage());
    }
  }
}
