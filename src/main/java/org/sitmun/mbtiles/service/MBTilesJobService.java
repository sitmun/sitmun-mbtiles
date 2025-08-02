package org.sitmun.mbtiles.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.mbtiles.dto.MBTilesJobStatusDto;
import org.sitmun.mbtiles.dto.MBTilesProgressDto;
import org.sitmun.mbtiles.dto.ResourceDto;
import org.sitmun.mbtiles.dto.TileRequestDto;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MBTilesJobService {

  private final JobLauncher batchJobLauncher;
  private final JobExplorer jobExplorer;
  private final Job mbTilesJob;
  private final MBTilesProgressService mbTilesProgressService;
  private final TemporaryFileService temporaryFileService;

  public MBTilesJobService(
      JobLauncher batchJobLauncher,
      JobExplorer jobExplorer,
      Job mbTilesJob,
      MBTilesProgressService mbTilesProgressService,
      TemporaryFileService temporaryFileService) {
    this.batchJobLauncher = batchJobLauncher;
    this.jobExplorer = jobExplorer;
    this.mbTilesJob = mbTilesJob;
    this.mbTilesProgressService = mbTilesProgressService;
    this.temporaryFileService = temporaryFileService;
  }

  public Long startJob(TileRequestDto tileRequest) {
    Map<String, JobParameter<?>> params = new HashMap<>();
    try {
      log.info(
          "Starting MBTiles job with JobLauncher: {}", batchJobLauncher.getClass().getSimpleName());
      log.info("JobLauncher TaskExecutor: {}", batchJobLauncher.getClass().getSimpleName());

      Path tempFile = temporaryFileService.createUniqueTempFile("mbtiles");
      String outputPath = tempFile.toAbsolutePath().toString();

      params.put("outputPath", new JobParameter<>(outputPath, String.class));
      params.put(
          "tileRequest",
          new JobParameter<>(new ObjectMapper().writeValueAsString(tileRequest), String.class));
      params.put("timestamp", new JobParameter<>(System.currentTimeMillis(), Long.class));

    } catch (Exception e) {
      log.error("Error starting MBTiles job", e);
      throw new MBTilesUnexpectedInternalException(e);
    }
    try {
      log.info("About to launch job with parameters: {}", params);
      JobExecution jobExecution = batchJobLauncher.run(mbTilesJob, new JobParameters(params));
      log.info("Job launched with ID: {}", jobExecution.getId());
      return jobExecution.getId();
    } catch (Exception e) {
      log.error("Error starting MBTiles job", e);
      throw new MBTilesUnexpectedInternalException(e);
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

      return MBTilesJobStatusDto.builder()
          .status(status)
          .processedTiles(processedTiles != null ? processedTiles : 0L)
          .totalTiles(totalTiles != null ? totalTiles : 0L)
          .build();
    } catch (Exception e) {
      throw new MBTilesUnexpectedInternalException(e);
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
}
