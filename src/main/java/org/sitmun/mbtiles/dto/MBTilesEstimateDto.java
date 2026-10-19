package org.sitmun.mbtiles.dto;

import lombok.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@With
@JsonDeserialize(builder = MBTilesEstimateDto.MBTilesEstimateDtoBuilder.class)
public class MBTilesEstimateDto {
  int tileCount;
  double estimatedTileSizeKb;
  double estimatedMbtilesSizeMb;

  @JsonPOJOBuilder(withPrefix = "")
  public static class MBTilesEstimateDtoBuilder {}
}
