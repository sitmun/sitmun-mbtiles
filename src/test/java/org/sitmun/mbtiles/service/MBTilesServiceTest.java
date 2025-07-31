package org.sitmun.mbtiles.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.mbtiles.dto.*;
import org.sitmun.mbtiles.jobs.TaskContext;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
@DisplayName("MBTilesService tests")
class MBTilesServiceTest {

  @Mock private JobLauncher jobLauncher;

  @Mock private JobExplorer jobExplorer;

  @Mock private Job mbTilesJob;

  @Mock private MBTilesProgressService mbTilesProgressService;

  @Mock private List<Function<TaskContext, MBTilesEstimateDto>> mbTilesDecorators;

  private MBTilesService mbTilesService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    mbTilesService =
        new MBTilesService(
            jobLauncher, jobExplorer, mbTilesJob, mbTilesProgressService, mbTilesDecorators);
  }

  @Test
  @DisplayName("Should start job successfully")
  void startJobSuccessfully() throws Exception {
    // Given
    TileRequestDto tileRequest = createSampleTileRequest();
    JobExecution jobExecution = new JobExecution(1L);
    jobExecution.setStatus(BatchStatus.STARTED);

    when(jobLauncher.run(eq(mbTilesJob), any(JobParameters.class))).thenReturn(jobExecution);

    // When
    Long jobId = mbTilesService.startJob(tileRequest);

    // Then
    assertThat(jobId).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should handle invalid TileRequestDto")
  void handleInvalidTileRequest() throws Exception {
    // Given
    TileRequestDto tileRequest = new TileRequestDto(); // Empty request
    when(jobLauncher.run(eq(mbTilesJob), any(JobParameters.class)))
        .thenThrow(new IllegalArgumentException("Invalid TileRequestDto"));

    // When/Then
    assertThatThrownBy(() -> mbTilesService.startJob(tileRequest))
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
    MBTilesJobStatusDto response = mbTilesService.getJobStatus(jobId);
    assertThat(response.getStatus()).isEqualTo("STARTED");

    // Test RUNNING status
    when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTING);
    response = mbTilesService.getJobStatus(jobId);
    assertThat(response.getStatus()).isEqualTo("STARTING");
  }

  @Test
  @DisplayName("Should handle edge cases in size calculation")
  void handleEdgeCasesInSizeCalculation() {
    // Given
    TileRequestDto tileRequest = createSampleTileRequest();

    // Test with zero tiles
    Function<TaskContext, MBTilesEstimateDto> zeroDecorator =
        taskContext -> new MBTilesEstimateDto(0, 0.0, 0.0);

    // Test with reasonable large numbers to avoid overflow
    Function<TaskContext, MBTilesEstimateDto> largeDecorator =
        taskContext -> new MBTilesEstimateDto(1000000, 1024.0, 1024.0);

    // Test with the null result
    Function<TaskContext, MBTilesEstimateDto> nullDecorator = taskContext -> null;

    mbTilesService =
        new MBTilesService(
            jobLauncher,
            jobExplorer,
            mbTilesJob,
            mbTilesProgressService,
            List.of(zeroDecorator, largeDecorator, nullDecorator));

    // When
    MBTilesEstimateDto response = mbTilesService.estimateSize(tileRequest);

    // Then
    assertThat(response.getTileCount()).isEqualTo(1000000);
    assertThat(response.getEstimatedMbtilesSizeMb()).isEqualTo(1024.0);
    assertThat(response.getEstimatedTileSizeKb()).isEqualTo(1024.0);
  }

  @Test
  @DisplayName("Should get job status correctly")
  void getJobStatusCorrectly() {
    // Given
    long jobId = 123L;
    JobExecution jobExecution = mock(JobExecution.class);
    MBTilesProgressDto progressDto = new MBTilesProgressDto(1000L, 500L);

    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);
    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(mbTilesProgressService.getJobProgress(jobId)).thenReturn(progressDto);

    // When
    MBTilesJobStatusDto response = mbTilesService.getJobStatus(jobId);

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
    assertThatThrownBy(() -> mbTilesService.getJobStatus(jobId))
        .isInstanceOf(MBTilesFileNotFoundException.class);
  }

  @Test
  @DisplayName("Should handle failed job")
  void handleFailedJob() {
    // Given
    long jobId = 123L;
    JobExecution jobExecution = mock(JobExecution.class);
    MBTilesProgressDto progressDto = new MBTilesProgressDto(1000L, 500L);

    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);
    when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
    when(mbTilesProgressService.getJobProgress(jobId)).thenReturn(progressDto);

    // When
    MBTilesJobStatusDto response = mbTilesService.getJobStatus(jobId);

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
    Path tempFile = tempDir.resolve("test.mbtiles");
    Files.write(tempFile, "test data".getBytes());

    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(jobExecution.getJobParameters()).thenReturn(jobParameters);
    when(jobParameters.getString("outputPath")).thenReturn(tempFile.toString());
    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);

    // When
    ResourceDto response = mbTilesService.getMBTilesFile(jobId);

    // Then
    assertThat(response.getFileName()).isEqualTo(tempFile.getFileName().toString());
    assertThat(response.getFileBytes()).hasSize(9);
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
    when(jobParameters.getString("outputPath")).thenReturn("/non/existent/file.mbtiles");
    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);

    // When & Then
    assertThatThrownBy(() -> mbTilesService.getMBTilesFile(jobId))
        .isInstanceOf(MBTilesFileNotFoundException.class);
  }

  @Test
  @DisplayName("Should estimate size correctly")
  void estimateSizeCorrectly() {
    // Given
    TileRequestDto tileRequest = createSampleTileRequest();

    Function<TaskContext, MBTilesEstimateDto> decorator1 =
        taskContext -> new MBTilesEstimateDto(100, 10.0, 1.0);
    Function<TaskContext, MBTilesEstimateDto> decorator2 =
        taskContext -> new MBTilesEstimateDto(200, 20.0, 2.0);

    List<Function<TaskContext, MBTilesEstimateDto>> decorators = new ArrayList<>();
    decorators.add(decorator1);
    decorators.add(decorator2);

    mbTilesService =
        new MBTilesService(
            jobLauncher, jobExplorer, mbTilesJob, mbTilesProgressService, decorators);

    // When
    MBTilesEstimateDto response = mbTilesService.estimateSize(tileRequest);

    // Then
    assertThat(response.getTileCount()).isEqualTo(300); // 100 + 200
    assertThat(response.getEstimatedMbtilesSizeMb()).isEqualTo(3.0); // 1.0 + 2.0
    // Average tile size: (10.0 * 100 + 20.0 * 200) / 300 = 16.67
    assertThat(response.getEstimatedTileSizeKb()).isCloseTo(16.67, within(0.01));
  }

  @Test
  @DisplayName("Should handle clearJobProgress exception")
  void handleClearJobProgressException() {
    // Given
    long jobId = 123L;
    JobExecution jobExecution = mock(JobExecution.class);

    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);
    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    doThrow(new RuntimeException("Clear failed"))
        .when(mbTilesProgressService)
        .clearJobProgress(jobId);

    // When & Then
    assertThatThrownBy(() -> mbTilesService.getJobStatus(jobId))
        .isInstanceOf(MBTilesUnexpectedInternalException.class);
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
    MBTilesJobStatusDto response = mbTilesService.getJobStatus(jobId);

    // Then
    assertThat(response.getStatus()).isEqualTo("COMPLETED");
    assertThat(response.getProcessedTiles()).isNull();
    assertThat(response.getTotalTiles()).isNull();
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
    when(jobParameters.getString("outputPath")).thenReturn("invalid-path-no-slashes");
    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);

    // When & Then
    assertThatThrownBy(() -> mbTilesService.getMBTilesFile(jobId))
        .isInstanceOf(MBTilesFileNotFoundException.class);
  }

  @Test
  @DisplayName("Should handle empty map services in size estimation")
  void handleEmptyMapServicesInSizeEstimation() {
    // Given
    TileRequestDto tileRequest = createSampleTileRequest();
    tileRequest.setMapServices(Collections.emptyList());

    // When
    MBTilesEstimateDto response = mbTilesService.estimateSize(tileRequest);

    // Then
    assertThat(response.getTileCount()).isZero();
    assertThat(response.getEstimatedMbtilesSizeMb()).isZero();
    assertThat(response.getEstimatedTileSizeKb()).isZero();
  }

  @Test
  @DisplayName("Should handle decorator exception in size estimation")
  void handleDecoratorExceptionInSizeEstimation() {
    // Given
    TileRequestDto tileRequest = createSampleTileRequest();
    Function<TaskContext, MBTilesEstimateDto> decorator =
        taskContext -> {
          throw new RuntimeException("Decorator failed");
        };

    mbTilesService =
        new MBTilesService(
            jobLauncher, jobExplorer, mbTilesJob, mbTilesProgressService, List.of(decorator));

    // When & Then
    assertThatThrownBy(() -> mbTilesService.estimateSize(tileRequest))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("Should handle IOException in getMBTilesFile")
  void handleIOExceptionInGetMBTilesFile() throws IOException {
    // Given
    long jobId = 1L;
    JobExecution jobExecution = mock(JobExecution.class);
    JobParameters jobParameters = mock(JobParameters.class);

    // Create a temporary file that will be deleted before reading
    Path tempFile = tempDir.resolve("test.mbtiles");
    Files.write(tempFile, "test data".getBytes());
    Files.delete(tempFile);

    when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(jobExecution.getJobParameters()).thenReturn(jobParameters);
    when(jobParameters.getString("outputPath")).thenReturn(tempFile.toString());
    when(jobExplorer.getJobExecution(jobId)).thenReturn(jobExecution);

    // When & Then
    assertThatThrownBy(() -> mbTilesService.getMBTilesFile(jobId))
        .isInstanceOf(MBTilesFileNotFoundException.class);
  }

  private TileRequestDto createSampleTileRequest() {
    MapServiceDto mapService =
        MapServiceDto.builder()
            .url("https://example.com/wms")
            .layers(List.of("layer1", "layer2"))
            .type("WMS")
            .build();

    return TileRequestDto.builder()
        .minLat(0.0)
        .maxLat(1.0)
        .minLon(0.0)
        .maxLon(1.0)
        .minZoom(0)
        .maxZoom(1)
        .srs("EPSG:4326")
        .mapServices(List.of(mapService))
        .build();
  }
}
