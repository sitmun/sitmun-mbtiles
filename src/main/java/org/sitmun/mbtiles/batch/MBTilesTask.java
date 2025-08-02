package org.sitmun.mbtiles.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.mbtiles.dto.MapServiceDto;
import org.sitmun.mbtiles.dto.TileRequestDto;
import org.sitmun.mbtiles.dto.TileServiceDto;
import org.sitmun.mbtiles.tilesources.wmts.WMTSConstants;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MBTilesTask {

  private final List<MBTilesTaskStrategy> mbtilesStrategies;
  private final ObjectMapper objectMapper;

  public MBTilesTask(List<MBTilesTaskStrategy> mbtilesStrategies, ObjectMapper objectMapper) {
    this.mbtilesStrategies = mbtilesStrategies;
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

    for (MapServiceDto mapService : mapServices) {
      if (mapService == null) {
        log.warn("Skipping null MapServiceDto");
        continue;
      }

      TileServiceDto tileService =
          TileServiceDto.builder()
              .url(mapService.getUrl())
              .layers(mapService.getLayers())
              .type(mapService.getType())
              .bbox(tileRequest.getBbox())
              .minZoom(tileRequest.getMinZoom())
              .maxZoom(tileRequest.getMaxZoom())
              .matrixSet(WMTSConstants.MB_TILES_SRS)
              .build();
      processTile(tileService, outputPath, stepContext);
    }
  }

  private void processTile(TileServiceDto service, String outputPath, StepContext stepContext)
      throws JobExecutionException {
    final MBTilesTaskContext taskContext = new MBTilesTaskContext(service, outputPath);
    try {
      mbtilesStrategies.stream()
          .filter(strategy -> strategy.accept(taskContext))
          .findFirst()
          .orElseThrow(MBTilesNoStrategyException::new)
          .process(stepContext, taskContext);
    } catch (Exception e) {
      throw new JobExecutionException(e.getMessage(), e);
    }
  }
}
