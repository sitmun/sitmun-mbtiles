package org.sitmun.mbtiles.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.mbtiles.batch.MBTilesTaskContext;
import org.sitmun.mbtiles.dto.BoundingBoxDto;
import org.sitmun.mbtiles.dto.MBTilesEstimateDto;
import org.sitmun.mbtiles.dto.MapServiceDto;
import org.sitmun.mbtiles.dto.TileRequestDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("MBTilesEstimateService Tests")
class MBTilesEstimateServiceTest {

  @Mock private List<MBTilesEstimateStrategy> mbTilesStrategies;

  private MBTilesEstimateService mbTilesEstimateService;

  @BeforeEach
  void setUp() {
    mbTilesEstimateService = new MBTilesEstimateService(mbTilesStrategies);
  }

  @Test
  @DisplayName("Should estimate size correctly")
  void estimateSizeCorrectly() {
    // Given
    TileRequestDto tileRequest = createSampleTileRequest();

    MBTilesEstimateStrategy strategy1 =
        createStrategy(
            taskContext ->
                MBTilesEstimateDto.builder()
                    .tileCount(100)
                    .estimatedTileSizeKb(10.0)
                    .estimatedMbtilesSizeMb(1.0)
                    .build());

    List<MBTilesEstimateStrategy> strategies = new ArrayList<>();
    strategies.add(strategy1);

    mbTilesEstimateService = new MBTilesEstimateService(strategies);

    // When
    MBTilesEstimateDto response = mbTilesEstimateService.estimateSize(tileRequest);

    // Then
    assertThat(response.getTileCount()).isEqualTo(100);
    assertThat(response.getEstimatedMbtilesSizeMb()).isEqualTo(1.0, within(0.01));
    assertThat(response.getEstimatedTileSizeKb()).isEqualTo(10.0, within(0.01));
  }

  @Test
  @DisplayName("Should handle empty map services in size estimation")
  void handleEmptyMapServicesInSizeEstimation() {
    // Given
    TileRequestDto tileRequest = createSampleTileRequest();
    tileRequest = tileRequest.withMapServices(Collections.emptyList()); // Empty map services

    // When
    MBTilesEstimateDto response = mbTilesEstimateService.estimateSize(tileRequest);

    // Then
    assertThat(response.getTileCount()).isZero();
    assertThat(response.getEstimatedMbtilesSizeMb()).isZero();
    assertThat(response.getEstimatedTileSizeKb()).isZero();
  }

  @Test
  @DisplayName("Should handle strategy exception in size estimation")
  void handleStrategyExceptionInSizeEstimation() {
    // Given
    TileRequestDto tileRequest = createSampleTileRequest();
    MBTilesEstimateStrategy strategy =
        createStrategy(
            taskContext -> {
              throw new RuntimeException("Strategy failed");
            });

    mbTilesEstimateService = new MBTilesEstimateService(List.of(strategy));

    // When & Then
    assertThatThrownBy(() -> mbTilesEstimateService.estimateSize(tileRequest))
        .isInstanceOf(RuntimeException.class);
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

  private static MBTilesEstimateStrategy createStrategy(
      Function<MBTilesTaskContext, MBTilesEstimateDto> contextFunction) {
    return new MBTilesEstimateStrategy() {

      @Override
      public boolean accept(MBTilesTaskContext context) {
        return true;
      }

      @Override
      public MBTilesEstimateDto estimate(MBTilesTaskContext context) {
        return contextFunction.apply(context);
      }
    };
  }
}
