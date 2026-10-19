package org.sitmun.mbtiles.dto;

import lombok.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@With
@JsonDeserialize(builder = MBTilesJobStatusDto.MBTilesJobStatusDtoBuilder.class)
public class MBTilesJobStatusDto {
  String status;
  long processedTiles;
  long totalTiles;
  String errorMessage;

  @JsonPOJOBuilder(withPrefix = "")
  public static class MBTilesJobStatusDtoBuilder {}
}
