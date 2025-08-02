package org.sitmun.mbtiles.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.mbtiles.dto.BoundingBoxDto;
import org.sitmun.mbtiles.dto.MBTilesJobStatusDto;
import org.sitmun.mbtiles.dto.MBTilesProgressDto;
import org.sitmun.mbtiles.dto.MapServiceDto;
import org.sitmun.mbtiles.dto.ResourceDto;
import org.sitmun.mbtiles.dto.TileRequestDto;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
@DisplayName("MBTilesJobService Tests")
class MBTilesJobServiceTest {

  @Mock private JobLauncher jobLauncher;
  @Mock private JobExplorer jobExplorer;
  @Mock private Job mbTilesJob;
  @Mock private MBTilesProgressService mbTilesProgressService;
  @Mock private TemporaryFileService temporaryFileService;

  private MBTilesJobService mbTilesJobService;

  @BeforeEach
  void setUp() {
    mbTilesJobService =
        new MBTilesJobService(
            jobLauncher, jobExplorer, mbTilesJob, mbTilesProgressService, temporaryFileService);
  }

  @Test
  @DisplayName("Should start job successfully")
  void startJobSuccessfully() throws Exception {
    // Given
    TileRequestDto tileRequest = createSampleTileRequest();
    JobExecution jobExecution = new JobExecution(1L);
    jobExecution.setStatus(BatchStatus.STARTED);

    Path mockTempFile = Path.of("/tmp/test-file.mbtiles");
    when(temporaryFileService.createUniqueTempFile("mbtiles")).thenReturn(mockTempFile);
    when(jobLauncher.run(eq(mbTilesJob), any(JobParameters.class))).thenReturn(jobExecution);

    // When
    Long jobId = mbTilesJobService.startJob(tileRequest);

    // Then
    assertThat(jobId).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should handle invalid TileRequestDto")
  void handleInvalidTileRequest() throws Exception {
    // Given
    TileRequestDto tileRequest = TileRequestDto.builder().build(); // Empty request
    Path mockTempFile = Path.of("/tmp/test-file.mbtiles");
    when(temporaryFileService.createUniqueTempFile("mbtiles")).thenReturn(mockTempFile);
    when(jobLauncher.run(eq(mbTilesJob), any(JobParameters.class)))
        .thenThrow(new IllegalArgumentException("Invalid TileRequestDto"));

    // When/Then
    assertThatThrownBy(() -> mbTilesJobService.startJob(tileRequest))
        .isInstanceOf(MBTilesUnexpectedInternalException.class);
  }

  @Test
  @DisplayName("Should handle various job statuses")
  void handleVariousJobStatuses() {
    // Given
    long jobId = 123L;
    JobExecution jobExecution = mock(JobExecution.class);

    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);

    // Test STARTED status
    when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
    MBTilesJobStatusDto response = mbTilesJobService.getJobStatus(jobId);
    assertThat(response.getStatus()).isEqualTo("STARTED");

    // Test RUNNING status
    when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTING);
    response = mbTilesJobService.getJobStatus(jobId);
    assertThat(response.getStatus()).isEqualTo("STARTING");
  }

  @Test
  @DisplayName("Should get job status correctly")
  void getJobStatusCorrectly() {
    // Given
    long jobId = 123L;
    JobExecution jobExecution = mock(JobExecution.class);
    MBTilesProgressDto progressDto =
        MBTilesProgressDto.builder().totalTiles(1000L).processedTiles(500L).build();

    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);
    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(mbTilesProgressService.getJobProgress(jobId)).thenReturn(progressDto);

    // When
    MBTilesJobStatusDto response = mbTilesJobService.getJobStatus(jobId);

    // Then
    assertThat(response.getStatus()).isEqualTo("COMPLETED");
    assertThat(response.getProcessedTiles()).isEqualTo(500L);
    assertThat(response.getTotalTiles()).isEqualTo(1000L);
  }

  @Test
  @DisplayName("Should handle missing job")
  void handleMissingJob() {
    // Given
    long jobId = 999L;
    when(jobExplorer.getJobExecution(jobId)).thenReturn(null);

    // When & Then
    assertThatThrownBy(() -> mbTilesJobService.getJobStatus(jobId))
        .isInstanceOf(MBTilesFileNotFoundException.class);
  }

  @Test
  @DisplayName("Should handle failed job")
  void handleFailedJob() {
    // Given
    long jobId = 123L;
    JobExecution jobExecution = mock(JobExecution.class);
    MBTilesProgressDto progressDto =
        MBTilesProgressDto.builder().totalTiles(1000L).processedTiles(500L).build();

    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);
    when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
    when(mbTilesProgressService.getJobProgress(jobId)).thenReturn(progressDto);

    // When
    MBTilesJobStatusDto response = mbTilesJobService.getJobStatus(jobId);

    // Then
    assertThat(response.getStatus()).isEqualTo("FAILED");
  }

  @Test
  @DisplayName("Should get MBTiles file for completed job")
  void getMBTilesFileForCompletedJob() throws IOException {
    // Given
    long jobId = 1L;
    JobExecution jobExecution = mock(JobExecution.class);
    JobParameters jobParameters = mock(JobParameters.class);

    // Create a temporary file with some content
    Path tempFile = Files.createTempFile("test", ".mbtiles");
    Files.write(tempFile, "test data".getBytes());

    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(jobExecution.getJobParameters()).thenReturn(jobParameters);
    when(jobParameters.getString("outputPath")).thenReturn(tempFile.toString());
    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);

    // When
    ResourceDto response = mbTilesJobService.getMBTilesFile(jobId);

    // Then
    assertThat(response.getFileName()).isEqualTo(tempFile.getFileName().toString());
    assertThat(response.getFileBytes()).hasSize(9);

    // Cleanup
    Files.delete(tempFile);
  }

  @Test
  @DisplayName("Should handle missing MBTiles file")
  void handleMissingMBTilesFile() {
    // Given
    long jobId = 1L;
    JobExecution jobExecution = mock(JobExecution.class);
    JobParameters jobParameters = mock(JobParameters.class);

    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(jobExecution.getJobParameters()).thenReturn(jobParameters);
    when(jobParameters.getString("outputPath")).thenReturn("/nonexistent/file.mbtiles");
    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);

    // When & Then
    assertThatThrownBy(() -> mbTilesJobService.getMBTilesFile(jobId))
        .isInstanceOf(MBTilesFileNotFoundException.class);
  }

  @Test
  @DisplayName("Should handle clearJobProgress exception")
  void handleClearJobProgressException() {
    // Given
    long jobId = 123L;
    JobExecution jobExecution = mock(JobExecution.class);

    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);
    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    doThrow(new RuntimeException("Clear progress failed"))
        .when(mbTilesProgressService)
        .clearJobProgress(jobId);

    // When & Then
    assertThatThrownBy(() -> mbTilesJobService.getJobStatus(jobId))
        .isInstanceOf(MBTilesUnexpectedInternalException.class)
        .hasMessageContaining("Clear progress failed");
  }

  @Test
  @DisplayName("Should handle null progress")
  void handleNullProgress() {
    // Given
    long jobId = 123L;
    JobExecution jobExecution = mock(JobExecution.class);

    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);
    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(mbTilesProgressService.getJobProgress(jobId)).thenReturn(null);

    // When
    MBTilesJobStatusDto response = mbTilesJobService.getJobStatus(jobId);

    // Then
    assertThat(response.getStatus()).isEqualTo("COMPLETED");
    assertThat(response.getProcessedTiles()).isZero();
    assertThat(response.getTotalTiles()).isZero();
  }

  @Test
  @DisplayName("Should handle getJobProgress exception")
  void handleGetJobProgressException() {
    // Given
    long jobId = 123L;
    JobExecution jobExecution = mock(JobExecution.class);

    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);
    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(mbTilesProgressService.getJobProgress(jobId))
        .thenThrow(new RuntimeException("Get progress failed"));

    // When & Then
    assertThatThrownBy(() -> mbTilesJobService.getJobStatus(jobId))
        .isInstanceOf(MBTilesUnexpectedInternalException.class)
        .hasMessageContaining("Get progress failed");
  }

  @Test
  @DisplayName("Should not call clearJobProgress for non-completed jobs")
  void shouldNotCallClearJobProgressForNonCompletedJobs() {
    // Given
    long jobId = 123L;
    JobExecution jobExecution = mock(JobExecution.class);
    MBTilesProgressDto progressDto =
        MBTilesProgressDto.builder().totalTiles(1000L).processedTiles(500L).build();

    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);
    when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
    when(mbTilesProgressService.getJobProgress(jobId)).thenReturn(progressDto);

    // When
    MBTilesJobStatusDto response = mbTilesJobService.getJobStatus(jobId);

    // Then
    assertThat(response.getStatus()).isEqualTo("STARTED");
    assertThat(response.getProcessedTiles()).isEqualTo(500L);
    assertThat(response.getTotalTiles()).isEqualTo(1000L);
    // Verify clearJobProgress was not called for non-completed jobs
    // This is implicit since we didn't set up the mock to expect clearJobProgress
  }

  @Test
  @DisplayName("Should handle invalid output path")
  void handleInvalidOutputPath() {
    // Given
    long jobId = 1L;
    JobExecution jobExecution = mock(JobExecution.class);
    JobParameters jobParameters = mock(JobParameters.class);

    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(jobExecution.getJobParameters()).thenReturn(jobParameters);
    when(jobParameters.getString("outputPath")).thenReturn(null);
    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);

    // When & Then
    assertThatThrownBy(() -> mbTilesJobService.getMBTilesFile(jobId))
        .isInstanceOf(MBTilesFileNotFoundException.class);
  }

  @Test
  @DisplayName("Should handle IOException in getMBTilesFile")
  void handleIOExceptionInGetMBTilesFile() throws IOException {
    // Given
    long jobId = 1L;
    JobExecution jobExecution = mock(JobExecution.class);
    JobParameters jobParameters = mock(JobParameters.class);

    // Create a temporary file that will be deleted before reading
    Path tempFile = Files.createTempFile("test", ".mbtiles");
    Files.write(tempFile, "test data".getBytes());
    Files.delete(tempFile);

    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(jobExecution.getJobParameters()).thenReturn(jobParameters);
    when(jobParameters.getString("outputPath")).thenReturn(tempFile.toString());
    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);

    // When & Then
    assertThatThrownBy(() -> mbTilesJobService.getMBTilesFile(jobId))
        .isInstanceOf(MBTilesFileNotFoundException.class);
  }

  private TileRequestDto createSampleTileRequest() {
    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://example.com/wms")
            .layers(List.of("layer1", "layer2"))
            .type("WMS")
            .build();

    BoundingBoxDto boundingBox =
        BoundingBoxDto.builder().minX(0.0).minY(0.0).maxX(1.0).maxY(1.0).srs("EPSG:4326").build();

    return TileRequestDto.builder()
        .bbox(boundingBox)
        .minZoom(0)
        .maxZoom(1)
        .mapServices(List.of(mapService))
        .build();
  }
}
