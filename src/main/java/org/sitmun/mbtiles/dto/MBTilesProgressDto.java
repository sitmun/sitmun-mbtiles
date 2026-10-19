package org.sitmun.mbtiles.dto;

import lombok.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@With
@JsonDeserialize(builder = MBTilesProgressDto.MBTilesProgressDtoBuilder.class)
public class MBTilesProgressDto {

  long totalTiles;
  long processedTiles;

  @JsonPOJOBuilder(withPrefix = "")
  public static class MBTilesProgressDtoBuilder {}
}
