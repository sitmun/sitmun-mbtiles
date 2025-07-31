package org.sitmun.mbtiles.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.sitmun.mbtiles.Constants;
import org.sitmun.mbtiles.dto.*;
import org.sitmun.mbtiles.jobs.TaskContext;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MBTilesService {

  private final JobLauncher batchJobLauncher;

  private final JobExplorer jobExplorer;

  private final Job mbTilesJob;

  private final MBTilesProgressService mbTilesProgressService;

  private final List<Function<TaskContext, MBTilesEstimateDto>> mbTilesDecorators;

  public MBTilesService(
      JobLauncher batchJobLauncher,
      JobExplorer jobExplorer,
      Job mbTilesJob,
      MBTilesProgressService mbTilesProgressService,
      List<Function<TaskContext, MBTilesEstimateDto>> mbTilesDecorators) {
    this.batchJobLauncher = batchJobLauncher;
    this.jobExplorer = jobExplorer;
    this.mbTilesJob = mbTilesJob;
    this.mbTilesProgressService = mbTilesProgressService;
    this.mbTilesDecorators = mbTilesDecorators;
  }

  public Long startJob(TileRequestDto tileRequest) {
    Map<String, JobParameter<?>> params = new HashMap<>();
    try {
      log.info(
          "Starting MBTiles job with JobLauncher: {}", batchJobLauncher.getClass().getSimpleName());
      log.info("JobLauncher TaskExecutor: {}", batchJobLauncher.getClass().getSimpleName());

      String uuid = UUID.randomUUID().toString();
      String outputPath = File.createTempFile(uuid, ".mbtiles").getAbsolutePath();

      params.put("outputPath", new JobParameter<>(outputPath, String.class));
      params.put(
          "tileRequest",
          new JobParameter<>(new ObjectMapper().writeValueAsString(tileRequest), String.class));
      params.put("timestamp", new JobParameter<>(System.currentTimeMillis(), Long.class));

    } catch (Exception e) {
      log.error("Error starting MBTiles job", e);
      throw new MBTilesUnexpectedRequestException();
    }
    try {
      log.info("About to launch job with parameters: {}", params);
      JobExecution jobExecution = batchJobLauncher.run(mbTilesJob, new JobParameters(params));
      log.info("Job launched with ID: {}", jobExecution.getId());
      return jobExecution.getId();
    } catch (Exception e) {
      log.error("Error starting MBTiles job", e);
      throw new MBTilesUnexpectedInternalException();
    }
  }

  public MBTilesJobStatusDto getJobStatus(long jobId) {
    JobExecution jobExecution = jobExplorer.getJobExecution(jobId);
    if (jobExecution == null) {
      throw new MBTilesFileNotFoundException();
    }

    BatchStatus batchStatus = jobExecution.getStatus();
    String status = batchStatus.toString();
    if (BatchStatus.FAILED.equals(batchStatus)) {
      status = "FAILED";
    }

    try {
      if (BatchStatus.COMPLETED.equals(batchStatus) || BatchStatus.FAILED.equals(batchStatus)) {
        mbTilesProgressService.clearJobProgress(jobId);
      }

      MBTilesProgressDto progressDto = mbTilesProgressService.getJobProgress(jobId);
      Long processedTiles = null;
      Long totalTiles = null;

      if (progressDto != null) {
        processedTiles = progressDto.getProcessedTiles();
        totalTiles = progressDto.getTotalTiles();
      }

      return new MBTilesJobStatusDto(status, processedTiles, totalTiles, null);
    } catch (Exception e) {
      throw new MBTilesUnexpectedInternalException();
    }
  }

  public ResourceDto getMBTilesFile(long jobId) {
    JobExecution jobExecution = jobExplorer.getJobExecution(jobId);

    if (jobExecution == null || !BatchStatus.COMPLETED.equals(jobExecution.getStatus())) {
      log.warn(
          "Job {} not found or not completed. Status: {}",
          jobId,
          jobExecution != null ? jobExecution.getStatus() : "null");
      throw new MBTilesFileNotFoundException();
    }

    String outputPath = jobExecution.getJobParameters().getString("outputPath");
    if (outputPath == null || outputPath.isEmpty()) {
      log.warn("Job {} completed but output path is null or empty", jobId);
      throw new MBTilesFileNotFoundException();
    }

    Path path = Path.of(outputPath);
    log.debug("Attempting to access MBTiles file at: {}", path);

    if (!Files.exists(path)) {
      log.warn("MBTiles file does not exist at path: {}", path);
      throw new MBTilesFileNotFoundException();
    }

    if (!Files.isReadable(path)) {
      log.error("MBTiles file exists but is not readable: {}", path);
      throw new MBTilesFileNotFoundException();
    }

    String[] pathParts = outputPath.split("/");
    String fileName = pathParts[pathParts.length - 1];

    byte[] fileBytes;
    try {
      fileBytes = Files.readAllBytes(path);
    } catch (IOException e) {
      log.error("Error reading MBTiles file at path: {}", path, e);
      throw new MBTilesFileNotFoundException();
    }
    return ResourceDto.builder().fileName(fileName).fileBytes(fileBytes).build();
  }

  public MBTilesEstimateDto estimateSize(TileRequestDto tileRequest) {
    MBTilesEstimateDto estimation = MBTilesEstimateDto.builder().build();
    for (MapServiceDto ms : tileRequest.getMapServices()) {
      TaskContext context = getContext(tileRequest, ms);
      try {
        for (Function<TaskContext, MBTilesEstimateDto> md : mbTilesDecorators) {
          MBTilesEstimateDto result = md.apply(context);
          if (result != null) {
            sumMBTilesEstimations(estimation, result);
          }
        }
      } catch (Exception e) {
        log.error("Error estimating MBTiles size", e);
        throw new MBTilesUnexpectedInternalException();
      }
    }
    return estimation;
  }

  @NotNull
  private static TaskContext getContext(TileRequestDto tileRequest, MapServiceDto ms) {
    TileServiceDto tileService =
        TileServiceDto.builder()
            .url(ms.getUrl())
            .layers(ms.getLayers())
            .type(ms.getType())
            .minLat(tileRequest.getMinLat())
            .minLon(tileRequest.getMinLon())
            .maxLat(tileRequest.getMaxLat())
            .maxLon(tileRequest.getMaxLon())
            .minZoom(tileRequest.getMinZoom())
            .maxZoom(tileRequest.getMaxZoom())
            .srs(tileRequest.getSrs())
            .matrixSet(Constants.MB_TILES_SRS)
            .build();
    return new TaskContext(tileService, null);
  }

  private void sumMBTilesEstimations(
      MBTilesEstimateDto total, @NotNull MBTilesEstimateDto newSize) {
    // Store old values before updating tile count
    int oldTotalTileCount = total.getTileCount();
    double oldEstimatedTileSizeKb = total.getEstimatedTileSizeKb();
    double oldTotalWeightedSize = oldEstimatedTileSizeKb * oldTotalTileCount;

    // Update tile count first
    int newTotalTileCount = oldTotalTileCount + newSize.getTileCount();
    total.setTileCount(newTotalTileCount);

    // Calculate weighted average tile size only if we have tiles
    if (newTotalTileCount > 0) {
      double totalWeightedSize =
          oldTotalWeightedSize + newSize.getEstimatedTileSizeKb() * newSize.getTileCount();
      double newEstimateTileSize = totalWeightedSize / newTotalTileCount;
      total.setEstimatedTileSizeKb(newEstimateTileSize);
    } else {
      // If no tiles, set tile size to 0
      total.setEstimatedTileSizeKb(0.0);
    }

    // Sum the MBTiles size (maintain original behavior)
    total.setEstimatedMbtilesSizeMb(
        total.getEstimatedMbtilesSizeMb() + newSize.getEstimatedMbtilesSizeMb());
  }
}
