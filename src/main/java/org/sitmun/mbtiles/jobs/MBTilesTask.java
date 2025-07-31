package org.sitmun.mbtiles.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.mbtiles.Constants;
import org.sitmun.mbtiles.dto.MapServiceDto;
import org.sitmun.mbtiles.dto.TileRequestDto;
import org.sitmun.mbtiles.dto.TileServiceDto;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MBTilesTask {

  private final List<BiConsumer<StepContext, TaskContext>> mbtilesDecorators;
  private final ObjectMapper objectMapper;

  public MBTilesTask(
      List<BiConsumer<StepContext, TaskContext>> mbtilesDecorators, ObjectMapper objectMapper) {
    this.mbtilesDecorators = mbtilesDecorators;
    this.objectMapper = objectMapper;
  }

  public void execute(StepContext stepContext) throws JobExecutionException {
    String currentThread = Thread.currentThread().getName();
    log.info("MBTilesTask executing on thread: {}", currentThread);

    Map<String, Object> jobParameters = stepContext.getJobParameters();
    String outputPath = (String) jobParameters.get("outputPath");
    TileRequestDto tileRequest;
    try {
      tileRequest =
          objectMapper.readValue((String) jobParameters.get("tileRequest"), TileRequestDto.class);
    } catch (Exception e) {
      log.error(
          "Error al mapear la petición JSON a TileRequestDto: {}",
          jobParameters.get("tileRequest"));
      log.error("Error al mapear la petición", e);
      throw new JobExecutionException("Error al mapear la petición: " + e.getMessage());
    }

    // Validate tileRequest
    if (tileRequest == null) {
      throw new JobExecutionException("TileRequestDto is null");
    }

    List<MapServiceDto> mapServices = tileRequest.getMapServices();
    if (mapServices == null || mapServices.isEmpty()) {
      throw new JobExecutionException("MapServices list is null or empty");
    }

    for (MapServiceDto ms : mapServices) {
      if (ms == null) {
        log.warn("Skipping null MapServiceDto");
        continue;
      }

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
      processTile(tileService, outputPath, stepContext);
    }
  }

  private void processTile(TileServiceDto service, String outputPath, StepContext stepContext)
      throws JobExecutionException {
    TaskContext taskContext = new TaskContext(service, outputPath);
    for (BiConsumer<StepContext, TaskContext> md : mbtilesDecorators) {
      try {
        md.accept(stepContext, taskContext);
      } catch (Exception e) {
        throw new JobExecutionException(e.getMessage(), e);
      }
    }
  }
}
